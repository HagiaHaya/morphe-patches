package app.template.extension.pepper;

import android.util.Log;

import app.template.extension.pepper.settings.Settings;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds the price with shipping to the shipping costs of a deal, such as "+ 9,99zł = 59,98zł".
 */
@SuppressWarnings("unused")
public final class PriceWithShippingPatch {

    private static final String TAG = "MorphePepper";

    /**
     * The number in a price the server formatted, such as "2 465" in "2 465zł" or "1,299.99" in "£1,299.99".
     */
    private static final Pattern NUMBER = Pattern.compile("\\d(?:[\\d\\s\\u00a0\\u202f.,']*\\d)?");

    private static final Pattern DECIMALS = Pattern.compile("[.,]\\d{1,2}$");

    /**
     * Injection point. Called instead of the getter of the shipping costs of a deal the app shows.
     */
    public static String getShippingCostsDisplay(String shippingCostsDisplay, Double price,
                                                 Double shippingCosts, String priceDisplay) {
        try {
            if (!Settings.isEnabled(Settings.PRICE_WITH_SHIPPING) || shippingCostsDisplay == null
                    || price == null || shippingCosts == null || shippingCosts <= 0 || priceDisplay == null) {
                return shippingCostsDisplay;
            }
            String total = formatLike(priceDisplay, Math.round((price + shippingCosts) * 100) / 100.0);
            return total == null ? shippingCostsDisplay : shippingCostsDisplay + " = " + total;
        } catch (Exception ex) {
            Log.e(TAG, "getShippingCostsDisplay failure", ex);
            return shippingCostsDisplay;
        }
    }

    /**
     * Formats an amount the way the server formatted another one, with the same currency and separators.
     *
     * @return The amount, or null if the format is not a number, such as "FREE".
     */
    static String formatLike(String formatted, double amount) {
        Matcher number = NUMBER.matcher(formatted);
        if (!number.find()) {
            return null;
        }

        String digits = number.group();
        char decimalSeparator = 0;
        Matcher decimals = DECIMALS.matcher(digits);
        if (decimals.find()) {
            decimalSeparator = digits.charAt(decimals.start());
        }
        char groupingSeparator = 0;
        for (char character : digits.toCharArray()) {
            if (!Character.isDigit(character) && character != decimalSeparator) {
                groupingSeparator = character;
                break;
            }
        }
        if (decimalSeparator == 0) {
            decimalSeparator = groupingSeparator == ',' ? '.' : ',';
        }
        if (groupingSeparator == 0) {
            groupingSeparator = decimalSeparator == ',' ? ' ' : ',';
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(decimalSeparator);
        symbols.setGroupingSeparator(groupingSeparator);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        if (Math.round(amount * 100) % 100 == 0) {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
        }

        return formatted.substring(0, number.start()) + format.format(amount) + formatted.substring(number.end());
    }
}
