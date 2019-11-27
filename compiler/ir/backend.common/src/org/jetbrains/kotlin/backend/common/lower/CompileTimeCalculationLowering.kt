/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.interpreter.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.compileTimeAnnotation
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CompileTimeCalculationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(Transformer(), null)
    }
}

private class Transformer : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(SignatureVisitor(), null)) {
            return when {
                expression.accept(BodyVisitor(), null) -> IrInterpreter().interpret(expression)
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
            initializer.expression = IrInterpreter().interpret(expression)
        }
        return declaration
    }

    //todo annotation call
}

private open class BasicVisitor : IrElementVisitor<Boolean, Nothing?> {
    // used to understand if IrGetValue is valid or not
    // todo figure something better or just remove it
    protected val parameters = mutableListOf<DeclarationDescriptor>()

    protected fun hasCompileCompileTimeAnnotation(container: IrAnnotationContainer): Boolean {
        if (container.annotations.isNotEmpty()) {
            return container.annotations.any { it.symbol.descriptor.containingDeclaration.fqNameSafe == compileTimeAnnotation }
        }
        if (container is IrDeclaration) return (container.parent as? IrClass)?.let { hasCompileCompileTimeAnnotation(it) } ?: false
        return false
    }

    override fun visitElement(element: IrElement, data: Nothing?): Boolean {
        return false
    }

    protected fun visitValueParameters(expression: IrFunctionAccessExpression, data: Nothing?): Boolean {
        for (i in 0 until expression.valueArgumentsCount) {
            parameters += expression.symbol.descriptor.valueParameters[i]
            if (expression.getValueArgument(i)?.accept(this, data) == false) {
                return false
            }
        }
        return true
    }

    private fun visitOverridden(symbol: IrFunctionSymbol): Boolean {
        val owner = symbol.owner as IrFunctionImpl
        val overridden = owner.overriddenSymbols.first()

        return if (overridden.owner.body != null) {
            overridden.owner.body!!.accept(this, null)
        } else {
            visitOverridden(overridden.owner.symbol)
        }
    }

    protected fun visitCall(expression: IrCall, withoutBodyCheck: Boolean = true): Boolean {
        val property = (expression.symbol.owner as? IrFunctionImpl)?.correspondingPropertySymbol?.owner
        if (hasCompileCompileTimeAnnotation(expression.symbol.owner) ||
            //TODO set CompileTimeCalculation annotation in generated getter
            property?.isConst == true || property?.let { hasCompileCompileTimeAnnotation(it) } == true
        ) {
            val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
            val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true
            if (!visitValueParameters(expression, null)) return false
            val bodyComputable = when {
                withoutBodyCheck -> true
                expression.getBody() != null -> expression.getBody()!!.accept(this, null)
                expression.isAbstract() -> true // todo make full check
                expression.isFakeOverridden() -> visitOverridden(expression.symbol)
                else -> true // todo find method in builtins
            }
            return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
        }

        return false
    }

    protected fun visitConstructor(expression: IrFunctionAccessExpression, withoutBodyCheck: Boolean = true): Boolean {
        if (hasCompileCompileTimeAnnotation(expression.symbol.owner)) {
            parameters += expression.getThisAsReceiver()
            if (!visitValueParameters(expression, null)) return false
            return when {
                withoutBodyCheck -> true
                else -> expression.getBody()?.statements?.all { it.accept(this, null) } ?: false
            }
        }
        return false
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): Boolean = true

    override fun visitWhen(expression: IrWhen, data: Nothing?): Boolean {
        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: Nothing?): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): Boolean {
        return hasCompileCompileTimeAnnotation(expression.symbol.owner) || expression.symbol.owner.isCompanion
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
        return visitStatements(expression.statements, data)
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean {
        // receiver always present
        if (expression.symbol.descriptor is ReceiverParameterDescriptor) return true
        return parameters.any { it.equalTo(expression.symbol.descriptor) }
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean = true

    override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean {
        //todo check receiver?
        return expression.value.accept(this, data)
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean {
        parameters += declaration.descriptor
        return declaration.initializer?.accept(this, data) ?: true
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Boolean {
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> true
            else -> false
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): Boolean {
        return loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
    }
}