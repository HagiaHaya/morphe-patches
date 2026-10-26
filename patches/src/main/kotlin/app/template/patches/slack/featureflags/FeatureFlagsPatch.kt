package app.template.patches.slack.featureflags

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/FeatureFlagsPatch;"

@Suppress("unused")
val featureFlagsPatch = bytecodePatch(
    name = "Feature flags",
    description = "Adds a screen to the patch settings to turn the feature flags of Slack on or off, " +
        "which can enable features and experiments Slack has not released yet.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "onStoreCreated", "override")
        addPatchedFeature("featureFlags")

        // Keep the store once it is initialized as an object. The flags it loads afterwards
        // are read only when the settings are opened.
        FeatureFlagStoreConstructorFingerprint.method.apply {
            val superConstructorIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_DIRECT &&
                    ((it as ReferenceInstruction).reference as MethodReference).name == "<init>"
            }
            addInstruction(
                superConstructorIndex + 1,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onStoreCreated($FEATURE_FLAG_STORE_CLASS)V"
            )
        }

        FeatureFlagIsEnabledFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->override(Lslack/featureflag/FeatureFlagEnum;)I
                move-result v0
                if-ltz v0, :check
                return v0
                :check
                nop
            """
        )
    }
}
