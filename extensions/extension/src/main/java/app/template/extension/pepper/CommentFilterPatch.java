package app.template.extension.pepper;

import android.util.Log;

import app.template.extension.pepper.settings.Settings;

import com.pepper.network.apirepresentation.CommentAdditionalDataApiRepresentation;
import com.pepper.network.apirepresentation.CommentApiRepresentation;
import com.pepper.network.apirepresentation.CommentListingContextApiRepresentation;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Hides comments, and their replies, that contain words set in the settings.
 * The last comment of each page stays, as the app loads the next page after it, but without its text.
 */
@SuppressWarnings("unused")
public final class CommentFilterPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Injection point. Called with the data of each response of the REST API.
     *
     * @return The data, without the comments the filter hides if it is a list of comments.
     */
    @SuppressWarnings("unchecked")
    public static Object filterComments(Object data, Object additionalData) {
        try {
            if (!(data instanceof List)) {
                return data;
            }
            List<?> comments = (List<?>) data;
            if (comments.isEmpty() || !(comments.get(0) instanceof CommentApiRepresentation)) {
                return data;
            }

            TextPatterns words = new TextPatterns(Settings.getLines(Settings.COMMENT_FILTER_WORDS), false);
            if (words.isEmpty()) {
                return data;
            }

            List<CommentListingContextApiRepresentation.ContextItemApiRepresentation> contextItems = null;
            if (additionalData instanceof CommentAdditionalDataApiRepresentation) {
                CommentListingContextApiRepresentation context =
                        ((CommentAdditionalDataApiRepresentation) additionalData).getCommentListingContext();
                contextItems = context != null ? context.getContextItems() : null;
            }

            for (CommentApiRepresentation comment : (List<CommentApiRepresentation>) comments) {
                hideHighlightedChild(comment, words);
            }

            ListingFilter.removeHidden((List<CommentApiRepresentation>) comments, contextItems,
                    comment -> words.matches(comment.getContentUnformatted()),
                    CommentApiRepresentation::getId,
                    CommentListingContextApiRepresentation.ContextItemApiRepresentation::getCommentId);

            CommentApiRepresentation last = (CommentApiRepresentation) comments.get(comments.size() - 1);
            if (words.matches(last.getContentUnformatted())) {
                setField(last, "contentUnformatted", "<i>" + DealMenu.text(
                        "Komentarz ukryty przez filtr", "Comment hidden by the filter") + "</i>");
            }
        } catch (Exception ex) {
            Log.e(TAG, "filterComments failure", ex);
        }
        return data;
    }

    /**
     * Removes the reply the app shows below a comment, if the filter hides it.
     * The other replies are filtered when they are loaded.
     */
    private static void hideHighlightedChild(CommentApiRepresentation comment, TextPatterns words)
            throws ReflectiveOperationException {
        CommentApiRepresentation child = comment.getHighlightedChild();
        if (child != null && words.matches(child.getContentUnformatted())) {
            setField(comment, "highlightedChild", null);
        }
    }

    /**
     * Sets a field of a comment, which are final, as kotlinx.serialization creates the comments.
     */
    private static void setField(CommentApiRepresentation comment, String name, Object value)
            throws ReflectiveOperationException {
        Field field = CommentApiRepresentation.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(comment, value);
    }
}
