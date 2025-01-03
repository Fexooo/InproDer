package de.felixkat.InproDer.derivationtrees.exporter

import de.felixkat.InproDer.derivationtrees.DerivationNode
import sootup.core.signatures.MethodSignature

fun DerivationNode.exportAsDotGraph(): String {
    var methodMap = this.exportMethodMap(mutableMapOf())
    var result = "digraph G {\n"
    methodMap.keys.forEach { key ->
        result += "    subgraph \"s${key.hashCode()}\" {\n"
        result += "        label=\"${key.name}\"\n"
        methodMap.get(key)!!.forEach { variableName ->
            result += "        \"n${key.hashCode()}${variableName}\"[label=\"${variableName}\"]\n"
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
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"n${it.methodSignature.hashCode()}${it.variableName}\";\n"
        result += it.exportAsDotGraphRecursive()
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