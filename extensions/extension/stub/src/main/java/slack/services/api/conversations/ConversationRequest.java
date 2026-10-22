package slack.services.api.conversations;

/**
 * Parameters of a conversations.history request.
 */
public final class ConversationRequest {
    public String channelId;
    public boolean inclusive;
    public int limit;
    public String latestTs;
    public String oldestTs;
    public String cursor;
}
