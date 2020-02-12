/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.kotlin.backend.common.interpreter.builtins.*
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import java.lang.invoke.MethodHandle

enum class Code(var info: String = "") {
    NEXT, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

private const val MAX_STACK_SIZE = 10_000

class IrInterpreter(irModule: IrModuleFragment) {
    private val irBuiltIns = irModule.irBuiltins
    private val irExceptions = irModule.files.flatMap { it.declarations }.filterIsInstance<IrClass>()
        .filter { it.isSubclassOf(irBuiltIns.throwableClass.owner) }
    private val classCastException = irExceptions.first { it.name.asString() == ClassCastException::class.java.simpleName }
    private val illegalArgumentException = irExceptions.first { it.name.asString() == IllegalArgumentException::class.java.simpleName }

    private val stackTrace = mutableListOf<String>()

    private val mapOfEnums = mutableMapOf<Pair<IrClass, String>, Common>()

    private fun Any?.getType(defaultType: IrType): IrType {
        return when (this) {
            is Boolean -> irBuiltIns.booleanType
            is Char -> irBuiltIns.charType
            is Byte -> irBuiltIns.byteType
            is Short -> irBuiltIns.shortType
            is Int -> irBuiltIns.intType
            is Long -> irBuiltIns.longType
            is String -> irBuiltIns.stringType
            is Float -> irBuiltIns.floatType
            is Double -> irBuiltIns.doubleType
            null -> irBuiltIns.nothingType
            else -> defaultType
        }
    }

    fun interpret(expression: IrExpression): IrExpression {
        val data = InterpreterFrame()
        return runBlocking {
            return@runBlocking when (val code = withContext(this.coroutineContext) { expression.interpret(data) }) {
                Code.NEXT -> data.popReturnValue().toIrExpression(expression)
                Code.EXCEPTION -> {
                    val message = (data.popReturnValue() as ExceptionState).getFullDescription()
                    IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + message)
                }
                else -> TODO("$code not supported as result of interpretation")
            }
        }
    }

    private suspend fun IrElement.interpret(data: Frame): Code {
        try {
            val code = when (this) {
                is IrFunctionImpl -> interpretFunction(this, data)
                is IrCall -> interpretCall(this, data)
                is IrConstructorCall -> interpretConstructorCall(this, data)
                is IrDelegatingConstructorCall -> interpretDelegatedConstructorCall(this, data)
                is IrBody -> interpretBody(this, data)
                is IrBlock -> interpretBlock(this, data)
                is IrReturn -> interpretReturn(this, data)
                is IrSetField -> interpretSetField(this, data)
                is IrGetField -> interpretGetField(this, data)
                is IrGetValue -> interpretGetValue(this, data)
                is IrGetObjectValue -> interpretGetObjectValue(this, data)
                is IrGetEnumValue -> interpretGetEnumValue(this, data)
                is IrEnumEntry -> interpretEnumEntry(this, data)
                is IrConst<*> -> interpretConst(this, data)
                is IrVariable -> interpretVariable(this, data)
                is IrSetVariable -> interpretSetVariable(this, data)
                is IrTypeOperatorCall -> interpretTypeOperatorCall(this, data)
                is IrBranch -> interpretBranch(this, data)
                is IrWhileLoop -> interpretWhile(this, data)
                is IrWhen -> interpretWhen(this, data)
                is IrBreak -> interpretBreak(this, data)
                is IrContinue -> interpretContinue(this, data)
                is IrVararg -> interpretVararg(this, data)
                is IrTry -> interpretTry(this, data)
                is IrCatch -> interpretCatch(this, data)
                is IrThrow -> interpretThrow(this, data)
                is IrStringConcatenation -> interpretStringConcatenation(this, data)

                else -> TODO("${this.javaClass} not supported")
            }

            return when (code) {
                Code.RETURN -> when (this) { // TODO check label
                    is IrCall, is IrReturnableBlock, is IrFunctionImpl -> Code.NEXT
                    else -> Code.RETURN
                }
                Code.BREAK_WHEN -> when (this) {
                    is IrWhen -> Code.NEXT
                    else -> code
                }
                Code.BREAK_LOOP -> when (this) {
                    is IrWhileLoop -> if ((this.label ?: "") == code.info) Code.NEXT else code
                    else -> code
                }
                Code.CONTINUE -> when (this) {
                    is IrWhileLoop -> if ((this.label ?: "") == code.info) this.interpret(data) else code
                    else -> code
                }
                Code.EXCEPTION -> Code.EXCEPTION
                Code.NEXT -> Code.NEXT
            }
        } catch (e: Throwable) {
            // catch exception from JVM such as: ArithmeticException, StackOverflowError and others
            val exceptionName = e::class.java.simpleName
            val irExceptionClass = irExceptions.firstOrNull { it.name.asString() == exceptionName } ?: irBuiltIns.throwableClass.owner

            // TODO do we really need this?... It will point to JVM stdlib
            val additionalStack = mutableListOf<String>()
            if (e.stackTrace.any { it.className == "java.lang.invoke.MethodHandle" }) {
                for ((index, stackTraceElement) in e.stackTrace.withIndex()) {
                    if (stackTraceElement.methodName == "invokeWithArguments") {
                        additionalStack.addAll(e.stackTrace.slice(0 until index).reversed().map { "at $it" })
                        break
                    }
                }

                var cause = e.cause
                val lastNeededValue = e.stackTrace.first().className + "." + e.stackTrace.first().methodName
                while (cause != null) {
                    for ((causeStackIndex, causeStackTraceElement) in cause.stackTrace.withIndex()) {
                        val currentStackTraceValue = causeStackTraceElement.className + "." + causeStackTraceElement.methodName
                        if (currentStackTraceValue== lastNeededValue) {
                            cause.stackTrace = cause.stackTrace.sliceArray(0 until causeStackIndex).reversedArray()
                            break
                        }
                    }
                    cause = cause.cause
                }
            }

            val exceptionState = when {
                // this block is used to replace jvm null pointer exception with common one
                // TODO figure something better
                irExceptionClass == irBuiltIns.throwableClass.owner && exceptionName == "KotlinNullPointerException" ->
                    ExceptionState(e, irExceptions.first { it.name.asString() == "NullPointerException" }, stackTrace + additionalStack)
                else -> ExceptionState(e, irExceptionClass, stackTrace + additionalStack)
            }

            data.pushReturnValue(exceptionState)
            return Code.EXCEPTION
        }
    }

