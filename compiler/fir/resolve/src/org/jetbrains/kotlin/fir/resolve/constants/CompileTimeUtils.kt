/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.constants

import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType

val kotlinFqName = FqName("kotlin")
val compileTimeAnnotation = FqName("kotlin.CompileTimeCalculation")

data class CompileTimeFunction(val reference: CallableId, val args: List<ClassId>)

@Suppress("UNCHECKED_CAST")
fun <T> unaryOperation(
    className: CompileTimeType<T>, methodName: String, function: (T) -> Any
): Pair<CompileTimeFunction, Function1<Any?, Any>> {
    return CompileTimeFunction(toCallableId(className.toFqName(), methodName), listOf()) to function as Function1<Any?, Any>
}

@Suppress("UNCHECKED_CAST")
fun <T, E> binaryOperation(
    className: CompileTimeType<T>, parameterType: CompileTimeType<E>, methodName: String, function: (T, E) -> Any
): Pair<CompileTimeFunction, Function2<Any?, Any?, Any>> {
    return CompileTimeFunction(
        toCallableId(className.toFqName(), methodName), listOf(parameterType.toClassId())
    ) to function as Function2<Any?, Any?, Any>
}

private fun <T> CompileTimeType<T>.toFqName(): FqName {
    return FqName(name)
}

private fun <T> CompileTimeType<T>.toClassId(): ClassId {
    return ClassId(kotlinFqName, toFqName(), false)
}

private fun toCallableId(className: FqName, methodName: String): CallableId {
    return CallableId(kotlinFqName, className, Name.identifier(methodName))
}