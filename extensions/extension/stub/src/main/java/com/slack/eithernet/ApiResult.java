package com.slack.eithernet;

import java.util.Map;

/**
 * Result of an API call (EitherNet).
 */
public interface ApiResult {
    final class Success implements ApiResult {
        public Object value;

        public Success(Object value, Map<?, ?> tags) {
        }
    }

    interface Failure extends ApiResult {
    }
}
