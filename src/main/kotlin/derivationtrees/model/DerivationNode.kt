package de.felixkat.InproDer.derivationtrees

import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.signatures.MethodSignature

data class DerivationNode(
    var variableName: String,
    var methodSignature: MethodSignature,
    var positionInfo: StmtPositionInfo,
    var successors: MutableList<DerivationNode>,
    var returnInformation: MutableList<ReturnInformation>,
    var classField: Boolean,
) {
    /*
     * Function to print the tree in stdout
     */
    fun printTree() {
        printTreeRecursive(this)
    }

    /*
     * Function to add a successor to the node
     */
    fun addSuccessor(succ: DerivationNode) {
        successors.add(succ)
    }

    /*
     * Function to add a list of successors to the node
     */
    fun addSuccessors(succs: List<DerivationNode>) {
        successors.addAll(succs)
    }

    /*
     * Function to add return information to the node
     */
    fun addReturnInformation(info: ReturnInformation) {
        returnInformation.add(info)
    }

    /*
     * Internal recursive function to print the tree
     */
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