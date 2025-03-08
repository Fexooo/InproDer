<h1 align="center">InproDer</h1>
<div align="center">
    <strong>In</strong>tra- and <strong>In</strong>ter<strong>pro</strong>cedural <strong>Der</strong>ivation Trees
</div>

---

## What is it doing?

This project generates derivation trees and privacy flow graphs for given variables in source code and visualizes how the variable is being processed.
It is using the SootUp framework to generate the derivation trees and privacy flow graphs.
This means you will need to use SootUp in a given analysis to get the necessary parameters for the algorithm.

---

## Why is it useful?

It can help to understand how a variable is being processed in a given source code or how data in general is flowing through a java program.
Therefore it can help to understand privacy related data flows in a program and help to identify potential privacy threats.

---

## How do I use it?
In order to use this library you will need to get the necessary parameters from SootUp.
First get a `Java View` by using a list of `AnalysisInputLocations`.
```kotlin
val locations = listOf(
    JrtFileSystemAnalysisInputLocation(),
    JavaClassPathAnalysisInputLocation("path/to/jar/or/classes")
)

val view: View = JavaView(locations)
```

After that you can use the `JavaView` to get the SootClass and Method to analyze.
Refer to the SootUp documentation for more information on how to get the necessary parameters.
After that you will be able to call the `generateDerivationTree` or `generatePrivacyFlowGraph` functions.
```kotlin
generateDerivationTree(variable, sootMethod, sootClass, view)
// or
generatePrivacyFlowGraph(view, sourceMethodList, false)
```
For in-depth information on the parameters refer to the function Usage below.

---

## Function Usage

```kotlin
generateDerivationTree(
    variable: String | LValue | (LValue) -> Boolean,
    sootMethod: SootMethod,
    sootClass: SootClass,
    view: View,
    visitClassVars: Boolean = true
)
```

 - The parameter `variable` is either a `String`, `LValue` or Callback function that determines the variable for the derivation tree analysis.
 - The parameter `visitClassVars` determines if the class variables should be visited or not.
 - The parameters `sootMethod`, `sootClass` and `view` should be self-explained by the **How do I use it?** chapter above (SootUp parameters).

```kotlin
generatePrivacyFlowGraph(
    view: View,
    sourceMethodList: List<MethodSignature> | (SootMethod) -> Boolean,
    useDerivationTrees: Boolean
)
```

 - The parameter `sourceMethodList` is a list of `MethodSignature` or can be replaced by a callback function to determine source methods dynamically.
   The callback function should return a boolean value to determine if the given `SootMethod` is a source method or not.
 - The parameter `useDerivationTrees` determines if the derivation trees should be generated with the privacy flow graphs.
 - The parameter `view` should be self-explained by the **How do I use it?** chapter above (SootUp parameters).

---

## Related Work/Projects
 - [Privacy Flow Graphs](http://doi.org/10.1145/3549035.3561185) by **Feiyang Tang** and **Bjarte M. Østvold**.
    - This paper introduces the concept of privacy flow graphs. It is the basis of this library. This library is extending the concept of Privacy Flow Graphs by adding derivation trees to privacy flow graphs.
 - [InproDer-Eval](https://github.com/Fexooo/InproDer-Eval) by **me**.
    - This project contains source code to evaluate the approach used in this library.