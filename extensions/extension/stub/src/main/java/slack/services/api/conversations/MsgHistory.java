package slack.services.api.conversations;

import java.util.List;
import java.util.Map;

import slack.api.response.MutationTimestamps;
import slack.api.response.ResponseMetadata;

/**
 * Response of conversations.history.
 */
public final class MsgHistory {
    public boolean ok;
    public String error;
    public List<Object> messages;
    public boolean hasMore;
    public boolean isLimited;
    public List<String> deleted;
    public ResponseMetadata metadata;
    public MutationTimestamps mutationTimestamps;
    public Map<String, String> latestUpdates;
    public List<String> unchangedMessages;

    public MsgHistory(boolean ok, String error, List<Object> messages, boolean hasMore, boolean isLimited,
                      List<String> deleted, ResponseMetadata metadata, MutationTimestamps mutationTimestamps,
                      Map<String, String> latestUpdates, List<String> unchangedMessages) {
    }
}
