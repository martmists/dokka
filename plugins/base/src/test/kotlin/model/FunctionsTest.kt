package model

import org.jetbrains.dokka.model.*
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.comments
import utils.name

class FunctionTest : AbstractModelTest("/src/main/kotlin/function/Test.kt", "function") {

    @Test
    fun function() {
        inlineModelTest(
            """
            |/**
            | * Function fn
            | */
            |fun fn() {}
        """
        ) {
            with((this / "function" / "fn").cast<DFunction>()) {
                name equals "fn"
                type.name equals "Unit"
                this.children.assertCount(0, "Function children: ")
            }
        }
    }

    @Test
    fun overloads() {
        inlineModelTest(
            """
            |/**
            | * Function fn
            | */
            |fun fn() {}
            | /**
            | * Function fn(Int)
            | */
            |fun fn(i: Int) {}
        """
        ) {
            with((this / "function").cast<DPackage>()) {
                val fn1 = functions.find {
                    it.name == "fn" && it.parameters.isNullOrEmpty()
                }.assertNotNull("fn()")
                val fn2 = functions.find {
                    it.name == "fn" && it.parameters.isNotEmpty()
                }.assertNotNull("fn(Int)")

                with(fn1) {
                    name equals "fn"
                    parameters.assertCount(0)
                }

                with(fn2) {
                    name equals "fn"
                    parameters.assertCount(1)
                    parameters.first().type.name equals "Int"
                }
            }
        }
    }

    @Test
    fun functionWithReceiver() {
        inlineModelTest(
            """
            |/**
            | * Function with receiver
            | */
            |fun String.fn() {}
            |
            |/**
            | * Function with receiver
            | */
            |fun String.fn(x: Int) {}
        """
        ) {
            with((this / "function").cast<DPackage>()) {
                val fn1 = functions.find {
                    it.name == "fn" && it.parameters.isNullOrEmpty()
                }.assertNotNull("fn()")
                val fn2 = functions.find {
                    it.name == "fn" && it.parameters.count() == 1
                }.assertNotNull("fn(Int)")

                with(fn1) {
                    name equals "fn"
                    parameters counts 0
                    receiver.assertNotNull("fn() receiver")
                }

                with(fn2) {
                    name equals "fn"
                    parameters counts 1
                    receiver.assertNotNull("fn(Int) receiver")
                    parameters.first().type.name equals "Int"
                }
            }
        }
    }

    @Test
    fun functionWithParams() {
        inlineModelTest(
            """
                |/**
                | * Multiline
                | *
                | * Function
                | * Documentation
                | */
                |fun function(/** parameter */ x: Int) {
                |}
        """
        ) {
            with((this / "function" / "function").cast<DFunction>()) {
                comments() equals "Multiline\nFunction Documentation"

                name equals "function"
                parameters counts 1
                parameters.firstOrNull().assertNotNull("Parameter: ").also {
                    it.name equals "x"
                    it.type.name equals "Int"
                    it.comments() equals "parameter"
                }

                type.assertNotNull("Return type: ").name equals "Unit"
            }
        }
    }

