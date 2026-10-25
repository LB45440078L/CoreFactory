package io.corefactory.paper.artillery;

/**
 * The time limit of the listening mode. A session ends when its player has clicked nothing for the configured time, so the mode cannot get
 * stuck open. Pure, so the decision is tested without a server.
 */
public final class TargetingTimeout {

    private TargetingTimeout() {
    }

    /**
     * True when the time since the player's last click has reached the limit.
     *
     * @param lastActivityMillis the time of the player's last click, in milliseconds
     * @param nowMillis          the current time, in milliseconds
     * @param timeoutSeconds     the limit, in seconds, above 0
     * @return true when the session should end
     */
    public static boolean expired(long lastActivityMillis, long nowMillis, int timeoutSeconds) {
        return nowMillis - lastActivityMillis >= timeoutSeconds * 1000L;
    }
}
