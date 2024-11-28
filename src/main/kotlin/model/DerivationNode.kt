package de.felixkat.InproDer.model

import sootup.core.jimple.basic.StmtPositionInfo

data class DerivationNode(
    var variableName: String,
    var methodName: String,
    var positionInfo: StmtPositionInfo,
    var successors: MutableList<DerivationNode>
) {
    fun printTree() {
        printTreeRecursive(this)
    }

    fun addSuccessor(succ: DerivationNode) {
        successors.add(succ)
    }

    private fun printTreeRecursive(node: DerivationNode?, prefix: String = "", isLast: Boolean = true) {
        if (node == null) return
        print(prefix + (if (isLast) "└── " else "├── ") + node.variableName + " (method: " + node.methodName + "; line: " + node.positionInfo.stmtPosition + ")\n")

        val childPrefix = prefix + if (isLast) "    " else "│   "

        val childCount = node.successors.size
        node.successors.forEachIndexed { index, child ->
            val isLastChild = index == childCount - 1
            printTreeRecursive(child, childPrefix, isLastChild)
        }
    }
}