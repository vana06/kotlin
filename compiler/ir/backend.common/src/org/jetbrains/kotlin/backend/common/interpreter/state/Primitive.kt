/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.state

import org.jetbrains.kotlin.backend.common.interpreter.getLastOverridden
import org.jetbrains.kotlin.backend.common.interpreter.stack.Variable
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isFakeOverride

class Primitive<T>(var value: T, val type: IrType) : State {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()
    override val irClass: IrClass = type.classOrNull!!.owner

    override fun getState(symbol: IrSymbol): State {
        return super.getState(symbol) ?: this
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val descriptor = expression.symbol.descriptor
        // must add property's getter to declaration's list because they are not present in ir class for primitives
        val declarations = irClass.declarations.map { if (it is IrProperty) it.getter else it }
        return declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
            ?.let { if (it.isFakeOverride) it.getLastOverridden() else it }
    }

    override fun toString(): String {
        return "Primitive(value=$value, type=${irClass.descriptor.defaultType})"
    }
}
