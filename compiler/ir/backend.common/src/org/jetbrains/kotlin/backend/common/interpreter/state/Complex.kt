/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.state

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.backend.common.interpreter.getLastOverridden
import org.jetbrains.kotlin.backend.common.interpreter.getCorrectReceiverByFunction
import org.jetbrains.kotlin.backend.common.interpreter.stack.Variable
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isInterface

abstract class Complex(override val irClass: IrClass, override val fields: MutableList<Variable>) : State {
    var superClass: Complex? = null
    var subClass: Complex? = null
    val typeArguments: MutableList<Variable> = mutableListOf()
    var outerClass: Variable? = null

    fun setSuperClassInstance(superClass: Complex) {
        if (this.irClass == superClass.irClass) {
            // if superClass is just secondary constructor instance, then copy properties that isn't already present in instance
            superClass.fields.forEach { if (!this.contains(it)) fields.add(it) }
            this.superClass = superClass.superClass
            superClass.superClass?.subClass = this
        } else {
            this.superClass = superClass
            superClass.subClass = this
        }
    }

    fun getOriginal(): Complex {
        return subClass?.getOriginal() ?: this
    }

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    private fun contains(variable: Variable) = fields.any { it.descriptor == variable.descriptor }

    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    protected fun copyFrom(other: Complex): State {
        this.superClass = other.superClass
        this.subClass = other.subClass ?: other
        this.typeArguments.addAll(other.typeArguments)
        this.outerClass = other.outerClass
        return this
    }

    private fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        val propertyGetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + functions).singleOrNull { it.descriptor.equalTo(descriptor) }
    }

    private fun getThisOrSuperReceiver(superIrClass: IrClass?): Complex? {
        return when {
            superIrClass == null -> this.getOriginal()
            superIrClass.isInterface -> {
                val interfaceState = Common(superIrClass)
                (this.copy() as Complex).setSuperClassInstance(interfaceState)
                interfaceState
            }
            else -> this.superClass
        }
    }

    protected fun getOverridden(owner: IrSimpleFunction, qualifier: State?): IrSimpleFunction {
        if (!owner.isFakeOverride) return owner
        if (qualifier == null || qualifier is ExceptionState || (qualifier as? Complex)?.superClass == null) {
            return owner.getLastOverridden() as IrSimpleFunction
        }

        val overriddenOwner = owner.overriddenSymbols.single().owner
        return when {
            overriddenOwner.body != null -> overriddenOwner
            else -> getOverridden(overriddenOwner, qualifier.superClass!!)
        }
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val receiver = getThisOrSuperReceiver(expression.superQualifierSymbol?.owner) ?: return null

        val irFunction = receiver.getIrFunction(expression.symbol.descriptor) ?: return null

        return when (irFunction.body) {
            null -> getOverridden(irFunction as IrSimpleFunction, this.getCorrectReceiverByFunction(irFunction))
            else -> irFunction
        }
    }
}