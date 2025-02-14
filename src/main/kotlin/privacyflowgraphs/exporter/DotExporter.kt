package de.felixkat.InproDer.privacyflowgraphs.exporter

import de.felixkat.InproDer.privacyflowgraphs.model.GlobalDataFlow
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowType

fun GlobalDataFlow.exportAsDotGraph(): String {
    var result = "digraph G {\n"
    result += this.exportAsDotGraphRecursive(true)
    result += "}"
    return result
}

private fun GlobalDataFlow.exportAsDotGraphRecursive(startingProcess: Boolean): String {
    var shape = "circle"
    if(this.node.type == DataFlowType.SOURCE_FLOW) {
        shape = "triangle"
        if(this.call == null) {
            shape = "triangle, style=filled, fillcolor=grey"
        }
    } else if(this.node.type == DataFlowType.SINK_FLOW) {
        shape = "invtriangle"
    }
    if(startingProcess) { shape = "circle, style=filled, fillcolor=grey" }
    var result = "    \"${this.node.method.hashCode()}${this.node.method.name}\"[shape=${shape}, label=\"${this.node.method.toString()}\"];\n"
    if(this.call != null) {
        this.call.forEach {
            result += it.exportAsDotGraphRecursive(false)
            result += "    \"${it.node.method.hashCode()}${it.node.method.name}\" -> \"${this.node.method.hashCode()}${this.node.method.name}\";\n"
        }

    }
    return result

}