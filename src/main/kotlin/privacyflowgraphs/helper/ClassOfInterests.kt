package de.felixkat.InproDer.privacyflowgraphs.helper

import sootup.core.model.SootClass
import sootup.core.signatures.MethodSignature
import sootup.core.views.View

fun findCOIs(
    view: View,
    sourceMethods: List<MethodSignature>
): List<SootClass> {
    var result = mutableListOf<SootClass>()
    view.classes.forEach { c ->
        c.methods.forEach { m ->
            m.body.stmtGraph.forEach { stmt ->
                if(stmt.containsInvokeExpr()) {
                    if(sourceMethods.contains(stmt.invokeExpr.methodSignature)) {
                        result.add(c)
                    }
                }
            }
        }
    }
    return result
}