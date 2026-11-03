package app.template.patches.pepper.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.requireExtensionMethods

private const val SETTINGS_CLASS = "Lapp/template/extension/pepper/settings/Settings;"

private const val PATCHED_FEATURES_CLASS = "Lapp/template/extension/pepper/settings/PatchedFeatures;"

private const val SETTINGS_ACTIVITY = "app.template.extension.pepper.settings.SettingsActivity"

private const val SETTINGS_ICON = "hagiahaya_ic_settings"

private val settingsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.getElementsByTagName("application").item(0).appendChild(
                document.createElement("activity").apply {
                    setAttribute("android:name", SETTINGS_ACTIVITY)
                    setAttribute("android:exported", "false")
                }
            )
        }

        // The settings tint the icons of their own entries only, so tint a copy of the icon alike.
        get("res/drawable/ic_settings.xml").copyTo(get("res/drawable/$SETTINGS_ICON.xml"), overwrite = true)
        document("res/drawable/$SETTINGS_ICON.xml").use { document ->
            document.documentElement.setAttribute("android:tint", "@color/graphic_translucent_secondary")
        }

        // The settings list the screen of each preferences file, so this adds an entry
        // below "Customize experience", which opens the settings of the patches.
        document("res/xml/preferences_application.xml").use { document ->
            document.documentElement.appendChild(
                document.createElement("androidx.preference.Preference").apply {
                    setAttribute("android:key", "hagiahaya_patches")
                    setAttribute("android:title", "HagiaHaya Patches")
                    setAttribute("android:icon", "@drawable/$SETTINGS_ICON")
                    appendChild(
                        document.createElement("intent").apply {
                            setAttribute("android:targetPackage", packageMetadata.packageName)
                            setAttribute("android:targetClass", SETTINGS_ACTIVITY)
                        }
                    )
                }
            )
        }
    }
}

/**
 * Adds the settings of the patches to the settings of Pepper.
 * Patches with settings depend on it and call [addPatchedFeature].
 */
internal val settingsPatch = bytecodePatch {
    dependsOn(settingsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(SETTINGS_CLASS, "initialize")

        PepperApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $SETTINGS_CLASS->initialize(Landroid/content/Context;)V"
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
