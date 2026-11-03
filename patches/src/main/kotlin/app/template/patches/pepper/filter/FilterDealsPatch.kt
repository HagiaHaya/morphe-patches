package app.template.patches.pepper.filter

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.api.processApiResponseData
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/DealFilterPatch;"

@Suppress("unused")
val filterDealsPatch = bytecodePatch(
    name = "Filter deals",
    description = "Adds settings to hide deals and discussions from all lists by keyword, store, member, " +
        "category, highest price or lowest temperature.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "filterThreads")

        addPatchedFeature("filterDeals")

        processApiResponseData("$EXTENSION_CLASS->filterThreads")
    }
}
