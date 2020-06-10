/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.evaluate

import org.jetbrains.kotlin.backend.common.interpreter.builtins.compileTimeAnnotation
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer
import java.io.File

val DESTINATION = File("compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/interpreter/builtins/IrBuiltInsMapGenerated.kt")

fun main() {
    DESTINATION.writeText(generateMap())
}

fun generateMap(): String {
    val sb = StringBuilder()
    val p = Printer(sb)
    p.println(File("license/COPYRIGHT.txt").readText())
    p.println("@file:Suppress(\"DEPRECATION_ERROR\")")
    p.println()
    p.println("package org.jetbrains.kotlin.backend.common.interpreter.builtins")
    p.println()
    p.println("/** This file is generated by org.jetbrains.kotlin.backend.common.interpreter.builtins.GenerateBuiltInsMap.generateMap(). DO NOT MODIFY MANUALLY */")
    p.println()

    val unaryOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val binaryOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val ternaryOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val builtIns = DefaultBuiltIns.Instance

    //save common built ins
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
                3 -> ternaryOperationsMap[function] = parametersTypes
                else -> throw IllegalStateException("Couldn't add following method from builtins to operations map: ${function.name} in class ${descriptor.name}")
            }
        }
    }

    //save ir built ins
    val binaryIrOperationsMap = mutableMapOf<CallableDescriptor, List<KotlinType>>()
    val irBuiltIns = getIrBuiltIns()
    val irFunSymbols =
        (irBuiltIns.lessFunByOperandType.values +
                irBuiltIns.lessOrEqualFunByOperandType.values +
                irBuiltIns.greaterFunByOperandType.values +
                irBuiltIns.greaterOrEqualFunByOperandType.values +
                irBuiltIns.eqeqSymbol +
                irBuiltIns.eqeqeqSymbol +
                irBuiltIns.ieee754equalsFunByOperandType.values +
                irBuiltIns.andandSymbol +
                irBuiltIns.ororSymbol)
            .map { it.descriptor }
            .filter { it.annotations.hasAnnotation(compileTimeAnnotation) }

    for (function in irFunSymbols) {
        val parametersTypes = function.valueParameters.map { TypeUtils.makeNotNullable(it.type) }

        check(parametersTypes.size == 2) { "Couldn't add following method from ir builtins to operations map: ${function.name}" }
        binaryIrOperationsMap[function] = parametersTypes
    }

    //save to file
    p.println("val unaryFunctions = mapOf<CompileTimeFunction, Function1<Any?, Any>>(")
    val unaryBody = unaryOperationsMap.entries.joinToString(",\n") { (function, parameters) ->
        val receiverType = parameters.last().toString()
        val methodName = "${function.name}"
        val parentheses = if (function is FunctionDescriptor) "()" else ""
        "    unaryOperation<$receiverType>(\"$methodName\", \"$receiverType\") { a -> a.$methodName$parentheses }"
    }
    p.println(unaryBody)
    p.println(")")
    p.println()

    p.println("val binaryFunctions = mapOf<CompileTimeFunction, Function2<Any?, Any?, Any>>(")
    val binaryBody = binaryOperationsMap.entries.joinToString(",\n", postfix = ",\n") { (function, parameters) ->
        val receiverType = parameters.last().toString()
        val parameter = parameters.first().toString()
        val methodName = "${function.name}"
        "    binaryOperation<$receiverType, $parameter>(\"$methodName\", \"$receiverType\", \"$parameter\") { a, b -> a.$methodName(b) }"
    } + binaryIrOperationsMap.entries.joinToString(",\n") { (function, parameters) ->
        val firstParameterType = parameters.last().toString()
        val secondParameterType = parameters.first().toString()
        val methodName = "${function.name}"
        val methodSymbol = when (methodName) {
            IrBuiltIns.OperatorNames.LESS -> "<"
            IrBuiltIns.OperatorNames.LESS_OR_EQUAL -> "<="
            IrBuiltIns.OperatorNames.GREATER -> ">"
            IrBuiltIns.OperatorNames.GREATER_OR_EQUAL -> ">="
            IrBuiltIns.OperatorNames.EQEQ -> "=="
            IrBuiltIns.OperatorNames.EQEQEQ -> "==="
            IrBuiltIns.OperatorNames.IEEE754_EQUALS -> "=="
            IrBuiltIns.OperatorNames.ANDAND -> "&&"
            IrBuiltIns.OperatorNames.OROR -> "||"
            else -> throw UnsupportedOperationException("Unknown ir operation \"$methodName\"")
        }
        "    binaryOperation<$firstParameterType, $secondParameterType>(\"$methodName\", \"$firstParameterType\", \"$secondParameterType\") { a, b -> a $methodSymbol b }"
    }
    p.println(binaryBody)
    p.println(")")

    p.println("val ternaryFunctions = mapOf<CompileTimeFunction, Function3<Any?, Any?, Any?, Any>>(")
    val ternaryBody = ternaryOperationsMap.entries.joinToString(",\n") { (function, parameters) ->
        val receiverType = parameters.last().toString()
        val firstParameter = parameters[0].toString()
        val secondParameter = parameters[1].toString()
        val methodName = "${function.name}"
        "    ternaryOperation<$receiverType, $firstParameter, $secondParameter>" +
                "(\"$methodName\", \"$receiverType\", \"$firstParameter\", \"$secondParameter\") { a, b, c -> a.$methodName(b, c) }"
    }
    p.println(ternaryBody)
    p.println(")")
    p.println()

    return sb.toString()
}

private fun getIrBuiltIns(): IrBuiltIns {
    val builtIns = DefaultBuiltIns.Instance
    val languageSettings = LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3)

    val moduleDescriptor = ModuleDescriptorImpl(Name.special("<test-module>"), LockBasedStorageManager(""), builtIns)
    val mangler = JvmManglerDesc()
    val signaturer = JvmIdSignatureDescriptor(mangler)
    val symbolTable = SymbolTable(signaturer)
    val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
    val typeTranslator = TypeTranslator(symbolTable, languageSettings, builtIns)
    constantValueGenerator.typeTranslator = typeTranslator
    typeTranslator.constantValueGenerator = constantValueGenerator

    return IrBuiltIns(builtIns, typeTranslator, signaturer)
}