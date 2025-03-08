package de.felixkat.InproDer.privacyflowgraphs.model

import de.felixkat.InproDer.derivationtrees.DerivationNode
import sootup.core.signatures.MethodSignature

data class LocalDataFlow(
    var method: MethodSignature,
    var type: DataFlowType,
    var derivationNode: List<DerivationNode>
) { }

enum class DataFlowType {
    SOURCE_FLOW,
    SINK_FLOW,
    PROCESS,
}

data class GlobalDataFlow (
    var node: LocalDataFlow,
    var call: MutableList<GlobalDataFlow>
) { }