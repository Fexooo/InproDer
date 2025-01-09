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

data class GlobalDataFlow (
    var node: LocalDataFlow,
    var calls: List<GlobalDataFlow>
) {
    fun hasSourceFlow(): Boolean {
        if (this.node.type == DataFlowType.SOURCE_FLOW) {
            return true
        }
        return this.calls.any { it.hasSourceFlow() }
    }
}

enum class DataFlowSpecialGraphType {
    NONE,
    SECURITY_PROCESS,
    AUTH_PROCESS,
    INIT_PROCESS
}

fun DataFlowSpecialGraphType.toShape(): String {
    return when (this) {
        DataFlowSpecialGraphType.NONE -> "ellipse"
        DataFlowSpecialGraphType.SECURITY_PROCESS -> "octagon"
        DataFlowSpecialGraphType.AUTH_PROCESS -> "diamond"
        DataFlowSpecialGraphType.INIT_PROCESS -> "doublecircle"
    }
}