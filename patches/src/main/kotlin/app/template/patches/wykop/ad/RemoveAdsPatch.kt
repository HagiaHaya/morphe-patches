package app.template.patches.wykop.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WYKOP

private const val EXTENSION_CLASS = "Lapp/template/extension/wykop/RemoveAdsPatch;"

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Hides ads and blocks ad networks on the website shown by the app.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WYKOP)

    extendWith("extensions/extension.mpe")

    execute {
        webViewClientClasses.forEach { webViewClientClass ->
            // Answer requests to ad servers with an empty response,
            // before the plugin gets a chance to load them.
            ShouldInterceptRequestFingerprint(webViewClientClass).method.addInstructionsWithLabels(
                0,
                """
                    invoke-virtual { p2 }, $WEB_RESOURCE_REQUEST_EXT_CLASS->getUrl()Ljava/lang/String;
                    move-result-object v0
                    invoke-static { v0 }, $EXTENSION_CLASS->blockAdRequest(Ljava/lang/String;)Landroid/webkit/WebResourceResponse;
                    move-result-object v0
                    if-eqz v0, :load
                    return-object v0
                    :load
                    nop
                """
            )

            // The website serves its own ads from wykop.pl and keeps empty, fixed height
            // ad slots, so hide those with a stylesheet on every page load.
            listOf(
                "loadCustomJavaScriptOnPageStarted",
                "loadCustomJavaScriptOnPageFinished",
            ).forEach { methodName ->
                LoadCustomJavaScriptFingerprint(webViewClientClass, methodName).method.addInstruction(
                    0,
                    "invoke-static { p1 }, $EXTENSION_CLASS->hideAdSlots(Landroid/webkit/WebView;)V"
                )
            }
        }
    }
}
