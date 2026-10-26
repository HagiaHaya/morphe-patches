package com.pepper.network.apirepresentation;

import java.util.List;

public final class ThreadListingContextApiRepresentation {

    public List<ContextItemApiRepresentation> getContextItems() {
        throw new UnsupportedOperationException();
    }

    public static final class ContextItemApiRepresentation {

        public long getThreadId() {
            throw new UnsupportedOperationException();
        }
    }
}
