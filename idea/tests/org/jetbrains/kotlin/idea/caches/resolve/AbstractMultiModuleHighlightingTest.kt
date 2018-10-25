/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.ExpectedHighlightingData
import org.jetbrains.kotlin.idea.codeInsight.AbstractLineMarkersTest
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiHighlightingTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allJavaFiles
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import java.io.File

abstract class AbstractMultiModuleHighlightingTest : AbstractMultiHighlightingTest() {

    protected open fun checkLineMarkersInProject(
        findFiles: () -> List<PsiFile> = { project.allKotlinFiles().excludeByDirective() }
    ) {
        checkFiles(findFiles) {
            val document = myEditor.document
            val data = ExpectedHighlightingData(
                document,
                /* checkWarnings = */ false,
                /* checkWeakWarnings = */ false,
                /* checkInfos = */ false,
                myFile
            )
            setActiveEditor(myEditor)
            data.init()
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            doHighlighting()

            val markers = DaemonCodeAnalyzerImpl.getLineMarkers(getDocument(file), project)
            val expectedFile = File(file.virtualFile.path)

            try {
                data.checkLineMarkers(markers, document.text)
            } catch (error: AssertionError) {
                try {
                    val actualTextWithTestData = TagsTestDataUtil.insertInfoTags(markers, true, file.text)
                    KotlinTestUtils.assertEqualsToFile(expectedFile, actualTextWithTestData)
                } catch (failure: FileComparisonFailure) {
                    throw FileComparisonFailure(
                        error.message + "\n" + failure.message,
                        failure.expected,
                        failure.actual,
                        failure.filePath
                    )
                }
            }
            AbstractLineMarkersTest.assertNavigationElements(project, myFile as KtFile, markers)
        }
    }

    protected open fun checkHighlightingInProject(
        findFiles: () -> List<PsiFile> = { project.allKotlinFiles().excludeByDirective() }
    ) {
        checkFiles(findFiles) {
            checkHighlighting(myEditor, true, false)
        }
    }
}

abstract class AbstractMultiPlatformHighlightingTest : AbstractMultiModuleHighlightingTest() {

    protected open fun doTest(path: String) {
        setupMppProjectFromDirStructure(File(path))
        checkHighlightingInProject {
            (project.allKotlinFiles() + project.allJavaFiles()).excludeByDirective()
        }
    }

    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleHighlighting/multiplatform/"
}

private fun List<PsiFile>.excludeByDirective() = filter { !it.text.contains("// !CHECK_HIGHLIGHTING") }