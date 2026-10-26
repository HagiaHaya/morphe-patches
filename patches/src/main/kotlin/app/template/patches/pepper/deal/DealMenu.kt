package app.template.patches.pepper.deal

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext

/**
 * Adds an item to the menu of the screen of a deal.
 *
 * @param extensionClass Class of the extension with `addMenuItem(Activity, Menu)`,
 * and `onMenuItemSelected(Activity, MenuItem)`, which returns if it handled the item.
 */
internal fun BytecodePatchContext.addDealMenuItem(extensionClass: String) {
    DealThreadOnCreateOptionsMenuFingerprint.method.addInstruction(
        0,
        "invoke-static/range { p0 .. p1 }, $extensionClass->addMenuItem(Landroid/app/Activity;Landroid/view/Menu;)V"
    )

    DealThreadOnOptionsItemSelectedFingerprint.method.addInstructionsWithLabels(
        0,
        """
            invoke-static/range { p0 .. p1 }, $extensionClass->onMenuItemSelected(Landroid/app/Activity;Landroid/view/MenuItem;)Z
            move-result v0
            if-eqz v0, :not_handled
            return v0
            :not_handled
            nop
        """
    )
}
