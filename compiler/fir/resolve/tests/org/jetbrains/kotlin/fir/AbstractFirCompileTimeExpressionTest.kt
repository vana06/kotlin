/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.runner.RunWith
import java.io.File

abstract class AbstractFirCompileTimeExpressionTest: AbstractFirResolveTestCase() {
    override fun doTest(path: String) {
        val firFiles = processInputFile(path)

        val renderResult = firFiles.first().render()
        val expectedPath = path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), renderResult)
    }
}

@RunWith(JUnit3RunnerWithInners::class)
class FirCompileTimeExpressionTest: AbstractFirCompileTimeExpressionTest() {
    fun testExample() {
        doTest("compiler/fir/resolve/testData/resolve/constant/example.kt")
    }

    fun testInt() {
        doTest("compiler/fir/resolve/testData/resolve/constant/basicBuiltins/intMethods.kt")
    }

    fun testBoolean() {
        doTest("compiler/fir/resolve/testData/resolve/constant/basicBuiltins/booleanMethods.kt")
    }

    fun testInAnnotations() {
        doTest("compiler/fir/resolve/testData/resolve/constant/basicBuiltins/inAnnotations.kt")
    }
}
