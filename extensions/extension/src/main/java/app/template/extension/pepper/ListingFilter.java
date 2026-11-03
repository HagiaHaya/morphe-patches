package app.template.extension.pepper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Removes items from a list of the REST API, such as deals or comments.
 * <p>
 * The app rejects a list whose listing context does not have the same items, so they are removed
 * from both. It loads the next page after the last item of the listing context, so the last item
 * is never removed.
 */
final class ListingFilter {

    private ListingFilter() {
    }

    /**
     * @param items        The list, which kotlinx.serialization created as an ArrayList.
     * @param contextItems The items of its listing context, or null if it has none.
     */
    static <T, C> void removeHidden(List<T> items, List<C> contextItems, Predicate<T> hides,
                                    ToLongFunction<T> itemId, ToLongFunction<C> contextItemId) {
        if (!(items instanceof ArrayList) || (contextItems != null && !(contextItems instanceof ArrayList))) {
            return;
        }

        Set<Long> hidden = new HashSet<>();
        for (int i = 0; i < items.size() - 1; i++) {
            if (hides.test(items.get(i))) {
                hidden.add(itemId.applyAsLong(items.get(i)));
            }
        }
        if (hidden.isEmpty()) {
            return;
        }

        if (contextItems != null) {
            contextItems.removeIf(contextItem -> hidden.contains(contextItemId.applyAsLong(contextItem)));
        }
        T last = items.get(items.size() - 1);
        items.removeIf(item -> item != last && hidden.contains(itemId.applyAsLong(item)));
    }
}
