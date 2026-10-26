package app.template.patches.pepper.api

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.replaceMethod
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/api/PepperApi;"

/**
 * Lets the extension make requests to the REST API of Pepper with the HTTP client of the app.
 * The classes of OkHttp are obfuscated, so the methods of the extension that use them
 * are generated here.
 */
internal val pepperApiPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(
            EXTENSION_CLASS,
            "onCallCreated",
            "newRequest",
            "execute",
            "responseCode",
            "responseBody",
        )

        val realCallClass = RealCallExecuteFingerprint.classDef
        val responseType = RealCallExecuteFingerprint.method.returnType
        val realCallConstructor = realCallClass.methods.single {
            it.name == "<init>" && it.parameterTypes.size == 3 && it.parameterTypes[2] == "Z"
        }
        val (clientType, requestType) = realCallConstructor.parameterTypes.take(2).map { it.toString() }

        // Request(builder)
        val builderType = classDefBy(requestType).methods.single {
            it.name == "<init>" && it.parameterTypes.size == 1
        }.parameterTypes.single().toString()

        // Request.Builder.url(String), which turns "ws:" and "wss:" into "http:" and "https:".
        val urlMethod = Fingerprint(
            definingClass = builderType,
            parameters = listOf("Ljava/lang/String;"),
            strings = listOf("wss:"),
        ).method

        // Request.Builder.header(String, String)
        val headerMethod = classDefBy(builderType).methods.single {
            it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;", "Ljava/lang/String;")
        }

        val responseFields = classDefBy(responseType).fields
        val codeField = responseFields.single { it.type == "I" }
        val bodyField = responseFields.single { field ->
            classDefByOrNull(field.type)?.methods?.any {
                it.name == "string" && it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;"
            } == true
        }

        // Keep the client of the requests to the REST API.
        realCallConstructor.addInstruction(
            0,
            "invoke-static/range { p1 .. p2 }, $EXTENSION_CLASS->onCallCreated(Ljava/lang/Object;Ljava/lang/Object;)V"
        )

        mutableClassDefBy(EXTENSION_CLASS).apply {
            fun stub(name: String) = methods.single { it.name == name }

            replaceMethod(
                stub("newRequest"),
                registerCount = 5,
                """
                    new-instance v0, $builderType
                    invoke-direct { v0 }, $builderType-><init>()V
                    invoke-virtual { v0, p0 }, $builderType->${urlMethod.name}(Ljava/lang/String;)${urlMethod.returnType}
                    if-eqz p1, :build
                    invoke-virtual { v0, p1, p2 }, $builderType->${headerMethod.name}(Ljava/lang/String;Ljava/lang/String;)${headerMethod.returnType}
                    :build
                    new-instance v1, $requestType
                    invoke-direct { v1, v0 }, $requestType-><init>($builderType)V
                    return-object v1
                """
            )

            replaceMethod(
                stub("execute"),
                registerCount = 4,
                """
                    new-instance v0, ${realCallClass.type}
                    check-cast p0, $clientType
                    check-cast p1, $requestType
                    const/4 v1, 0x0
                    invoke-direct { v0, p0, p1, v1 }, ${realCallClass.type}-><init>(${clientType}${requestType}Z)V
                    invoke-virtual { v0 }, ${realCallClass.type}->execute()$responseType
                    move-result-object v0
                    return-object v0
                """
            )

            replaceMethod(
                stub("responseCode"),
                registerCount = 2,
                """
                    check-cast p0, $responseType
                    iget v0, p0, $responseType->${codeField.name}:I
                    return v0
                """
            )

            replaceMethod(
                stub("responseBody"),
                registerCount = 2,
                """
                    check-cast p0, $responseType
                    iget-object v0, p0, $responseType->${bodyField.name}:${bodyField.type}
                    if-nez v0, :body
                    const/4 v0, 0x0
                    return-object v0
                    :body
                    invoke-virtual { v0 }, ${bodyField.type}->string()Ljava/lang/String;
                    move-result-object v0
                    return-object v0
                """
            )
        }
    }
}
