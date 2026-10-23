package app.template.patches.slack.files

import app.morphe.patcher.Fingerprint

/**
 * Asks Slack for the address to upload a file to, naming the file, with the defaults of the
 * optional parameters. The uploader is the only caller, for files of messages and of the files screen.
 */
internal object GetUploadUrlFingerprint : Fingerprint(
    definingClass = "Lslack/api/methods/files/FilesApi;",
    name = "getUploadURL\$default",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Lslack/api/methods/files/FilesApi;",
        "J",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Lslack/telemetry/tracing/TraceContext;",
        "Lslack/guinness/BackgroundTeamRequest;",
        "Lkotlin/coroutines/Continuation;",
        "I",
        "Ljava/lang/Object;",
    ),
)
