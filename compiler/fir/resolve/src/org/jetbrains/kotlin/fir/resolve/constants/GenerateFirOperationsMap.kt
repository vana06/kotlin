/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.constants

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer
import java.io.File

val DESTINATION = File("compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/constants/FirOperationsMapGenerated.kt")

fun main() {
    DESTINATION.writeText(generateMap())
}

//TODO try to reuse code from GenerateOperationsMap.kt
fun generateMap(): String {
    val sb = StringBuilder()
    val p = Printer(sb)
    p.println(File("license/COPYRIGHT.txt").readText())
    p.println("@file:Suppress(\"DEPRECATION_ERROR\")")
    p.println()
    p.println("package org.jetbrains.kotlin.fir.resolve.constants")
    p.println()
    p.println("import org.jetbrains.kotlin.resolve.constants.evaluate.*")
    p.println()
    p.println("/** This file is generated by org.jetbrains.kotlin.fir.resolve.constants.GenerateFirOperationsMap.generateMap(). DO NOT MODIFY MANUALLY */")
    p.println()

    val unaryOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val binaryOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val builtIns = DefaultBuiltIns.Instance

    @Suppress("UNCHECKED_CAST")
    val allPrimitiveTypes = builtIns.builtInsPackageScope.getContributedDescriptors()
        .filter { it is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(it.defaultType) } as List<ClassDescriptor>

    for (descriptor in allPrimitiveTypes + builtIns.string) {
        val descriptorType = descriptor.defaultType
        @Suppress("UNCHECKED_CAST")
        val functions = descriptor.getMemberScope(listOf()).getContributedDescriptors()
            .filter { it is CallableDescriptor && it.annotations.hasAnnotation(compileTimeAnnotation) } as List<CallableDescriptor>

        for (function in functions) {
            val parametersTypes = function.valueParameters.map { TypeUtils.makeNotNullable(it.type) } + listOf(descriptorType)

            when (parametersTypes.size) {
                1 -> unaryOperationsMap[function] = parametersTypes
                2 -> binaryOperationsMap[function] = parametersTypes
                else -> throw IllegalStateException("Couldn't add following method from builtins to operations map: ${function.name} in class ${descriptor.name}")
            }
        }
    }

    p.println("val unaryFunctions = mapOf<CompileTimeFunction, Function1<Any?, Any>>(")
    val unaryBody = unaryOperationsMap.entries.joinToString(",\n") { (function, parameters) ->
        val receiverType = parameters.last().toString().toUpperCase()
        val methodName = "${function.name}"
        val parentheses = if (function is FunctionDescriptor) "()" else ""
        "    unaryOperation($receiverType, \"$methodName\") { a -> a.$methodName$parentheses }"
    }
    p.println(unaryBody)
    p.println(")")
    p.println()

    p.println("val binaryFunctions = mapOf<CompileTimeFunction, Function2<Any?, Any?, Any>>(")
    val binaryBody = binaryOperationsMap.entries.joinToString(",\n") { (function, parameters) ->
        val receiverType = parameters.last().toString().toUpperCase()
        val parameter = parameters.first().toString().toUpperCase()
        val methodName = "${function.name}"
        "    binaryOperation($receiverType, $parameter, \"$methodName\") { a, b -> a.$methodName(b) }"
    }
    p.println(binaryBody)
    p.println(")")

    return sb.toString()
}
