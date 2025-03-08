package de.felixkat.InproDer.derivationtrees

import de.felixkat.InproDer.error.VariableNotFoundException
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.helper.findLValueWithCallback
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.ref.JFieldRef
import sootup.core.jimple.common.stmt.JGotoStmt
import sootup.core.jimple.common.stmt.JIdentityStmt
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.JReturnStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import sootup.core.signatures.FieldSignature
import sootup.core.signatures.MethodSignature
import sootup.core.types.ClassType
import sootup.core.views.View

/**
 * Generate Derivation Tree by using the variable name from a method
 */
fun generateDerivationTree(variableName: String, sootMethod: SootMethod, sootClass: SootClass, view: View, visitClassVars: Boolean = true): DerivationNode {
    fun findByString(lVal: LValue): Boolean { return lVal.toString() == variableName }
    val lVal: LValue = findLValueWithCallback(sootMethod, ::findByString)
        ?: throw VariableNotFoundException("Given variable could not be found in definitions of sootMethod.")
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        sootClass,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo,
        null,
        visitClassVars
    )
}

/**
 * Generate Derivation Tree by using own callback function for variable finding
 */
fun generateDerivationTree(variableCallback: (LValue) -> Boolean, sootMethod: SootMethod, sootClass: SootClass, view: View, visitClassVars: Boolean = true): DerivationNode {
    val lVal: LValue = findLValueWithCallback(sootMethod, variableCallback)
        ?: throw VariableNotFoundException("Given variable could not be found in definitions of sootMethod.")
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        sootClass,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo,
        null,
        visitClassVars
    )
}

fun generateDerivationTree(variable: LValue, sootMethod: SootMethod, sootClass: SootClass, view: View, visitClassVars: Boolean = true): DerivationNode
    = generateDerivationNode(variable, sootMethod.body.stmtGraph.stmts, sootMethod, sootClass, view, sootMethod.body.stmtGraph.stmts[0].positionInfo, null, visitClassVars)

/*
 * Recursive algorithm to generate derivation nodes
 */
private fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    oldSootClass: SootClass,
    view: View,
    stmtPositionInfo: StmtPositionInfo,
    returnInformation: ReturnInformation? = null,
    visitClassVars: Boolean = true,
    classField: Boolean = false,
    visitedMethodVars: MutableList<Pair<MethodSignature, LValue>> = mutableListOf(),
    visitedClassFields: MutableList<FieldSignature> = mutableListOf()
): DerivationNode {
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf(), mutableListOf(), classField)
    while (stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue) { // Stmt uses the watchValue
                val def = stmt.def
                if (stmt.containsInvokeExpr()) { // Stmt contains an invoke expression
                    val classType: ClassType = stmt.invokeExpr.methodSignature.declClassType
                    val sootClass = view.getClass(classType)
                    if (sootClass.isPresent) { // Class of method is available in view
                        val sMethod = sootClass.get().getMethod(stmt.invokeExpr.methodSignature.subSignature) // Retrieve method from class
                        if(sMethod.isPresent) {
                            var sootMethod = sMethod.get()
                            if (!sootMethod.isAbstract && !sootMethod.isNative && !sootMethod.isBuiltInMethod) { // Method is valid to retrieve stmtGraph
                                val graph = sootMethod.body.stmtGraph
                                val parameterIndex = stmt.invokeExpr.args.indexOfFirst { it == watchValue }
                                val newWatchValue = findLValueFromParameter(parameterIndex, graph.stmts)
                                if (newWatchValue.isPresent && !visitedMethodVars.contains(Pair(sootMethod.signature, newWatchValue.get()))) {
                                    visitedMethodVars.add(Pair(sootMethod.signature, newWatchValue.get())) // Infinite recursion prevention
                                    node.addSuccessor( // Add new successor to node with new watchValue and method (recursive)
                                        generateDerivationNode(
                                            newWatchValue.get(),
                                            graph.stmts,
                                            sootMethod,
                                            sootClass.get(),
                                            view,
                                            stmt.positionInfo,
                                            ReturnInformation(
                                                "$watchValue",
                                                method.signature,
                                                stmt.positionInfo
                                            ),
                                            visitClassVars,
                                            false,
                                            visitedMethodVars,
                                            visitedClassFields
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        //println("Invoked class is not in view! Invoked method signature: " + stmt.invokeExpr.methodSignature)
                    }
                }
                if (def.isPresent) { // Stmt saves presumably changed watchValue to a new variable
                    val tempstmts = stmts.toMutableList() // Clone remaining stmts
                    if(!visitedMethodVars.contains(Pair(method.signature, def.get()))) {
                        visitedMethodVars.add(Pair(method.signature, def.get())) // Infinite recursion prevention
                        node.addSuccessor( // Add new successor to node with new watchValue and remaining stmts (recursive)
                            generateDerivationNode(
                                def.get(),
                                tempstmts,
                                method,
                                oldSootClass,
                                view,
                                stmt.positionInfo,
                                returnInformation,
                                visitClassVars,
                                false,
                                visitedMethodVars,
                                visitedClassFields
                            )
                        )
                    }
                    if(def.get() is JFieldRef && visitClassVars) { // Stmt saves presumably changed watchValue to a class field
                        node.addSuccessors( // Add new successors to node that are using the class field
                            generateDerivationNodeFromField(
                                (def.get() as JFieldRef).fieldSignature,
                                method,
                                oldSootClass,
                                view,
                                visitedMethodVars,
                                visitedClassFields
                            )
                        )
                    }
                }
                if(stmt is JReturnStmt && returnInformation != null) { // Stmt returns the watchValue
                    node.addReturnInformation( // Add return information to node
                        ReturnInformation(
                            returnInformation.toVariableName,
                            returnInformation.toMethodSignature,
                            stmt.positionInfo
                        )
                    )
                }
            }
        }
    }
    return node
}

/*
 * Internal function to generate derivation nodes from a selected class field,
 * by iterating through every possible function that may contain the class field.
 */
private fun generateDerivationNodeFromField(
    fieldSignature: FieldSignature,
    method: SootMethod,
    sootClass: SootClass,
    view: View,
    visitedMethodVars: MutableList<Pair<MethodSignature, LValue>>,
    visitedClassFields: MutableList<FieldSignature>
): List<DerivationNode> {
    if(visitedClassFields.contains(fieldSignature)) { // Infinite recursion prevention check
        return mutableListOf()
    }
    visitedClassFields.add(fieldSignature) // Infinite recursion prevention
    val succs: MutableList<DerivationNode> = mutableListOf()
    sootClass.methods.forEach { m -> // Iterate through every method to check for class field
        if(method != m && m.isConcrete && !m.isBuiltInMethod) { // Check if method is valid to retrieve stmtGraph
            m.body.uses.forEach { use ->
                if (use is JFieldRef && use.fieldSignature == fieldSignature) { // Check if method uses the class field
                    succs.add( // Add new successor to node with class field as watchvalue and the method (recursive)
                        generateDerivationNode(
                            use,
                            m.body.stmtGraph.stmts,
                            m,
                            sootClass,
                            view,
                            m.body.stmtGraph.stmts[0].positionInfo,
                            null,
                            true,
                            true,
                            visitedMethodVars,
                            visitedClassFields
                        )
                    )
                }
            }
        }
    }
    return succs
}

/*
 * Generate DerivationNode without considering further changes in other methods
 */
fun generateDerivationNode(
    view: View,
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    stmtPositionInfo: StmtPositionInfo,
): DerivationNode {
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf(), mutableListOf(), false)
    while (stmts.isNotEmpty()) { // Iterate through every stmt
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue) { // Stmt uses watchValue
                if(stmt.containsInvokeExpr()) { // Stmt contains an invoke expression (function call)
                    stmt.invokeExpr.args.forEach { immediate ->
                        if(immediate == watchValue) { // Stmt uses watchValue as argument
                            node.addSuccessor( // Add new successor to node with watchValue and the method (no recursion)
                                DerivationNode(
                                    "$immediate",
                                    methodSignature = stmt.invokeExpr.methodSignature,
                                    positionInfo = stmt.positionInfo,
                                    successors = mutableListOf(),
                                    returnInformation = mutableListOf(),
                                    classField = false,
                                )
                            )
                        }
                    }
                }
                val def = stmt.def
                if (def.isPresent) { // Stmt saves presumably changed watchValue to a new variable
                    val definition = def.get()
                    if(definition is JFieldRef) { // Stmt saves presumably changed watchValue to a class field
                        try {
                            val sootClass = view.getClass(method.declaringClassType).get() // Retrieve class from method
                            sootClass.methods.forEach { m -> // Iterate through every method to check for class field
                                if (method != m && m.isConcrete && !m.isBuiltInMethod) { // Check if method is valid to retrieve stmtGraph
                                    m.body.uses.forEach { use ->
                                        if (use is JFieldRef && use.fieldSignature == definition.fieldSignature) { // Check if method is using class field
                                            node.addSuccessor( // Add new successor to node with class field as watchValue and the method (no recursion)
                                                DerivationNode(
                                                    "$definition",
                                                    methodSignature = m.signature,
                                                    positionInfo = stmt.positionInfo,
                                                    successors = mutableListOf(),
                                                    returnInformation = mutableListOf(),
                                                    classField = true,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } catch(e: Exception) {
                            println(e)
                        }
                    }
                    val tempstmts = stmts.toMutableList()
                    node.addSuccessor( // Add new successor to node with new watchValue and remaining stmts (recursion)
                        generateDerivationNode(
                            view,
                            def.get(),
                            tempstmts,
                            method,
                            stmt.positionInfo,
                        )
                    )
                }
            }
        }
    }
    return node
}