package io.corefactory.paper.config;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version of the code-coupled data in processes.yml: its machines, processes and drills. Those sections are tied to the
 * code (slot counts, layouts, hosts, process ids), so a server takes the shipped copy when its file is older instead of
 * merging entries into it. The first line of processes.yml names its version, and {@link #CURRENT} is the version this build
 * ships. Pure, so the rule is tested without a server.
 */
public final class DataVersion {

    /** The version of processes.yml this build ships. Raise it when the machines, processes or drills change. */
    public static final int CURRENT = 10;

    private static final Pattern STAMP = Pattern.compile("#\\s*data-version:\\s*(\\d+)");

    /** What to do with a server's processes.yml, given its version. */
    public enum Action {
        /** The file has no version or an older one: it is backed up and replaced with the shipped file. */
        REPLACE,
        /** The file has this version: nothing changes. */
        KEEP,
        /** The file has a newer version than this build: it is left as it is and a warning is logged. */
        NEWER
    }

    private DataVersion() {
    }

    /** The version a first line of a data file names, or empty when the line is not a version line. */
    public static OptionalInt parse(String firstLine) {
        Matcher matcher = STAMP.matcher(firstLine.replace("﻿", "").trim());
        if (!matcher.matches()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * What to do with a file, from its version and the version of this build. A missing version counts as older.
     *
     * @param server  the version the file names, empty when it names none
     * @param bundled the version of the shipped file
     */
    public static Action decide(OptionalInt server, int bundled) {
        if (server.isEmpty() || server.getAsInt() < bundled) {
            return Action.REPLACE;
        }
        return server.getAsInt() == bundled ? Action.KEEP : Action.NEWER;
    }
}
