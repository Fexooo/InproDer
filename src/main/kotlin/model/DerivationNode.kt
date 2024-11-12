package de.felixkat.InproDer.model

data class DerivationNode(
    var variableName: String,
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
        print(prefix + (if (isLast) "└── " else "├── ") + node.variableName + "\n")

        val childPrefix = prefix + if (isLast) "    " else "│   "

        val childCount = node.successors.size
        node.successors.forEachIndexed { index, child ->
            val isLastChild = index == childCount - 1
            printTreeRecursive(child, childPrefix, isLastChild)
        }
    }
}