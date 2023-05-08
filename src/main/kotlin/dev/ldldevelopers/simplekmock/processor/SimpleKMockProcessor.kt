package dev.ldldevelopers.simplekmock.processor

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

class SimpleKMockProcessor(
    val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val mocksGenerated = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("dev.ldldevelopers.simplekmock.Mocked")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols.filter { it is KSClassDeclaration && it.isOpen() && !it.modifiers.contains(Modifier.SEALED) && !it.isLocal() }
            .forEach { it.accept(SymbolProcessorVisitor(), Unit) }
        return ret
    }

    inner class SymbolProcessorVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val isClass = classDeclaration.primaryConstructor != null
            val packageName = classDeclaration.packageName.asString()
            val fullName = classDeclaration.qualifiedName!!.asString()
            val classSimpleName = classDeclaration.simpleName.asString()
            val openFunctions = classDeclaration.getAllFunctions().filter { it.isOpen() && it.functionKind == FunctionKind.MEMBER }
            val openProperties = classDeclaration.getAllProperties().filter { it.isOpen() }
            with(codeGenerator.createNewFile(Dependencies(true, classDeclaration.containingFile!!), packageName, classSimpleName + "Mock")) {
                initIndentation {
                    if (packageName.isNotBlank()) appendTextIndented("package $packageName")
                    appendLine()
                    appendTextIndented("import dev.ldldevelopers.simplekmock.*")
                    appendLine()
                    appendTextIndented("class ${classSimpleName}Mock")
                    appendText(classDeclaration.getSimpleTypeParametersWithVarianceAsString())
                    if (isClass) {
                        val constructorParameters = classDeclaration.primaryConstructor!!.parameters
                        appendText("(")
                        appendText(constructorParameters.joinToString { it.name!!.asString() + ": " + it.type.getFullNullableName() })
                        appendText(") : $fullName")
                        appendText(classDeclaration.getSimpleTypeParametersAsString())
                        appendText("(")
                        appendText(constructorParameters.joinToString { it.name!!.asString() })
                        appendText(")")
                    } else {
                        appendText(" : $fullName")
                        appendText(classDeclaration.getSimpleTypeParametersAsString())
                    }
                    if (classDeclaration.typeParameters.isNotEmpty()) {
                        appendText(" where ")
                        appendText(classDeclaration.getBoundedTypeParametersAsString())
                    }
                    appendText(" {")
                    indent {

                        appendTextIndented("interface Mock")
                        appendText(classDeclaration.getSimpleTypeParametersWithVarianceAsString())
                        if (classDeclaration.typeParameters.isNotEmpty()) {
                            appendText(" where ")
                            appendText(classDeclaration.getBoundedTypeParametersAsString())
                        }
                        appendText(" {")

                        indent { // mock interface "functions"
                            for (function in openFunctions) {
                                val parameters = function.parameters
                                val returnType = function.returnType!!
                                val unitReturn = returnType.resolve().declaration.let { it is KSClassDeclaration
                                        && it.simpleName.asString() == "Unit" }
                                val name =
                                    if (openFunctions.count { it.simpleName.asString() == function.simpleName.asString() } > 1) {
                                        function.simpleName.asString() + parameters.joinToString("_", "_") {
                                            it.type.resolve().declaration.simpleName.asString()
                                        }
                                    } else function.simpleName.asString()
                                val typeParameterNames = function.typeParameters.map { it.name.asString() }
                                writeMock(parameters.size, unitReturn)
                                val mockName = "Mock${parameters.size}${if (unitReturn) 0 else 1}"
                                appendTextIndented("val $name: $mockName")
                                if (parameters.isNotEmpty()) {
                                    appendText("<")
                                    appendText(parameters.joinToString { it.type.getFullNullableName(typeParameterNames) })
                                    if (!unitReturn) appendText(", " + returnType.getFullNullableName(typeParameterNames))
                                    appendText(">")
                                } else if (!unitReturn) {
                                    appendText("<")
                                    appendText(returnType.getFullNullableName(typeParameterNames))
                                    appendText(">")
                                }
                            }
                        }

                        indent { // mock interface "properties"
                            for (property in openProperties) {
                                val type = property.type.getFullNullableName()
                                val upperName = property.simpleName.asString().replaceFirstChar { it.uppercase() }
                                val isMutable = property.isMutable
                                appendTextIndented("val get$upperName: Mock01<$type>")
                                if (isMutable) appendTextIndented("val set$upperName: Mock10<$type>")
                            }
                        }
                        appendTextIndented("}")
                    }
                    indent {

                        appendTextIndented("val mock = object : Mock")
                        appendText(classDeclaration.getSimpleTypeParametersAsString())
                        appendText(" {")

                        indent { // mock "function" overrides
                            for (function in openFunctions) {
                                val parameters = function.parameters
                                val returnType = function.returnType!!
                                val unitReturn = returnType.resolve().declaration.let { it is KSClassDeclaration
                                        && it.simpleName.asString() == "Unit" }
                                val name =
                                    if (openFunctions.count { it.simpleName.asString() == function.simpleName.asString() } > 1) {
                                        function.simpleName.asString() + parameters.joinToString("_", "_") {
                                            it.type.resolve().declaration.simpleName.asString()
                                        }
                                    } else function.simpleName.asString()
                                val typeParameterNames = function.typeParameters.map { it.name.asString() }
                                writeMock(parameters.size, unitReturn)
                                val mockName = "Mock${parameters.size}${if (unitReturn) 0 else 1}"
                                appendTextIndented("override val $name = $mockName")
                                if (parameters.isNotEmpty()) {
                                    appendText("<")
                                    appendText(parameters.joinToString { it.type.getFullNullableName(typeParameterNames) })
                                    if (!unitReturn) appendText(", " + returnType.getFullNullableName(typeParameterNames))
                                    appendText(">")
                                } else if (!unitReturn) {
                                    appendText("<")
                                    appendText(returnType.getFullNullableName(typeParameterNames))
                                    appendText(">")
                                }
                                appendText("()")
                            }
                        }

                        indent {  // mock "property" overrides
                            for (property in openProperties) {
                                val type = property.type.getFullNullableName()
                                val upperName = property.simpleName.asString().replaceFirstChar { it.uppercase() }
                                val isMutable = property.isMutable
                                appendTextIndented("override val get$upperName = Mock01<$type>()")
                                if (isMutable) appendTextIndented("override val set$upperName = Mock10<$type>()")
                            }
                        }

                        appendTextIndented("}")

                        appendTextIndented("fun resetMocks() {")
                        indent {
                            for (function in openFunctions) {
                                val name =
                                    if (openFunctions.count { it.simpleName.asString() == function.simpleName.asString() } > 1) {
                                        val parameters = function.parameters
                                        function.simpleName.asString() + parameters.joinToString("_", "_") {
                                            it.type.resolve().declaration.simpleName.asString()
                                        }
                                    } else function.simpleName.asString()
                                appendTextIndented("mock.$name.reset()")
                            }
                            for (property in openProperties) {
                                val upperName = property.simpleName.asString().replaceFirstChar { it.uppercase() }
                                val isMutable = property.isMutable
                                appendTextIndented("mock.get$upperName.reset()")
                                if (isMutable) {
                                    appendTextIndented("mock.set$upperName.reset()")
                                }
                            }
                        }
                        appendTextIndented("}")

                        appendTextIndented("fun relaxMocks() {")
                        indent {
                            for (function in openFunctions) {
                                val returnType = function.returnType!!.resolve()
                                val unitReturn = returnType.declaration is KSClassDeclaration
                                        && returnType.declaration.simpleName.asString() == "Unit"
                                val name =
                                    if (openFunctions.count { it.simpleName.asString() == function.simpleName.asString() } > 1) {
                                        val parameters = function.parameters
                                        function.simpleName.asString() + parameters.joinToString("_", "_") {
                                            it.type.resolve().declaration.simpleName.asString()
                                        }
                                    } else function.simpleName.asString()
                                if (unitReturn) {
                                    appendTextIndented("mock.$name.doesNothing()")
                                }
                            }
                            for (property in openProperties) {
                                val upperName = property.simpleName.asString().replaceFirstChar { it.uppercase() }
                                val isMutable = property.isMutable
                                if (isMutable) {
                                    appendTextIndented("mock.set$upperName.doesNothing()")
                                }
                            }
                        }
                        appendTextIndented("}")

                        appendTextIndented("infix fun setMocks(block: Mock")
                        appendText(classDeclaration.getSimpleTypeParametersAsString())
                        appendText(".() -> Unit) {")
                        indent {
                            appendTextIndented("mock.apply(block)")
                        }
                        appendTextIndented("}")

                    }
                    indent { // Function overrides
                        for (function in openFunctions) {
                            val parameters = function.parameters
                            val simpleName = function.simpleName.asString()
                            val name =
                                if (openFunctions.count { it.simpleName.asString() == function.simpleName.asString() } > 1) {
                                    function.simpleName.asString() + parameters.joinToString("_", "_") {
                                        it.type.resolve().declaration.simpleName.asString()
                                    }
                                } else function.simpleName.asString()
                            val typeParams = function.typeParameters
                            val returnType = function.returnType!!.getFullNullableName()
                            appendTextIndented("override fun ")
                            if (typeParams.isNotEmpty()) {
                                appendText(typeParams.joinToString(", ", "<", ">") { it.name.asString() })
                                appendText(" ")
                            }
                            appendText("$simpleName(")
                            appendText(parameters.joinToString { "${it.name!!.asString()}: ${it.type.getFullNullableName()}" })
                            appendText(")")
                            appendText(": $returnType")
                            appendText(" = mock.$name.call(")
                            appendText(parameters.joinToString { it.name!!.asString() })
                            appendText(")")
                            appendText(" as $returnType")
                        }
                    }
                    indent { // Property overrides
                        for (property in openProperties) {
                            val type = property.type.getFullNullableName()
                            val name = property.simpleName.asString()
                            val upperName = name.replaceFirstChar { it.uppercase() }
                            val isMutable = property.isMutable
                            if (isMutable) {
                                appendTextIndented("override var $name: $type")
                                indent {
                                    appendTextIndented("get() = mock.get$upperName.call()")
                                    appendTextIndented("set(value) = mock.set$upperName.call(value)")
                                }
                            } else {
                                appendTextIndented("override val $name: $type")
                                indent {
                                    appendTextIndented("get() = mock.get$upperName.call()")
                                }
                            }
                        }
                    }

                    appendTextIndented("}")
                    appendLine()
                }
                close()
            }
        }

        private fun writeMock(paramSize: Int, returnIsUnit: Boolean) {
            if (mocksGenerated.contains("Mock$paramSize${if (returnIsUnit) 0 else 1}")) return
            if (paramSize == 0) {
                if(returnIsUnit) writeMock00()
                else writeMock01()
            } else if (returnIsUnit) writeUnitMock(paramSize)
            else writeNonUnitMock(paramSize)
        }
        private fun writeMock00() {
            val className = "Mock00"
            mocksGenerated.add(className)
            val packageName = "dev.ldldevelopers.simplekmock"
            with(codeGenerator.createNewFile(Dependencies(true), packageName, className)) {
                appendText(
                    """
dev.ldldevelopers.simplekmock

class Mock00 {
    private var callMock: () -> Unit = { throw MockNotSetException() }
    private var calls: Int = 0
    fun callsRemembered(): Int {
        return calls
    }
    fun reset() {
        calls = 0
        callMock = { throw MockNotSetException() }
    }
    fun doesNothing() {
        callMock = {  }
    }
    infix fun does(mock: () -> Unit) {
        callMock = mock
    }
    infix fun throws(exception: Exception) {
        callMock = { throw exception }
    }
    fun call() {
        calls++
        callMock()
    }
}
                    """.trimIndent()
                )
            }
        }
        private fun writeMock01() {
            val className = "Mock01"
            mocksGenerated.add(className)
            val packageName = "dev.ldldevelopers.simplekmock"
            with(codeGenerator.createNewFile(Dependencies(true), packageName, className)) {
                appendText(
                    """
dev.ldldevelopers.simplekmock

class Mock01<R> {
    private var callMock: () -> R = { throw MockNotSetException() }
    private var calls: Int = 0
    fun callsRemembered(): Int {
        return calls
    }
    fun reset() {
        calls = 0
        callMock = { throw MockNotSetException() }
    }
    infix fun answersWith(value: R) {
        callMock = { value }
    }
    infix fun answersWithSequence(values: Iterable<R>) {
        val iterator = values.iterator()
        callMock = { iterator.next() }
    }
    infix fun answers(mock: () -> R) {
        callMock = mock
    }
    infix fun throws(exception: Exception) {
        callMock = { throw exception }
    }
    fun call(): R {
        calls++
        return callMock()
    }
}
                    """.trimIndent()
                )
            }
        }
        private fun writeUnitMock(paramSize: Int) {
            val className = "Mock${paramSize}0"
            mocksGenerated.add(className)
            val packageName = "dev.ldldevelopers.simplekmock"
            with(codeGenerator.createNewFile(Dependencies(true), packageName, className)) {
                initIndentation {
                    appendTextIndented("package $packageName")
                    appendLine()
                    appendTextIndented("class $className")
                    val types = buildList {
                        for (i in 0 until paramSize) {
                            if (i == 0) add("P0")
                            else add("P$i")
                        }
                    }
                    val arguments = types.map { it.lowercase() }
                    val typedArguments = arguments.zip(types)
                    appendText(types.joinToString(", ", "<", ">"))
                    appendText(" {")
                    indent {
                        appendTextIndented("private var callMock: (")
                        appendText(types.joinToString())
                        appendText(") -> Unit = { ")
                        appendText(types.joinToString { "_" })
                        appendText(" -> throw MockNotSetException() }")
                    }
                    indent {
                        appendTextIndented("private val callsRemembered = mutableListOf<CallHolder")
                        appendText(types.joinToString(", ", "<", ">"))
                        appendText(">()")
                    }
                    indent {
                        appendTextIndented("fun callsRemembered(predicate: (")
                        appendText(types.joinToString())
                        appendText(") -> Boolean): Int = callsRemembered.count {")
                        indent {
                            appendTextIndented("predicate(")
                            appendText(arguments.joinToString { "it.$it" })
                            appendText(")")
                        }
                        appendTextIndented("}")
                    }
                    if (paramSize > 1) indent {
                        appendTextIndented("fun callsRemembered(")
                        appendText(types.joinToString { "predicate$it: Predicate<$it>" })
                        appendText("): Int = callsRemembered { ")
                        appendText(arguments.joinToString())
                        appendText(" ->")
                        indent {
                            appendTextIndented(typedArguments.joinToString(" && ") { "predicate${it.second}(${it.first})" })
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun callsRemembered(): Int = callsRemembered(")
                        appendText(types.joinToString { "any()" })
                        appendText(")")
                    }
                    indent {
                        appendTextIndented("fun reset() {")
                        indent {
                            appendTextIndented("callsRemembered.clear()")
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> throw MockNotSetException() }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun doesNothing() {")
                        indent {
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> throw MockNotSetException() }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun does(mock: (")
                        appendText(types.joinToString())
                        appendText(") -> Unit) {")
                        indent {
                            appendTextIndented("callMock = mock")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun throws(exception: Exception) {")
                        indent {
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> throw exception }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun call(")
                        appendText(typedArguments.joinToString { "${it.first}: ${it.second}" })
                        appendText(") {")
                        indent {
                            appendTextIndented("callsRemembered.add(CallHolder(")
                            appendText(arguments.joinToString())
                            appendText("))")
                            appendTextIndented("callMock(")
                            appendText(arguments.joinToString())
                            appendText(")")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("private data class CallHolder")
                        appendText(types.joinToString(", ", "<", ">"))
                        appendText(typedArguments.joinToString(", ", "(", ")") { "val ${it.first}: ${it.second}" })
                    }
                    appendTextIndented("}")
                    appendLine()
                }
                close()
            }
        }
        private fun writeNonUnitMock(paramSize: Int) {
            val className = "Mock${paramSize}1"
            mocksGenerated.add(className)
            val packageName = "dev.ldldevelopers.simplekmock"
            with(codeGenerator.createNewFile(Dependencies(true), packageName, className)) {
                initIndentation {
                    appendTextIndented("package $packageName")
                    appendLine()
                    appendTextIndented("class $className")
                    val types = buildList {
                        for (i in 0 until paramSize) {
                            if (i == 0) add("P0")
                            else add("P$i")
                        }
                    }
                    val arguments = types.map { it.lowercase() }
                    val typedArguments = arguments.zip(types)
                    appendText((types + "R").joinToString(", ", "<", ">"))
                    appendText(" {")
                    indent {
                        appendTextIndented("private var callMock: (")
                        appendText(types.joinToString())
                        appendText(") -> R = { ")
                        appendText(types.joinToString { "_" })
                        appendText(" -> throw MockNotSetException() }")
                    }
                    indent {
                        appendTextIndented("private val callsRemembered = mutableListOf<CallHolder")
                        appendText(types.joinToString(", ", "<", ">"))
                        appendText(">()")
                    }
                    indent {
                        appendTextIndented("fun callsRemembered(predicate: (")
                        appendText(types.joinToString())
                        appendText(") -> Boolean): Int = callsRemembered.count {")
                        indent {
                            appendTextIndented("predicate(")
                            appendText(arguments.joinToString { "it.$it" })
                            appendText(")")
                        }
                        appendTextIndented("}")
                    }
                    if (paramSize > 1) indent {
                        appendTextIndented("fun callsRemembered(")
                        appendText(types.joinToString { "predicate$it: Predicate<$it>" })
                        appendText("): Int = callsRemembered { ")
                        appendText(arguments.joinToString())
                        appendText(" ->")
                        indent {
                            appendTextIndented(typedArguments.joinToString(" && ") { "predicate${it.second}(${it.first})" })
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun callsRemembered(): Int = callsRemembered(")
                        appendText(types.joinToString { "any()" })
                        appendText(")")
                    }
                    indent {
                        appendTextIndented("fun reset() {")
                        indent {
                            appendTextIndented("callsRemembered.clear()")
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> throw MockNotSetException() }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun answersWith(value: R) {")
                        indent {
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> value }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun answersWithSequence(values: Iterable<R>) {")
                        indent {
                            appendTextIndented("val iterator = values.iterator()")
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> iterator.next() }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun answersWithSequence(vararg values: R) {")
                        indent {
                            appendTextIndented("val iterator = values.iterator()")
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> iterator.next() }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun answers(mock: (")
                        appendText(types.joinToString())
                        appendText(") -> R) {")
                        indent {
                            appendTextIndented("callMock = mock")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun answersSequence(mocks: Iterable<(")
                        appendText(types.joinToString())
                        appendText(") -> R>) {")
                        indent {
                            appendTextIndented("val iterator = mocks.iterator()")
                            appendTextIndented("callMock = { ")
                            appendText(arguments.joinToString())
                            appendText(" -> iterator.next()(")
                            appendText(arguments.joinToString())
                            appendText(") }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun answersSequence(vararg mocks: (")
                        appendText(types.joinToString())
                        appendText(") -> R) {")
                        indent {
                            appendTextIndented("val iterator = mocks.iterator()")
                            appendTextIndented("callMock = { ")
                            appendText(arguments.joinToString())
                            appendText(" -> iterator.next()(")
                            appendText(arguments.joinToString())
                            appendText(") }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("infix fun throws(exception: Exception) {")
                        indent {
                            appendTextIndented("callMock = { ")
                            appendText(types.joinToString { "_" })
                            appendText(" -> throw exception }")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("fun call(")
                        appendText(typedArguments.joinToString { "${it.first}: ${it.second}" })
                        appendText("): R {")
                        indent {
                            appendTextIndented("callsRemembered.add(CallHolder(")
                            appendText(arguments.joinToString())
                            appendText("))")
                            appendTextIndented("return callMock(")
                            appendText(arguments.joinToString())
                            appendText(")")
                        }
                        appendTextIndented("}")
                    }
                    indent {
                        appendTextIndented("private data class CallHolder")
                        appendText(types.joinToString(", ", "<", ">"))
                        appendText(typedArguments.joinToString(", ", "(", ")") { "val ${it.first}: ${it.second}" })
                    }
                    appendTextIndented("}")
                    appendLine()
                }
                close()
            }
        }
        private fun KSTypeReference.getFullNullableName(): String {
            val resolved = this.resolve()
            val simpleName = when (val declaration = resolved.declaration) {
                is KSClassDeclaration -> declaration.qualifiedName!!.asString()
                else -> declaration.simpleName.asString()
            }
            val nullability = if (resolved.isMarkedNullable) "?" else ""
            var params = this.element?.typeArguments?.joinToString {
                val variance = if (it.variance == Variance.COVARIANT || it.variance == Variance.CONTRAVARIANT) it.variance.label + " "
                else ""
                variance + it.type!!.getFullNullableName()
            } ?: ""
            if (params.isNotBlank()) params = "<$params>"
            return simpleName + params + nullability
        }

        private fun KSTypeReference.getFullNullableName(forbidden: List<String>): String {

            fun KSTypeReference.getFullNullableNameStar(): String {
                val resolved = this.resolve()
                val simpleName = when (val declaration = resolved.declaration) {
                    is KSClassDeclaration -> declaration.qualifiedName!!.asString()
                    else -> declaration.simpleName.asString()
                }
                if (forbidden.contains(simpleName)) return "*"
                val nullability = if (resolved.isMarkedNullable) "?" else ""
                var params = this.element?.typeArguments?.joinToString {
                    val variance = if (it.variance == Variance.COVARIANT || it.variance == Variance.CONTRAVARIANT) it.variance.label + " "
                    else ""
                    val fullName = it.type!!.getFullNullableNameStar()
                    if (fullName == "*") fullName else variance + fullName
                } ?: ""
                if (params.contains("*")) return "*"
                if (params.isNotBlank()) params = "<$params>"
                return simpleName + params + nullability
            }



            val resolved = this.resolve()
            val simpleName = when (val declaration = resolved.declaration) {
                is KSClassDeclaration -> declaration.qualifiedName!!.asString()
                else -> declaration.simpleName.asString()
            }
            if (forbidden.contains(simpleName)) return "Any?"
            val nullability = if (resolved.isMarkedNullable) "?" else ""
            var params = this.element?.typeArguments?.joinToString {
                val variance = if (it.variance == Variance.COVARIANT || it.variance == Variance.CONTRAVARIANT) it.variance.label + " "
                else ""
                val fullName = it.type!!.getFullNullableNameStar()
                if (fullName == "*") fullName else variance + fullName
            } ?: ""
            if (params.isNotBlank()) params = "<$params>"
            return simpleName + params + nullability
        }


        private fun KSClassDeclaration.getSimpleTypeParametersAsString(): String = if (typeParameters.isNotEmpty()) {
            typeParameters.joinToString(", ", "<", ">") { it.name.asString() }
        } else ""

        private fun KSClassDeclaration.getSimpleTypeParametersWithVarianceAsString(): String = if (typeParameters.isNotEmpty()) {
            typeParameters.joinToString(", ", "<", ">") {
                val variance = it.variance
                if (variance == Variance.CONTRAVARIANT || variance == Variance.COVARIANT)
                    variance.label + " " + it.name.asString()
                else
                    it.name.asString()
            }
        } else ""

        private fun KSClassDeclaration.getBoundedTypeParametersAsString(): String = if (typeParameters.isNotEmpty()) {
            typeParameters.joinToString(", ") {
                val name = it.name.asString()
                it.bounds.joinToString { bound -> "$name:" + bound.getFullNullableName() }
            }
        } else ""
    }
}

class SimpleKMockProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SimpleKMockProcessor(environment.codeGenerator)
    }
}



context(IndentHolder)
private fun OutputStream.appendTextIndented(str: String) {
    if (str.contains('\n')) {
        appendLine()
        write(("    ".repeat(indentLevel) + str).toByteArray())
    } else {
        str.split("\n").forEach { appendTextIndented(it) }
    }
}

private fun OutputStream.appendText(str: String) {
    write(str.toByteArray())
}

private fun OutputStream.appendLine() {
    appendText("\n")
}

private data class IndentHolder(val indentLevel: Int) {
    fun indent(block: IndentHolder.() -> Unit) {
        with(IndentHolder(indentLevel + 1)) {
            block()
        }
    }
}

private fun initIndentation(block: IndentHolder.() -> Unit) {
    with(IndentHolder(0)) {
        block()
    }
}
