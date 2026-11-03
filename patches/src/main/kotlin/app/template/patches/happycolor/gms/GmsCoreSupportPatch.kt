/*
 * Based on the GmsCore support patch of Morphe Patches, changed for Happy Color.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.template.patches.happycolor.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_HAPPY_COLOR
import app.template.patches.shared.gms.gmsCoreSupportResourcePatch
import app.template.patches.shared.gms.resetPushRegistration
import app.template.patches.shared.gms.spoofFirebaseCertificate
import app.template.patches.shared.gms.useGmsCore
import app.template.patches.shared.requireExtensionMethods

/**
 * SHA-1 of the certificate the original app is signed with.
 */
private const val ORIGINAL_SIGNATURE = "97ad836d29b9e1ddbef6df329bdcc171bfd5cf83"

private const val EXTENSION_CLASS = "Lapp/template/extension/happycolor/GmsCoreSupportPatch;"

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Allows signing in with Google and receiving notifications " +
        "by using MicroG-RE instead of Google Play services.",
    default = true
) {
    compatibleWith(COMPATIBILITY_HAPPY_COLOR)

    dependsOn(gmsCoreSupportResourcePatch(COMPATIBILITY_HAPPY_COLOR.packageName!!, ORIGINAL_SIGNATURE))

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "checkGmsCore", "resetPushRegistration")

        useGmsCore()

        // Firebase Auth, which the Google sign-in of Happy Color ends in, sends the certificate to Google.
        spoofFirebaseCertificate(ORIGINAL_SIGNATURE)

        resetPushRegistration(EXTENSION_CLASS)

        // Ask to install GmsCore if it is missing.
        MainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkGmsCore(Landroid/app/Activity;)V"
        )
    }
}
