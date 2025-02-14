package privacyflowgraphs.helper

import de.felixkat.InproDer.privacyflowgraphs.model.GlobalDataFlow

fun removeSubsets(list: List<GlobalDataFlow>): List<GlobalDataFlow> {
    var resList = list.toMutableList()
    var removeList = mutableListOf<GlobalDataFlow>()
    resList.forEach { it1 ->
        resList.forEach { it2 ->
            if(it1 != it2) {
                if(isSubset(it1, it2)) removeList.add(it1)
            }
        }
    }
    resList.removeAll(removeList)
    return resList
}

fun isSubset(subFlow: GlobalDataFlow?, mainFlow: GlobalDataFlow?): Boolean {
    if (subFlow == null) return true
    if (mainFlow == null) return false
    if (areIdentical(subFlow, mainFlow)) return true
    return mainFlow.call.any { isSubset(subFlow, it) }
}

fun areIdentical(flow1: GlobalDataFlow?, flow2: GlobalDataFlow?): Boolean {
    if (flow1 == null && flow2 == null) return true
    if (flow1 == null || flow2 == null) return false
    if (flow1.node.method != flow2.node.method) return false

    val calls1 = flow1.call.sortedBy { it.node.method }
    val calls2 = flow2.call.sortedBy { it.node.method }

    if (calls1.size != calls2.size) return false

    return calls1.zip(calls2).all { (c1, c2) -> areIdentical(c1, c2) }
}