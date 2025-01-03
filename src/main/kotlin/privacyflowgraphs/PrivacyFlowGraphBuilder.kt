package de.felixkat.InproDer.privacyflowgraphs

import de.felixkat.InproDer.privacyflowgraphs.helper.findCOIs
import de.felixkat.InproDer.privacyflowgraphs.helper.getSourceMethods
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowEdge
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowType
import de.felixkat.InproDer.privacyflowgraphs.model.LocalDataFlow
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.Value
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import sootup.core.views.View
import java.util.*

fun generatePrivacyFlowGraph(
    view: View
): List<DataFlowEdge> {
    var result: MutableList<DataFlowEdge> = mutableListOf()
    var sourceMethods = getSourceMethods(view)
    var cois = findCOIs(view, sourceMethods)
    println("Source methods found: ${sourceMethods}")
    cois.forEach { c ->
        result.addAll(buildGlobalDataflowForClass(view, c))
    }
    var filteredList = result.filter { edge ->
        edge.calls.any { it.hasSourceFlow() }
    }
    return filteredList
}

fun buildGlobalDataflowForClass(
    view: View,
    sootClass: SootClass,
): List<DataFlowEdge> {
    var result = mutableListOf<DataFlowEdge>()
    sootClass.methods.forEach { m ->
        result.add(DataFlowEdge(parseLocalDataFlow(listOf(), Optional.empty(), m), buildGlobalDataflow(view, m)))
    }
    return result
}

fun buildGlobalDataflow(
    view: View,
    sootMethod: SootMethod
): List<DataFlowEdge> {
    var result = mutableListOf<DataFlowEdge>()
    sootMethod.body.stmtGraph.forEach { stmt ->
        if(stmt.containsInvokeExpr()) {
            var method = view.getMethod(stmt.invokeExpr.methodSignature)
            if(method.isPresent) {
                result.add(DataFlowEdge(parseLocalDataFlow(stmt.invokeExpr.uses.toList(), stmt.def, method.get()), buildGlobalDataflow(view, method.get())))
            } else { println("Method not in view") }
        }
    }
    return result.toList()
}

fun parseLocalDataFlow(
    startDataPoint: List<Value>,
    endDataPoint: Optional<LValue>,
    method: SootMethod
): LocalDataFlow {
    if(method.returnType.toString() == "void") {
        return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.PROCESS)
    }

    method.parameterTypes.forEach { pType ->
        println("Comparing ${pType} with ${method.returnType}")
        if(pType == method.returnType) {
            return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SOURCE_FLOW)
        }
    }

    return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SINK_FLOW)
}