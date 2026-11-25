package app.template.extension.pepper;

import android.util.Log;

import app.template.extension.pepper.settings.Settings;

import com.pepper.network.apirepresentation.GroupApiRepresentation;
import com.pepper.network.apirepresentation.MerchantApiRepresentation;
import com.pepper.network.apirepresentation.ThreadAdditionalDataApiRepresentation;
import com.pepper.network.apirepresentation.ThreadApiRepresentation;
import com.pepper.network.apirepresentation.ThreadListingContextApiRepresentation;
import com.pepper.network.apirepresentation.UserLightApiRepresentation;

import java.util.List;

/**
 * Hides deals and discussions from the lists of the app by keyword, store, user, category,
 * price or temperature, as set in the settings. The last deal of each page stays, as the app
 * loads the next page after it.
 */
@SuppressWarnings("unused")
public final class DealFilterPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Injection point. Called with the data of each response of the REST API.
     *
     * @return The data, without the deals the filters hide if it is a list of deals.
     */
    @SuppressWarnings("unchecked")
    public static Object filterThreads(Object data, Object additionalData) {
        try {
            if (!(data instanceof List)) {
                return data;
            }
            List<?> threads = (List<?>) data;
            if (threads.isEmpty() || !(threads.get(0) instanceof ThreadApiRepresentation)) {
                return data;
            }

            Filters filters = new Filters();
            if (filters.isEmpty()) {
                return data;
            }

            List<ThreadListingContextApiRepresentation.ContextItemApiRepresentation> contextItems = null;
            if (additionalData instanceof ThreadAdditionalDataApiRepresentation) {
                ThreadListingContextApiRepresentation context =
                        ((ThreadAdditionalDataApiRepresentation) additionalData).getThreadListingContext();
                contextItems = context != null ? context.getContextItems() : null;
            }

            ListingFilter.removeHidden((List<ThreadApiRepresentation>) threads, contextItems, filters::hides,
                    ThreadApiRepresentation::getId,
                    ThreadListingContextApiRepresentation.ContextItemApiRepresentation::getThreadId);
        } catch (Exception ex) {
            Log.e(TAG, "filterThreads failure", ex);
        }
        return data;
    }

    private static final class Filters {
        final TextPatterns keywords = new TextPatterns(Settings.getLines(Settings.FILTER_KEYWORDS), false);
        final TextPatterns merchants = new TextPatterns(Settings.getLines(Settings.FILTER_MERCHANTS), true);
        final TextPatterns users = new TextPatterns(Settings.getLines(Settings.FILTER_USERS), true);
        final TextPatterns categories = new TextPatterns(Settings.getLines(Settings.FILTER_CATEGORIES), true);
        final Double maxPrice = parseNumber(Settings.getString(Settings.FILTER_MAX_PRICE));
        final Double minTemperature = parseNumber(Settings.getString(Settings.FILTER_MIN_TEMPERATURE));

        boolean isEmpty() {
            return keywords.isEmpty() && merchants.isEmpty() && users.isEmpty() && categories.isEmpty()
                    && maxPrice == null && minTemperature == null;
        }

        boolean hides(ThreadApiRepresentation thread) {
            if (keywords.matches(thread.getTitle())) {
                return true;
            }

            MerchantApiRepresentation merchant = thread.getMerchant();
            if (merchants.matches(merchant != null ? merchant.getName() : thread.getMerchantNameDisplay())) {
                return true;
            }

            UserLightApiRepresentation user = thread.getUser();
            if (user != null && users.matches(user.getUsername())) {
                return true;
            }

            GroupApiRepresentation category = thread.getMainGroup();
            if (category != null && categories.matches(category.getName())) {
                return true;
            }

            Double price = thread.getPrice();
            if (maxPrice != null && price != null && price > maxPrice) {
                return true;
            }

            Integer temperature = thread.getTemperatureRating();
            return minTemperature != null && temperature != null && temperature < minTemperature;
        }

        private static Double parseNumber(String text) {
            try {
                return text.trim().isEmpty() ? null : Double.valueOf(text.trim().replace(',', '.'));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
