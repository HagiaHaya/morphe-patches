package app.template.patches.pepper.flamedeer

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.PepperApplicationOnCreateFingerprint
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/CatchFlamedeerPatch;"

@Suppress("unused")
val catchFlamedeerPatch = bytecodePatch(
    name = "Catch Flamedeer",
    description = "Adds a setting to get a notification when a Flamedeer (\"Ognisty Renifer\") " +
        "of the Flamedeer hunt shows up, or to catch it at once by opening its prize screen. " +
        "Flamedeers found while the app is in the background get a notification instead of being missed.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(
            EXTENSION_CLASS,
            "initialize",
            "onFlamedeerFound",
            "hideFlamedeer",
            "onPrizeScreenCreated",
        )

        addPatchedFeature("catchFlamedeer")

        // Track the screen in the foreground, which opens the prize screen.
        PepperApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->initialize(Landroid/app/Application;)V"
        )

        // Hand the prize the server reports over, before the app shows its Flamedeer.
        FlamedeerHeaderInterceptorFingerprint.let {
            val parseLongIndex = it.instructionMatches.last().index
            val moveResult = it.method.getInstruction<OneRegisterInstruction>(parseLongIndex + 1)
            if (moveResult.opcode != Opcode.MOVE_RESULT_WIDE) {
                throw PatchException("Expected the prize ID to be moved after it is parsed")
            }
            val register = moveResult.registerA

            it.method.addInstruction(
                parseLongIndex + 2,
                "invoke-static/range { v$register .. v${register + 1} }, " +
                    "$EXTENSION_CLASS->onFlamedeerFound(J)V"
            )
        }

        // Do not show the Flamedeer of a prize the patch caught already, which could not be claimed again.
        ShowFlamedeerFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static/range { p0 .. p2 }, $EXTENSION_CLASS->hideFlamedeer(Landroid/app/Activity;J)Z
                move-result v0
                if-eqz v0, :show
                return-void
                :show
                nop
            """
        )

        // Remove the notification and the Flamedeer of the prize, once its prize screen opens.
        PrizeScreenOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onPrizeScreenCreated(Landroid/app/Activity;)V"
        )
    }
}
