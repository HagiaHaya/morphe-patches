package app.template.patches.slack.markdown

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.replaceMethod
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/StandardMarkdownPatch;"

@Suppress("unused")
val standardMarkdownPatch = bytecodePatch(
    name = "Standard Markdown",
    description = "Adds an option to format messages as you type standard Markdown, such as **bold**, " +
        "*italic*, ~~strikethrough~~ and [text](link), instead of Slack markup.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "tagResult", "newBoldSpan", "newItalicSpan", "newStrikethroughSpan")
        addPatchedFeature("standardMarkdown")

        // R8 removed the constructors of the bold, italic and strikethrough spans,
        // so create them like Slack does, with the constructor of their superclass.
        mapOf(
            "newBoldSpan" to """
                new-instance v0, Lslack/textformatting/spans/BoldStyleSpan;
                const/4 v1, 0x1
                invoke-direct { v0, v1 }, Landroid/text/style/StyleSpan;-><init>(I)V
                return-object v0
            """,
            "newItalicSpan" to """
                new-instance v0, Lslack/textformatting/spans/ItalicStyleSpan;
                const/4 v1, 0x2
                invoke-direct { v0, v1 }, Landroid/text/style/StyleSpan;-><init>(I)V
                return-object v0
            """,
            "newStrikethroughSpan" to """
                new-instance v0, Lslack/textformatting/spans/StrikethroughStyleSpan;
                invoke-direct { v0 }, Landroid/text/style/StrikethroughSpan;-><init>()V
                return-object v0
            """,
        ).forEach { (name, smali) ->
            Fingerprint(
                definingClass = EXTENSION_CLASS,
                name = name,
            ).let { fingerprint ->
                fingerprint.classDef.replaceMethod(fingerprint.method, registerCount = 2, smali)
            }
        }

        // Registers: p0 (this), p1 (text), p2 (cursor), p3 (text view), which are high ones.
        MarkdownTagResultFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static/range { p0 .. p3 }, $EXTENSION_CLASS->tagResult(Lslack/services/autotag/inline/MarkdownAutoInlineTagProvider;Landroid/text/Editable;ILslack/services/slacktextview/SlackTextView${'$'}autoInlineTagView${'$'}1;)I
                move-result v0
                if-ltz v0, :format
                return v0
                :format
                nop
            """
        )
    }
}
