package app.template.patches.slack.history

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.shared.wrapMethod
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch
import app.template.patches.slack.shared.SINGLE_CLASS
import app.template.patches.slack.shared.SlackAppOnCreateFingerprint
import app.template.patches.slack.shared.singleMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/KeepMessageHistoryPatch;"

private const val ARCHIVE_ACTIVITY = "app.template.extension.slack.MessageArchiveActivity"

private const val ARCHIVE_SEARCH_ACTIVITY = "app.template.extension.slack.MessageArchiveSearchActivity"

private const val JSON_INFLATER_CLASS = "Lslack/commons/json/JsonInflater;"

private const val SHORTCUT_BACKGROUND_COLOR = "hagiahaya_shortcut_background"

/**
 * How many instructions before loading the SQL of a statement the lambda binding it is looked for.
 */
private const val BINDER_SEARCH_DISTANCE = 10

private val messageArchiveShortcutPatch = resourcePatch {
    execute {
        document("res/values/colors.xml").use { document ->
            document.documentElement.appendChild(
                document.createElement("color").apply {
                    setAttribute("name", SHORTCUT_BACKGROUND_COLOR)
                    textContent = "#4A154B"
                }
            )
        }

        /**
         * Adds a screen of the extension and a shortcut to it to the app icon,
         * with an icon of Slack in white on the aubergine of the app icon.
         */
        fun addScreen(activity: String, id: String, label: String, slackIcon: String) {
            document("AndroidManifest.xml").use { document ->
                document.getElementsByTagName("application").item(0).appendChild(
                    document.createElement("activity").apply {
                        setAttribute("android:name", activity)
                        setAttribute("android:label", "@string/$id")
                        setAttribute("android:exported", "false")
                    }
                )
            }

            document("res/values/strings.xml").use { document ->
                document.documentElement.appendChild(
                    document.createElement("string").apply {
                        setAttribute("name", id)
                        textContent = label
                    }
                )
            }

            val icon = "${id}_shortcut"
            get("res/drawable/$slackIcon.xml").copyTo(get("res/drawable/${icon}_foreground.xml"), overwrite = true)
            document("res/drawable/${icon}_foreground.xml").use { document ->
                val paths = document.getElementsByTagName("path")
                for (i in 0 until paths.length) {
                    (paths.item(i) as org.w3c.dom.Element).setAttribute("android:fillColor", "#FFFFFF")
                }
            }
            get("res/drawable/$icon.xml").writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@color/$SHORTCUT_BACKGROUND_COLOR" />
                    <foreground>
                        <inset android:drawable="@drawable/${icon}_foreground" android:inset="34%" />
                    </foreground>
                </adaptive-icon>
                """.trimIndent()
            )

            // Long pressing the app icon offers the screen.
            document("res/xml/shortcut.xml").use { document ->
                document.documentElement.appendChild(
                    document.createElement("shortcut").apply {
                        setAttribute("android:shortcutId", id)
                        setAttribute("android:enabled", "true")
                        setAttribute("android:icon", "@drawable/$icon")
                        setAttribute("android:shortcutShortLabel", "@string/$id")
                        appendChild(
                            document.createElement("intent").apply {
                                setAttribute("android:action", "android.intent.action.VIEW")
                                setAttribute("android:targetPackage", packageMetadata.packageName)
                                setAttribute("android:targetClass", activity)
                            }
                        )
                    }
                )
            }
        }

        addScreen(ARCHIVE_ACTIVITY, "hagiahaya_message_archive", "Message archive", "history")
        addScreen(ARCHIVE_SEARCH_ACTIVITY, "hagiahaya_message_archive_search", "Search archive", "search")
    }
}

/**
 * Calls the extension method [hookName] with the leading arguments of the lambda that binds
 * [statement], everywhere it is created.
 */
private fun BytecodePatchContext.hookCacheStatement(statement: CacheStatement, hookName: String) {
    val parameters = statement.binderParameters
    val registerCount = parameters.sumOf { if (it == "J" || it == "D") 2L else 1L }.toInt()
    // Classes of the app are passed as objects, which the extension cannot name.
    val hookParameters = parameters.joinToString("") {
        if (it.startsWith("L") && !it.startsWith("Ljava/")) "Ljava/lang/Object;" else it
    }
    var hooked = 0

    classDefByStrings(statement.sqlPrefix, StringComparisonType.STARTS_WITH).forEach { classDef ->
        mutableClassDefBy(classDef).methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val sqlIndices = instructions.indices.filter { index ->
                val instruction = instructions[index]
                (instruction.opcode == Opcode.CONST_STRING || instruction.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((instruction as ReferenceInstruction).reference as StringReference).string
                        .startsWith(statement.sqlPrefix)
            }

            // The lambda is created a few instructions before the SQL is loaded.
            val binderIndices = sqlIndices.map { sqlIndex ->
                (sqlIndex - 1 downTo maxOf(0, sqlIndex - BINDER_SEARCH_DISTANCE)).firstOrNull { index ->
                    val instruction = instructions[index]
                    val constructor = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    instruction.opcode == Opcode.INVOKE_DIRECT_RANGE && constructor?.name == "<init>" &&
                        constructor.parameterTypes.take(parameters.size).map(CharSequence::toString) == parameters
                } ?: throw PatchException("Could not find the arguments of \"${statement.sqlPrefix}\" in $method")
            }

            // From the last, so the indices of the others stay valid.
            binderIndices.distinct().sortedDescending().forEach { index ->
                // The first register is the lambda being created.
                val firstArgument = method.getInstruction<RegisterRangeInstruction>(index).startRegister + 1
                method.addInstruction(
                    index,
                    "invoke-static/range { v$firstArgument .. v${firstArgument + registerCount - 1} }, " +
                        "$EXTENSION_CLASS->$hookName($hookParameters)V"
                )
                hooked++
            }
        }
    }

    if (hooked == 0) throw PatchException("Could not find where \"${statement.sqlPrefix}\" is run")
}

private fun BytecodePatchContext.fieldOfType(classType: String, fieldType: String) =
    classDefBy(classType).fields.singleOrNull { it.type == fieldType }
        ?: throw IllegalStateException("Could not find the $fieldType field of $classType")

@Suppress("unused")
val keepMessageHistoryPatch = bytecodePatch(
    name = "Keep message history",
    description = "Keeps a copy of the messages Slack loads on the device, and shows the copies " +
        "where the server hides older messages, such as after 90 days on the free plan. " +
        "Only the messages loaded while the patched app is used are kept. " +
        "Long press the app icon and choose \"Search archive\" to search the copies, " +
        "or \"Message archive\" to turn it off, export or delete them.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(messageArchiveShortcutPatch, settingsPatch)

    execute {
        requireExtensionMethods(
            EXTENSION_CLASS,
            "initialize",
            "onMessageInserted",
            "onMessageInsertedIfAbsent",
            "onMessageUpdated",
            "onThreadMessageInserted",
            "onThreadMessageUpdated",
            "onMessageDeleted",
            "getArchivedHistory",
            "getHistoryMapper",
            "addArchivedReplies",
        )
        addPatchedFeature("keepMessageHistory")

        SlackAppOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->initialize(Landroid/content/Context;)V"
        )

        // region Archive the messages Slack caches.

        // The arguments are passed on as they are, from local_id to message_json.
        hookCacheStatement(insertMessageStatement, "onMessageInserted")
        hookCacheStatement(insertOrIgnoreMessageStatement, "onMessageInsertedIfAbsent")
        // From ts to message_blob.
        hookCacheStatement(insertThreadMessageStatement, "onThreadMessageInserted")
        // From ts to local_id.
        UpdateMessageByLocalIdFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p1 .. p10 }, $EXTENSION_CLASS->onMessageUpdated(" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
        )
        UpdateThreadMessageBlobFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p1 .. p3 }, $EXTENSION_CLASS->onThreadMessageUpdated(" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
        )

        RemoveMessageEventFingerprint.let { fingerprint ->
            val (getChannel, getDeletedTs) = fingerprint.instructionMatches
            fingerprint.method.apply {
                // The results are moved into registers right after the calls.
                val channelRegister = getInstruction<OneRegisterInstruction>(getChannel.index + 1).registerA
                val deletedTsIndex = getDeletedTs.index + 1
                val deletedTsRegister = getInstruction<OneRegisterInstruction>(deletedTsIndex).registerA

                addInstruction(
                    deletedTsIndex + 1,
                    "invoke-static { v$channelRegister, v$deletedTsRegister }, " +
                        "$EXTENSION_CLASS->onMessageDeleted(Ljava/lang/String;Ljava/lang/String;)V"
                )
            }
        }

        // endregion

        // region Show archived messages where the server hides them.

        ConversationHistoryFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val jsonInflater = fieldOfType(method.definingClass, JSON_INFLATER_CLASS)
            val historyWithoutArchive = "historyWithoutArchive"

            // Registers: v0, v1, p0 (this), p1 (request), p2 (trace context), p3 (background).
            fingerprint.classDef.wrapMethod(
                method,
                historyWithoutArchive,
                registerCount = 6,
                """
                    iget-object v0, p0, ${method.definingClass}->${jsonInflater.name}:$JSON_INFLATER_CLASS

                    # Answer the request for a page of the archive without sending it.
                    invoke-static { v0, p1 }, $EXTENSION_CLASS->getArchivedHistory($JSON_INFLATER_CLASS$CONVERSATION_REQUEST_CLASS)Lslack/services/api/conversations/MsgHistory;
                    move-result-object v1
                    if-eqz v1, :send
                    invoke-static { v1 }, ${singleMethod("just", "Ljava/lang/Object;")}
                    move-result-object v1
                    return-object v1

                    :send
                    invoke-virtual { p0, p1, p2, p3 }, ${method.definingClass}->$historyWithoutArchive(${CONVERSATION_REQUEST_CLASS}Lslack/telemetry/tracing/TraceContext;Z)$SINGLE_CLASS
                    move-result-object v1
                    invoke-static { v0, p1 }, $EXTENSION_CLASS->getHistoryMapper($JSON_INFLATER_CLASS$CONVERSATION_REQUEST_CLASS)Lio/reactivex/rxjava3/functions/Function;
                    move-result-object v0
                    invoke-virtual { v1, v0 }, ${singleMethod("map", "Lio/reactivex/rxjava3/functions/Function;")}
                    move-result-object v1
                    return-object v1
                """
            )
        }

        GetThreadInvokeSuspendFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val coroutine = classDefBy(GET_THREAD_COROUTINE_CLASS)
            fun capturedField(name: String) = coroutine.fields.singleOrNull { it.name == name }
                ?: throw IllegalStateException("Could not find the $name field of $GET_THREAD_COROUTINE_CLASS")

            val repository = capturedField("this\$0")
            val jsonInflater = fieldOfType(repository.type, JSON_INFLATER_CLASS)
            val fields = listOf("\$channelId", "\$threadTs", "\$oldestTs", "\$latestTs").map(::capturedField)
            val inclusive = capturedField("\$inclusive")
            val invokeSuspendWithoutArchive = "invokeSuspendWithoutArchive"

            // Registers: v0 to v6, p0 (this), p1 (result of the previous step).
            fingerprint.classDef.wrapMethod(
                method,
                invokeSuspendWithoutArchive,
                registerCount = 9,
                """
                    invoke-virtual { p0, p1 }, $GET_THREAD_COROUTINE_CLASS->$invokeSuspendWithoutArchive(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    iget-object v1, p0, $GET_THREAD_COROUTINE_CLASS->${repository.name}:${repository.type}
                    iget-object v1, v1, ${repository.type}->${jsonInflater.name}:$JSON_INFLATER_CLASS
                    ${fields.mapIndexed { i, field ->
                        "iget-object v${i + 2}, p0, $GET_THREAD_COROUTINE_CLASS->${field.name}:Ljava/lang/String;"
                    }.joinToString("\n")}
                    iget-boolean v6, p0, $GET_THREAD_COROUTINE_CLASS->${inclusive.name}:Z
                    invoke-static/range { v0 .. v6 }, $EXTENSION_CLASS->addArchivedReplies(Ljava/lang/Object;${JSON_INFLATER_CLASS}Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;
                    move-result-object v0
                    return-object v0
                """
            )
        }

        // endregion
    }
}
