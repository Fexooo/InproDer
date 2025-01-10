package de.felixkat.InproDer.derivationtrees

import de.felixkat.InproDer.error.VariableNotFound
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.helper.findLValueWithCallback
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.common.stmt.JReturnStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootMethod
import sootup.core.types.ClassType
import sootup.core.views.View

/**
 * Generate Derivation Tree by using the variable name from a method
 */
fun generateDerivationTree(variableName: String, sootMethod: SootMethod, view: View): DerivationNode {
    fun findByString(lVal: LValue): Boolean { return lVal.toString() == variableName }
    val lVal: LValue = findLValueWithCallback(sootMethod, ::findByString)
        ?: throw VariableNotFound("Given variable could not be found in definitions of sootMethod.")
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo,
        null
    )
}

/**
 * Generate Derivation Tree by using own callback function for variable finding
 */
fun generateDerivationTree(variableCallback: (LValue) -> Boolean, sootMethod: SootMethod, view: View): DerivationNode {
    val lVal: LValue = findLValueWithCallback(sootMethod, variableCallback)
        ?: throw VariableNotFound("Given variable could not be found in definitions of sootMethod.")
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo,
        null
    )
}

private fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    view: View,
    stmtPositionInfo: StmtPositionInfo,
    returnInformation: ReturnInformation?
): DerivationNode {
    //println("$watchValue, ${method.signature}")
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf(), mutableListOf())
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
                            view,
                            stmt.positionInfo,
                            returnInformation
                        )
                    )
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