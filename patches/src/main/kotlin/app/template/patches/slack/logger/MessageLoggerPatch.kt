package app.template.patches.slack.logger

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch
import app.template.patches.slack.shared.SINGLE_CLASS
import app.template.patches.slack.shared.replaceMethod
import app.template.patches.slack.shared.singleMethod
import app.template.patches.slack.shared.wrapMethod

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/MessageLoggerPatch;"

private const val JSON_INFLATER_CLASS = "Lslack/commons/json/JsonInflater;"

@Suppress("unused")
val messageLoggerPatch = bytecodePatch(
    name = "Message logger",
    description = "Adds options to keep the messages others delete, marked as deleted, and to show " +
        "the earlier versions of the messages others edit below them. Patch settings list them in a message log.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(
            EXTENSION_CLASS,
            "onJsonInflaterCreated",
            "newSyncedState",
            "keepDeletedMessage",
            "onMessageUpdate",
            "getHistoryMapper",
            "addLoggedReplies",
        )
        addPatchedFeature("messageLogger")

        // Keep the JSON parser, to turn messages into JSON and back.
        JsonInflaterConstructorFingerprint.method.addInstruction(
            1,
            "invoke-static { p0 }, $EXTENSION_CLASS->onJsonInflaterCreated($JSON_INFLATER_CLASS)V"
        )

        // R8 removed the constructor of the state, so create it like Slack does.
        Fingerprint(
            definingClass = EXTENSION_CLASS,
            name = "newSyncedState",
        ).let { fingerprint ->
            fingerprint.classDef.replaceMethod(
                fingerprint.method,
                registerCount = 1,
                """
                    new-instance v0, Lslack/model/Synced;
                    invoke-direct { v0 }, Ljava/lang/Object;-><init>()V
                    return-object v0
                """
            )
        }

        // Record edits and decorate every message Slack replaces in its cache.
        UpdateMessageFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val withoutLog = "updateMessageWithoutLog"

            // Registers: v0, p0 (this), p1 (conversation), p2 (message), p3 (state), p4 (notify the screens).
            fingerprint.classDef.wrapMethod(
                method,
                withoutLog,
                registerCount = 6,
                """
                    invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->onMessageUpdate($MESSAGE_PERSISTENCE_HELPER_CLASS${"Ljava/lang/String;"}Lslack/model/Message;)Lslack/model/Message;
                    move-result-object v0
                    invoke-virtual { p0, p1, v0, p3, p4 }, $MESSAGE_PERSISTENCE_HELPER_CLASS->$withoutLog(Ljava/lang/String;Lslack/model/Message;Lslack/model/MessageState;Z)Ljava/util/Optional;
                    move-result-object v0
                    return-object v0
                """
            )
        }

        // Keep deleted messages instead of removing them.
        RemoveMessageFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val withoutLog = "removeMessageWithoutLog"
            val jsonData = classDefBy(SOCKET_EVENT_WRAPPER_CLASS).fields.singleOrNull { it.name == "jsonData" }
                ?: throw IllegalStateException("Could not find the JSON of $SOCKET_EVENT_WRAPPER_CLASS")
            fun field(name: String) = fingerprint.classDef.fields.singleOrNull { it.name == name }
                ?: throw IllegalStateException("Could not find the $name field of $MESSAGE_EVENT_HANDLER_CLASS")
            val jsonInflater = field("jsonInflater")
            val persistenceHelper = field("messagePersistenceHelper")

            // Registers: v0 to v3, p0 (this), p1 (event). The event is parsed like Slack does,
            // and the getters keep their names for the patch keeping the message history,
            // which may hook this wrapper after them.
            fingerprint.classDef.wrapMethod(
                method,
                withoutLog,
                registerCount = 6,
                """
                    iget-object v0, p0, $MESSAGE_EVENT_HANDLER_CLASS->${jsonInflater.name}:${jsonInflater.type}
                    iget-object v1, p1, $SOCKET_EVENT_WRAPPER_CLASS->jsonData:${jsonData.type}
                    const-class v2, $MESSAGE_DELETED_EVENT_CLASS
                    invoke-interface { v0, v1, v2 }, $JSON_INFLATER_CLASS->inflate(${jsonData.type}Ljava/lang/Class;)Ljava/lang/Object;
                    move-result-object v1
                    check-cast v1, $MESSAGE_DELETED_EVENT_CLASS
                    invoke-virtual { v1 }, $MESSAGE_DELETED_EVENT_CLASS->getChannel()Ljava/lang/String;
                    move-result-object v2
                    invoke-virtual { v1 }, $MESSAGE_DELETED_EVENT_CLASS->getDeletedTs()Ljava/lang/String;
                    move-result-object v3
                    iget-object v1, p0, $MESSAGE_EVENT_HANDLER_CLASS->${persistenceHelper.name}:${persistenceHelper.type}
                    invoke-static { v1, v2, v3 }, $EXTENSION_CLASS->keepDeletedMessage($MESSAGE_PERSISTENCE_HELPER_CLASS${"Ljava/lang/String;Ljava/lang/String;"})Z
                    move-result v1
                    if-nez v1, :kept
                    invoke-virtual { p0, p1 }, $MESSAGE_EVENT_HANDLER_CLASS->$withoutLog($SOCKET_EVENT_WRAPPER_CLASS)V
                    :kept
                    return-void
                """
            )
        }

        // Add the logged messages to the pages of history, as Slack replaces its cache with them.
        HistoryFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val withoutLog = "historyWithoutLog"

            // Registers: v0, v1, p0 (this), p1 (request), p2 (trace context), p3 (background).
            fingerprint.classDef.wrapMethod(
                method,
                withoutLog,
                registerCount = 6,
                """
                    invoke-virtual { p0, p1, p2, p3 }, ${method.definingClass}->$withoutLog(${CONVERSATION_REQUEST_CLASS}Lslack/telemetry/tracing/TraceContext;Z)$SINGLE_CLASS
                    move-result-object v1
                    invoke-static { p1 }, $EXTENSION_CLASS->getHistoryMapper($CONVERSATION_REQUEST_CLASS)Lio/reactivex/rxjava3/functions/Function;
                    move-result-object v0
                    invoke-virtual { v1, v0 }, ${singleMethod("map", "Lio/reactivex/rxjava3/functions/Function;")}
                    move-result-object v1
                    return-object v1
                """
            )
        }

        GetThreadFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val withoutLog = "invokeSuspendWithoutLog"
            fun capturedField(name: String) = fingerprint.classDef.fields.singleOrNull { it.name == name }
                ?: throw IllegalStateException("Could not find the $name field of $GET_THREAD_COROUTINE_CLASS")
            val fields = listOf("\$channelId", "\$threadTs", "\$oldestTs", "\$latestTs").map(::capturedField)
            val inclusive = capturedField("\$inclusive")

            // Registers: v0 to v5, p0 (this), p1 (result of the previous step).
            fingerprint.classDef.wrapMethod(
                method,
                withoutLog,
                registerCount = 8,
                """
                    invoke-virtual { p0, p1 }, $GET_THREAD_COROUTINE_CLASS->$withoutLog(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    ${fields.mapIndexed { i, field ->
                        "iget-object v${i + 1}, p0, $GET_THREAD_COROUTINE_CLASS->${field.name}:Ljava/lang/String;"
                    }.joinToString("\n")}
                    iget-boolean v5, p0, $GET_THREAD_COROUTINE_CLASS->${inclusive.name}:Z
                    invoke-static/range { v0 .. v5 }, $EXTENSION_CLASS->addLoggedReplies(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;
                    move-result-object v0
                    return-object v0
                """
            )
        }
    }
}
