/*
 * Copyright (C) 2017 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.tock.bot.admin.kotlin.compiler

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *
 */
class KotlinCompilerTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            KotlinCompiler.init()
        }

        var mark = false
    }

    class CompilationResultClassLoader(
            val className: String,
            val bytes: ByteArray) : ClassLoader(CompilationResultClassLoader::class.java.classLoader) {

        @Override
        override fun findClass(name: String): Class<*> {
            return if (name == className) {
                return defineClass(name, bytes, 0, bytes.size)
            } else {
                super.findClass(name)
            }
        }

    }

    @Test
    fun `simple compilation with erroneous file reports error`() {
        val erroneousSourceCode = mapOf(
                "ClassToBeCompiled.kt"
                        to
                        """
                fun main(args: Array<String>) {
                    println("Hello)
                }"""
        )

        val errors = KotlinCompiler.getErrors(erroneousSourceCode)
        assertEquals(1, errors.size)
        assertEquals(
                listOf(
                        CompileError(
                                TextInterval(
                                        TextPosition(2, 34),
                                        TextPosition(2, 35)
                                ),
                                "Expecting '\"'",
                                Severity.ERROR,
                                "red_wavy_line"
                        ),
                        CompileError(
                                TextInterval(
                                        TextPosition(2, 34),
                                        TextPosition(2, 35)
                                ),
                                "Expecting ')'",
                                Severity.ERROR,
                                "red_wavy_line"
                        )
                ),
                errors["ClassToBeCompiled.kt"]
        )
    }

    @Test
    fun `simple compilation and execution succeed`() {
        val sourceCode = mapOf(
                "ClassToBeCompiled.kt"
                        to
                        """
                fun main(args: Array<String>) {
                    fr.vsct.tock.bot.admin.kotlin.compiler.KotlinCompilerTest.mark = true
                }"""
        )
        assertFalse(mark)
        assertEquals(emptyList(), KotlinCompiler.getErrors(sourceCode)["ClassToBeCompiled.kt"])
        val result = KotlinCompiler.compileCorrectFiles(sourceCode, "ClassToBeCompiled.kt", true)
        val compiledClassLoader = CompilationResultClassLoader("ClassToBeCompiledKt", result.files["ClassToBeCompiledKt.class"]!!)
        val c = compiledClassLoader.loadClass("ClassToBeCompiledKt")
        c.declaredMethods[0].invoke(null, arrayOf<String>())
        assertTrue(mark)
    }
}