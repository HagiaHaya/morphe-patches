package app.template.patches.slack.featureflags

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal const val FEATURE_FLAG_STORE_CLASS = "Lslack/services/featureflag/store/FeatureFlagStoreImpl;"

private const val FEATURE_FLAG_LAYER_CLASS = "Lslack/services/featureflag/store/FeatureFlagLayer;"

/**
 * Loads the feature flags of the signed in workspace, the server values cached on the device.
 */
internal object FeatureFlagStoreConstructorFingerprint : Fingerprint(
    definingClass = FEATURE_FLAG_STORE_CLASS,
    name = "<init>",
)

/**
 * Checks a feature flag. Every store of feature flags checks them through it, also for the
 * dependencies of a flag. R8 moved it into another class, so it is found by its parameters:
 *
 * ```
 * fun FeatureFlagLayer.resolve(flag: FeatureFlagEnum, live: Boolean, logExposure: Boolean): Boolean {
 *     if (isForcedOn(flag.key)) return true
 *     if (flag.dependencies.any { !resolve(it, live, false) }) return false
 *     return ownValue(flag, live, logExposure)
 * }
 * ```
 */
internal object ResolveFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(FEATURE_FLAG_LAYER_CLASS, "Lslack/featureflag/FeatureFlagEnum;", "Z", "Z"),
    filters = listOf(
        methodCall(definingClass = FEATURE_FLAG_LAYER_CLASS, name = "ownValue"),
    ),
)