    // this method is used to get stack trace after exception
    private suspend fun interpretFunction(irFunction: IrFunctionImpl, data: Frame): Code {
        return try {
            yield()

            if (irFunction.fileOrNull != null) {
                val fileName = irFunction.file.name
                val lineNum = irFunction.fileEntry.getLineNumber(irFunction.startOffset) + 1
                stackTrace += "at ${fileName.replace(".kt", "Kt").capitalize()}.${irFunction.fqNameForIrSerialization}($fileName:$lineNum)"
            }

            if (stackTrace.size == MAX_STACK_SIZE) {
                throw StackOverflowError("")
            }

            when (val kind = (irFunction.body as? IrSyntheticBody)?.kind) {
                IrSyntheticBodyKind.ENUM_VALUES -> {
                    val enumClass = irFunction.parentAsClass
                    val enumEntries = enumClass.declarations
                        .filterIsInstance<IrEnumEntry>()
                        .map { entry ->
                            entry.interpret(data).also { if (it != Code.NEXT) return it }
                            data.popReturnValue() as Common
                        }
                    data.pushReturnValue(enumEntries.toTypedArray().toState(irBuiltIns.arrayClass.defaultType))
                    Code.NEXT
                }
                IrSyntheticBodyKind.ENUM_VALUEOF -> {
                    val enumClass = irFunction.parentAsClass
                    val enumEntryName = (data.getAll().single().state as Primitive<*>).value.toString()
                    val enumEntry = enumClass.declarations
                        .filterIsInstance<IrEnumEntry>()
                        .singleOrNull { it.name.asString() == enumEntryName }
                    if (enumEntry == null) {
                        val message = "No enum constant ${enumClass.fqNameForIrSerialization}.$enumEntryName"
                        data.pushReturnValue(ExceptionState(IllegalArgumentException(message), illegalArgumentException, stackTrace))
                        Code.EXCEPTION
                    } else {
                        enumEntry.interpret(data).also { if (it != Code.NEXT) return it }
                        Code.NEXT
                    }
                }
                null -> irFunction.body?.interpret(data) ?: throw AssertionError("Ir function must be with body")
                else -> throw AssertionError("Unsupported IrSyntheticBodyKind $kind")
            }
        } finally {
            if (irFunction.fileOrNull != null) stackTrace.removeAt(stackTrace.lastIndex)
        }
    }

