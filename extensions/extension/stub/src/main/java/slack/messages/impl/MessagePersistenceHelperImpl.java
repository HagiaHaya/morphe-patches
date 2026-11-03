package slack.messages.impl;

import java.util.Optional;

import slack.model.Message;
import slack.model.MessageState;

/**
 * Reads and writes the messages Slack caches, and tells its screens about the changes.
 */
public final class MessagePersistenceHelperImpl {
    /**
     * The LoggedInUser of the workspace.
     */
    public dagger.Lazy loggedInUserLazy;

    /**
     * @return The cached message, a PersistedMessageObj, if there is one.
     */
    public Optional<Object> getMessage(String channelId, String ts) {
        return null;
    }

    /**
     * Replaces a cached message, and updates the screens showing it.
     */
    public Optional<Object> updateMessage(String channelId, Message message, MessageState state, boolean notify) {
        return null;
    }
}
