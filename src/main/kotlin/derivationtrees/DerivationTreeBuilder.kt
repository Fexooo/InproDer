package de.felixkat.InproDer.derivationtrees

import de.felixkat.InproDer.error.VariableNotFoundException
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.helper.findLValueWithCallback
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.common.ref.JFieldRef
import sootup.core.jimple.common.stmt.JGotoStmt
import sootup.core.jimple.common.stmt.JIdentityStmt
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.JReturnStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import sootup.core.signatures.FieldSignature
import sootup.core.types.ClassType
import sootup.core.views.View

/**
 * Generate Derivation Tree by using the variable name from a method
 */
fun generateDerivationTree(variableName: String, sootMethod: SootMethod, sootClass: SootClass, view: View): DerivationNode {
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
        null
    )
}

/**
 * Generate Derivation Tree by using own callback function for variable finding
 */
fun generateDerivationTree(variableCallback: (LValue) -> Boolean, sootMethod: SootMethod, sootClass: SootClass, view: View): DerivationNode {
    val lVal: LValue = findLValueWithCallback(sootMethod, variableCallback)
        ?: throw VariableNotFoundException("Given variable could not be found in definitions of sootMethod.")
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        sootClass,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo,
        null
    )
}

private fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    oldSootClass: SootClass,
    view: View,
    stmtPositionInfo: StmtPositionInfo,
    returnInformation: ReturnInformation?,
    classField: Boolean = false
): DerivationNode {
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf(), mutableListOf(), classField)
    while (stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue) {
                val def = stmt.def
                if (stmt.containsInvokeExpr()) {
                    val classType: ClassType = stmt.invokeExpr.methodSignature.declClassType
                    val sootClass = view.getClass(classType)
                    if (sootClass.isPresent) {
                        val sMethod = sootClass.get().getMethod(stmt.invokeExpr.methodSignature.subSignature)
                        if(sMethod.isPresent) {
                            var sootMethod = sMethod.get()
                            if (!sootMethod.isAbstract && !sootMethod.isNative && !sootMethod.isBuiltInMethod) {
                                println("Calling ${sootMethod.signature}")
                                val graph = sootMethod.body.stmtGraph
                                val parameterIndex = stmt.invokeExpr.args.indexOfFirst { it == watchValue }
                                val newWatchValue = findLValueFromParameter(parameterIndex, graph.stmts)
                                if (newWatchValue.isPresent)
                                    node.addSuccessor(
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
                                            )
                                        )
                                    )
                            }
                        }
                    } else {
                        println("Invoked class is not in view! Invoked method signature: " + stmt.invokeExpr.methodSignature)
                    }
                }
                if (def.isPresent) {
                    val tempstmts = stmts.toMutableList()
                    node.addSuccessor(
                        generateDerivationNode(
                            def.get(),
                            tempstmts,
                            method,
                            oldSootClass,
                            view,
                            stmt.positionInfo,
                            returnInformation
                        )
                    )
                    if(def.get() is JFieldRef) {
                        node.addSuccessors(
                            generateDerivationNodeFromField(
                                def.get(),
                                (def.get() as JFieldRef).fieldSignature,
                                method,
                                oldSootClass,
                                view,
                                stmt.positionInfo
                            )
                        )
                    }
                }
                if(stmt is JReturnStmt && returnInformation != null) {
                    node.addReturnInformation(
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

fun generateDerivationNodeFromField(
    watchValue: LValue,
    fieldSignature: FieldSignature,
    method: SootMethod,
    sootClass: SootClass,
    view: View,
    stmtPositionInfo: StmtPositionInfo,
): List<DerivationNode> {
    val succs: MutableList<DerivationNode> = mutableListOf()
    sootClass.methods.forEach { m ->
        if(method != m) {
            m.body.uses.forEach { use ->
                if (use is JFieldRef && use.fieldSignature == fieldSignature) {
                    succs.add(
                        generateDerivationNode(
                            use,
                            m.body.stmtGraph.stmts,
                            m,
                            sootClass,
                            view,
                            m.body.stmtGraph.stmts[0].positionInfo,
                            null,
                            true
                        )
                    )
                }
            }
        }
    }
    return succs
}

fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    stmtPositionInfo: StmtPositionInfo
): DerivationNode {
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf(), mutableListOf(), false)
    while (stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue) {
                val def = stmt.def
                if (def.isPresent) {
                    val tempstmts = stmts.toMutableList()
                    node.addSuccessor(
                        generateDerivationNode(
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