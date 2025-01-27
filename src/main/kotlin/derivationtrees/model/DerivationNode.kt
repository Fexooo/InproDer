package de.felixkat.InproDer.derivationtrees

import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.signatures.MethodSignature

data class DerivationNode(
    var variableName: String,
    var methodSignature: MethodSignature,
    var positionInfo: StmtPositionInfo,
    var successors: MutableList<DerivationNode>,
    var returnInformation: MutableList<ReturnInformation>,
    var classField: Boolean
) {
    fun printTree() {
        printTreeRecursive(this)
    }

    fun addSuccessor(succ: DerivationNode) {
        successors.add(succ)
    }

    fun addSuccessors(succs: List<DerivationNode>) {
        successors.addAll(succs)
    }

    fun addReturnInformation(info: ReturnInformation) {
        returnInformation.add(info)
    }

    private fun printTreeRecursive(node: DerivationNode?, prefix: String = "", isLast: Boolean = true) {
        if (node == null) return
        print(prefix + (if (isLast) "└── " else "├── ") + node.variableName + " (method: " + node.methodSignature.name + "; line: " + node.positionInfo.stmtPosition + "; classfield: " + node.classField + ")\n")

        val childPrefix = prefix + if (isLast) "    " else "│   "

        val childCount = node.successors.size
        node.successors.forEachIndexed { index, child ->
            val isLastChild = index == childCount - 1
            printTreeRecursive(child, childPrefix, isLastChild)
        }
    }
}

data class ReturnInformation(
    var toVariableName: String,
    var toMethodSignature: MethodSignature,
    var stmtPositionInfo: StmtPositionInfo
)