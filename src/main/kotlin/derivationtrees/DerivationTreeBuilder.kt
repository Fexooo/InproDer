package de.felixkat.InproDer.derivationtrees

import de.felixkat.InproDer.error.VariableNotFound
import de.felixkat.InproDer.helper.findLValueFromParameter
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootMethod
import sootup.core.types.ClassType
import sootup.core.views.View

fun generateDerivationTree(variableName: String, sootMethod: SootMethod, view: View): DerivationNode {
    val lVal: LValue? = sootMethod.body.defs.find { comp -> comp.toString() == variableName }
    if (lVal == null) {
        throw VariableNotFound("Given variable could not be found in definitions of sootMethod.")
    }
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo
    )
}

fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    view: View,
    stmtPositionInfo: StmtPositionInfo
): DerivationNode {
    val node = DerivationNode("$watchValue", method.signature, stmtPositionInfo, mutableListOf())
    while (stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue && stmt.def != use) {
                val def = stmt.def
                if (stmt.containsInvokeExpr()) {
                    val classType: ClassType = stmt.invokeExpr.methodSignature.declClassType
                    val sootClass = view.getClass(classType)
                    if (sootClass.isPresent) {
                        val sootMethod: SootMethod =
                            sootClass.get().getMethod(stmt.invokeExpr.methodSignature.subSignature).get()
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
                                    stmt.positionInfo
                                )
                            )
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
                            stmt.positionInfo
                        )
                    )
                }
            }
        }
    }
    return node
}