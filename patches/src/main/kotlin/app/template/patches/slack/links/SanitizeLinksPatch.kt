package app.template.patches.slack.links

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/SanitizeLinksPatch;"

private const val LINK_CHUNK_CLASS = "Lslack/model/text/richtext/chunks/FormattedChunk\$LinkChunk;"

/**
 * Parameters of the constructor of a link chunk: type, style, link, text, two flags,
 * and the bit mask of the parameters left at their defaults.
 */
private val LINK_CHUNK_PARAMETERS = listOf(
    "Ljava/lang/String;",
    "Lslack/model/text/richtext/chunks/Style;",
    "Ljava/lang/String;",
    "Ljava/lang/String;",
    "Z",
    "Z",
    "I",
)

@Suppress("unused")
val sanitizeLinksPatch = bytecodePatch(
    name = "Sanitize links",
    description = "Adds an option to remove tracking parameters, such as utm_source or fbclid, " +
        "from the links in the messages you send.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "sanitize", "sanitizeText")
        addPatchedFeature("sanitizeLinks")

        EncodeFormattedChunksFingerprint.method.apply {
            val constructorIndices = implementation!!.instructions.withIndex().filter { (_, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                instruction.opcode == Opcode.INVOKE_DIRECT_RANGE && reference?.definingClass == LINK_CHUNK_CLASS &&
                    reference.name == "<init>" &&
                    reference.parameterTypes.map(CharSequence::toString) == LINK_CHUNK_PARAMETERS
            }.map { it.index }
            if (constructorIndices.isEmpty()) throw PatchException("Could not find where link chunks are created")

            // From the last, so the indices of the others stay valid.
            constructorIndices.sortedDescending().forEach { index ->
                // The first register is the chunk being created.
                val link = getInstruction<RegisterRangeInstruction>(index).startRegister + 3
                val text = link + 1
                addInstructions(
                    index,
                    """
                        invoke-static/range { v$link .. v$text }, $EXTENSION_CLASS->sanitizeText(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$text
                        invoke-static/range { v$link .. v$link }, $EXTENSION_CLASS->sanitize(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$link
                    """
                )
            }
        }
    }
}
