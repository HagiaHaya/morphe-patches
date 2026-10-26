package slack.services.featureflag.store;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The feature flags of the signed in workspace.
 */
public final class FeatureFlagStoreImpl {
    /**
     * Value of every known flag, by flag.
     */
    public ConcurrentHashMap<Object, Boolean> featureFlagMap;
}
