package app.template.patches.slack.settings

import app.morphe.patcher.Fingerprint

/**
 * The state of the Preferences screen of Slack, with its list of entries and its event handler.
 */
internal object PreferenceSettingsStateFingerprint : Fingerprint(
    definingClass = "Lslack/features/settings/circuit/PreferenceSettings\$State;",
    name = "<init>",
    parameters = listOf(
        "Ljava/util/List;",
        "Lslack/features/settings/circuit/PreferenceSettings\$Dialog;",
        "Lkotlin/jvm/functions/Function1;",
    ),
)
