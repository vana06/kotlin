/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.multiplatform

import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationArgumentImpl
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns

class JavaActualAnnotationArgumentExtractor : ExpectedActualDeclarationChecker.ActualAnnotationArgumentExtractor {
    override fun extractActualValue(argument: PsiElement, expectedType: KotlinType): ConstantValue<*>? =
        (argument as? PsiAnnotationMethod)
            ?.defaultValue
            ?.let { JavaAnnotationArgumentImpl.create(it, null) }
            ?.convert(expectedType)

    // This code is similar to LazyJavaAnnotationDescriptor.resolveAnnotationArgument, but cannot be reused until
    // KClassValue/AnnotationValue are untied from descriptors/types, because here we do not have an instance of LazyJavaResolverContext.
    private fun JavaAnnotationArgument.convert(expectedType: KotlinType): ConstantValue<*>? {
        return when (this) {
            is JavaLiteralAnnotationArgument -> {
                ConstantValueFactory.createConstantValue(convertJavaLiteralValue(value, expectedType))
            }
            is JavaEnumValueAnnotationArgument -> {
                enumClassId?.let { enumClassId ->
                    entryName?.let { entryName ->
                        EnumValue(enumClassId, entryName)
                    }
                }
            }
            is JavaArrayAnnotationArgument -> {
                val elementType = expectedType.builtIns.getArrayElementType(expectedType)
                ConstantValueFactory.createArrayValue(getElements().mapNotNull { it.convert(elementType) }, expectedType)
            }
            is JavaAnnotationAsAnnotationArgument -> {
                // TODO: support annotations as annotation arguments
                null
            }
            is JavaClassObjectAnnotationArgument -> {
                // TODO: support class literals as annotation arguments
                null
            }
            else -> null
        }
    }

    private fun convertJavaLiteralValue(value: Any?, expectedType: KotlinType): Any? = when (value) {
        // For Java code inside an @interface:
        //
        //     byte b() default 1;
        //
        // PSI constant expression evaluator returns an instance of Integer equal to 1, which we need to convert to the Byte instance
        // in order to create the correct ConstantValue. Similarly for short and long.
        is Int -> when {
            KotlinBuiltIns.isByte(expectedType) -> value.toByte()
            KotlinBuiltIns.isShort(expectedType) -> value.toShort()
            KotlinBuiltIns.isLong(expectedType) -> value.toLong()
            else -> value
        }
        else -> value
    }
}
