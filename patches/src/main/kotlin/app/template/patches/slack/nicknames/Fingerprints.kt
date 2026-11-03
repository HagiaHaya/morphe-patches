package app.template.patches.slack.nicknames

import app.morphe.patcher.Fingerprint

/**
 * Resolves the names Slack shows for a member: the main name and a secondary one,
 * following the workspace setting to show display names or full names.
 */
internal object GetDisplayNamesFingerprint : Fingerprint(
    definingClass = "Lslack/libraries/sharedprefs/api/member/DisplayNameHelperImpl;",
    name = "getDisplayNamesInt",
    returnType = "Lkotlin/Pair;",
    parameters = listOf(
        "Lslack/model/Member;",
        "Z",
        "Z",
        "Lslack/libraries/sharedprefs/api/member/DisplayNameHelper\$HiddenUserModel;",
    ),
)
