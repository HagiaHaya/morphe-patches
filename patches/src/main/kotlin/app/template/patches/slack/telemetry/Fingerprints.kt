package app.template.patches.slack.telemetry

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * Passes a request to the next interceptor of OkHttp. Every request of Slack goes through it,
 * including the ones to its telemetry endpoints.
 */
internal object InterceptorChainProceedFingerprint : Fingerprint(
    definingClass = "Lokhttp3/internal/http/RealInterceptorChain;",
    name = "proceed",
    returnType = "Lokhttp3/Response;",
    parameters = listOf("Lokhttp3/Request;"),
)

/**
 * Queues a telemetry event (usage tracking, metric or trace) in the telemetry database,
 * from which a periodic job uploads them.
 */
internal object TrackTelemetryEventFingerprint : Fingerprint(
    definingClass = "Lslack/telemetry/internal/EventSyncManagerImpl;",
    name = "track",
    returnType = "V",
    parameters = listOf("Lslack/telemetry/internal/TelemetryEvent;"),
)

/**
 * Initializes the Sentry crash and performance reporter. R8 moved it into another class,
 * but kept its name.
 */
internal object ConditionallyInitializeSentryFingerprint : Fingerprint(
    name = "conditionallyInitializeSentry",
    returnType = "V",
    filters = listOf(
        string("Feature flag enabled -- initializing Sentry"),
    ),
)
