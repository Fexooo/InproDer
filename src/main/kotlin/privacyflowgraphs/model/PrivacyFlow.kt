package de.felixkat.InproDer.privacyflowgraphs.model

import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.Value
import sootup.core.signatures.MethodSignature
import java.util.*

data class LocalDataFlow(
    var startDataFlowPoint: List<Value>,
    var endDataFlowPoint: Optional<LValue>,
    var method: MethodSignature,
    var type: DataFlowType
) { }

enum class DataFlowType {
    SOURCE_FLOW,
    SINK_FLOW,
    PROCESS,
}

data class DataFlowEdge (
    var node: LocalDataFlow,
    var calls: List<DataFlowEdge>
) {
    fun hasSourceFlow(): Boolean {
        if (this.node.type == DataFlowType.SOURCE_FLOW) {
            return true
        }
        return this.calls.any { it.hasSourceFlow() }
    }
}