package app.template.patches.slack.nicknames

import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.shared.wrapMethod
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/NicknamesPatch;"

@Suppress("unused")
val nicknamesPatch = bytecodePatch(
    name = "Nicknames",
    description = "Adds a screen to the patch settings to give other members nicknames, " +
        "which Slack shows instead of their names, only on this device.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "getNickname")
        addPatchedFeature("nicknames")

        GetDisplayNamesFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val withoutNicknames = "getDisplayNamesIntWithoutNicknames"

            // Registers: v0 to v2, p0 (this), p1 (member), p2 and p3 (flags), p4 (hidden user model).
            // R8 removed the getters of Pair, so its fields are read directly.
            fingerprint.classDef.wrapMethod(
                method,
                withoutNicknames,
                registerCount = 8,
                """
                    invoke-virtual { p0, p1, p2, p3, p4 }, ${method.definingClass}->$withoutNicknames(Lslack/model/Member;ZZLslack/libraries/sharedprefs/api/member/DisplayNameHelper${'$'}HiddenUserModel;)Lkotlin/Pair;
                    move-result-object v0
                    invoke-static { p1 }, $EXTENSION_CLASS->getNickname(Lslack/model/Member;)Ljava/lang/String;
                    move-result-object v1
                    if-eqz v1, :names
                    iget-object v2, v0, Lkotlin/Pair;->first:Ljava/lang/Object;
                    new-instance v0, Lkotlin/Pair;
                    invoke-direct { v0, v1, v2 }, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    :names
                    return-object v0
                """
            )
        }
    }
}
