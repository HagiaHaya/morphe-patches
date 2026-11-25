package app.template.extension.slack;

import java.io.IOException;
import java.util.Locale;

import app.template.extension.slack.settings.Settings;

/**
 * Blocks the requests that send usage tracking, performance traces and errors to Slack,
 * and keeps the Sentry crash reporter from starting.
 */
@SuppressWarnings("unused")
public final class BlockTelemetryPatch {

    /**
     * Hosts that only receive telemetry: events, traces and errors.
     */
    private static final String[] TELEMETRY_HOSTS = {"slackb.com", "slackb-gov.com", "slackb.mil"};

    /**
     * API methods that only receive telemetry, and the proxy of the Sentry crash reports.
     */
    private static final String[] TELEMETRY_PATHS = {
            "/api/client.clog.track",
            "/api/clog.track",
            "/api/slog.track",
            "/apps/sentryproxy/",
    };

    private static boolean isEnabled() {
        return Settings.isEnabled(Settings.BLOCK_TELEMETRY);
    }

    /**
     * Injection point. Called with every telemetry event before it is queued for upload.
     *
     * @return If the event is dropped.
     */
    public static boolean skipEvent() {
        return isEnabled();
    }

    /**
     * Injection point. Called before Slack initializes Sentry.
     *
     * @return If Sentry is not initialized.
     */
    public static boolean skipSentry() {
        return isEnabled();
    }

    /**
     * Injection point. Called with the address of every request of the HTTP client of Slack.
     * The request fails like a request without network access.
     */
    public static void checkRequest(String host, String path) throws IOException {
        if (host == null || !isEnabled()) {
            return;
        }
        host = host.toLowerCase(Locale.ROOT);
        for (String telemetryHost : TELEMETRY_HOSTS) {
            if (host.equals(telemetryHost) || host.endsWith("." + telemetryHost)) {
                throw new IOException("Blocked telemetry request to " + host);
            }
        }
        if (path != null) {
            for (String telemetryPath : TELEMETRY_PATHS) {
                if (path.startsWith(telemetryPath)) {
                    throw new IOException("Blocked telemetry request to " + path);
                }
            }
        }
    }
}
