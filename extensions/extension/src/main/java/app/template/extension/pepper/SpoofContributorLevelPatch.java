package app.template.extension.pepper;

import app.template.extension.pepper.settings.Settings;

/**
 * Shows the contributor level chosen in the settings instead of the one earned.
 * A level of 0 in the settings shows the one earned.
 */
@SuppressWarnings("unused")
public final class SpoofContributorLevelPatch {

    /**
     * Injection point.
     *
     * @param tierLevel Contributor level earned, or null if the user is not part of the loyalty program.
     */
    public static Integer getCurrentTierLevel(Integer tierLevel) {
        int spoofedTierLevel = Settings.getContributorLevel();
        return tierLevel == null || spoofedTierLevel == 0 ? tierLevel : Integer.valueOf(spoofedTierLevel);
    }

    /**
     * Injection point. Hides "10 points to unlock Platinum rewards!".
     */
    public static Integer getPointsToNextTierLevel(Integer points) {
        return points == null || Settings.getContributorLevel() == 0 ? points : Integer.valueOf(0);
    }

    /**
     * Injection point. Hides "You are about to lose your rewards!", which is about the level earned.
     */
    public static Boolean isTierLevelAboutToBeLost(Boolean aboutToBeLost) {
        return Settings.getContributorLevel() == 0 ? aboutToBeLost : Boolean.FALSE;
    }
}
