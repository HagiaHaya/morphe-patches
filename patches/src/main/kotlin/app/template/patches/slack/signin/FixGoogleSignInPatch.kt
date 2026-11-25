package app.template.patches.slack.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/FixGoogleSignInPatch;"

private const val GET_STARTED_KEY_CLASS = "Lslack/navigation/key/AuthOptionsIntentKey\$GetStarted;"

@Suppress("unused")
val fixGoogleSignInPatch = bytecodePatch(
    name = "Fix Google sign-in",
    description = "Fixes signing in with Google staying on a loading button after choosing the account.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "openGetStarted")

        // After Google accepts the account, the email screen opens the sign-in screen again with
        // the next step, the workspaces of the account. That screen is already shown and launched
        // as singleTop, so the step arrives as a new intent. The new sign-in screen of Slack hands
        // it to the workspaces screen through a channel, but only the email screen is shown,
        // which never reads it, so signing in waits forever. Open the screen with the step
        // instead, which shows the workspaces screen like a deep link does.
        AuthOptionsActivityOnNewIntentFingerprint.let {
            val relayIndex = it.instructionMatches.last().index
            val castIndex = relayIndex - 1
            val cast = it.method.getInstruction<ReferenceInstruction>(castIndex)
            check(
                it.method.getInstruction(castIndex).opcode == Opcode.CHECK_CAST &&
                    (cast.reference as TypeReference).type == GET_STARTED_KEY_CLASS
            ) { "Could not find the next step of signing in" }
            val keyRegister = (cast as OneRegisterInstruction).registerA

            it.method.addInstructions(
                relayIndex,
                """
                    invoke-static { p0, v$keyRegister }, $EXTENSION_CLASS->openGetStarted(Landroid/app/Activity;Landroid/os/Parcelable;)V
                    return-void
                """
            )
        }
    }
}
