/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.interpreter.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.compileTimeAnnotation
import org.jetbrains.kotlin.backend.common.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

class CompileTimeCalculationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(Transformer(), null)
    }

    private inner class Transformer : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            if (expression.accept(SignatureVisitor(), null)) {
                return when {
                    expression.accept(BodyVisitor(), null) -> IrInterpreter(context.ir.irModule).interpret(expression)
                    else -> throw AssertionError("Ir call is marked as @CompileTimeCalculation but body contains not const expressions or statements")
                }
            }
            return expression
        }

        override fun visitField(declaration: IrField): IrStatement {
            val initializer = declaration.initializer
            val expression = initializer?.expression ?: return declaration
            val hasRightSignature = expression.accept(SignatureVisitor(), null)
            val isCompileTimeComputable = expression.accept(BodyVisitor(), null)
            if (declaration.descriptor.isConst && (!hasRightSignature || !isCompileTimeComputable)) {
                initializer.expression = IrErrorExpressionImpl(
                    declaration.startOffset, declaration.endOffset, declaration.type,
                    "Const property is used only with functions annotated as CompileTimeCalculation"
                )
            } else if (hasRightSignature && isCompileTimeComputable) {
                initializer.expression = IrInterpreter(context.ir.irModule).interpret(expression)
            }
            return declaration
        }

        //todo annotation call
    }

}

private open class BasicVisitor : IrElementVisitor<Boolean, Nothing?> {
    protected fun isMarkedAsCompileTime(container: IrAnnotationContainer) = container.isMarkedWith(compileTimeAnnotation)
    protected fun isMarkedAsEvaluateIntrinsic(container: IrAnnotationContainer) = container.isMarkedWith(evaluateIntrinsicAnnotation)

    protected fun IrAnnotationContainer.isMarkedWith(annotation: FqName): Boolean {
        if (this.hasAnnotation(annotation)) return true
        if (this is IrDeclaration) return (this.parent as? IrClass)?.isMarkedWith(annotation) ?: false
        return false
    }

    override fun visitElement(element: IrElement, data: Nothing?): Boolean {
        return false
    }

    protected fun visitValueParameters(expression: IrFunctionAccessExpression, data: Nothing?): Boolean {
        for (i in 0 until expression.valueArgumentsCount) {
            if (expression.getValueArgument(i)?.accept(this, data) == false) {
                return false
            }
        }
        return true
    }

    private fun visitOverridden(symbol: IrFunctionSymbol): Boolean {
        val owner = symbol.owner as IrFunctionImpl
        val overridden = owner.overriddenSymbols.first()

        val property = (overridden.owner as? IrFunctionImpl)?.correspondingPropertySymbol?.owner
        if (!(isMarkedAsCompileTime(overridden.owner) ||
            //TODO set CompileTimeCalculation annotation in generated getter
            property?.isConst == true || property?.let { isMarkedAsCompileTime(it) } == true) &&
            overridden.owner.body != null
        ) {
            return false
        }
        return when {
            //!hasCompileCompileTimeAnnotation((overridden as IrFunctionSymbol).owner) && overridden.owner.body != null -> return false
            overridden.owner.body != null -> overridden.owner.body!!.accept(this, null)
            overridden.owner.symbol.owner.overriddenSymbols.isNotEmpty() -> visitOverridden(overridden.owner.symbol)
            else -> true // todo find method in builtins or it is abstract
        }
    }

    protected fun visitCall(expression: IrCall, withoutBodyCheck: Boolean = true): Boolean {
        val property = (expression.symbol.owner as? IrFunctionImpl)?.correspondingPropertySymbol?.owner
        if (isMarkedAsCompileTime(expression.symbol.owner) ||
            //TODO set CompileTimeCalculation annotation in generated getter
            property?.isConst == true || property?.let { isMarkedAsCompileTime(it) } == true
        ) {
            val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
            val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true
            if (!visitValueParameters(expression, null)) return false
            val bodyComputable = when {
                withoutBodyCheck -> true
                isMarkedAsEvaluateIntrinsic(expression.symbol.owner) -> true
                expression.isAbstract() -> true // todo make full check
                expression.isFakeOverridden() -> visitOverridden(expression.symbol)
                expression.getBody() == null -> true // todo find method in builtins
                else -> expression.getBody()!!.accept(this, null)
            }
            return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
        }

        return false
    }

