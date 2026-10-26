/*
 * Based on the GmsCore support patch of Morphe Patches, changed for Wykop.pl.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.template.patches.wykop.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_WYKOP
import app.template.patches.shared.gms.gmsCoreSupportResourcePatch
import app.template.patches.shared.gms.spoofFirebaseCertificate
import app.template.patches.shared.gms.useGmsCore
import app.template.patches.shared.requireExtensionMethods

/**
 * SHA-1 of the certificate the original app is signed with.
 */
private const val ORIGINAL_SIGNATURE = "7d58ecc599b638626b92aa4e2ae58f8b83b5db6c"

private const val MAIN_ACTIVITY_CLASS = "Lpl/wykop/droid/MainActivity;"

private const val EXTENSION_CLASS = "Lapp/template/extension/wykop/GmsCoreSupportPatch;"

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Allows signing in with Google and receiving notifications " +
        "by using MicroG-RE instead of Google Play services.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WYKOP)

    dependsOn(gmsCoreSupportResourcePatch(COMPATIBILITY_WYKOP.packageName!!, ORIGINAL_SIGNATURE))

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "checkGmsCore")

        useGmsCore()

        // The API key of Wykop.pl only accepts the certificate of the original app.
        spoofFirebaseCertificate(ORIGINAL_SIGNATURE)

        // Ask to install GmsCore if it is missing.
        val flutterActivityClass = classDefBy(MAIN_ACTIVITY_CLASS).superclass!!
        ActivityOnCreateFingerprint(flutterActivityClass).method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkGmsCore(Landroid/app/Activity;)V"
        )
    }
}
