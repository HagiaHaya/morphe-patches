package app.template.patches.pepper.icon

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/UnlockCustomAppIconsPatch;"

@Suppress("unused")
val unlockCustomAppIconsPatch = bytecodePatch(
    name = "Unlock custom app icons",
    description = "Unlocks all app icons regardless of the contributor level, " +
        "and offers the seasonal icons, such as the Black Friday one, all year round.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "getMinTierLevel", "isEventThemingAppIcon")

        addPatchedFeature("unlockCustomAppIcons")

        CustomAppIconListFingerprint.method.apply {
            // CustomAppIcon(resIcon, name, manifestAliasId, minTierLevel, analyticsName, isEventThemingAppIcon)
            val customAppIconConstructor = instructions.firstNotNullOf { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.takeIf {
                    it.name == "<init>" && it.parameterTypes.getOrNull(2) == MANIFEST_ALIAS_ID_CLASS
                }
            }
            val customAppIconClass = customAppIconConstructor.definingClass
            val tierLevelClass = customAppIconConstructor.parameterTypes[3].toString()

            fun indexOfCustomAppIconFieldRead(opcode: Opcode, fieldType: String) = instructions.indexOfFirst {
                val field = (it as? ReferenceInstruction)?.reference as? FieldReference
                it.opcode == opcode && field?.definingClass == customAppIconClass && field.type == fieldType
            }.also { index ->
                check(index >= 0) { "Could not find the read of the $fieldType field of $customAppIconClass" }
            }

            val minTierLevelIndex = indexOfCustomAppIconFieldRead(Opcode.IGET_OBJECT, tierLevelClass)
            val isEventThemingAppIconIndex = indexOfCustomAppIconFieldRead(Opcode.IGET_BOOLEAN, "Z")

            // Add the later instructions first, so the earlier index stays valid.
            mapOf(
                // Read the level an icon needs as DEFAULT, so all icons are listed and unlocked,
                // also when signed out.
                minTierLevelIndex to { register: Int ->
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->getMinTierLevel(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$register
                        check-cast v$register, $tierLevelClass
                    """
                },
                // Read isEventThemingAppIcon as false, so the seasonal icons are listed like the others.
                isEventThemingAppIconIndex to { register: Int ->
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->isEventThemingAppIcon(Z)Z
                        move-result v$register
                    """
                },
            ).toSortedMap(reverseOrder()).forEach { (index, smali) ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(index + 1, smali(register))
            }
        }
    }
}
