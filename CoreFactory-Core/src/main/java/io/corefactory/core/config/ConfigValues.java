package io.corefactory.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed reads from the plain data a YAML file parses into: nested maps for sections, lists for lists, and numbers,
 * strings and booleans for scalars. A path is dotted, so {@code "oil.tube-capacity-litres"} walks the nested maps.
 * A missing value, or a value of the wrong type, gives the default the caller passes, so a reader never throws.
 * A null map is read as an empty one.
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    /**
     * The value at a dotted path, or null when a key is missing or a step on the way is not a section. A key that holds
     * a dot is found whole, so a path that is itself a key of the map is read as that key.
     */
    public static Object value(Map<String, Object> map, String path) {
        if (map == null) {
            return null;
        }
        if (map.get(path) != null) {
            return map.get(path);
        }
        Object current = map;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> section)) {
                return null;
            }
            current = section.get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** True when the dotted path names a value, whatever its type. */
    public static boolean contains(Map<String, Object> map, String path) {
        return value(map, path) != null;
    }

    /** The section at a dotted path, or null when it is missing or is not a section. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> optionalSection(Map<String, Object> map, String path) {
        return value(map, path) instanceof Map<?, ?> section ? (Map<String, Object>) section : null;
    }

    /** The section at a dotted path, or an empty map when it is missing or is not a section, so callers never null-check. */
    public static Map<String, Object> section(Map<String, Object> map, String path) {
        Map<String, Object> section = optionalSection(map, path);
        return section != null ? section : Map.of();
    }

    /** True when the value is a whole number: an {@code Integer}, {@code Long}, {@code Short} or {@code Byte}. */
    public static boolean isInteger(Map<String, Object> map, String path) {
        Object value = value(map, path);
        return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte;
    }

    /** The boolean at a path, or the default when it is missing or not a boolean. */
    public static boolean bool(Map<String, Object> map, String path, boolean def) {
        return value(map, path) instanceof Boolean flag ? flag : def;
    }

    /** The number at a path as a double, or the default when it is missing or not a number. */
    public static double number(Map<String, Object> map, String path, double def) {
        return value(map, path) instanceof Number n ? n.doubleValue() : def;
    }

    /** The number at a path as an int, or the default when it is missing or not a number. A fraction is cut off. */
    public static int integer(Map<String, Object> map, String path, int def) {
        return value(map, path) instanceof Number n ? n.intValue() : def;
    }

    /** The number at a path as a long, or the default when it is missing or not a number. A fraction is cut off. */
    public static long longValue(Map<String, Object> map, String path, long def) {
        return value(map, path) instanceof Number n ? n.longValue() : def;
    }

    /** The text at a path, or the default when it is missing or not text. */
    public static String string(Map<String, Object> map, String path, String def) {
        return value(map, path) instanceof String text ? text : def;
    }

    /**
     * The list of text at a path, or the default when it is missing or not a list. A number, boolean or character in
     * the list is kept as its text; a section or a nested list is dropped.
     */
    public static List<String> stringList(Map<String, Object> map, String path, List<String> def) {
        if (!(value(map, path) instanceof List<?> list)) {
            return def;
        }
        List<String> out = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof String text) {
                out.add(text);
            } else if (element instanceof Number || element instanceof Boolean || element instanceof Character) {
                out.add(String.valueOf(element));
            }
        }
        return List.copyOf(out);
    }

    /** The list of numbers at a path as doubles, or the default when it is missing or not a list. Other entries are dropped. */
    public static List<Double> numberList(Map<String, Object> map, String path, List<Double> def) {
        if (!(value(map, path) instanceof List<?> list)) {
            return def;
        }
        List<Double> out = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Number n) {
                out.add(n.doubleValue());
            }
        }
        return List.copyOf(out);
    }

    /** The list of numbers at a path as ints, or the default when it is missing or not a list. Other entries are dropped. */
    public static List<Integer> integerList(Map<String, Object> map, String path, List<Integer> def) {
        if (!(value(map, path) instanceof List<?> list)) {
            return def;
        }
        List<Integer> out = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Number n) {
                out.add(n.intValue());
            }
        }
        return List.copyOf(out);
    }

    /** The list of sections at a path, in order. Entries that are not sections are dropped; a missing list gives an empty one. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mapList(Map<String, Object> map, String path) {
        if (!(value(map, path) instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> section) {
                out.add((Map<String, Object>) section);
            }
        }
        return List.copyOf(out);
    }
}
