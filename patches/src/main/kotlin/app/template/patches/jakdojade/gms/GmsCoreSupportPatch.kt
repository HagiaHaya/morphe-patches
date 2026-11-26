/*
 * Based on the GmsCore support patch of Morphe Patches, changed for Jakdojade.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.template.patches.jakdojade.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JAKDOJADE
import app.template.patches.shared.gms.gmsCoreSupportResourcePatch
import app.template.patches.shared.gms.resetPushRegistration
import app.template.patches.shared.gms.spoofFirebaseCertificate
import app.template.patches.shared.gms.useGmsCore
import app.template.patches.shared.requireExtensionMethods

/**
 * SHA-1 of the certificate the original app is signed with.
 */
private const val ORIGINAL_SIGNATURE = "b1265d6f4874f6b847a45b6691efdfe6d62bdd55"

private const val EXTENSION_CLASS = "Lapp/template/extension/jakdojade/GmsCoreSupportPatch;"

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Allows signing in with Google, finding the location and receiving notifications " +
        "by using MicroG-RE instead of Google Play services.",
    default = true
) {
    compatibleWith(COMPATIBILITY_JAKDOJADE)

    dependsOn(gmsCoreSupportResourcePatch(COMPATIBILITY_JAKDOJADE.packageName!!, ORIGINAL_SIGNATURE))

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "checkGmsCore", "resetPushRegistration")

        useGmsCore()

        // The API key of Jakdojade only accepts the certificate of the original app.
        spoofFirebaseCertificate(ORIGINAL_SIGNATURE)

        resetPushRegistration(EXTENSION_CLASS)

        // Ask to install GmsCore if it is missing. The launcher activity is a plain Activity
        // that overrides onCreate(), so the check is injected there directly.
        SplashScreenActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkGmsCore(Landroid/app/Activity;)V"
        )
    }
}
