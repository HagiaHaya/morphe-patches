package app.template.extension.pepper;

import app.template.extension.pepper.settings.Settings;

@SuppressWarnings("unused")
public final class RemoveAdsPatch {

    /**
     * Injection point. Called when the deal lists and the deal pages decide whether to load ads.
     *
     * @param adsEnabled If the app would show ads.
     */
    public static boolean areAdsEnabled(boolean adsEnabled) {
        return adsEnabled && !Settings.isEnabled(Settings.REMOVE_ADS);
    }
}
