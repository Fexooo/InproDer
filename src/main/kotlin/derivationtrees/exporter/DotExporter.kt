package de.felixkat.InproDer.derivationtrees.exporter

import de.felixkat.InproDer.derivationtrees.DerivationNode

fun DerivationNode.exportAsDotGraph(): String {
    var result = "digraph G {"
    result += this.exportAsDotGraphRecursive()
    result += "}"
    return result
}

private fun DerivationNode.exportAsDotGraphRecursive(): String {
    var result = ""
    this.successors.forEach {
        if(this.variableName != it.variableName) {
            result += "    \"${this.variableName}\" -> \"${it.variableName}\";\n"
        }
        result += it.exportAsDotGraphRecursive()
    }
    return result
}