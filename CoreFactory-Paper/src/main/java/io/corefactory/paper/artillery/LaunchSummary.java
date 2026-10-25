package io.corefactory.paper.artillery;

import io.corefactory.core.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The summary of a launch: how many connected pieces fired and how many were refused, and the lines the owner reads. The first line holds
 * the two counts, and each refused piece has a line of its own with the reason its shot was refused. The lines are built from the attempts
 * alone, so they are tested without a server.
 */
public final class LaunchSummary {

    /**
     * One piece's attempt at a launch.
     *
     * @param piece  the piece's label, such as its name and block; it is escaped, so it shows as typed
     * @param result what its shot did
     */
    public record Attempt(String piece, ArtilleryService.ShotResult result) {
    }

    /**
     * The counts and the lines of a launch.
     *
     * @param fired   the pieces that fired
     * @param refused the pieces whose shot was refused
     * @param lines   the summary line, then one line for each refused piece, in the order of the attempts, with their colour codes
     */
    public record Summary(int fired, int refused, List<String> lines) {
    }

    private LaunchSummary() {
    }

    /**
     * Counts the attempts and writes their lines. A piece that fired counts toward the fired; a refused piece counts toward the refused and
     * gets a line with its reason.
     *
     * @param attempts one attempt for each connected piece, in order
     * @return the counts and the lines
     */
    public static Summary of(List<Attempt> attempts) {
        int fired = 0;
        List<String> lines = new ArrayList<>();
        for (Attempt attempt : attempts) {
            if (attempt.result().fired()) {
                fired++;
            }
        }
        int refused = attempts.size() - fired;
        lines.add("<accent>Launch:</accent> " + count(fired, "fired", "good") + ", " + count(refused, "refused", "bad"));
        for (Attempt attempt : attempts) {
            if (!attempt.result().fired()) {
                lines.add("<muted>" + Text.escape(attempt.piece()) + ":</muted> " + attempt.result().reason());
            }
        }
        return new Summary(fired, refused, lines);
    }

    /** A count with its word, in the palette's colour when it is above zero and muted when it is not. */
    private static String count(int number, String word, String colour) {
        String tag = number > 0 ? colour : "muted";
        return "<" + tag + ">" + number + " " + word + "</" + tag + ">";
    }
}
