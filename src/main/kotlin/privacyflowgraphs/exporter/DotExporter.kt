package de.felixkat.InproDer.privacyflowgraphs.exporter

import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowEdge
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowSpecialGraphType
import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowType
import de.felixkat.InproDer.privacyflowgraphs.model.toShape

fun DataFlowEdge.exportAsDotGraph(): String {
    var result = "digraph G {\n"
    result += this.exportAsDotGraphRecursive(true)
    result += "}"
    return result
}

private fun DataFlowEdge.exportAsDotGraphRecursive(startingProcess: Boolean): String {
    var type = this.evalSpecialGraphType()
    var shape = type.toShape()
    if(this.node.type == DataFlowType.SOURCE_FLOW) {
        shape = "triangle"
        if(this.calls.isEmpty()) {
            shape = "triangle, style=filled, fillcolor=grey"
        }
    } else if(this.node.type == DataFlowType.SINK_FLOW) {
        shape = "invtriangle"
    }
    if(startingProcess) { shape = "circle, style=filled, fillcolor=grey" }
    var result = "    ${this.node.method.name}[shape=${shape}];\n"
    this.calls.forEach {
        result += it.exportAsDotGraphRecursive(false)
    }
    this.calls.forEach {
        result += "    ${it.node.method.name} -> ${this.node.method.name};\n"
    }
    return result

}

private fun DataFlowEdge.evalSpecialGraphType(): DataFlowSpecialGraphType {
    var securitySubstrings = listOf("encrypt", "db", "send", "connect")
    var authSubstrings = listOf("auth")
    var initSubstrings = listOf("init")

    if (securitySubstrings.any { this.node.method.toString().contains(it) }) {
        return DataFlowSpecialGraphType.SECURITY_PROCESS
    } else if(authSubstrings.any { this.node.method.toString().contains(it) }) {
        return DataFlowSpecialGraphType.AUTH_PROCESS
    } else if(initSubstrings.any { this.node.method.toString().contains(it) }) {
        return DataFlowSpecialGraphType.INIT_PROCESS
    }
    return DataFlowSpecialGraphType.NONE
}