package slack.model;

/**
 * A message as Slack caches it, with its state.
 */
public final class PersistedMessageObj extends PersistedModelObj {
    public MessageState msgState;
}
