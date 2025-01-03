package de.felixkat.InproDer.privacyflowgraphs.helper

import sootup.core.signatures.MethodSignature
import sootup.core.views.View

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