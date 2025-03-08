package de.felixkat.InproDer.derivationtrees.exporter

import de.felixkat.InproDer.derivationtrees.DerivationNode
import sootup.core.signatures.MethodSignature

/*
 * Function to export a string of the derivation tree as a dot graph
 */
fun DerivationNode.exportAsDotGraph(): String {
    var methodMap = this.exportMethodMap(mutableMapOf()) // Retrieve a method map of the derivation tree
    var result = "digraph G {\n"
    methodMap.keys.forEach { key -> // Draw every method as a cluster with corresponding nodes
        result += "    subgraph \"cluster${key.hashCode()}\" {\n" // A cluster id here is always "cluster" followed by the hashCode of the method signature
        result += "        label=\"${key}\"\n"
        methodMap.get(key)!!.forEach { variableName ->
            var style = "[label=\"${variableName}\"]"
            if(key == this.methodSignature && variableName == this.variableName) {
                style = "[label=\"${variableName}\", style=filled, color=lightgreen]"
            }
            var name = "n${key.hashCode()}${variableName}" // A node id here is always "n" (for node) followed by the hashCode of the method signature and the variable name
            result += "        \"$name\"$style\n"
        }
        result += "    }\n\n"
    }
    result += this.exportAsDotGraphRecursive() // Draw the edges between the nodes
    result += "}"
    return result
}

/*
 * Internal recursive function to draw edges between the nodes
 */
private fun DerivationNode.exportAsDotGraphRecursive(): String {
    var result = ""
    this.successors.forEach {
        var style = "[label=\"${this.positionInfo.stmtPosition}\""
        if(this.methodSignature != it.methodSignature && !it.classField) {
            style += ",color=orange"
        } else if(it.classField) {
            style += ",color=red"
        }
        style += "]"
        var itName = "n${it.methodSignature.hashCode()}${it.variableName}" // Building corresponding node id
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"$itName\"${style};\n"
        if(this.methodSignature != it.methodSignature) {
            result += it.exportAsDotGraphRecursive("${this.methodSignature.hashCode()}${this.variableName}")
        } else {
            result += it.exportAsDotGraphRecursive()
        }
    }
    return result
}

/*
 * Internal recursive function to export the derivation tree as a dot graph, when return information is available
 */
private fun DerivationNode.exportAsDotGraphRecursive(returnTo: String): String {
    var result = ""
    this.successors.forEach {
        var style = "[label=\"${this.positionInfo.stmtPosition}\""
        if(this.methodSignature != it.methodSignature && !it.classField) {
            style += ",color=orange"
        } else if(it.classField) {
            style += ",color=red"
        }
        style += "]"
        var itName = "n${it.methodSignature.hashCode()}${it.variableName}" // Building corresponding node id
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"$itName\"${style};\n"
        result += it.exportAsDotGraphRecursive(returnTo)
    }
    this.returnInformation.forEach {
        result += "    \"n${this.methodSignature.hashCode()}${this.variableName}\" -> \"n${it.toMethodSignature.hashCode()}${it.toVariableName}\"[label=\"Returns at\\n${it.stmtPositionInfo.stmtPosition}\", color=blue];\n"
    }
    return result
}

/*
 * Get a method map from the derivation tree
 */
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