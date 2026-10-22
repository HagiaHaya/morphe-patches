package app.template.patches.pepper.icon

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val unlockCustomAppIconsPatch = bytecodePatch(
    name = "Unlock custom app icons",
    description = "Unlocks all app icons regardless of the contributor level, " +
        "and offers the seasonal icons, such as the Black Friday one, all year round.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    execute {
        CustomAppIconListFingerprint.method.apply {
            // CustomAppIcon(resIcon, name, manifestAliasId, minTierLevel, analyticsName, isEventThemingAppIcon)
            val customAppIconConstructor = instructions.firstNotNullOf { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.takeIf {
                    it.name == "<init>" && it.parameterTypes.getOrNull(2) == MANIFEST_ALIAS_ID_CLASS
                }
            }
            val customAppIconClass = customAppIconConstructor.definingClass
            val tierLevelClass = customAppIconConstructor.parameterTypes[3].toString()

            // The DEFAULT contributor level, which every user has.
            val defaultTierLevelField = classDefBy(tierLevelClass).methods
                .first { it.name == "<clinit>" }.instructions.toList().let { clinit ->
                    val defaultNameIndex = clinit.indexOfFirst {
                        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "DEFAULT"
                    }
                    clinit.drop(defaultNameIndex).first { it.opcode == Opcode.SPUT_OBJECT }
                        .let { (it as ReferenceInstruction).reference as FieldReference }
                }

            fun indexOfCustomAppIconFieldRead(opcode: Opcode, fieldType: String) = instructions.indexOfFirst {
                val field = (it as? ReferenceInstruction)?.reference as? FieldReference
                it.opcode == opcode && field?.definingClass == customAppIconClass && field.type == fieldType
            }.also { index ->
                check(index >= 0) { "Could not find the read of the $fieldType field of $customAppIconClass" }
            }

            // Read the level an icon needs as DEFAULT, so all icons are listed and unlocked,
            // also when signed out.
            indexOfCustomAppIconFieldRead(Opcode.IGET_OBJECT, tierLevelClass).let { index ->
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                replaceInstruction(index, "sget-object v$register, $defaultTierLevelField")
            }

            // Read isEventThemingAppIcon as false, so the seasonal icons are listed like the others.
            indexOfCustomAppIconFieldRead(Opcode.IGET_BOOLEAN, "Z").let { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                replaceInstruction(index, "const/16 v$register, 0x0")
            }
        }
    }
}
