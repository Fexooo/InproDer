package de.felixkat.InproDer.derivationtrees.exporter

import de.felixkat.InproDer.derivationtrees.DerivationNode
import sootup.core.signatures.MethodSignature

fun DerivationNode.exportAsDotGraph(): String {
    var methodMap = this.exportMethodMap(mutableMapOf())
    var result = "digraph G {\n"
    methodMap.keys.forEach { key ->
        result += "    subgraph \"cluster${key.hashCode()}\" {\n"
        result += "        label=\"${key}\"\n"
        methodMap.get(key)!!.forEach { variableName ->
            var style = "[label=\"${variableName}\"]"
            if(key == this.methodSignature && variableName == this.variableName) {
                style = "[label=\"${variableName}\", style=filled, color=lightgreen]"
            }
            result += "        \"n${key.hashCode()}${variableName}\"$style\n"
        }
        result += "    }\n\n"
    }
    result += this.exportAsDotGraphRecursive()
    result += "}"
    return result
}

private fun DerivationNode.exportAsDotGraphRecursive(): String {
    var result = ""
    this.successors.forEach {
        var style = "[label=\"${this.methodSignature.name}\\n${this.positionInfo.stmtPosition}\""
        if(this.methodSignature != it.methodSignature) {
            style += ",color=orange"
        }
        style += "]"
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"n${it.methodSignature.hashCode()}${it.variableName}\"${style};\n"
        if(this.methodSignature != it.methodSignature) {
            result += it.exportAsDotGraphRecursive("${this.methodSignature.hashCode()}${this.variableName}")
        } else {
            result += it.exportAsDotGraphRecursive()
        }
    }
    return result
}

private fun DerivationNode.exportAsDotGraphRecursive(returnTo: String): String {
    var result = ""
    this.successors.forEach {
        var style = "[label=\"${this.methodSignature.name}\\n${this.positionInfo.stmtPosition}\""
        if(this.methodSignature != it.methodSignature) {
            style += ",color=orange"
        }
        style += "]"
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"n${it.methodSignature.hashCode()}${it.variableName}\"${style};\n"
        result += it.exportAsDotGraphRecursive(returnTo)
    }
    this.returnInformation.forEach {
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"n${it.toMethodSignature.hashCode()}${it.toVariableName}\"[label=\"Returns at\\n${it.stmtPositionInfo.stmtPosition}\", color=blue];\n"
    }
    return result
}

private fun DerivationNode.exportMethodMap(currMap: MutableMap<MethodSignature, MutableList<String>>): MutableMap<MethodSignature, MutableList<String>> {
    var curr = currMap
    if(curr.containsKey(this.methodSignature)) {
        var temp = curr[this.methodSignature]
        temp!!.add(this.variableName)
        curr.set(this.methodSignature, temp)
    } else {
        curr.set(this.methodSignature, mutableListOf(this.variableName))
    }
    this.successors.forEach {
        curr = it.exportMethodMap(curr)
    }
    return curr
}