package slack.services.api.conversations;

import java.util.List;
import java.util.Map;

import slack.api.response.ResponseMetadata;

/**
 * Response of conversations.replies.
 */
public final class RepliesApiResponse {
    public boolean subscribed;
    public String lastRead;
    public int unreadCount;
    public List<Object> messages;
    public boolean hasMore;
    public ResponseMetadata responseMetadata;
    public Map<String, String> latestUpdates;
    public List<String> unchangedMessages;

    public RepliesApiResponse(boolean subscribed, String lastRead, int unreadCount, List<Object> messages,
                              boolean hasMore, ResponseMetadata responseMetadata,
                              Map<String, String> latestUpdates, List<String> unchangedMessages) {
    }
}
