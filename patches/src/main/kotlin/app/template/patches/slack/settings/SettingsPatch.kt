package app.template.patches.slack.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.shared.SlackAppOnCreateFingerprint

private const val SETTINGS_CLASS = "Lapp/template/extension/slack/settings/Settings;"

private const val PREFERENCES_ENTRY_CLASS = "Lapp/template/extension/slack/settings/PreferencesEntry;"

private const val PATCHED_FEATURES_CLASS = "Lapp/template/extension/slack/settings/PatchedFeatures;"

private const val EXTENSION_PACKAGE = "app.template.extension.slack.settings"

/**
 * Screens of the settings, which open from the preferences of Slack.
 */
private val SCREENS = listOf("SettingsActivity", "NicknamesActivity", "FeatureFlagsActivity")

private val settingsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)
            SCREENS.forEach { screen ->
                application.appendChild(
                    document.createElement("activity").apply {
                        setAttribute("android:name", "$EXTENSION_PACKAGE.$screen")
                        setAttribute("android:exported", "false")
                        // Search fields on the screens resize them instead of covering them.
                        setAttribute("android:windowSoftInputMode", "adjustResize")
                    }
                )
            }
        }
    }
}

/**
 * Adds the settings of the patches to the preferences of Slack.
 * Patches with settings depend on it and call [addPatchedFeature].
 */
internal val settingsPatch = bytecodePatch {
    dependsOn(settingsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(SETTINGS_CLASS, "initialize")
        requireExtensionMethods(PREFERENCES_ENTRY_CLASS, "addEntry", "wrapEventSink")

        SlackAppOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $SETTINGS_CLASS->initialize(Landroid/content/Context;)V"
        )

        // Registers: p1 (entries), p2 (dialog), p3 (event handler).
        PreferenceSettingsStateFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $PREFERENCES_ENTRY_CLASS->addEntry(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
                invoke-static { p3 }, $PREFERENCES_ENTRY_CLASS->wrapEventSink(Lkotlin/jvm/functions/Function1;)Lkotlin/jvm/functions/Function1;
                move-result-object p3
            """
        )
    }
}

/**
 * Shows the settings of a patch on the settings screen.
 *
 * @param name The name of the method of PatchedFeatures for the patch.
 */
internal fun BytecodePatchContext.addPatchedFeature(name: String) {
    requireExtensionMethods(PATCHED_FEATURES_CLASS, name)

    Fingerprint(
        definingClass = PATCHED_FEATURES_CLASS,
        name = name,
    ).method.addInstructions(
        0,
        """
            const/4 v0, 0x1
            return v0
        """
    )
}
