package app.template.patches.slack.typing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/SilentTypingPatch;"

@Suppress("unused")
val silentTypingPatch = bytecodePatch(
    name = "Silent typing",
    description = "Adds an option to not show others that you are typing, while still showing when they are.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "hideTyping")
        addPatchedFeature("silentTyping")

        SendTypingMessageFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS->hideTyping()Z
                move-result v0
                if-eqz v0, :send
                return-void
                :send
                nop
            """
        )
    }
}
