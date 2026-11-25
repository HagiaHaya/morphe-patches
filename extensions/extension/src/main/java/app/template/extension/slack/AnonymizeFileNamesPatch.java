package app.template.extension.slack;

import java.security.SecureRandom;

import app.template.extension.slack.settings.Settings;

@SuppressWarnings("unused")
public final class AnonymizeFileNamesPatch {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final int NAME_LENGTH = 12;

    /**
     * Longest extension kept, so a name with dots is not taken for one.
     */
    private static final int MAX_EXTENSION_LENGTH = 10;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Injection point. Called with the name of a file Slack asks to upload.
     *
     * @return A random name with the same extension.
     */
    public static String anonymize(String fileName) {
        if (fileName == null || !Settings.isEnabled(Settings.ANONYMIZE_FILE_NAMES)) {
            return fileName;
        }

        StringBuilder name = new StringBuilder(NAME_LENGTH + MAX_EXTENSION_LENGTH + 1);
        for (int i = 0; i < NAME_LENGTH; i++) {
            name.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && fileName.length() - dot - 1 <= MAX_EXTENSION_LENGTH) {
            String extension = fileName.substring(dot);
            if (extension.matches("\\.[A-Za-z0-9]+")) {
                name.append(extension);
            }
        }
        return name.toString();
    }
}
