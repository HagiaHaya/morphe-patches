package app.template.patches.pepper.calendar

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.api.processApiResponseData
import app.template.patches.pepper.deal.addDealMenuItem
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/DealCalendarPatch;"

@Suppress("unused")
val addDealsToCalendarPatch = bytecodePatch(
    name = "Add deals to calendar",
    description = "Adds \"Add to calendar\" to the menu of a deal, which creates an event when the deal " +
        "starts, or before it expires.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "rememberDeal", "addMenuItem", "onMenuItemSelected")

        // Keep the details of the deals the app loads, as the screen of a deal only knows its ID.
        processApiResponseData("$EXTENSION_CLASS->rememberDeal")

        addDealMenuItem(EXTENSION_CLASS)
    }
}
