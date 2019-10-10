/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.constants.evaluator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.constants.CompileTimeFunction
import org.jetbrains.kotlin.fir.resolve.constants.binaryFunctions
import org.jetbrains.kotlin.fir.resolve.constants.unaryFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.expressions.IrConstKind

class FirCompileTimeExpressionEvaluator : FirVisitor<FirExpression, Nothing?>() {
    override fun visitElement(element: FirElement, data: Nothing?): FirExpression {
        return element.accept(this, data)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Nothing?): FirExpression {
        return constExpression
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Nothing?): FirExpression {
        val args = operatorCall.arguments.map { (it.accept(this, data) as FirConstExpression<*>).value }
        val leftArg = args[0]!!
        val rightArg = args[1]!!

        val result = when (operatorCall.operation) {
            FirOperation.EQ -> leftArg::class.java == rightArg::class.java && leftArg == rightArg
            FirOperation.NOT_EQ -> leftArg != rightArg
            FirOperation.IDENTITY -> leftArg === rightArg
            FirOperation.NOT_IDENTITY -> leftArg !== rightArg
            else -> throw UnsupportedOperationException("Unsupported operator call operation ${operatorCall.operation}")
        }
        return result.toFirConst(operatorCall.psi, operatorCall.typeRef)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Nothing?): FirExpression {
        val resolvedSymbol = (qualifiedAccessExpression.calleeReference as FirResolvedCallableReference).resolvedSymbol
        if (resolvedSymbol is FirPropertySymbol) {
            resolvedSymbol.fir.initializer?.let { return it.accept(this, data) }
        }

        val callableId = (resolvedSymbol as FirPropertySymbol).callableId
        val compileTimeFunction = CompileTimeFunction(callableId, listOf())

        val receiverValue = (qualifiedAccessExpression.dispatchReceiver.accept(this, data) as FirConstExpression<*>).value
        val expectedReturnType = qualifiedAccessExpression.typeRef
        val function = unaryFunctions[compileTimeFunction]
            ?: throw NoSuchMethodException("For given function $compileTimeFunction there is no entry in unary map")
        return function.invoke(receiverValue).toFirConst(qualifiedAccessExpression.psi, expectedReturnType)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): FirExpression {
        val resolvedSymbol = (functionCall.calleeReference as FirResolvedCallableReference).resolvedSymbol
        val callableId = (resolvedSymbol as FirFunctionSymbol).callableId
        val args = functionCall.arguments.mapNotNull { it.typeRef.coneTypeSafe<ConeClassType>()?.lookupTag?.classId }
        val compileTimeFunction = CompileTimeFunction(callableId, args)

        val receiverValue = (functionCall.dispatchReceiver.accept(this, data) as FirConstExpression<*>).value
        val expectedReturnType = functionCall.typeRef
        val argsValues = functionCall.arguments.map { (it.accept(this, data) as FirConstExpression<*>).value }

        return when (args.size) {
            0 -> {
                val function = unaryFunctions[compileTimeFunction]
                    ?: throw NoSuchMethodException("For given function $compileTimeFunction there is no entry in unary map")
                function.invoke(receiverValue).toFirConst(functionCall.psi, expectedReturnType)
            }
            1 -> {
                val function = binaryFunctions[compileTimeFunction]
                    ?: throw NoSuchMethodException("For given function $compileTimeFunction there is no entry in binary map")
                function.invoke(receiverValue, argsValues.first()).toFirConst(functionCall.psi, expectedReturnType)
            }
            else -> {
                throw UnsupportedOperationException("Unsupported number of arguments")
            }
        }
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Nothing?): FirExpression {
        val leftOperand = (binaryLogicExpression.leftOperand.accept(this, data) as FirConstExpression<Boolean>).value
        val rightOperand = (binaryLogicExpression.rightOperand.accept(this, data) as FirConstExpression<Boolean>).value

        return when (binaryLogicExpression.kind) {
            FirBinaryLogicExpression.OperationKind.AND -> leftOperand && rightOperand
            FirBinaryLogicExpression.OperationKind.OR -> leftOperand && rightOperand
        }.toFirConst(binaryLogicExpression.psi, binaryLogicExpression.typeRef)
    }

    private fun Any.toFirConst(psi: PsiElement?, typeRef: FirTypeRef): FirConstExpression<*> {
        return when (this) {
            is Boolean -> FirConstExpressionImpl(psi, IrConstKind.Boolean, this)
            is Char -> FirConstExpressionImpl(psi, IrConstKind.Char, this)
            is Byte -> FirConstExpressionImpl(psi, IrConstKind.Byte, this)
            is Short -> FirConstExpressionImpl(psi, IrConstKind.Short, this)
            is Int -> FirConstExpressionImpl(psi, IrConstKind.Int, this)
            is Long -> FirConstExpressionImpl(psi, IrConstKind.Long, this)
            is String -> FirConstExpressionImpl(psi, IrConstKind.String, this)
            is Float -> FirConstExpressionImpl(psi, IrConstKind.Float, this)
            is Double -> FirConstExpressionImpl(psi, IrConstKind.Double, this)
            else -> throw UnsupportedOperationException("Unsupported const element type $this")
        }.apply { this.typeRef = typeRef }
    }
}