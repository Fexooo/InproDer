package de.felixkat.InproDer

import de.felixkat.InproDer.error.VariableNotFound
import de.felixkat.InproDer.helper.findLValueFromParameter
import de.felixkat.InproDer.model.*
import org.objectweb.asm.Type
import sootup.core.graph.StmtGraph
import sootup.core.jimple.basic.LValue
import sootup.core.jimple.basic.StmtPositionInfo
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.jimple.common.stmt.JReturnStmt
import sootup.core.jimple.common.stmt.JReturnVoidStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import sootup.core.signatures.MethodSignature
import sootup.core.types.ClassType
import sootup.core.views.View
import java.util.*

fun generateDerivationTree(variableName: String, sootMethod: SootMethod, view: View): DerivationNode {
    val lVal: LValue? = sootMethod.body.defs.find { comp -> comp.toString() == variableName }
    if (lVal == null) {
        throw VariableNotFound("Given variable could not be found in definitions of sootMethod.")
    }
    return generateDerivationNode(
        lVal,
        sootMethod.body.stmtGraph.stmts,
        sootMethod,
        view,
        sootMethod.body.stmtGraph.stmts[0].positionInfo
    )
}

fun generateDerivationNode(
    watchValue: LValue,
    stmts: MutableList<Stmt>,
    method: SootMethod,
    view: View,
    stmtPositionInfo: StmtPositionInfo
): DerivationNode {
    val node = DerivationNode("$watchValue", method.name, stmtPositionInfo, mutableListOf())
    while (stmts.isNotEmpty()) {
        val stmt = stmts.removeFirst()
        stmt.uses.forEach { use ->
            if (use == watchValue) {
                val def = stmt.def
                if (stmt.containsInvokeExpr()) {
                    val classType: ClassType = stmt.invokeExpr.methodSignature.declClassType
                    val sootClass = view.getClass(classType)
                    if (sootClass.isPresent) {
                        val sootMethod: SootMethod =
                            sootClass.get().getMethod(stmt.invokeExpr.methodSignature.subSignature).get()
                        val graph = sootMethod.body.stmtGraph
                        val parameterIndex = stmt.invokeExpr.args.indexOfFirst { it == watchValue }
                        val newWatchValue = findLValueFromParameter(parameterIndex, graph.stmts)
                        if (newWatchValue.isPresent)
                            node.addSuccessor(
                                generateDerivationNode(
                                    newWatchValue.get(),
                                    graph.stmts,
                                    sootMethod,
                                    view,
                                    stmt.positionInfo
                                )
                            )
                    } else {
                        println("Invoked class is not in view! Invoked method signature: " + stmt.invokeExpr.methodSignature)
                    }
                }
                if (def.isPresent) {
                    val tempstmts = stmts.toMutableList()
                    node.addSuccessor(
                        generateDerivationNode(
                            def.get(),
                            tempstmts,
                            method,
                            view,
                            stmt.positionInfo
                        )
                    )
                }
            }
        }
    }
    return node
}


fun generatePrivacyFlowGraph(
    view: View
): List<DataFlowEdge> {
    var result: MutableList<DataFlowEdge> = mutableListOf()
    var sourceMethods = getSourceMethods(view)
    var cois = findCOIs(view, sourceMethods)
    println("Source methods found: ${sourceMethods}")
    cois.forEach { c ->
        result.addAll(buildGlobalDataflowForClass(view, c))
    }
    var filteredList = result.filter { edge ->
        edge.calls.any { hasSourceFlow(it) }
    }
    filteredList.forEach { edge ->
        println(edge)
    }
    return filteredList
}

fun hasSourceFlow(edge: DataFlowEdge): Boolean {
    if (edge.node.type == DataFlowType.SOURCE_FLOW) {
        return true
    }
    return edge.calls.any { hasSourceFlow(it) }
}

fun getSourceMethods(
    view: View
): List<MethodSignature> {
    var result = mutableListOf<MethodSignature>()
    view.classes.forEach { c ->
        c.methods.forEach methods@ { m ->
            m.parameterTypes.forEach { pType ->
                if(pType == m.returnType) {
                    result.add(m.signature)
                    return@methods
                }
            }
        }
    }
    return result.toList()
}

fun findCOIs(
    view: View,
    sourceMethods: List<MethodSignature>
): List<SootClass> {
    var result = mutableListOf<SootClass>()
    view.classes.forEach { c ->
        c.methods.forEach { m ->
            m.body.stmtGraph.forEach { stmt ->
                if(stmt.containsInvokeExpr()) {
                    if(sourceMethods.contains(stmt.invokeExpr.methodSignature)) {
                        result.add(c)
                    }
                }
            }
        }
    }
    return result
}

fun buildGlobalDataflowForClass(
    view: View,
    sootClass: SootClass,
): List<DataFlowEdge> {
    var result = mutableListOf<DataFlowEdge>()
    sootClass.methods.forEach { m ->
        result.add(DataFlowEdge(parseLocalDataFlow(listOf(), Optional.empty(), m), buildGlobalDataflow(view, m)))
    }
    return result
}

fun buildGlobalDataflow(
    view: View,
    sootMethod: SootMethod
): List<DataFlowEdge> {
    var result = mutableListOf<DataFlowEdge>()
    sootMethod.body.stmtGraph.forEach { stmt ->
        if(stmt.containsInvokeExpr()) {
            var method = view.getMethod(stmt.invokeExpr.methodSignature)
            if(method.isPresent) {
                result.add(DataFlowEdge(parseLocalDataFlow(stmt.invokeExpr.uses.toList(), stmt.def, method.get()), buildGlobalDataflow(view, method.get())))
            } else { println("Method not in view") }
        }
    }
    return result.toList()
}

fun parseLocalDataFlow(
    startDataPoint: List<Value>,
    endDataPoint: Optional<LValue>,
    method: SootMethod
): LocalDataFlow {
    if(method.returnType.toString() == "void") {
        return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.PROCESS)
    }

    method.parameterTypes.forEach { pType ->
        println("Comparing ${pType} with ${method.returnType}")
        if(pType == method.returnType) {
            return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SOURCE_FLOW)
        }
    }

    return LocalDataFlow(startDataPoint, endDataPoint, method.signature, DataFlowType.SINK_FLOW)
}