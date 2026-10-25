package app.template.extension.pepper.settings;

/**
 * Which patches the app was patched with, so the settings screen only offers their settings.
 * Each patch makes its method return true during patching.
 */
@SuppressWarnings("unused")
public final class PatchedFeatures {

    public static boolean removeAds() {
        return false;
    }

    public static boolean sanitizeSharingLinks() {
        return false;
    }

    public static boolean sanitizeDealLinks() {
        return false;
    }

    public static boolean unlockCustomAppIcons() {
        return false;
    }

    public static boolean spoofContributorLevel() {
        return false;
    }

    public static boolean catchFlamedeer() {
        return false;
    }

    public static boolean enableDebugMenu() {
        return false;
    }
}
