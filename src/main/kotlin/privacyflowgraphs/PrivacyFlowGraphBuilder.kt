package de.felixkat.InproDer.privacyflowgraphs

import de.felixkat.InproDer.derivationtrees.DerivationNode
import de.felixkat.InproDer.derivationtrees.generateDerivationNode
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.privacyflowgraphs.helper.getSourceMethods
import de.felixkat.InproDer.privacyflowgraphs.model.GlobalDataFlow
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowType
import de.felixkat.InproDer.privacyflowgraphs.model.LocalDataFlow
import privacyflowgraphs.helper.findFlows
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.Value
import sootup.core.model.SootMethod
import sootup.core.signatures.MethodSignature
import sootup.core.views.View
import java.util.*

/**
 * Generate Privacy Flow Graphs by using a method finding callback
 */
fun generatePrivacyFlowGraph(
    view: View,
    sourceMethodCallback: (SootMethod) -> Boolean,
    useDerivationTrees: Boolean
): List<GlobalDataFlow> {
    var sourceMethods = getSourceMethods(view, sourceMethodCallback)
    return generatePrivacyFlowGraph(view, sourceMethods, useDerivationTrees)
}

/**
 * Generate Privacy Flow Graphs by using a predefined method list
 */
fun generatePrivacyFlowGraph(
    view: View,
    methods: List<MethodSignature>,
    useDerivationTrees: Boolean
): List<GlobalDataFlow> {
    var result: MutableList<GlobalDataFlow> = mutableListOf()
    var globalFlows: Map<MethodSignature, List<List<MethodSignature>>> = findFlows(view, methods)
    globalFlows.keys.forEach {
        globalFlows[it]!!.forEach { list ->
            var flow = parseGlobalDataFlow(view, list, useDerivationTrees)
            if(flow != null) result.add(flow)
        }
    }
    return result
}

private fun parseGlobalDataFlow(view: View, methods: List<MethodSignature>, useDerivationTrees: Boolean): GlobalDataFlow? {
    var list = methods.toMutableList()
    if(list.isNotEmpty()) {
        var first = list.removeFirst()
        var method = view.getMethod(first)
        var nextFlow = parseGlobalDataFlow(view, list, useDerivationTrees)
        if(method.isPresent) {
            return GlobalDataFlow(
                parseLocalDataFlow(listOf(), Optional.empty(), method.get(), useDerivationTrees),
                nextFlow
            )
        } else {
            return null
        }
    } else {
        return null
    }
}

private fun parseLocalDataFlow(
    startDataPoint: List<Value>,
    endDataPoint: Optional<LValue>,
    method: SootMethod,
    useDerivationTrees: Boolean
): LocalDataFlow {
    var derivationNodes = mutableListOf<DerivationNode>()
    if(useDerivationTrees) {
        for (i in 0 until method.parameterCount) {
            var param = findLValueFromParameter(i, method.body.stmtGraph.stmts)
            if (param.isPresent) {
                derivationNodes.add(
                    generateDerivationNode(
                        param.get(),
                        method.body.stmtGraph.stmts,
                        method,
                        method.body.stmtGraph.stmts.first().positionInfo
                    )
                )
            }
        }
    }

    if(method.returnType.toString() == "void") {
        return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.PROCESS, derivationNodes)
    }

    method.parameterTypes.forEach { pType ->
        if(pType == method.returnType) {
            return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SOURCE_FLOW, derivationNodes)
        }
    }

    return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SINK_FLOW, derivationNodes)
}