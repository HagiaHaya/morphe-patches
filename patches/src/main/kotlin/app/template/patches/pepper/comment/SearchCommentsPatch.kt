package app.template.patches.pepper.comment

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.pepper.api.pepperApiPatch
import app.template.patches.pepper.deal.addDealMenuItem
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/CommentSearchPatch;"

private const val SEARCH_ACTIVITY = "app.template.extension.pepper.CommentSearchActivity"

private val searchCommentsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.getElementsByTagName("application").item(0).appendChild(
                document.createElement("activity").apply {
                    setAttribute("android:name", SEARCH_ACTIVITY)
                    setAttribute("android:exported", "false")
                }
            )
        }
    }
}

@Suppress("unused")
val searchCommentsPatch = bytecodePatch(
    name = "Search comments",
    description = "Adds \"Search comments\" to the menu of a deal, which loads all its comments and replies, " +
        "and finds the ones with the words entered.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(pepperApiPatch, searchCommentsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "addMenuItem", "onMenuItemSelected")

        addDealMenuItem(EXTENSION_CLASS)
    }
}
