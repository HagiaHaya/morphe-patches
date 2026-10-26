package app.template.patches.pepper.price

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.replaceMethod
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/PriceWithShippingPatch;"

/**
 * Deals in lists and with their details, as the REST API returns them. Their names are kept,
 * because kotlinx.serialization needs them, and the app reads them through the getters only.
 */
private val THREAD_CLASSES = listOf(
    "Lcom/pepper/network/apirepresentation/ThreadApiRepresentation;",
    "Lcom/pepper/network/apirepresentation/ThreadFullApiRepresentation;",
)

@Suppress("unused")
val showPriceWithShippingPatch = bytecodePatch(
    name = "Show price with shipping",
    description = "Adds a setting to show the price with shipping next to the shipping costs of deals, " +
        "such as \"+ 9,99zł = 59,98zł\".",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "getShippingCostsDisplay")

        addPatchedFeature("showPriceWithShipping")

        THREAD_CLASSES.forEach { threadClass ->
            mutableClassDefBy(threadClass).apply {
                replaceMethod(
                    methods.single { it.name == "getShippingCostsDisplay" && it.parameterTypes.isEmpty() },
                    registerCount = 5,
                    """
                        iget-object v0, p0, $threadClass->shippingCostsDisplay:Ljava/lang/String;
                        iget-object v1, p0, $threadClass->price:Ljava/lang/Double;
                        iget-object v2, p0, $threadClass->shippingCosts:Ljava/lang/Double;
                        iget-object v3, p0, $threadClass->priceDisplay:Ljava/lang/String;
                        invoke-static { v0, v1, v2, v3 }, $EXTENSION_CLASS->getShippingCostsDisplay(Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v0
                        return-object v0
                    """
                )
            }
        }
    }
}
