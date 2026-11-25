package app.template.patches.jakdojade.game

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JAKDOJADE
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/template/extension/jakdojade/GameShortcut;"

@Suppress("unused")
val gameShortcutPatch = bytecodePatch(
    name = "Add mini-game shortcut",
    description = "Adds the built-in mini-game to the menu shown when you long-press " +
        "the Jakdojade icon. Open Jakdojade once to add the shortcut.",
    default = false
) {
    compatibleWith(COMPATIBILITY_JAKDOJADE)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "publish")

        val method = SplashScreenOnCreateFingerprint.method
        val superCall = method.instructions.withIndex().filter { (_, instruction) ->
            instruction.opcode in listOf(Opcode.INVOKE_SUPER, Opcode.INVOKE_SUPER_RANGE) &&
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "onCreate"
        }.singleOrNull() ?: throw PatchException("Expected one super.onCreate call in SplashScreenActivity")

        method.addInstruction(
            superCall.index + 1,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->publish(Landroid/content/Context;)V"
        )
    }
}
