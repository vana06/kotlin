/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.interpreter.builtins

import org.jetbrains.kotlin.backend.common.interpreter.state.*

/** This file is generated by org.jetbrains.kotlin.backend.common.interpreter.builtins.GenerateBuiltInsMap.generateMap(). DO NOT MODIFY MANUALLY */

val unaryFunctions = mapOf<CompileTimeFunction, Function1<Any?, Any?>>(
    unaryOperation<Boolean>("hashCode", "Boolean") { a -> a.hashCode() },
    unaryOperation<Boolean>("not", "Boolean") { a -> a.not() },
    unaryOperation<Boolean>("toString", "Boolean") { a -> a.toString() },
    unaryOperation<Char>("dec", "Char") { a -> a.dec() },
    unaryOperation<Char>("hashCode", "Char") { a -> a.hashCode() },
    unaryOperation<Char>("inc", "Char") { a -> a.inc() },
    unaryOperation<Char>("toByte", "Char") { a -> a.toByte() },
    unaryOperation<Char>("toChar", "Char") { a -> a.toChar() },
    unaryOperation<Char>("toDouble", "Char") { a -> a.toDouble() },
    unaryOperation<Char>("toFloat", "Char") { a -> a.toFloat() },
    unaryOperation<Char>("toInt", "Char") { a -> a.toInt() },
    unaryOperation<Char>("toLong", "Char") { a -> a.toLong() },
    unaryOperation<Char>("toShort", "Char") { a -> a.toShort() },
    unaryOperation<Char>("toString", "Char") { a -> a.toString() },
    unaryOperation<Byte>("dec", "Byte") { a -> a.dec() },
    unaryOperation<Byte>("hashCode", "Byte") { a -> a.hashCode() },
    unaryOperation<Byte>("inc", "Byte") { a -> a.inc() },
    unaryOperation<Byte>("toByte", "Byte") { a -> a.toByte() },
    unaryOperation<Byte>("toChar", "Byte") { a -> a.toChar() },
    unaryOperation<Byte>("toDouble", "Byte") { a -> a.toDouble() },
    unaryOperation<Byte>("toFloat", "Byte") { a -> a.toFloat() },
    unaryOperation<Byte>("toInt", "Byte") { a -> a.toInt() },
    unaryOperation<Byte>("toLong", "Byte") { a -> a.toLong() },
    unaryOperation<Byte>("toShort", "Byte") { a -> a.toShort() },
    unaryOperation<Byte>("toString", "Byte") { a -> a.toString() },
    unaryOperation<Byte>("unaryMinus", "Byte") { a -> a.unaryMinus() },
    unaryOperation<Byte>("unaryPlus", "Byte") { a -> a.unaryPlus() },
    unaryOperation<Short>("dec", "Short") { a -> a.dec() },
    unaryOperation<Short>("hashCode", "Short") { a -> a.hashCode() },
    unaryOperation<Short>("inc", "Short") { a -> a.inc() },
    unaryOperation<Short>("toByte", "Short") { a -> a.toByte() },
    unaryOperation<Short>("toChar", "Short") { a -> a.toChar() },
    unaryOperation<Short>("toDouble", "Short") { a -> a.toDouble() },
    unaryOperation<Short>("toFloat", "Short") { a -> a.toFloat() },
    unaryOperation<Short>("toInt", "Short") { a -> a.toInt() },
    unaryOperation<Short>("toLong", "Short") { a -> a.toLong() },
    unaryOperation<Short>("toShort", "Short") { a -> a.toShort() },
    unaryOperation<Short>("toString", "Short") { a -> a.toString() },
    unaryOperation<Short>("unaryMinus", "Short") { a -> a.unaryMinus() },
    unaryOperation<Short>("unaryPlus", "Short") { a -> a.unaryPlus() },
    unaryOperation<Int>("dec", "Int") { a -> a.dec() },
    unaryOperation<Int>("hashCode", "Int") { a -> a.hashCode() },
    unaryOperation<Int>("inc", "Int") { a -> a.inc() },
    unaryOperation<Int>("inv", "Int") { a -> a.inv() },
    unaryOperation<Int>("toByte", "Int") { a -> a.toByte() },
    unaryOperation<Int>("toChar", "Int") { a -> a.toChar() },
    unaryOperation<Int>("toDouble", "Int") { a -> a.toDouble() },
    unaryOperation<Int>("toFloat", "Int") { a -> a.toFloat() },
    unaryOperation<Int>("toInt", "Int") { a -> a.toInt() },
    unaryOperation<Int>("toLong", "Int") { a -> a.toLong() },
    unaryOperation<Int>("toShort", "Int") { a -> a.toShort() },
    unaryOperation<Int>("toString", "Int") { a -> a.toString() },
    unaryOperation<Int>("unaryMinus", "Int") { a -> a.unaryMinus() },
    unaryOperation<Int>("unaryPlus", "Int") { a -> a.unaryPlus() },
    unaryOperation<Float>("dec", "Float") { a -> a.dec() },
    unaryOperation<Float>("hashCode", "Float") { a -> a.hashCode() },
    unaryOperation<Float>("inc", "Float") { a -> a.inc() },
    unaryOperation<Float>("toByte", "Float") { a -> a.toByte() },
    unaryOperation<Float>("toChar", "Float") { a -> a.toChar() },
    unaryOperation<Float>("toDouble", "Float") { a -> a.toDouble() },
    unaryOperation<Float>("toFloat", "Float") { a -> a.toFloat() },
    unaryOperation<Float>("toInt", "Float") { a -> a.toInt() },
    unaryOperation<Float>("toLong", "Float") { a -> a.toLong() },
    unaryOperation<Float>("toShort", "Float") { a -> a.toShort() },
    unaryOperation<Float>("toString", "Float") { a -> a.toString() },
    unaryOperation<Float>("unaryMinus", "Float") { a -> a.unaryMinus() },
    unaryOperation<Float>("unaryPlus", "Float") { a -> a.unaryPlus() },
    unaryOperation<Long>("dec", "Long") { a -> a.dec() },
    unaryOperation<Long>("hashCode", "Long") { a -> a.hashCode() },
    unaryOperation<Long>("inc", "Long") { a -> a.inc() },
    unaryOperation<Long>("inv", "Long") { a -> a.inv() },
    unaryOperation<Long>("toByte", "Long") { a -> a.toByte() },
    unaryOperation<Long>("toChar", "Long") { a -> a.toChar() },
    unaryOperation<Long>("toDouble", "Long") { a -> a.toDouble() },
    unaryOperation<Long>("toFloat", "Long") { a -> a.toFloat() },
    unaryOperation<Long>("toInt", "Long") { a -> a.toInt() },
    unaryOperation<Long>("toLong", "Long") { a -> a.toLong() },
    unaryOperation<Long>("toShort", "Long") { a -> a.toShort() },
    unaryOperation<Long>("toString", "Long") { a -> a.toString() },
    unaryOperation<Long>("unaryMinus", "Long") { a -> a.unaryMinus() },
    unaryOperation<Long>("unaryPlus", "Long") { a -> a.unaryPlus() },
    unaryOperation<Double>("dec", "Double") { a -> a.dec() },
    unaryOperation<Double>("hashCode", "Double") { a -> a.hashCode() },
    unaryOperation<Double>("inc", "Double") { a -> a.inc() },
    unaryOperation<Double>("toByte", "Double") { a -> a.toByte() },
    unaryOperation<Double>("toChar", "Double") { a -> a.toChar() },
    unaryOperation<Double>("toDouble", "Double") { a -> a.toDouble() },
    unaryOperation<Double>("toFloat", "Double") { a -> a.toFloat() },
    unaryOperation<Double>("toInt", "Double") { a -> a.toInt() },
    unaryOperation<Double>("toLong", "Double") { a -> a.toLong() },
    unaryOperation<Double>("toShort", "Double") { a -> a.toShort() },
    unaryOperation<Double>("toString", "Double") { a -> a.toString() },
    unaryOperation<Double>("unaryMinus", "Double") { a -> a.unaryMinus() },
    unaryOperation<Double>("unaryPlus", "Double") { a -> a.unaryPlus() },
    unaryOperation<String>("length", "String") { a -> a.length },
    unaryOperation<String>("hashCode", "String") { a -> a.hashCode() },
    unaryOperation<String>("toString", "String") { a -> a.toString() },
    unaryOperation<BooleanArray>("size", "BooleanArray") { a -> a.size },
    unaryOperation<BooleanArray>("iterator", "BooleanArray") { a -> a.iterator() },
    unaryOperation<CharArray>("size", "CharArray") { a -> a.size },
    unaryOperation<CharArray>("iterator", "CharArray") { a -> a.iterator() },
    unaryOperation<ByteArray>("size", "ByteArray") { a -> a.size },
    unaryOperation<ByteArray>("iterator", "ByteArray") { a -> a.iterator() },
    unaryOperation<ShortArray>("size", "ShortArray") { a -> a.size },
    unaryOperation<ShortArray>("iterator", "ShortArray") { a -> a.iterator() },
    unaryOperation<IntArray>("size", "IntArray") { a -> a.size },
    unaryOperation<IntArray>("iterator", "IntArray") { a -> a.iterator() },
    unaryOperation<FloatArray>("size", "FloatArray") { a -> a.size },
    unaryOperation<FloatArray>("iterator", "FloatArray") { a -> a.iterator() },
    unaryOperation<LongArray>("size", "LongArray") { a -> a.size },
    unaryOperation<LongArray>("iterator", "LongArray") { a -> a.iterator() },
    unaryOperation<DoubleArray>("size", "DoubleArray") { a -> a.size },
    unaryOperation<DoubleArray>("iterator", "DoubleArray") { a -> a.iterator() },
    unaryOperation<Array<Any?>>("size", "Array<T>") { a -> a.size },
    unaryOperation<Array<Any?>>("iterator", "Array<T>") { a -> a.iterator() },
    unaryOperation<Any>("hashCode", "Any") { a -> a.hashCode() },
    unaryOperation<Any>("toString", "Any") { a -> a.defaultToString() },
    unaryOperation<Any?>("CHECK_NOT_NULL", "T0?") { a -> a!! },
    unaryOperation<ExceptionState>("message", "Throwable") { a -> a.getMessage() },
    unaryOperation<ExceptionState>("cause", "Throwable") { a -> a.getCause() }
)

