package app.template.patches.pepper.comment

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.api.processApiResponseData
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/CommentFilterPatch;"

@Suppress("unused")
val filterCommentsPatch = bytecodePatch(
    name = "Filter comments",
    description = "Adds a setting to hide comments, and the replies to them, that contain certain words.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "filterComments")

        addPatchedFeature("filterComments")

        processApiResponseData("$EXTENSION_CLASS->filterComments")
    }
}
