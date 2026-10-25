package io.corefactory.paper.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The ids a mistyped id may have meant, for the unknown-id message of {@code /cf give} and {@code /cf recipe}. Pure: the caller
 * passes the ids its command accepts, so the rule is tested without a server.
 *
 * <p>The typed text is read in lower case, with spaces and hyphens as underscores, so a display name such as {@code Energy Wire}
 * finds {@code energy_wire}.
 */
public final class IdSuggestions {

    /** The most ids one message lists. */
    public static final int LIMIT = 5;

    /** The number of leading letters that two ids must share to be close: {@code railgun} is close to {@code rail_gun}. */
    private static final int SHARED_PREFIX = 3;

    private IdSuggestions() {
    }

    /**
     * The ids close to the typed text, at most {@link #LIMIT} of them. The ids are ranked in three groups, each in alphabetical order:
     * the ids that start with the typed text, the ids that contain it, and the ids that start with its first three letters. An id that
     * is in an earlier group is not listed again in a later one.
     *
     * @param typed the text the player typed; blank gives none
     * @param ids   the ids the command accepts
     * @return the close ids, best first; empty when nothing is close
     */
    public static List<String> closest(String typed, Collection<String> ids) {
        String key = normalise(typed);
        if (key.isEmpty()) {
            return List.of();
        }
        String shared = key.length() > SHARED_PREFIX ? key.substring(0, SHARED_PREFIX) : key;
        List<String> ranked = new ArrayList<>();
        for (List<String> group : List.of(
                matching(ids, id -> id.startsWith(key)),
                matching(ids, id -> id.contains(key)),
                matching(ids, id -> id.startsWith(shared)))) {
            for (String id : group) {
                if (!ranked.contains(id)) {
                    ranked.add(id);
                }
                if (ranked.size() == LIMIT) {
                    return ranked;
                }
            }
        }
        return ranked;
    }

    /** The typed text in the form the ids are written in: lower case, with spaces and hyphens as underscores, and trimmed. */
    static String normalise(String typed) {
        if (typed == null) {
            return "";
        }
        return typed.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static List<String> matching(Collection<String> ids, Predicate<String> test) {
        return ids.stream().distinct().filter(test).sorted().toList();
    }
}
