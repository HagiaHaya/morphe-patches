package app.template.patches.wykop.newsletter

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WYKOP
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.wykop.ad.LoadCustomJavaScriptFingerprint
import app.template.patches.wykop.ad.webViewClientClasses

private const val EXTENSION_CLASS = "Lapp/template/extension/wykop/HideNewsletterPromptPatch;"

@Suppress("unused")
val hideNewsletterPromptPatch = bytecodePatch(
    name = "Hide newsletter prompt",
    description = "Hides the prompt to sign up for the newsletter on the website shown by the app.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WYKOP)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "hideNewsletterPrompt")

        // The prompt is part of the website, so hide it with a stylesheet on every page load.
        webViewClientClasses.forEach { webViewClientClass ->
            listOf(
                "loadCustomJavaScriptOnPageStarted",
                "loadCustomJavaScriptOnPageFinished",
            ).forEach { methodName ->
                LoadCustomJavaScriptFingerprint(webViewClientClass, methodName).method.addInstruction(
                    0,
                    "invoke-static { p1 }, $EXTENSION_CLASS->hideNewsletterPrompt(Landroid/webkit/WebView;)V"
                )
            }
        }
    }
}
