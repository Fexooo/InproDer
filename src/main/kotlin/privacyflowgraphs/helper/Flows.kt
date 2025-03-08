package privacyflowgraphs.helper

import de.felixkat.InproDer.privacyflowgraphs.model.GlobalDataFlow
import sootup.core.signatures.MethodSignature
import sootup.core.views.View
import java.util.*

/*
 * Find all flows from source methods to other methods
 */
fun findFlows(view: View, sourceMethods: List<MethodSignature>): Map<MethodSignature, List<List<MethodSignature>>> {
    var allMethods = view.classes.flatMap { it.methods }
    var methodMap = mutableMapOf<MethodSignature, List<MethodSignature>>()
    allMethods.forEach {
        if(it.isConcrete && !it.isBuiltInMethod) {
            try {
                var allMethodInvocations = it.body.stmtGraph.filter { it.containsInvokeExpr() }
                var allMethodSignatures = allMethodInvocations.map { it.invokeExpr.methodSignature }
                methodMap.put(it.signature, allMethodSignatures)
            } catch (IllegalStateException: Exception) {
                println("Method ${it.signature} has invalid stmtGraph")
            }
        }
    }

    var result = mutableMapOf<MethodSignature, List<List<MethodSignature>>>()
    sourceMethods.forEach { sourceMethod ->
        var itResults: MutableList<List<MethodSignature>> = mutableListOf()
        allMethods.forEach {
            if(it != sourceMethod) {
                var res = breadthFirstSearch(methodMap, it.signature, sourceMethod)
                if(res != null) itResults.add(res)
            }
        }
        result.put(sourceMethod, itResults)

    }
    return result
}

/*
 * Internal function to perform a breadth first search on a "graph"
 */
private fun breadthFirstSearch(
    graph: Map<MethodSignature, List<MethodSignature>>,
    root: MethodSignature,
    goal: MethodSignature
): List<MethodSignature>? {
    val queue: Queue<MethodSignature> = LinkedList()
    val explored = mutableSetOf<MethodSignature>()
    val parentMap = mutableMapOf<MethodSignature, MethodSignature?>()

    explored.add(root)
    queue.add(root)
    parentMap[root] = null

    while (queue.isNotEmpty()) {
        val v = queue.poll()

        if (v == goal) {
            return reconstructPath(parentMap, goal)
        }

        for (w in graph[v] ?: emptyList()) {
            if (w !in explored) {
                explored.add(w)
                parentMap[w] = v
                queue.add(w)
            }
        }
    }

    return null
}

/*
 * Internal function to reconstruct the path from the parent map
 */
private fun reconstructPath(parentMap: Map<MethodSignature, MethodSignature?>, goal: MethodSignature): List<MethodSignature> {
    val path = mutableListOf<MethodSignature>()
    var current: MethodSignature? = goal

    while (current != null) {
        path.add(current)
        current = parentMap[current]
    }

    return path.reversed()
}