package de.felixkat.InproDer.privacyflowgraphs.exporter

import de.felixkat.InproDer.privacyflowgraphs.model.DataFlowEdge

fun DataFlowEdge.exportAsDotGraph(): String {
    var result = "digraph G {\n"
    result += this.exportAsDotGraphRecursive()
    result += "}"
    return result
}

private fun DataFlowEdge.exportAsDotGraphRecursive(): String {
    var result = ""
    this.calls.forEach {
        result += it.exportAsDotGraphRecursive()
    }
    this.calls.forEach {
        result += "    ${it.node.method.name} -> ${this.node.method.name};\n"
    }
    return result

}