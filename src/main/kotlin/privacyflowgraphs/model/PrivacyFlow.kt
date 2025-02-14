package de.felixkat.InproDer.privacyflowgraphs.model

import de.felixkat.InproDer.derivationtrees.DerivationNode
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.Value
import sootup.core.signatures.MethodSignature
import java.util.*

data class LocalDataFlow(
    var startDataFlowPoint: List<Value>,
    var endDataFlowPoint: Optional<LValue>,
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
    var call: GlobalDataFlow?
) { }