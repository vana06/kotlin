/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberPropertyImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.constants.compileTimeAnnotation
import org.jetbrains.kotlin.fir.resolve.constants.evaluator.FirCompileTimeExpressionEvaluator
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.utils.addToStdlib.cast

open class FirCompileTimeExpressionTransformer : FirAbstractTreeTransformer(phase = FirResolvePhase.COMPILE_TIME_EXPRESSION) {
    override lateinit var session: FirSession
    private val evaluator = FirCompileTimeExpressionEvaluator()

    private fun FirExpression?.isCompileTimeComputable(): Boolean {
        if (this is FirConstExpression<*>) return true
        if (this !is FirResolvable) return false
        if (this is FirFunctionCall && !(this.dispatchReceiver.isCompileTimeComputable() && this.arguments.all { it.isCompileTimeComputable() }))
            return false

        return when (val resolvedSymbol = (this.calleeReference as? FirResolvedCallableReference)?.resolvedSymbol) {
            is FirPropertySymbol -> resolvedSymbol.fir.isConst || resolvedSymbol.fir.containsCompileTimeAnnotation()
            is FirFunctionSymbol<*> -> resolvedSymbol.fir.containsCompileTimeAnnotation()
            else -> false
        }
    }

    private fun FirAnnotationContainer.containsCompileTimeAnnotation() : Boolean {
        return this.annotations.any { it.isCompileTimeAnnotation() }
    }

    private fun FirAnnotationCall.isCompileTimeAnnotation(): Boolean {
        return this.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId?.asSingleFqName() == compileTimeAnnotation
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        session = file.fileSession
        return transformElement(file, data)
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (property.isConst) {
            val initializer = property.initializer
            if (!(initializer.isCompileTimeComputable() || initializer is FirOperatorCall)) {
                return FirErrorFunction(session, initializer?.psi, "Const property is used only with functions annotated as CompileTimeCalculation").compose()
            } else {
                (property as FirMemberPropertyImpl).initializer = evaluator.visitExpression(initializer!!, data)
            }
        }

        //can't just return 'property' because need to transform children elements such as annotations
        return super.transformProperty(property, data)
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        annotationCall.arguments.transformFlat {
            return@transformFlat when {
                it.isCompileTimeComputable() -> listOf(evaluator.visitExpression(it, data))
                else -> null
            }
        }

        return super.transformAnnotationCall(annotationCall, data)
    }
}