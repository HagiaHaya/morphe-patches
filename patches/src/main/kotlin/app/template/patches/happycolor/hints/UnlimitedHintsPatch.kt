package app.template.patches.happycolor.hints

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_HAPPY_COLOR

private const val LIBAPP_PATH = "lib/arm64-v8a/libapp.so"

/**
 * Body of AdsContentDependentResolver.hasUnlimitedHints(picture) in the arm64 Dart AOT
 * snapshot (Happy Color 2.24.1, Dart 3.12.2, VM offset 0x8ed4bc), from the prologue up to and
 * including the branch taken when InAppPurchasesRepository.isHintsRemoved() returns false.
 * The bl displacements make the sequence unique in libapp.so.
 */
private val HAS_UNLIMITED_HINTS_SIGNATURE = intArrayOf(
    0xfd, 0x79, 0xbf, 0xa9, // stp  fp, lr, [sp, #-0x10]!
    0xfd, 0x03, 0x0f, 0xaa, // mov  fp, sp
    0xef, 0x41, 0x00, 0xd1, // sub  sp, sp, #0x10
    0xe0, 0x03, 0x01, 0xaa, // mov  x0, x1
    0xa1, 0x83, 0x1f, 0xf8, // stur x1, [fp, #-8]
    0xa2, 0x03, 0x1f, 0xf8, // stur x2, [fp, #-0x10]
    0x50, 0x27, 0x40, 0xf9, // ldr  x16, [THR, #0x48]
    0xff, 0x01, 0x10, 0xeb, // cmp  sp, x16
    0x29, 0x02, 0x00, 0x54, // b.ls stack overflow
    0x01, 0x70, 0x40, 0xb8, // ldur w1, [x0, #7]
    0x21, 0x80, 0x1c, 0x8b, // add  x1, x1, HEAP, lsl #32
    0x62, 0x0a, 0xf5, 0x97, // bl   InAppPurchasesRepository.isHintsRemoved
    0xa0, 0x00, 0x20, 0x37, // tbnz w0, #4, -> isPictureInBoughtPack (false path)
).map { it.toByte() }.toByteArray()

/** Offset of the tbnz inside the signature. */
private const val BRANCH_OFFSET = 0x30

private val NOP = intArrayOf(0x1f, 0x20, 0x03, 0xd5).map { it.toByte() }.toByteArray()

/**
 * Happy Color: unlimited hints.
 *
 * The game already has an unlimited-hints mode for buyers of its "remove ads and hints"
 * product. GameCubit stores AdsContentDependentResolver.hasUnlimitedHints() in its state, and
 * when that flag is set onHintsTap skips the balance check, never calls
 * HintsRepository.useHint (so the balance is not decremented), and never starts the
 * empty-hints timer. The same flag also switches off the hint fairy and ad breaks that
 * advertise hints.
 *
 * All of that is Dart code compiled into libapp.so, so this patch edits the AOT snapshot
 * directly: the branch that falls through to the per-pack check when isHintsRemoved() is
 * false becomes a nop, so hasUnlimitedHints() always returns true. isHintsRemoved() itself
 * is untouched, so other features keyed off the purchase (such as ad removal) are unchanged.
 */
@Suppress("unused")
val unlimitedHintsPatch = rawResourcePatch(
    name = "Unlimited hints",
    description = "Enables the game's built-in unlimited hints mode, so using a hint never lowers the hint balance. arm64-v8a devices only.",
    default = true
) {
    compatibleWith(COMPATIBILITY_HAPPY_COLOR)

    execute {
        val libapp = get(LIBAPP_PATH)
        if (!libapp.exists()) {
            throw PatchException("$LIBAPP_PATH not found. This patch supports arm64-v8a builds only.")
        }

        val bytes = libapp.readBytes()
        val index = bytes.indexOf(HAS_UNLIMITED_HINTS_SIGNATURE)
        if (index == -1) {
            throw PatchException("hasUnlimitedHints() not found in $LIBAPP_PATH. Unsupported app version.")
        }
        if (bytes.indexOf(HAS_UNLIMITED_HINTS_SIGNATURE, index + 1) != -1) {
            throw PatchException("hasUnlimitedHints() signature is not unique in $LIBAPP_PATH.")
        }

        NOP.copyInto(bytes, index + BRANCH_OFFSET)
        libapp.writeBytes(bytes)
    }
}

private fun ByteArray.indexOf(pattern: ByteArray, from: Int = 0): Int {
    outer@ for (i in from..size - pattern.size) {
        for (j in pattern.indices) {
            if (this[i + j] != pattern[j]) continue@outer
        }
        return i
    }
    return -1
}
