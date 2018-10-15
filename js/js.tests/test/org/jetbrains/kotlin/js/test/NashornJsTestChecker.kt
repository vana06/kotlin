/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptRuntime
import org.junit.Assert
import java.io.Closeable
import javax.script.*

fun createScriptEngine(): ScriptEngine =
    // TODO use "-strict"
    NashornScriptEngineFactory().getScriptEngine("--language=es5", "--no-java", "--no-syntax-extensions")

fun ScriptEngine.overrideAsserter() {
    eval("this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(new this['kotlin-test'].kotlin.test.DefaultAsserter());")
}

@Deprecated("")
fun ScriptEngine.runTestFunction(
    testModuleName: String?,
    testPackageName: String?,
    testFunctionName: String,
    withModuleSystem: Boolean
): Any? {
    return runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem, getBindings(ScriptContext.ENGINE_SCOPE))
}

fun ScriptEngine.runTestFunction(
    testModuleName: String?,
    testPackageName: String?,
    testFunctionName: String,
    withModuleSystem: Boolean,
    bindings: Bindings
): Any? {
    val testModule =
        when {
            withModuleSystem ->
                eval(BasicBoxTest.KOTLIN_TEST_INTERNAL + ".require('" + testModuleName!! + "')", bindings)
            testModuleName === null ->
                eval("this", bindings)
            else ->
                get(testModuleName)
        }
    testModule as ScriptObjectMirror

    val testPackage =
        when {
            testPackageName === null ->
                testModule
            testPackageName.contains(".") ->
                testPackageName.split(".").fold(testModule) { p, part -> p[part] as ScriptObjectMirror }
            else ->
                testModule[testPackageName]!!
        }

    return (this as Invocable).invokeMethod(testPackage, testFunctionName)
}

@Deprecated("")
fun ScriptEngine.loadFile(path: String) {
    eval("load('${path.replace('\\', '/')}');")
}

fun ScriptEngine.loadFile(path: String, bindings: Bindings) {
    eval("load('${path.replace('\\', '/')}');", bindings)
}

@Deprecated("")
fun ScriptEngine.runAndRestoreContext(
    f: ScriptEngine.() -> Any?
): Any? {
    val globalObject = eval("this") as ScriptObjectMirror
    val before = globalObject.toMapWithAllMembers()

    return try {
        this.f()
    } finally {
        val after = globalObject.toMapWithAllMembers()
        val diff = after.entries - before.entries


        diff.forEach {
            globalObject.put(it.key, before[it.key] ?: ScriptRuntime.UNDEFINED)
        }
    }
}

fun ScriptEngine.runAndRestoreContext(
    originalKeys: Map<String, Any?>,
    f: ScriptEngine.(Bindings) -> Any?
): Any? {
    DelegatingBindings(getBindings(ScriptContext.ENGINE_SCOPE), originalKeys).use { b ->
        return f(b)
    }
}

private fun ScriptObjectMirror.toMapWithAllMembers(): Map<String, Any?> = getOwnKeys(true).associate { it to this[it] }

abstract class AbstractNashornJsTestChecker {

    private var engineUsageCnt = 0

    private var engineCache: ScriptEngine? = null

    abstract val originalKeys: Map<String, Any?>

    protected val engine
        get() = engineCache ?: createScriptEngineForTest().also { engineCache = it }

    fun check(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        val actualResult = run(files, testModuleName, testPackageName, testFunctionName, withModuleSystem)
        Assert.assertEquals(expectedResult, actualResult)
    }

    fun run(files: List<String>) {
//        run(files) { null }
        run0(files) { null }
    }

    private fun run(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        withModuleSystem: Boolean
    ) =
//        run(files) { runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem) }
        run0(files) { b -> runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem, b) }

    protected open fun beforeRun() {}

    @Deprecated("")
    private fun run(
        files: List<String>,
        f: ScriptEngine.() -> Any?
    ): Any? {
        // Recreate the engine once in a while
        if (engineUsageCnt++ > 100) {
            engineUsageCnt = 0
            engineCache = createScriptEngineForTest()
        }

        beforeRun()

        return engine.runAndRestoreContext { ->
            files.forEach(engine::loadFile)
            engine.f()
        }
    }

    private fun run0(
        files: List<String>,
        f: ScriptEngine.(Bindings) -> Any?
    ): Any? {
        // Recreate the engine once in a while
        if (engineUsageCnt++ > 100) {
            engineUsageCnt = 0
            engineCache = createScriptEngineForTest()
        }

        beforeRun()

        return engine.runAndRestoreContext(originalKeys) { b ->
            files.forEach { engine.loadFile(it, b) }
            engine.f(b)
        }
    }

    abstract protected fun createScriptEngineForTest(): ScriptEngine
}

object NashornJsTestChecker : AbstractNashornJsTestChecker() {
    override val originalKeys: Map<String, Any?>
        get() = originalKeys_!!

    private var originalKeys_: Map<String, Any?>? = null

