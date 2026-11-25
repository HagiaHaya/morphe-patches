package app.template.patches.slack.files

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/AnonymizeFileNamesPatch;"

@Suppress("unused")
val anonymizeFileNamesPatch = bytecodePatch(
    name = "Anonymize file names",
    description = "Adds an option to upload files under a random name, keeping the extension, " +
        "so the name does not reveal anything about the file or the device.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "anonymize")
        addPatchedFeature("anonymizeFileNames")

        // Registers: p0 (API), p1 and p2 (length), p3 (file name).
        GetUploadUrlFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p3 .. p3 }, $EXTENSION_CLASS->anonymize(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p3
            """
        )
    }
}