    @Test
    fun functionWithNotDocumentedAnnotation() {
        inlineModelTest(
            """
                |@Suppress("FOO") fun f() {}
        """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    content counts 1
                    with(content.first()) {
                        dri.classNames equals "Suppress"
                        params.entries counts 1
                        params["names"].assertNotNull("names") equals "[\"FOO\"]"
                    }
                }
            }
        }
    }

    @Test
    fun inlineFunction() {
        inlineModelTest(
            """
                |inline fun f(a: () -> String) {}
        """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                extra[AdditionalModifiers]?.content counts 1
                extra[AdditionalModifiers]?.content exists ExtraModifiers.INLINE
            }
        }
    }

    @Test
    fun suspendFunction() {
        inlineModelTest(
            """
                |suspend fun f() {}
        """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                extra[AdditionalModifiers]?.content counts 1
                extra[AdditionalModifiers]?.content exists ExtraModifiers.SUSPEND
            }
        }
    }

    @Test
    fun suspendInlineFunctionOrder() {
        inlineModelTest(
            """
                |suspend inline fun f(a: () -> String) {}
        """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                extra[AdditionalModifiers]?.content counts 2
                extra[AdditionalModifiers]?.content exists ExtraModifiers.SUSPEND
                extra[AdditionalModifiers]?.content exists ExtraModifiers.INLINE
            }
        }
    }

    @Test
    fun inlineSuspendFunctionOrderChanged() {
        inlineModelTest(
            """
                |inline suspend fun f(a: () -> String) {}
        """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                with(extra[AdditionalModifiers].assertNotNull("AdditionalModifiers")) {
                    content counts 2
                    content exists ExtraModifiers.SUSPEND
                    content exists ExtraModifiers.INLINE
                }
            }
        }
    }

    @Test
    fun functionWithAnnotatedParam() {
        inlineModelTest(
            """
                |@Target(AnnotationTarget.VALUE_PARAMETER)
                |@Retention(AnnotationRetention.SOURCE)
                |@MustBeDocumented
                |public annotation class Fancy
                |
                |fun function(@Fancy notInlined: () -> Unit) {}
        """
        ) {
            with((this / "function" / "Fancy").cast<DAnnotation>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    content counts 3
                    with(content.map { it.dri.classNames to it }.toMap()) {
                        with(this["Target"].assertNotNull("Target")) {
                            params["allowedTargets"].assertNotNull("allowedTargets") equals "[AnnotationTarget.VALUE_PARAMETER]"
                        }
                        with(this["Retention"].assertNotNull("Retention")) {
                            params["value"].assertNotNull("value") equals "(kotlin/annotation/AnnotationRetention, SOURCE)"
                        }
                        this["MustBeDocumented"].assertNotNull("MustBeDocumented").params.entries counts 0
                    }
                }

            }
            with((this / "function" / "function" / "notInlined").cast<DParameter>()) {
                with(this.extra[Annotations].assertNotNull("Annotations")) {
                    content counts 1
                    with(content.first()) {
                        dri.classNames equals "Fancy"
                        params.entries counts 0
                    }
                }
            }
        }
    }

    @Test
    fun functionWithNoinlineParam() {
        inlineModelTest(
            """
                |fun f(noinline notInlined: () -> Unit) {}
        """
        ) {
            with((this / "function" / "f" / "notInlined").cast<DParameter>()) {
                extra[AdditionalModifiers]?.content counts 1
                extra[AdditionalModifiers]?.content exists ExtraModifiers.NOINLINE
            }
        }
    }

    @Test
    fun annotatedFunctionWithAnnotationParameters() {
        inlineModelTest(
            """
                |@Target(AnnotationTarget.VALUE_PARAMETER)
                |@Retention(AnnotationRetention.SOURCE)
                |@MustBeDocumented
                |public annotation class Fancy(val size: Int)
                |
                |@Fancy(1) fun f() {}
        """
        ) {
            with((this / "function" / "Fancy").cast<DAnnotation>()) {
                constructors counts 1
                with(constructors.first()) {
                    parameters counts 1
                    with(parameters.first()) {
                        type.name equals "Int"
                        name equals "size"
                    }
                }

                with(extra[Annotations].assertNotNull("Annotations")) {
                    content counts 3
                    with(content.map { it.dri.classNames to it }.toMap()) {
                        with(this["Target"].assertNotNull("Target")) {
                            params["allowedTargets"].assertNotNull("allowedTargets") equals "[AnnotationTarget.VALUE_PARAMETER]"
                        }
                        with(this["Retention"].assertNotNull("Retention")) {
                            params["value"].assertNotNull("value") equals "(kotlin/annotation/AnnotationRetention, SOURCE)"
                        }
                        this["MustBeDocumented"].assertNotNull("MustBeDocumented").params.entries counts 0
                    }
                }

            }
            with((this / "function" / "f").cast<DFunction>()) {
                with(this.extra[Annotations].assertNotNull("Annotations")) {
                    content counts 1
                    with(content.first()) {
                        dri.classNames equals "Fancy"
                        params.entries counts 1
                        params["size"] equals "1"
                    }
                }
            }
        }
    }

    @Test
    fun functionWithDefaultParameter() {
        inlineModelTest(
            """
                |/src/main/kotlin/function/Test.kt
                |package function
                |fun f(x: String = "") {}
        """
        ) {
            // TODO add default value data

            with((this / "function" / "f").cast<DFunction>()) {
                parameters.forEach { p ->
                    p.name equals "x"
                    p.type.name.assertNotNull("Parameter type: ") equals "String"
                    assert(false) { "No default value data" }
                }
            }
        }
    }

//    @Test fun functionWithDefaultParameter() {
//        checkSourceExistsAndVerifyModel("testdata/functions/functionWithDefaultParameter.kt", defaultModelConfig) { model ->
//            with(model.members.single().members.single()) {
//                with(details.elementAt(3)) {
//                    val value = details(NodeKind.Value)
//                    assertEquals(1, value.count())
//                    with(value[0]) {
//                        assertEquals("\"\"", name)
//                    }
//                }
//            }
//        }
//    }

    @Test
    fun sinceKotlin() {
        inlineModelTest(
            """
                |/**
                | * Quite useful [String]
                | */
                |@SinceKotlin("1.1")
                |fun f(): String = "1.1 rulezz"
                """
        ) {
            with((this / "function" / "f").cast<DFunction>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    this.content counts 1
                    with(content.first()) {
                        dri.classNames equals "SinceKotlin"
                        params.entries counts 1
                        params["version"].assertNotNull("version") equals "1.1"
                    }
                }
            }
        }
    }

}