    val SETUP_KOTLIN_OUTPUT = "kotlin.kotlin.io.output = new kotlin.kotlin.io.BufferedOutput();"
    private val GET_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.buffer;"

    override fun beforeRun() {
        engine.eval(SETUP_KOTLIN_OUTPUT)
    }

    fun checkStdout(files: List<String>, expectedResult: String) {
        run(files)
        val actualResult = engine.eval(GET_KOTLIN_OUTPUT)
        Assert.assertEquals(expectedResult, actualResult)
    }

    override fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            BasicBoxTest.DIST_DIR_JS_PATH + "kotlin.js",
            BasicBoxTest.DIST_DIR_JS_PATH + "kotlin-test.js"
        ).forEach { engine.loadFile(it) }

        engine.overrideAsserter()

        originalKeys_ = engine.getBindings(ScriptContext.ENGINE_SCOPE).toMap()

        return engine
    }
}

object NashornIrJsTestChecker : AbstractNashornJsTestChecker() {
    override val originalKeys: Map<String, Any?>
        get() = originalKeys_!!

    private var originalKeys_: Map<String, Any?>? = null


    override fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            "js/js.translator/testData/out/irBox/testRuntime.js"
        ).forEach { engine.loadFile(it) }

        originalKeys_ = engine.getBindings(ScriptContext.ENGINE_SCOPE).toMap()

        return engine
    }
}

object NashornIrJsTestChecker0 {

    private var engineUsageCnt = 0

    private var engineCache: ScriptEngine? = null

    private val engine
        get() = engineCache ?: createScriptEngineForTest().also { engineCache = it }

//    private var compiledRuntime: CompiledScript? = null

//    fun ScriptEngine.loadFile(path: String) {
//        eval("load('${path.replace('\\', '/')}');")
//    }

    private var originalKeys_: Map<String, Any?>? = null

    fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        /*val source = */listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            "js/js.translator/testData/out/irBox/testRuntime.js"
        ).forEach { engine.loadFile(it) }
//            joinToString(separator = "\n\n") {
//            File(it).readText()
//        }
//        val cb = engine.getBindings(ScriptContext.ENGINE_SCOPE)
//        engine.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
//        engine.setBindings(cb, ScriptContext.GLOBAL_SCOPE)

//        compiledRuntime = (engine as Compilable).compile(source)

//        val context = SimpleScriptContext()
//        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
//
//        compiledRuntime!!.eval()

        originalKeys_ = engine.getBindings(ScriptContext.ENGINE_SCOPE).toMap()
        return engine
    }

    fun check(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        val actualResult = run(files, testModuleName, testPackageName, testFunctionName, withModuleSystem)
        Assert.assertEquals(expectedResult, actualResult)
    }

    private fun run(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        withModuleSystem: Boolean
    ) = run(files) {
        runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem)
    }

    private fun run(
        files: List<String>,
        f: ScriptEngine.() -> Any?
    ): Any? {
//        // Recreate the engine once in a while
//        if (engineUsageCnt++ > 100) {
//            engineUsageCnt = 0
//            engineCache = createScriptEngineForTest()
//        }

//        val b = engine.createBindings()
        val cb = engine.getBindings(ScriptContext.ENGINE_SCOPE)

//        b.putAll(cb)
//        val o = engine.eval("this") as ScriptObjectMirror
//        val o2 = engine.eval("this", b) as ScriptObjectMirror
//        o.getOwnKeys(true).forEach {
//            o2[it] = o[it]
//        }

        DelegatingBindings(cb, originalKeys_!!).use { b ->
            files.forEach {
                engine.eval("load('${it.replace('\\', '/')}');", b)
            }

            return engine.eval("box()", b)
        }
    }
}

private const val NASHORN_GLOBAL = "nashorn.global"

private class DelegatingBindings(private val parent: Bindings, val originalKeys: Map<String, Any?>) : SimpleBindings(), Closeable {
//    private var global: Bindings? = parent

//    private val originalKeys = parent.keys.toSet()

    override fun put(key: String?, value: Any?): Any? {
////        if (key == NASHORN_GLOBAL && value is Bindings) {
////            global = value
//////            original_keys = new HashSet<>(global.keySet());
//////            return null
////        }
        return super.put(key, value);
    }

    override fun get(key: String?): Any? {
        if (super.containsKey(key)) {
            return super.get(key)
        }

        if (key == NASHORN_GLOBAL) {
            return parent
        }

        return parent[key]
    }

    override fun containsKey(key: String?): Boolean {
        return key == NASHORN_GLOBAL || super.containsKey(key) || parent.containsKey(key)
    }

    override fun containsValue(value: Any?): Boolean {
        return super.containsValue(value) || parent.containsValue(value)
    }

    override fun close() {
        for (key in parent.keys) {
            parent[key] = originalKeys[key] ?: ScriptRuntime.UNDEFINED
//            if (key in originalKeys) {
//                parent[key] = originalKeys[key]
//            } else {
//                // parent.remove(key)
//                // remove doesn't work :(
//                parent[key] = ScriptRuntime.UNDEFINED
//            }
        }
    }
}
