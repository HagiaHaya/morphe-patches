package slack.model;

/**
 * A message as Slack keeps it, parsed from the API and stored as JSON.
 */
public final class Message {
    /**
     * Timestamp of the message, which identifies it in its conversation.
     */
    public String ts;
    public String threadTs;
    public String text;
    public String user;
}
