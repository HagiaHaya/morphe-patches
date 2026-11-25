package app.template.extension.slack;

import app.template.extension.slack.settings.Settings;

@SuppressWarnings("unused")
public final class HideHistoryLimitBannerPatch {

    /**
     * Injection point. Called before Slack shows the banner about the history limit of the free plan.
     *
     * @return If the banner is hidden.
     */
    public static boolean hideBanner() {
        return Settings.isEnabled(Settings.HIDE_HISTORY_LIMIT_BANNER);
    }
}
