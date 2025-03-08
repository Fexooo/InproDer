package de.felixkat.InproDer.helper

import sootup.core.jimple.basic.LValue
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootMethod
import java.util.*

/*
 * Get Parameter LValue from stmts
 */
fun findLValueFromParameter(parameterIndex: Int, stmts: MutableList<Stmt>): Optional<LValue> {
    var retVal: Optional<LValue> = Optional.empty()
    stmts.forEach {
        it.uses.forEach { use ->
            if(use.toString().contains("@parameter${parameterIndex}")) {
                retVal = it.def
                return@forEach
            }
        }
    }
    return retVal
}

/*
 * Find LValue with a callback
 */
fun findLValueWithCallback(sootMethod: SootMethod, callback: (LValue) -> Boolean): LValue? {
    return sootMethod.body.defs.find { comp -> callback(comp) }
}