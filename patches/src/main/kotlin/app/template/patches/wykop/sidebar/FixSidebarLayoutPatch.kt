package app.template.patches.wykop.sidebar

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WYKOP
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.wykop.ad.LoadCustomJavaScriptFingerprint
import app.template.patches.wykop.ad.webViewClientClasses

private const val EXTENSION_CLASS = "Lapp/template/extension/wykop/FixSidebarLayoutPatch;"

@Suppress("unused")
val fixSidebarLayoutPatch = bytecodePatch(
    name = "Fix sidebar layout",
    description = "Keeps the sidebar of the website shown by the app clear of the status bar and the navigation bar.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WYKOP)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "fixSidebarLayout")

        // The app draws the WebView edge-to-edge and the website pads its header for the system bars,
        // but not its sidebar, so move the sidebar with a stylesheet on every page load.
        webViewClientClasses.forEach { webViewClientClass ->
            listOf(
                "loadCustomJavaScriptOnPageStarted",
                "loadCustomJavaScriptOnPageFinished",
            ).forEach { methodName ->
                LoadCustomJavaScriptFingerprint(webViewClientClass, methodName).method.addInstruction(
                    0,
                    "invoke-static { p1 }, $EXTENSION_CLASS->fixSidebarLayout(Landroid/webkit/WebView;)V"
                )
            }
        }
    }
}