    protected fun visitConstructor(expression: IrFunctionAccessExpression, withoutBodyCheck: Boolean = true): Boolean {
        return when {
            isMarkedAsEvaluateIntrinsic(expression.symbol.owner) -> true
            isMarkedAsCompileTime(expression.symbol.owner) -> when {
                !visitValueParameters(expression, null) -> false
                withoutBodyCheck -> true
                else -> expression.getBody()?.statements?.all { it.accept(this, null) } ?: false
            }
            else -> false
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): Boolean = true

    override fun visitWhen(expression: IrWhen, data: Nothing?): Boolean {
        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: Nothing?): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): Boolean {
        return isMarkedAsCompileTime(expression.symbol.owner) || expression.symbol.owner.isCompanion
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): Boolean {
        return expression.elements.any { it.accept(this, data) }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean {
        return expression.arguments.all { it.accept(this, data) }
    }
}

/**
 * This visitor purpose is to understand if method call can be possibly evaluated in compile time
 */
private class SignatureVisitor : BasicVisitor() {
    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        return visitCall(expression, withoutBodyCheck = true)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression, withoutBodyCheck = true)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): Boolean {
        return (expression is IrReturnableBlock && expression.inlineFunctionSymbol?.owner?.let { isMarkedAsCompileTime(it) } == true) ||
                expression.isLambdaFunction()
    }

    override fun visitTry(aTry: IrTry, data: Nothing?): Boolean {
        return aTry.catches.all {
            val catchParameterIrClass = it.catchParameter.type.classOrNull!!.owner
            return@all isMarkedAsCompileTime(catchParameterIrClass) || isMarkedAsEvaluateIntrinsic(catchParameterIrClass)
        }
    }
}

/**
 * This visitor purpose is to answer the question: can or can not be evaluated this call?
 */
private class BodyVisitor : BasicVisitor() {
    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        return visitCall(expression, withoutBodyCheck = false)
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): Boolean {
        return true
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression, withoutBodyCheck = false)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): Boolean {
        if (expression.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            return true
        }
        return visitConstructor(expression, withoutBodyCheck = false)
    }

    private fun visitStatements(statements: List<IrStatement>, data: Nothing?): Boolean {
        return statements.all { it.accept(this, data) }
    }

    override fun visitBody(body: IrBody, data: Nothing?): Boolean {
        return visitStatements(body.statements, data)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): Boolean {
        if (expression.isLambdaFunction()) {
            val irLambdaReference = expression.statements.filterIsInstance<IrFunctionReference>().firstOrNull()
            return irLambdaReference?.symbol?.owner?.body?.accept(this, data) ?: false
        }
        return visitStatements(expression.statements, data)
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean = true

    override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean = true

    override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean {
        //todo check receiver?
        return expression.value.accept(this, data)
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Boolean {
        // todo check argument
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> expression.argument.accept(this, data)
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> true
            IrTypeOperator.CAST -> true
            else -> false
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): Boolean {
        return loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
    }

    override fun visitBreak(jump: IrBreak, data: Nothing?): Boolean = true

    override fun visitContinue(jump: IrContinue, data: Nothing?): Boolean = true

    override fun visitTry(aTry: IrTry, data: Nothing?): Boolean {
        if (!aTry.tryResult.accept(this, data)) return false
        if (aTry.finallyExpression != null && aTry.finallyExpression?.accept(this, data) == false) return false
        return aTry.catches.all { it.result.accept(this, data) }
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }
}