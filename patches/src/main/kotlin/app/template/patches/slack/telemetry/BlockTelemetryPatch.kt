package app.template.patches.slack.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.requireExtensionMethods
import app.template.patches.slack.settings.addPatchedFeature
import app.template.patches.slack.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/BlockTelemetryPatch;"

@Suppress("unused")
val blockTelemetryPatch = bytecodePatch(
    name = "Block telemetry",
    description = "Adds an option to block the usage tracking, performance traces and crash reports " +
        "Slack sends to its servers and to Sentry.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(settingsPatch)

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "checkRequest", "skipEvent", "skipSentry")
        addPatchedFeature("blockTelemetry")

        // Drop the events before they are stored for upload.
        TrackTelemetryEventFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS->skipEvent()Z
                move-result v0
                if-eqz v0, :track
                return-void
                :track
                nop
            """
        )

        // Fail requests to the telemetry endpoints like requests without network access,
        // so Slack drops the events queued before blocking was turned on.
        InterceptorChainProceedFingerprint.method.addInstructions(
            0,
            """
                iget-object v0, p1, Lokhttp3/Request;->url:Lokhttp3/HttpUrl;
                iget-object v1, v0, Lokhttp3/HttpUrl;->host:Ljava/lang/String;
                invoke-virtual { v0 }, Lokhttp3/HttpUrl;->encodedPath()Ljava/lang/String;
                move-result-object v0
                invoke-static { v1, v0 }, $EXTENSION_CLASS->checkRequest(Ljava/lang/String;Ljava/lang/String;)V
            """
        )

        ConditionallyInitializeSentryFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS->skipSentry()Z
                move-result v0
                if-eqz v0, :initialize
                return-void
                :initialize
                nop
            """
        )
    }
}
