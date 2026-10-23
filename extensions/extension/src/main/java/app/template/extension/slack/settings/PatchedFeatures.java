package app.template.extension.slack.settings;

/**
 * Which patches were applied, so the settings screen only lists their settings.
 * Each patch with settings replaces its method to return true.
 */
@SuppressWarnings("unused")
public final class PatchedFeatures {

    public static boolean silentTyping() {
        return false;
    }

    public static boolean blockTelemetry() {
        return false;
    }

    public static boolean sanitizeLinks() {
        return false;
    }

    public static boolean anonymizeFileNames() {
        return false;
    }

    public static boolean keepMessageHistory() {
        return false;
    }

    public static boolean hideHistoryLimitBanner() {
        return false;
    }
}