val binaryFunctions = mapOf<CompileTimeFunction, Function2<Any?, Any?, Any?>>(
    binaryOperation<Boolean, Boolean>("and", "Boolean", "Boolean") { a, b -> a.and(b) },
    binaryOperation<Boolean, Boolean>("compareTo", "Boolean", "Boolean") { a, b -> a.compareTo(b) },
    binaryOperation<Boolean, Any?>("equals", "Boolean", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Boolean, Boolean>("or", "Boolean", "Boolean") { a, b -> a.or(b) },
    binaryOperation<Boolean, Boolean>("xor", "Boolean", "Boolean") { a, b -> a.xor(b) },
    binaryOperation<Char, Char>("compareTo", "Char", "Char") { a, b -> a.compareTo(b) },
    binaryOperation<Char, Any?>("equals", "Char", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Char, Char>("minus", "Char", "Char") { a, b -> a.minus(b) },
    binaryOperation<Char, Int>("minus", "Char", "Int") { a, b -> a.minus(b) },
    binaryOperation<Char, Int>("plus", "Char", "Int") { a, b -> a.plus(b) },
    binaryOperation<Char, Char>("rangeTo", "Char", "Char") { a, b -> a.rangeTo(b) },
    binaryOperation<Byte, Byte>("compareTo", "Byte", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Double>("compareTo", "Byte", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Float>("compareTo", "Byte", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Int>("compareTo", "Byte", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Long>("compareTo", "Byte", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Short>("compareTo", "Byte", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Byte, Byte>("div", "Byte", "Byte") { a, b -> a.div(b) },
    binaryOperation<Byte, Double>("div", "Byte", "Double") { a, b -> a.div(b) },
    binaryOperation<Byte, Float>("div", "Byte", "Float") { a, b -> a.div(b) },
    binaryOperation<Byte, Int>("div", "Byte", "Int") { a, b -> a.div(b) },
    binaryOperation<Byte, Long>("div", "Byte", "Long") { a, b -> a.div(b) },
    binaryOperation<Byte, Short>("div", "Byte", "Short") { a, b -> a.div(b) },
    binaryOperation<Byte, Any?>("equals", "Byte", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Byte, Byte>("minus", "Byte", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Byte, Double>("minus", "Byte", "Double") { a, b -> a.minus(b) },
    binaryOperation<Byte, Float>("minus", "Byte", "Float") { a, b -> a.minus(b) },
    binaryOperation<Byte, Int>("minus", "Byte", "Int") { a, b -> a.minus(b) },
    binaryOperation<Byte, Long>("minus", "Byte", "Long") { a, b -> a.minus(b) },
    binaryOperation<Byte, Short>("minus", "Byte", "Short") { a, b -> a.minus(b) },
    binaryOperation<Byte, Byte>("plus", "Byte", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Byte, Double>("plus", "Byte", "Double") { a, b -> a.plus(b) },
    binaryOperation<Byte, Float>("plus", "Byte", "Float") { a, b -> a.plus(b) },
    binaryOperation<Byte, Int>("plus", "Byte", "Int") { a, b -> a.plus(b) },
    binaryOperation<Byte, Long>("plus", "Byte", "Long") { a, b -> a.plus(b) },
    binaryOperation<Byte, Short>("plus", "Byte", "Short") { a, b -> a.plus(b) },
    binaryOperation<Byte, Byte>("rangeTo", "Byte", "Byte") { a, b -> a.rangeTo(b) },
    binaryOperation<Byte, Int>("rangeTo", "Byte", "Int") { a, b -> a.rangeTo(b) },
    binaryOperation<Byte, Long>("rangeTo", "Byte", "Long") { a, b -> a.rangeTo(b) },
    binaryOperation<Byte, Short>("rangeTo", "Byte", "Short") { a, b -> a.rangeTo(b) },
    binaryOperation<Byte, Byte>("rem", "Byte", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Byte, Double>("rem", "Byte", "Double") { a, b -> a.rem(b) },
    binaryOperation<Byte, Float>("rem", "Byte", "Float") { a, b -> a.rem(b) },
    binaryOperation<Byte, Int>("rem", "Byte", "Int") { a, b -> a.rem(b) },
    binaryOperation<Byte, Long>("rem", "Byte", "Long") { a, b -> a.rem(b) },
    binaryOperation<Byte, Short>("rem", "Byte", "Short") { a, b -> a.rem(b) },
    binaryOperation<Byte, Byte>("times", "Byte", "Byte") { a, b -> a.times(b) },
    binaryOperation<Byte, Double>("times", "Byte", "Double") { a, b -> a.times(b) },
    binaryOperation<Byte, Float>("times", "Byte", "Float") { a, b -> a.times(b) },
    binaryOperation<Byte, Int>("times", "Byte", "Int") { a, b -> a.times(b) },
    binaryOperation<Byte, Long>("times", "Byte", "Long") { a, b -> a.times(b) },
    binaryOperation<Byte, Short>("times", "Byte", "Short") { a, b -> a.times(b) },
    binaryOperation<Short, Byte>("compareTo", "Short", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Double>("compareTo", "Short", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Float>("compareTo", "Short", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Int>("compareTo", "Short", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Long>("compareTo", "Short", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Short>("compareTo", "Short", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Short, Byte>("div", "Short", "Byte") { a, b -> a.div(b) },
    binaryOperation<Short, Double>("div", "Short", "Double") { a, b -> a.div(b) },
    binaryOperation<Short, Float>("div", "Short", "Float") { a, b -> a.div(b) },
    binaryOperation<Short, Int>("div", "Short", "Int") { a, b -> a.div(b) },
    binaryOperation<Short, Long>("div", "Short", "Long") { a, b -> a.div(b) },
    binaryOperation<Short, Short>("div", "Short", "Short") { a, b -> a.div(b) },
    binaryOperation<Short, Any?>("equals", "Short", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Short, Byte>("minus", "Short", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Short, Double>("minus", "Short", "Double") { a, b -> a.minus(b) },
    binaryOperation<Short, Float>("minus", "Short", "Float") { a, b -> a.minus(b) },
    binaryOperation<Short, Int>("minus", "Short", "Int") { a, b -> a.minus(b) },
    binaryOperation<Short, Long>("minus", "Short", "Long") { a, b -> a.minus(b) },
    binaryOperation<Short, Short>("minus", "Short", "Short") { a, b -> a.minus(b) },
    binaryOperation<Short, Byte>("plus", "Short", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Short, Double>("plus", "Short", "Double") { a, b -> a.plus(b) },
    binaryOperation<Short, Float>("plus", "Short", "Float") { a, b -> a.plus(b) },
    binaryOperation<Short, Int>("plus", "Short", "Int") { a, b -> a.plus(b) },
    binaryOperation<Short, Long>("plus", "Short", "Long") { a, b -> a.plus(b) },
    binaryOperation<Short, Short>("plus", "Short", "Short") { a, b -> a.plus(b) },
    binaryOperation<Short, Byte>("rangeTo", "Short", "Byte") { a, b -> a.rangeTo(b) },
    binaryOperation<Short, Int>("rangeTo", "Short", "Int") { a, b -> a.rangeTo(b) },
    binaryOperation<Short, Long>("rangeTo", "Short", "Long") { a, b -> a.rangeTo(b) },
    binaryOperation<Short, Short>("rangeTo", "Short", "Short") { a, b -> a.rangeTo(b) },
    binaryOperation<Short, Byte>("rem", "Short", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Short, Double>("rem", "Short", "Double") { a, b -> a.rem(b) },
    binaryOperation<Short, Float>("rem", "Short", "Float") { a, b -> a.rem(b) },
    binaryOperation<Short, Int>("rem", "Short", "Int") { a, b -> a.rem(b) },
    binaryOperation<Short, Long>("rem", "Short", "Long") { a, b -> a.rem(b) },
    binaryOperation<Short, Short>("rem", "Short", "Short") { a, b -> a.rem(b) },
    binaryOperation<Short, Byte>("times", "Short", "Byte") { a, b -> a.times(b) },
    binaryOperation<Short, Double>("times", "Short", "Double") { a, b -> a.times(b) },
    binaryOperation<Short, Float>("times", "Short", "Float") { a, b -> a.times(b) },
    binaryOperation<Short, Int>("times", "Short", "Int") { a, b -> a.times(b) },
    binaryOperation<Short, Long>("times", "Short", "Long") { a, b -> a.times(b) },
    binaryOperation<Short, Short>("times", "Short", "Short") { a, b -> a.times(b) },
    binaryOperation<Int, Int>("and", "Int", "Int") { a, b -> a.and(b) },
    binaryOperation<Int, Byte>("compareTo", "Int", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Double>("compareTo", "Int", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Float>("compareTo", "Int", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Int>("compareTo", "Int", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Long>("compareTo", "Int", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Short>("compareTo", "Int", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Int, Byte>("div", "Int", "Byte") { a, b -> a.div(b) },
    binaryOperation<Int, Double>("div", "Int", "Double") { a, b -> a.div(b) },
    binaryOperation<Int, Float>("div", "Int", "Float") { a, b -> a.div(b) },
    binaryOperation<Int, Int>("div", "Int", "Int") { a, b -> a.div(b) },
    binaryOperation<Int, Long>("div", "Int", "Long") { a, b -> a.div(b) },
    binaryOperation<Int, Short>("div", "Int", "Short") { a, b -> a.div(b) },
    binaryOperation<Int, Any?>("equals", "Int", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Int, Byte>("minus", "Int", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Int, Double>("minus", "Int", "Double") { a, b -> a.minus(b) },
    binaryOperation<Int, Float>("minus", "Int", "Float") { a, b -> a.minus(b) },
    binaryOperation<Int, Int>("minus", "Int", "Int") { a, b -> a.minus(b) },
    binaryOperation<Int, Long>("minus", "Int", "Long") { a, b -> a.minus(b) },
    binaryOperation<Int, Short>("minus", "Int", "Short") { a, b -> a.minus(b) },
    binaryOperation<Int, Int>("or", "Int", "Int") { a, b -> a.or(b) },
    binaryOperation<Int, Byte>("plus", "Int", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Int, Double>("plus", "Int", "Double") { a, b -> a.plus(b) },
    binaryOperation<Int, Float>("plus", "Int", "Float") { a, b -> a.plus(b) },
    binaryOperation<Int, Int>("plus", "Int", "Int") { a, b -> a.plus(b) },
    binaryOperation<Int, Long>("plus", "Int", "Long") { a, b -> a.plus(b) },
    binaryOperation<Int, Short>("plus", "Int", "Short") { a, b -> a.plus(b) },
    binaryOperation<Int, Byte>("rangeTo", "Int", "Byte") { a, b -> a.rangeTo(b) },
    binaryOperation<Int, Int>("rangeTo", "Int", "Int") { a, b -> a.rangeTo(b) },
    binaryOperation<Int, Long>("rangeTo", "Int", "Long") { a, b -> a.rangeTo(b) },
    binaryOperation<Int, Short>("rangeTo", "Int", "Short") { a, b -> a.rangeTo(b) },
    binaryOperation<Int, Byte>("rem", "Int", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Int, Double>("rem", "Int", "Double") { a, b -> a.rem(b) },
    binaryOperation<Int, Float>("rem", "Int", "Float") { a, b -> a.rem(b) },
    binaryOperation<Int, Int>("rem", "Int", "Int") { a, b -> a.rem(b) },
    binaryOperation<Int, Long>("rem", "Int", "Long") { a, b -> a.rem(b) },
    binaryOperation<Int, Short>("rem", "Int", "Short") { a, b -> a.rem(b) },
    binaryOperation<Int, Int>("shl", "Int", "Int") { a, b -> a.shl(b) },
    binaryOperation<Int, Int>("shr", "Int", "Int") { a, b -> a.shr(b) },
    binaryOperation<Int, Byte>("times", "Int", "Byte") { a, b -> a.times(b) },
    binaryOperation<Int, Double>("times", "Int", "Double") { a, b -> a.times(b) },
    binaryOperation<Int, Float>("times", "Int", "Float") { a, b -> a.times(b) },
    binaryOperation<Int, Int>("times", "Int", "Int") { a, b -> a.times(b) },
    binaryOperation<Int, Long>("times", "Int", "Long") { a, b -> a.times(b) },
    binaryOperation<Int, Short>("times", "Int", "Short") { a, b -> a.times(b) },
    binaryOperation<Int, Int>("ushr", "Int", "Int") { a, b -> a.ushr(b) },
    binaryOperation<Int, Int>("xor", "Int", "Int") { a, b -> a.xor(b) },
    binaryOperation<Float, Byte>("compareTo", "Float", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Double>("compareTo", "Float", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Float>("compareTo", "Float", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Int>("compareTo", "Float", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Long>("compareTo", "Float", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Short>("compareTo", "Float", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Float, Byte>("div", "Float", "Byte") { a, b -> a.div(b) },
    binaryOperation<Float, Double>("div", "Float", "Double") { a, b -> a.div(b) },
    binaryOperation<Float, Float>("div", "Float", "Float") { a, b -> a.div(b) },
    binaryOperation<Float, Int>("div", "Float", "Int") { a, b -> a.div(b) },
    binaryOperation<Float, Long>("div", "Float", "Long") { a, b -> a.div(b) },
    binaryOperation<Float, Short>("div", "Float", "Short") { a, b -> a.div(b) },
    binaryOperation<Float, Any?>("equals", "Float", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Float, Byte>("minus", "Float", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Float, Double>("minus", "Float", "Double") { a, b -> a.minus(b) },
    binaryOperation<Float, Float>("minus", "Float", "Float") { a, b -> a.minus(b) },
    binaryOperation<Float, Int>("minus", "Float", "Int") { a, b -> a.minus(b) },
    binaryOperation<Float, Long>("minus", "Float", "Long") { a, b -> a.minus(b) },
    binaryOperation<Float, Short>("minus", "Float", "Short") { a, b -> a.minus(b) },
    binaryOperation<Float, Byte>("plus", "Float", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Float, Double>("plus", "Float", "Double") { a, b -> a.plus(b) },
    binaryOperation<Float, Float>("plus", "Float", "Float") { a, b -> a.plus(b) },
    binaryOperation<Float, Int>("plus", "Float", "Int") { a, b -> a.plus(b) },
    binaryOperation<Float, Long>("plus", "Float", "Long") { a, b -> a.plus(b) },
    binaryOperation<Float, Short>("plus", "Float", "Short") { a, b -> a.plus(b) },
    binaryOperation<Float, Byte>("rem", "Float", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Float, Double>("rem", "Float", "Double") { a, b -> a.rem(b) },
    binaryOperation<Float, Float>("rem", "Float", "Float") { a, b -> a.rem(b) },
    binaryOperation<Float, Int>("rem", "Float", "Int") { a, b -> a.rem(b) },
    binaryOperation<Float, Long>("rem", "Float", "Long") { a, b -> a.rem(b) },
    binaryOperation<Float, Short>("rem", "Float", "Short") { a, b -> a.rem(b) },
    binaryOperation<Float, Byte>("times", "Float", "Byte") { a, b -> a.times(b) },
    binaryOperation<Float, Double>("times", "Float", "Double") { a, b -> a.times(b) },
    binaryOperation<Float, Float>("times", "Float", "Float") { a, b -> a.times(b) },
    binaryOperation<Float, Int>("times", "Float", "Int") { a, b -> a.times(b) },
    binaryOperation<Float, Long>("times", "Float", "Long") { a, b -> a.times(b) },
    binaryOperation<Float, Short>("times", "Float", "Short") { a, b -> a.times(b) },
    binaryOperation<Long, Long>("and", "Long", "Long") { a, b -> a.and(b) },
    binaryOperation<Long, Byte>("compareTo", "Long", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Double>("compareTo", "Long", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Float>("compareTo", "Long", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Int>("compareTo", "Long", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Long>("compareTo", "Long", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Short>("compareTo", "Long", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Long, Byte>("div", "Long", "Byte") { a, b -> a.div(b) },
    binaryOperation<Long, Double>("div", "Long", "Double") { a, b -> a.div(b) },
    binaryOperation<Long, Float>("div", "Long", "Float") { a, b -> a.div(b) },
    binaryOperation<Long, Int>("div", "Long", "Int") { a, b -> a.div(b) },
    binaryOperation<Long, Long>("div", "Long", "Long") { a, b -> a.div(b) },
    binaryOperation<Long, Short>("div", "Long", "Short") { a, b -> a.div(b) },
    binaryOperation<Long, Any?>("equals", "Long", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Long, Byte>("minus", "Long", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Long, Double>("minus", "Long", "Double") { a, b -> a.minus(b) },
    binaryOperation<Long, Float>("minus", "Long", "Float") { a, b -> a.minus(b) },
    binaryOperation<Long, Int>("minus", "Long", "Int") { a, b -> a.minus(b) },
    binaryOperation<Long, Long>("minus", "Long", "Long") { a, b -> a.minus(b) },
    binaryOperation<Long, Short>("minus", "Long", "Short") { a, b -> a.minus(b) },
    binaryOperation<Long, Long>("or", "Long", "Long") { a, b -> a.or(b) },
    binaryOperation<Long, Byte>("plus", "Long", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Long, Double>("plus", "Long", "Double") { a, b -> a.plus(b) },
    binaryOperation<Long, Float>("plus", "Long", "Float") { a, b -> a.plus(b) },
    binaryOperation<Long, Int>("plus", "Long", "Int") { a, b -> a.plus(b) },
    binaryOperation<Long, Long>("plus", "Long", "Long") { a, b -> a.plus(b) },
    binaryOperation<Long, Short>("plus", "Long", "Short") { a, b -> a.plus(b) },
    binaryOperation<Long, Byte>("rangeTo", "Long", "Byte") { a, b -> a.rangeTo(b) },
    binaryOperation<Long, Int>("rangeTo", "Long", "Int") { a, b -> a.rangeTo(b) },
    binaryOperation<Long, Long>("rangeTo", "Long", "Long") { a, b -> a.rangeTo(b) },
    binaryOperation<Long, Short>("rangeTo", "Long", "Short") { a, b -> a.rangeTo(b) },
    binaryOperation<Long, Byte>("rem", "Long", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Long, Double>("rem", "Long", "Double") { a, b -> a.rem(b) },
    binaryOperation<Long, Float>("rem", "Long", "Float") { a, b -> a.rem(b) },
    binaryOperation<Long, Int>("rem", "Long", "Int") { a, b -> a.rem(b) },
    binaryOperation<Long, Long>("rem", "Long", "Long") { a, b -> a.rem(b) },
    binaryOperation<Long, Short>("rem", "Long", "Short") { a, b -> a.rem(b) },
    binaryOperation<Long, Int>("shl", "Long", "Int") { a, b -> a.shl(b) },
    binaryOperation<Long, Int>("shr", "Long", "Int") { a, b -> a.shr(b) },
    binaryOperation<Long, Byte>("times", "Long", "Byte") { a, b -> a.times(b) },
    binaryOperation<Long, Double>("times", "Long", "Double") { a, b -> a.times(b) },
    binaryOperation<Long, Float>("times", "Long", "Float") { a, b -> a.times(b) },
    binaryOperation<Long, Int>("times", "Long", "Int") { a, b -> a.times(b) },
    binaryOperation<Long, Long>("times", "Long", "Long") { a, b -> a.times(b) },
    binaryOperation<Long, Short>("times", "Long", "Short") { a, b -> a.times(b) },
    binaryOperation<Long, Int>("ushr", "Long", "Int") { a, b -> a.ushr(b) },
    binaryOperation<Long, Long>("xor", "Long", "Long") { a, b -> a.xor(b) },
    binaryOperation<Double, Byte>("compareTo", "Double", "Byte") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Double>("compareTo", "Double", "Double") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Float>("compareTo", "Double", "Float") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Int>("compareTo", "Double", "Int") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Long>("compareTo", "Double", "Long") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Short>("compareTo", "Double", "Short") { a, b -> a.compareTo(b) },
    binaryOperation<Double, Byte>("div", "Double", "Byte") { a, b -> a.div(b) },
    binaryOperation<Double, Double>("div", "Double", "Double") { a, b -> a.div(b) },
    binaryOperation<Double, Float>("div", "Double", "Float") { a, b -> a.div(b) },
    binaryOperation<Double, Int>("div", "Double", "Int") { a, b -> a.div(b) },
    binaryOperation<Double, Long>("div", "Double", "Long") { a, b -> a.div(b) },
    binaryOperation<Double, Short>("div", "Double", "Short") { a, b -> a.div(b) },
    binaryOperation<Double, Any?>("equals", "Double", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Double, Byte>("minus", "Double", "Byte") { a, b -> a.minus(b) },
    binaryOperation<Double, Double>("minus", "Double", "Double") { a, b -> a.minus(b) },
    binaryOperation<Double, Float>("minus", "Double", "Float") { a, b -> a.minus(b) },
    binaryOperation<Double, Int>("minus", "Double", "Int") { a, b -> a.minus(b) },
    binaryOperation<Double, Long>("minus", "Double", "Long") { a, b -> a.minus(b) },
    binaryOperation<Double, Short>("minus", "Double", "Short") { a, b -> a.minus(b) },
    binaryOperation<Double, Byte>("plus", "Double", "Byte") { a, b -> a.plus(b) },
    binaryOperation<Double, Double>("plus", "Double", "Double") { a, b -> a.plus(b) },
    binaryOperation<Double, Float>("plus", "Double", "Float") { a, b -> a.plus(b) },
    binaryOperation<Double, Int>("plus", "Double", "Int") { a, b -> a.plus(b) },
    binaryOperation<Double, Long>("plus", "Double", "Long") { a, b -> a.plus(b) },
    binaryOperation<Double, Short>("plus", "Double", "Short") { a, b -> a.plus(b) },
    binaryOperation<Double, Byte>("rem", "Double", "Byte") { a, b -> a.rem(b) },
    binaryOperation<Double, Double>("rem", "Double", "Double") { a, b -> a.rem(b) },
    binaryOperation<Double, Float>("rem", "Double", "Float") { a, b -> a.rem(b) },
    binaryOperation<Double, Int>("rem", "Double", "Int") { a, b -> a.rem(b) },
    binaryOperation<Double, Long>("rem", "Double", "Long") { a, b -> a.rem(b) },
    binaryOperation<Double, Short>("rem", "Double", "Short") { a, b -> a.rem(b) },
    binaryOperation<Double, Byte>("times", "Double", "Byte") { a, b -> a.times(b) },
    binaryOperation<Double, Double>("times", "Double", "Double") { a, b -> a.times(b) },
    binaryOperation<Double, Float>("times", "Double", "Float") { a, b -> a.times(b) },
    binaryOperation<Double, Int>("times", "Double", "Int") { a, b -> a.times(b) },
    binaryOperation<Double, Long>("times", "Double", "Long") { a, b -> a.times(b) },
    binaryOperation<Double, Short>("times", "Double", "Short") { a, b -> a.times(b) },
    binaryOperation<String, String>("compareTo", "String", "String") { a, b -> a.compareTo(b) },
    binaryOperation<String, Any?>("equals", "String", "Any?") { a, b -> a.equals(b) },
    binaryOperation<String, Int>("get", "String", "Int") { a, b -> a.get(b) },
    binaryOperation<String, Any?>("plus", "String", "Any?") { a, b -> a.plus(b) },
    binaryOperation<BooleanArray, Int>("get", "BooleanArray", "Int") { a, b -> a.get(b) },
    binaryOperation<CharArray, Int>("get", "CharArray", "Int") { a, b -> a.get(b) },
    binaryOperation<ByteArray, Int>("get", "ByteArray", "Int") { a, b -> a.get(b) },
    binaryOperation<ShortArray, Int>("get", "ShortArray", "Int") { a, b -> a.get(b) },
    binaryOperation<IntArray, Int>("get", "IntArray", "Int") { a, b -> a.get(b) },
    binaryOperation<FloatArray, Int>("get", "FloatArray", "Int") { a, b -> a.get(b) },
    binaryOperation<LongArray, Int>("get", "LongArray", "Int") { a, b -> a.get(b) },
    binaryOperation<DoubleArray, Int>("get", "DoubleArray", "Int") { a, b -> a.get(b) },
    binaryOperation<Array<Any?>, Int>("get", "Array<T>", "Int") { a, b -> a.get(b) },
    binaryOperation<Any, Any?>("equals", "Any", "Any?") { a, b -> a.equals(b) },
    binaryOperation<Char, Char>("less", "Char", "Char") { a, b -> a < b },
    binaryOperation<Byte, Byte>("less", "Byte", "Byte") { a, b -> a < b },
    binaryOperation<Short, Short>("less", "Short", "Short") { a, b -> a < b },
    binaryOperation<Int, Int>("less", "Int", "Int") { a, b -> a < b },
    binaryOperation<Float, Float>("less", "Float", "Float") { a, b -> a < b },
    binaryOperation<Long, Long>("less", "Long", "Long") { a, b -> a < b },
    binaryOperation<Double, Double>("less", "Double", "Double") { a, b -> a < b },
    binaryOperation<Char, Char>("lessOrEqual", "Char", "Char") { a, b -> a <= b },
    binaryOperation<Byte, Byte>("lessOrEqual", "Byte", "Byte") { a, b -> a <= b },
    binaryOperation<Short, Short>("lessOrEqual", "Short", "Short") { a, b -> a <= b },
    binaryOperation<Int, Int>("lessOrEqual", "Int", "Int") { a, b -> a <= b },
    binaryOperation<Float, Float>("lessOrEqual", "Float", "Float") { a, b -> a <= b },
    binaryOperation<Long, Long>("lessOrEqual", "Long", "Long") { a, b -> a <= b },
    binaryOperation<Double, Double>("lessOrEqual", "Double", "Double") { a, b -> a <= b },
    binaryOperation<Char, Char>("greater", "Char", "Char") { a, b -> a > b },
    binaryOperation<Byte, Byte>("greater", "Byte", "Byte") { a, b -> a > b },
    binaryOperation<Short, Short>("greater", "Short", "Short") { a, b -> a > b },
    binaryOperation<Int, Int>("greater", "Int", "Int") { a, b -> a > b },
    binaryOperation<Float, Float>("greater", "Float", "Float") { a, b -> a > b },
    binaryOperation<Long, Long>("greater", "Long", "Long") { a, b -> a > b },
    binaryOperation<Double, Double>("greater", "Double", "Double") { a, b -> a > b },
    binaryOperation<Char, Char>("greaterOrEqual", "Char", "Char") { a, b -> a >= b },
    binaryOperation<Byte, Byte>("greaterOrEqual", "Byte", "Byte") { a, b -> a >= b },
    binaryOperation<Short, Short>("greaterOrEqual", "Short", "Short") { a, b -> a >= b },
    binaryOperation<Int, Int>("greaterOrEqual", "Int", "Int") { a, b -> a >= b },
    binaryOperation<Float, Float>("greaterOrEqual", "Float", "Float") { a, b -> a >= b },
    binaryOperation<Long, Long>("greaterOrEqual", "Long", "Long") { a, b -> a >= b },
    binaryOperation<Double, Double>("greaterOrEqual", "Double", "Double") { a, b -> a >= b },
    binaryOperation<Any?, Any?>("EQEQ", "Any?", "Any?") { a, b -> a == b },
    binaryOperation<Any?, Any?>("EQEQEQ", "Any?", "Any?") { a, b -> a === b },
    binaryOperation<Float?, Float?>("ieee754equals", "Float?", "Float?") { a, b -> a == b },
    binaryOperation<Double?, Double?>("ieee754equals", "Double?", "Double?") { a, b -> a == b },
    binaryOperation<Boolean, Boolean>("ANDAND", "Boolean", "Boolean") { a, b -> a && b },
    binaryOperation<Boolean, Boolean>("OROR", "Boolean", "Boolean") { a, b -> a || b }
)

val ternaryFunctions = mapOf<CompileTimeFunction, Function3<Any?, Any?, Any?, Any?>>(
    ternaryOperation<String, Int, Int>("subSequence", "String", "Int", "Int") { a, b, c -> a.subSequence(b, c) },
    ternaryOperation<BooleanArray, Int, Boolean>("set", "BooleanArray", "Int", "Boolean") { a, b, c -> a.set(b, c) },
    ternaryOperation<CharArray, Int, Char>("set", "CharArray", "Int", "Char") { a, b, c -> a.set(b, c) },
    ternaryOperation<ByteArray, Int, Byte>("set", "ByteArray", "Int", "Byte") { a, b, c -> a.set(b, c) },
    ternaryOperation<ShortArray, Int, Short>("set", "ShortArray", "Int", "Short") { a, b, c -> a.set(b, c) },
    ternaryOperation<IntArray, Int, Int>("set", "IntArray", "Int", "Int") { a, b, c -> a.set(b, c) },
    ternaryOperation<FloatArray, Int, Float>("set", "FloatArray", "Int", "Float") { a, b, c -> a.set(b, c) },
    ternaryOperation<LongArray, Int, Long>("set", "LongArray", "Int", "Long") { a, b, c -> a.set(b, c) },
    ternaryOperation<DoubleArray, Int, Double>("set", "DoubleArray", "Int", "Double") { a, b, c -> a.set(b, c) },
    ternaryOperation<Array<Any?>, Int, Any?>("set", "Array<T>", "Int", "T") { a, b, c -> a.set(b, c) }
)

private fun Any.defaultToString(): String {
    return when (this) {
        is State -> "${this.irClass.name}@" + System.identityHashCode(this).toString(16).padStart(8, '0')
        else -> this.toString().replaceAfter("@", System.identityHashCode(this).toString(16).padStart(8, '0'))
    }
}

