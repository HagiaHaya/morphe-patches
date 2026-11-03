package app.template.patches.slack.featureflags

import app.morphe.patcher.Fingerprint

internal const val FEATURE_FLAG_STORE_CLASS = "Lslack/services/featureflag/store/FeatureFlagStoreImpl;"

/**
 * Loads the feature flags of the signed in workspace, the server values cached on the device.
 */
internal object FeatureFlagStoreConstructorFingerprint : Fingerprint(
    definingClass = FEATURE_FLAG_STORE_CLASS,
    name = "<init>",
)

/**
 * Checks a feature flag. The other stores of feature flags delegate to this one.
 */
internal object FeatureFlagIsEnabledFingerprint : Fingerprint(
    definingClass = FEATURE_FLAG_STORE_CLASS,
    name = "isEnabled",
    returnType = "Z",
    parameters = listOf("Lslack/featureflag/FeatureFlagEnum;", "Z"),
)
