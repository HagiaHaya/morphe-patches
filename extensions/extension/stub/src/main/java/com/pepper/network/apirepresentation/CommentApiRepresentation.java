package com.pepper.network.apirepresentation;

/**
 * A comment, as the REST API returns it.
 */
public final class CommentApiRepresentation {

    public long getId() {
        throw new UnsupportedOperationException();
    }

    public String getContentUnformatted() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return The reply shown below the comment, or null.
     */
    public CommentApiRepresentation getHighlightedChild() {
        throw new UnsupportedOperationException();
    }
}
