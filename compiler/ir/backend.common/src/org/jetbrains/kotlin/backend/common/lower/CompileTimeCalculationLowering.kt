/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.interpreter.IrInterpreter
import org.jetbrains.kotlin.backend.common.interpreter.builtins.compileTimeAnnotation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CompileTimeCalculationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(Transformer(), null)
    }
}

private class Transformer : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(Visitor(), null)) {
            return IrInterpreter().interpret(expression)
        }
        return super.visitCall(expression)
    }
}

private class Visitor : IrElementVisitor<Boolean, Nothing?> {
    private fun hasCompileCompileTimeAnnotation(annotations: List<IrConstructorCall>): Boolean {
        if (annotations.isNotEmpty()) {
            return annotations.first().symbol.descriptor.containingDeclaration.fqNameSafe == compileTimeAnnotation
        }
        return false
    }

    override fun visitElement(element: IrElement, data: Nothing?): Boolean {
        return false
    }

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        if (hasCompileCompileTimeAnnotation(expression.symbol.owner.annotations)) {
            val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, data) ?: true
            val extensionReceiverComputable = expression.extensionReceiver?.accept(this, data) ?: true
            for (i in 0 until expression.valueArgumentsCount) {
                if (expression.getValueArgument(i)?.accept(this, data) == false) {
                    return false
                }
            }
            return dispatchReceiverComputable && extensionReceiverComputable
        }

        return false
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): Boolean {
        return true
    }
}