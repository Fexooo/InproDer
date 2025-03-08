package de.felixkat.InproDer.privacyflowgraphs

import de.felixkat.InproDer.derivationtrees.DerivationNode
import de.felixkat.InproDer.derivationtrees.generateDerivationNode
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.privacyflowgraphs.helper.getSourceMethods
import de.felixkat.InproDer.privacyflowgraphs.model.GlobalDataFlow
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowType
import de.felixkat.InproDer.privacyflowgraphs.model.LocalDataFlow
import privacyflowgraphs.helper.findFlows
import privacyflowgraphs.helper.isPrivacyFlow
import privacyflowgraphs.helper.removeSubsets
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.Value
import sootup.core.model.SootClass
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
    println(sourceMethods)
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
            var flow = parseDataFlow(view, list, useDerivationTrees)
            if(flow != null) result.add(flow)
        }
    }
    var res = removeSubsets(result)
    res = res.filter { isPrivacyFlow(it) }
    return res
}

/*
 * Parse Data Flow from a list of methods to a GlobalDataFlow
 */
private fun parseDataFlow(view: View, methods: List<MethodSignature>, useDerivationTrees: Boolean): GlobalDataFlow? {
    var list = methods.toMutableList()
    if(list.isNotEmpty()) {
        var first = list.removeFirst()
        var method = view.getMethod(first)
        var nextFlow = parseDataFlow(view, list, useDerivationTrees)
        if(method.isPresent) {
            return GlobalDataFlow(
                parseLocalDataFlow(view, method.get(), useDerivationTrees),
                nextFlow?.let { mutableListOf(it) } ?: mutableListOf()
            )
        } else {
            return null
        }
    } else {
        return null
    }
}


/*
 * Parse Local Data Flow from a method
 */
private fun parseLocalDataFlow(
    view: View,
    method: SootMethod,
    useDerivationTrees: Boolean
): LocalDataFlow {
    var derivationNodes = mutableListOf<DerivationNode>()
    if(useDerivationTrees && method.isConcrete) {
        for (i in 0 until method.parameterCount) {
            var param = findLValueFromParameter(i, method.body.stmtGraph.stmts)
            if (param.isPresent) {
                derivationNodes.add(
                    generateDerivationNode(
                        view,
                        param.get(),
                        method.body.stmtGraph.stmts,
                        method,
                        method.body.stmtGraph.stmts.first().positionInfo
                    )
                )
            }
        }
    }

    if(method.returnType.toString() == "void" && method.parameterCount != 0) {
        return LocalDataFlow(method.signature, DataFlowType.SINK_FLOW, derivationNodes)
    }

    if(method.returnType.toString() != "void" && method.parameterCount == 0) {
        return LocalDataFlow(method.signature, DataFlowType.SOURCE_FLOW, derivationNodes)
    }

    return LocalDataFlow(method.signature, DataFlowType.PROCESS, derivationNodes)
}