    private suspend fun MethodHandle.invokeMethod(irFunction: IrFunction, data: Frame): Code {
        val result = this.invokeWithArguments(irFunction.getArgsForMethodInvocation(data))
        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))

        return Code.NEXT
    }

    private suspend fun calculateAbstract(irFunction: IrFunction, data: Frame): Code {
        if (irFunction.body == null) {
            val receiver = data.getVariableState(irFunction.symbol.getReceiver()!!) as Complex
            val instance = receiver.instance!!

            val functionImplementation = instance.getIrFunction(irFunction.descriptor)
            if (functionImplementation?.body == null) throw NoSuchMethodException("Method \"${irFunction.name}\" wasn't implemented")
            val arguments = functionImplementation.valueParameters.map { Variable(it.descriptor, data.getVariableState(it.descriptor)) }
            val newFrame = InterpreterFrame()
            newFrame.addVar(Variable(functionImplementation.symbol.getReceiver()!!, instance))
            newFrame.addAll(arguments)
            return functionImplementation.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
        }
        return irFunction.body!!.interpret(data)
    }

    private suspend fun calculateOverridden(owner: IrFunctionImpl, data: Frame): Code {
        fun IrFunctionImpl.getLastPossibleOverridden(): IrFunctionImpl {
            // main usage: get throwable properties getter signature from exception classes
            // then use that signatures to get right method from ir builtins map
            var overridden = this
            while (overridden.overriddenSymbols.isNotEmpty()) overridden = overridden.overriddenSymbols.first().owner as IrFunctionImpl
            return overridden
        }

        val variableDescriptor = owner.symbol.getReceiver()!!
        val superQualifier = (data.getVariableState(variableDescriptor) as? Complex)?.superType
            ?: return calculateBuiltIns(owner.getLastPossibleOverridden(), data)
        val overridden = owner.overriddenSymbols.first { it.getReceiver()?.equalTo(superQualifier.getReceiver()) == true }

        val newStates = InterpreterFrame(mutableListOf(Variable(overridden.getReceiver()!!, superQualifier)))
        owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
            .forEach { newStates.addVar(it) }

        val overriddenOwner = overridden.owner as IrFunctionImpl
        return when {
            superQualifier.irClassFqName() == "kotlin.Enum" && owner.name.asString() == "hashCode" -> calculateOverridden(overriddenOwner, newStates)
            overriddenOwner.body != null -> overriddenOwner.interpret(newStates)
            superQualifier.superType == null -> calculateBuiltIns(overriddenOwner, newStates)
            else -> calculateOverridden(overriddenOwner, newStates)
        }.apply { data.pushReturnValue(newStates) }
    }

    private suspend fun calculateBuiltIns(irFunction: IrFunction, data: Frame): Code {
        val descriptor = irFunction.descriptor
        val methodName = descriptor.name.asString()
        val args = data.getAll().map { it.state }

        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { it.original.type }
        val argsValues = args.map { (it as? Complex)?.instance ?: (it as Primitive<*>).value }
        val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })

        val result = when (argsType.size) {
            1 -> {
                val function = unaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in unary map")
                function.invoke(argsValues.first())
            }
            2 -> {
                val function = binaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in binary map")
                when (methodName) {
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType, data)
                    else -> function.invoke(argsValues[0], argsValues[1])
                }
            }
            3 -> {
                val function = ternaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in ternary map")
                function.invoke(argsValues[0], argsValues[1], argsValues[2])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
        }

        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
        return Code.NEXT
    }

    private suspend fun calculateRangeTo(type: IrType, data: Frame): Code {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(primitive.type))
        }

        val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
        val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

        val code = constructorCall.interpret(newFrame)
        data.pushReturnValue(newFrame)
        return code
    }

    private suspend fun interpretValueParameters(parametersContainer: IrMemberAccessExpression, data: Frame): Code {
        for (i in (parametersContainer.valueArgumentsCount - 1) downTo 0) {
            val code = parametersContainer.getValueArgument(i)?.interpret(data) ?: Code.NEXT
            if (code != Code.NEXT) return code
        }
        return Code.NEXT
    }

    private suspend fun interpretCall(expression: IrCall, data: Frame): Code {
        val newFrame = InterpreterFrame()

        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        val dispatchReceiver = rawDispatchReceiver?.let { data.popReturnValue() }
        val irFunctionReceiver = if (expression.superQualifierSymbol == null) dispatchReceiver else (dispatchReceiver as Complex).superType
        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        val irFunction = irFunctionReceiver?.getIrFunction(expression.symbol.descriptor) ?: expression.symbol.owner
        irFunctionReceiver?.let { receiverState ->
            // dispatch receiver is null if irFunction is lambda and so there is no need to add it as variable in frame
            irFunction.symbol.getDispatchReceiver()?.let { newFrame.addVar(Variable(it, receiverState)) }
        }

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        val extensionReceiver = rawExtensionReceiver?.let { data.popReturnValue() }
        extensionReceiver?.let { newFrame.addVar(Variable(irFunction.symbol.getExtensionReceiver()!!, it)) }

        // if irFunction is lambda and it has receiver, then first descriptor must be taken from extension receiver
        val receiverAsFirstArgument = if (irFunction.isLocalLambda()) listOfNotNull(irFunction.symbol.getExtensionReceiver()) else listOf()
        val valueParametersDescriptors = receiverAsFirstArgument + irFunction.descriptor.valueParameters
        interpretValueParameters(expression, data).also { if (it != Code.NEXT) return it }
        newFrame.addAll(valueParametersDescriptors.map { Variable(it, data.popReturnValue()) })

        val isWrapper = (dispatchReceiver is Wrapper && rawExtensionReceiver == null) ||
                (extensionReceiver is Wrapper && rawDispatchReceiver == null)
        val code = when {
            isWrapper -> ((dispatchReceiver ?: extensionReceiver) as Wrapper).getMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be before fake overridden check
            irFunction.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
            irFunction.body == null -> calculateBuiltIns(irFunction, newFrame)
            else -> irFunction.interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private suspend fun interpretConstructor(constructorCall: IrFunctionAccessExpression, data: Frame): Code {
        interpretValueParameters(constructorCall, data).also { if (it != Code.NEXT) return it }
        val valueParameters = constructorCall.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }.toMutableList()
        val newFrame = InterpreterFrame(valueParameters)

        val owner = constructorCall.symbol.owner
        val parent = owner.parent as IrClass
        if (parent.hasAnnotation(evaluateIntrinsicAnnotation)) {
            return Wrapper.getConstructorMethod(owner).invokeMethod(owner, newFrame).apply { data.pushReturnValue(newFrame) }
        }

        if (parent.defaultType.isArray() || parent.defaultType.isPrimitiveArray()) {
            // array constructor doesn't have body so must be treated separately
            // here we must except only one possible constructor, with one value parameter size
            // the other one, with lambda, is transforming into ir returnable block
            val arrayConstructor = irBuiltIns.primitiveArrays.first().constructors.single { it.owner.valueParameters.size == 2 }
            val sizeDescriptor = arrayConstructor.owner.valueParameters.single { it.name.asString() == "size" }.descriptor
            val size = (newFrame.getVariableState(sizeDescriptor) as Primitive<Int>).value

            val array = when (parent.defaultType.getFqName()) {
                "kotlin.ByteArray" -> Primitive(ByteArray(size), parent.defaultType)
                "kotlin.CharArray" -> Primitive(CharArray(size), parent.defaultType)
                "kotlin.ShortArray" -> Primitive(ShortArray(size), parent.defaultType)
                "kotlin.IntArray" -> Primitive(IntArray(size), parent.defaultType)
                "kotlin.LongArray" -> Primitive(LongArray(size), parent.defaultType)
                "kotlin.FloatArray" -> Primitive(FloatArray(size), parent.defaultType)
                "kotlin.DoubleArray" -> Primitive(DoubleArray(size), parent.defaultType)
                "kotlin.BooleanArray" -> Primitive(BooleanArray(size), parent.defaultType)
                else -> throw AssertionError("Unsupported array class ${parent.defaultType.getFqName()}")
            }

            data.pushReturnValue(array)
            return Code.NEXT
        }

        val state = Common(parent, mutableListOf())
        newFrame.addVar(Variable(constructorCall.getThisAsReceiver(), state)) //used to set up fields in body
        val code = constructorCall.getBody()?.interpret(newFrame) ?: Code.NEXT
        state.superType = newFrame.popReturnValue() as Complex
        data.pushReturnValue(state)
        return code
    }

    private suspend fun interpretConstructorCall(constructorCall: IrConstructorCall, data: Frame): Code {
        return interpretConstructor(constructorCall, data).apply {
            // constructor can return primitive object; fot example, when create array by constructor
            (data.peekReturnValue() as? Complex)?.let { it.setInstanceRecursive(it) }
        }
    }

    private suspend fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall, data: Frame): Code {
        if (delegatingConstructorCall.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            val anyAsStateObject = Common(irBuiltIns.anyClass.owner, mutableListOf())
            data.pushReturnValue(anyAsStateObject)
            return Code.NEXT
        }

        return interpretConstructor(delegatingConstructorCall, data)
    }

    private suspend fun interpretConst(expression: IrConst<*>, data: Frame): Code {
        data.pushReturnValue(expression.toPrimitive())
        return Code.NEXT
    }

    private suspend fun interpretStatements(statements: List<IrStatement>, data: Frame): Code {
        val newFrame = data.copy()

        var code = Code.NEXT
        val iterator = statements.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private suspend fun interpretBlock(block: IrBlock, data: Frame): Code {
        if (block.isLambdaFunction()) {
            val irLambdaFunction = block.statements.filterIsInstance<IrFunctionReference>().first()
            data.pushReturnValue(Lambda(irLambdaFunction.symbol.owner, irLambdaFunction.type.classOrNull!!.owner))
            return Code.NEXT
        }

        val inlineFun = (block as? IrReturnableBlock)?.inlineFunctionSymbol?.owner
        if (inlineFun?.hasAnnotation(evaluateIntrinsicAnnotation) == true) {
            val newFrame = data.copy()
            block.statements.filterIsInstance<IrVariable>().forEach { variable ->
                variable.initializer!!.interpret(newFrame).also { if (it != Code.NEXT) return it }
                newFrame.addVar(Variable(variable.descriptor, newFrame.popReturnValue()))
            }

            val originalVarToInline = ParallelVisitorForInlineFun()
                .apply { this.visitElement(inlineFun.body!!.statements.first(), block.statements.last()) }
                .originalVarToInline

            val frameForWrapper = InterpreterFrame()
            (listOfNotNull(inlineFun.extensionReceiverParameter, inlineFun.dispatchReceiverParameter) + inlineFun.valueParameters)
                .map { it.descriptor }
                .forEach { frameForWrapper.addVar(Variable(it, newFrame.getVariableState(originalVarToInline[it]!!))) }

            return Wrapper.getStaticMethod(inlineFun)
                .invokeMethod(inlineFun, frameForWrapper)
                .apply { data.pushReturnValue(frameForWrapper) }
        }

        return interpretStatements(block.statements, data)
    }

    private suspend fun interpretBody(body: IrBody, data: Frame): Code {
        return interpretStatements(body.statements, data)
    }

    private suspend fun interpretReturn(expression: IrReturn, data: Frame): Code {
        val code = expression.value.interpret(data)
        return if (code == Code.NEXT) Code.RETURN else code
    }

    private suspend fun interpretWhile(expression: IrWhileLoop, data: Frame): Code {
        var code = Code.NEXT
        while (code == Code.NEXT) {
            code = expression.condition.interpret(data)
            if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
                code = expression.body?.interpret(data) ?: Code.NEXT
            } else {
                break
            }
        }
        return code
    }

    private suspend fun interpretWhen(expression: IrWhen, data: Frame): Code {
        var code = Code.NEXT
        val iterator = expression.branches.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(data)
        }
        return code
    }

    private suspend fun interpretBranch(expression: IrBranch, data: Frame): Code {
        var code = expression.condition.interpret(data)
        if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
            code = expression.result.interpret(data)
            if (code == Code.NEXT) return Code.BREAK_WHEN
        }
        return code
    }

    private suspend fun interpretBreak(breakStatement: IrBreak, data: Frame): Code {
        return Code.BREAK_LOOP.apply { info = breakStatement.label ?: "" }
    }

    private suspend fun interpretContinue(continueStatement: IrContinue, data: Frame): Code {
        return Code.CONTINUE.apply { info = continueStatement.label ?: "" }
    }

    private suspend fun interpretSetField(expression: IrSetField, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private suspend fun interpretGetField(expression: IrGetField, data: Frame): Code {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
        val result = receiver?.let { data.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
        if (result == null) {
            return expression.symbol.owner.initializer?.expression?.interpret(data) ?: Code.NEXT
        }
        data.pushReturnValue(result)
        return Code.NEXT
    }

    private suspend fun interpretGetValue(expression: IrGetValue, data: Frame): Code {
        data.pushReturnValue(data.getVariableState(expression.symbol.descriptor).copy())
        return Code.NEXT
    }

    private suspend fun interpretVariable(expression: IrVariable, data: Frame): Code {
        val code = expression.initializer?.interpret(data)
        if (code != Code.NEXT) return code ?: Code.NEXT
        data.addVar(Variable(expression.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private suspend fun interpretSetVariable(expression: IrSetVariable, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        if (data.contains(expression.symbol.descriptor)) {
            val variable = data.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, data.popReturnValue()))
        } else {
            data.addVar(Variable(expression.symbol.descriptor, data.popReturnValue()))
        }
        return Code.NEXT
    }

    private suspend fun interpretGetObjectValue(expression: IrGetObjectValue, data: Frame): Code {
        val objectState = Common(expression.symbol.owner, mutableListOf())
        val newFrame = data.copy().apply { addVar(Variable(expression.symbol.descriptor.thisAsReceiverParameter, objectState)) }

        val constructor = expression.symbol.owner.declarations.single { it is IrConstructor } as IrConstructor
        val constructorBody = constructor.body?.statements?.get(1) as? IrBlock
        val irAnnotatedSetFields = constructorBody?.statements?.filterIsInstance<IrSetField>()
            ?.filter { it.symbol.owner.correspondingPropertySymbol!!.owner.hasAnnotation(compileTimeAnnotation) }
        irAnnotatedSetFields?.forEach { irSetField -> irSetField.interpret(newFrame).also { if (it != Code.NEXT) return it } }

        data.pushReturnValue(objectState)
        return Code.NEXT
    }

    private suspend fun interpretGetEnumValue(expression: IrGetEnumValue, data: Frame): Code {
        return interpretEnumEntry(expression.symbol.owner, data)
    }

    private suspend fun interpretEnumEntry(enumEntry: IrEnumEntry, data: Frame): Code {
        val enumSignature = Pair(enumEntry.parentAsClass, enumEntry.name.asString())
        mapOfEnums[enumSignature]?.let {
            data.pushReturnValue(it)
            return Code.NEXT
        }

        val code = enumEntry.initializerExpression?.interpret(data)?.also { if (it != Code.NEXT) return it }
        code ?: throw AssertionError("Initializer at enum entry ${enumEntry.fqNameWhenAvailable} is null")
        mapOfEnums[enumSignature] = data.peekReturnValue() as Common
        return code
    }

    private suspend fun interpretTypeOperatorCall(expression: IrTypeOperatorCall, data: Frame): Code {
        val code = expression.argument.interpret(data).also { if (it != Code.NEXT) return it }

        return when (expression.operator) {
            // coercion to unit means that return value isn't used
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> code
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    val convertibleClassName = data.popReturnValue().irClass.fqNameForIrSerialization
                    val castClassName = expression.type.classOrNull?.owner?.fqNameForIrSerialization
                    val message = "$convertibleClassName cannot be cast to $castClassName"
                    data.pushReturnValue(ExceptionState(ClassCastException(message), classCastException, stackTrace))
                    Code.EXCEPTION
                } else {
                    code
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    data.popReturnValue()
                    data.pushReturnValue(null.toState(irBuiltIns.nothingType))
                }
                code
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = data.popReturnValue().irClass.defaultType.isSubtypeOf(expression.typeOperand, irBuiltIns)
                data.pushReturnValue(isInstance.toState(irBuiltIns.nothingType))
                code
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = data.popReturnValue().irClass.defaultType.isSubtypeOf(expression.typeOperand, irBuiltIns)
                data.pushReturnValue((!isInstance).toState(irBuiltIns.nothingType))
                code
            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun interpretVararg(expression: IrVararg, data: Frame): Code {
        val args = expression.elements.map {
            it.interpret(data).apply { if (this != Code.NEXT) return this }
            data.popReturnValue()
        }
        val type = expression.type
        val array = when (type.getFqName()) {
            "kotlin.ByteArray" -> Primitive(args.map { (it as Primitive<Byte>).value }.toByteArray(), type)
            "kotlin.CharArray" -> Primitive(args.map { (it as Primitive<Char>).value }.toCharArray(), type)
            "kotlin.ShortArray" -> Primitive(args.map { (it as Primitive<Short>).value }.toShortArray(), type)
            "kotlin.IntArray" -> Primitive(args.map { (it as Primitive<Int>).value }.toIntArray(), type)
            "kotlin.LongArray" -> Primitive(args.map { (it as Primitive<Long>).value }.toLongArray(), type)
            "kotlin.FloatArray" -> Primitive(args.map { (it as Primitive<Float>).value }.toFloatArray(), type)
            "kotlin.DoubleArray" -> Primitive(args.map { (it as Primitive<Double>).value }.toDoubleArray(), type)
            "kotlin.BooleanArray" -> Primitive(args.map { (it as Primitive<Boolean>).value }.toBooleanArray(), type)
            else -> Primitive<Array<*>>(args.map { (it as? Wrapper)?.value ?: (it as? Primitive<*>)?.value ?: it }.toTypedArray(), type)
        }
        data.pushReturnValue(array)

        return Code.NEXT
    }

    private suspend fun interpretTry(expression: IrTry, data: Frame): Code {
        var code = expression.tryResult.interpret(data)
        if (code == Code.EXCEPTION) {
            val exception = data.peekReturnValue() as ExceptionState
            for (catchBlock in expression.catches) {
                if (exception.isSubtypeOf(catchBlock.catchParameter.type.classOrNull!!.owner)) {
                    code = catchBlock.interpret(data)
                    break
                }
            }
        }
        // TODO check flow correctness; should I return finally result code if in catch there was an exception?
        return expression.finallyExpression?.interpret(data) ?: code
    }

    private suspend fun interpretCatch(expression: IrCatch, data: Frame): Code {
        val newFrame = InterpreterFrame(data.getAll().toMutableList())
        newFrame.addVar(Variable(expression.parameter, data.popReturnValue()))
        return expression.result.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private suspend fun interpretThrow(expression: IrThrow, data: Frame): Code {
        expression.value.interpret(data).also { if (it != Code.NEXT) return it }
        when (val exception = data.popReturnValue()) {
            is Common -> data.pushReturnValue(ExceptionState(exception, stackTrace))
            is Wrapper -> data.pushReturnValue(ExceptionState(exception, stackTrace))
            is ExceptionState -> data.pushReturnValue(exception)
            else -> throw AssertionError("${exception::class} cannot be used as exception state")
        }
        return Code.EXCEPTION
    }

    private suspend fun interpretStringConcatenation(expression: IrStringConcatenation, data: Frame): Code {
        val result = StringBuilder()
        expression.arguments.forEach {
            it.interpret(data).also { code -> if (code != Code.NEXT) return code }
            result.append(
                when (val returnValue = data.popReturnValue()) {
                    is Primitive<*> -> returnValue.value.toString()
                    is Wrapper -> returnValue.value.toString()
                    is Common -> {
                        val toStringFun = returnValue.getToStringFunction()
                        val newFrame = InterpreterFrame(mutableListOf(Variable(toStringFun.symbol.getReceiver()!!, returnValue)))
                        val code = toStringFun.body?.let { toStringFun.interpret(newFrame) } ?: calculateOverridden(toStringFun, newFrame)
                        if (code != Code.NEXT) return code
                        (newFrame.popReturnValue() as Primitive<*>).value.toString()
                    }
                    else -> throw AssertionError("$returnValue cannot be used in StringConcatenation expression")
                }
            )
        }

        data.pushReturnValue(result.toString().toState(expression.type))
        return Code.NEXT
    }
}