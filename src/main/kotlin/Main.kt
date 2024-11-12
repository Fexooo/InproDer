package de.felixkat.InproDer

import de.felixkat.InproDer.error.VariableNotFound
import de.felixkat.InproDer.model.DerivationNode
import de.felixkat.InproDer.helper.findLValueFromParameter
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import kotlin.math.log

fun generateDerivationTree(variableName: String, sootMethod: SootMethod, sootClass: SootClass): DerivationNode {
    val lVal: LValue? = sootMethod.body.defs.find { comp -> comp.toString() == variableName }
    if(lVal == null) {
        throw VariableNotFound("Given variable could not be found in definitions of sootMethod.")
    }
    return generateDerivationNode(lVal, sootMethod.body.stmtGraph.getStmts(), sootMethod, sootClass)
}

fun generateDerivationNode(watchValue: LValue, stmts: MutableList<Stmt>, method: SootMethod, sootClass: SootClass): DerivationNode {
    var node = DerivationNode("${watchValue} (${method.name})", mutableListOf())
    while(stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if(use == watchValue) {
                var def = stmt.def
                if(stmt.containsInvokeExpr()) {
                    try {
                        val sootMethod: SootMethod =
                            sootClass.getMethod(stmt.invokeExpr.methodSignature.subSignature).get();
                        val graph = sootMethod.body.stmtGraph
                        val parameterIndex = stmt.invokeExpr.args.indexOfFirst { it == watchValue }
                        val newWatchValue = findLValueFromParameter(parameterIndex, graph.getStmts())
                        if(newWatchValue.isPresent)
                            node.addSuccessor(generateDerivationNode(newWatchValue.get(), graph.getStmts(), sootMethod, sootClass))
                    } catch (e: Exception) {
                        println("Error occured while getting submethod. Probably method is not in given class. Error: ${e}")
                    }
                }
                if(def.isPresent) {
                    var tempstmts = stmts.toMutableList()
                    node.addSuccessor(generateDerivationNode(def.get(), tempstmts, method, sootClass))
                }
            }
        }
    }
    return node
}