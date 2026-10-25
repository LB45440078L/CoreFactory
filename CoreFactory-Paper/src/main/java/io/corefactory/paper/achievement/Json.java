package io.corefactory.paper.achievement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the JSON of the achievement datapack. The values are maps, lists, strings, numbers and booleans, and Gson writes them:
 * it escapes quotes, backslashes and control characters as JSON requires, and writes other characters, such as {@code é}, as the
 * UTF-8 they are. Gson is on the compile classpath through the Paper API and the server provides it at runtime, so the plugin
 * adds no library. HTML characters such as {@code <} and {@code &} are written as they are, not as escape sequences.
 */
final class Json {

    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private static final Gson PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private Json() {
    }

    /**
     * An object from alternating keys and values, in the order they are given.
     *
     * @param keysAndValues a key, then its value, then the next key, and so on
     * @return a mutable map, so a caller can add a member
     */
    static Map<String, Object> object(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Every key needs a value");
        }
        Map<String, Object> object = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            object.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return object;
    }

    /** The value on one line, with no spaces, as the server's own data is written. */
    static String compact(Object value) {
        return COMPACT.toJson(value);
    }

    /** The value indented by two spaces per level, for a file a person reads. */
    static String pretty(Object value) {
        return PRETTY.toJson(value);
    }
}
