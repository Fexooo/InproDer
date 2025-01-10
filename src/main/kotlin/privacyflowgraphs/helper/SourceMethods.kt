package de.felixkat.InproDer.privacyflowgraphs.helper

import sootup.core.model.SootMethod
import sootup.core.signatures.MethodSignature
import sootup.core.views.View

/**
 * Source Method getter using Tang and Østvold approach
 */
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

/**
 * Get source method list by using a callback for finding methods
 */
fun getSourceMethods(
    view: View,
    sourceMethodCallback: (SootMethod) -> Boolean
): List<MethodSignature> {
    var result = mutableListOf<MethodSignature>()
    view.classes.forEach { c ->
        c.methods.forEach methods@ { m ->
            if(sourceMethodCallback(m)) {
                result.add(m.signature)
            }
        }
    }
    return result.toList()
}