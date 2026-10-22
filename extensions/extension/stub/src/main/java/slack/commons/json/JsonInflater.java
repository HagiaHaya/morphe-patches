package slack.commons.json;

/**
 * Moshi based JSON (de)serializer of Slack, which also stores messages as JSON in its database.
 */
public interface JsonInflater {
    String deflate(Class<?> type, Object value);

    Object inflate(Class<?> type, String json);
}
