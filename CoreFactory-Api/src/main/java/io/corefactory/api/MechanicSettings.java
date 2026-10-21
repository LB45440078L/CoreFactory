package io.corefactory.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Typed, read-only view over the raw YAML map of one TNT type's mechanic settings.
 *
 * <p>Every getter takes a default, so a mechanic never fails on a missing key and a server admin can
 * remove any line from {@code tnts.yml} without breaking the plugin.
 */
public final class MechanicSettings {

    public static final MechanicSettings EMPTY = new MechanicSettings(Map.of());

    private final Map<String, Object> raw;

    public MechanicSettings(Map<String, Object> raw) {
        this.raw = Collections.unmodifiableMap(raw);
    }

    public boolean has(String key) {
        return raw.containsKey(key);
    }

    public int getInt(String key, int def) {
        Object value = raw.get(key);
        return value instanceof Number n ? n.intValue() : def;
    }

    public long getLong(String key, long def) {
        Object value = raw.get(key);
        return value instanceof Number n ? n.longValue() : def;
    }

    public double getDouble(String key, double def) {
        Object value = raw.get(key);
        return value instanceof Number n ? n.doubleValue() : def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object value = raw.get(key);
        return value instanceof Boolean b ? b : def;
    }

    public String getString(String key, String def) {
        Object value = raw.get(key);
        return value instanceof String s ? s : def;
    }

    /** A list of numbers, e.g. a vector written as {@code [0, 1.5, 0]}. Returns the default when absent. */
    public double[] getDoubleArray(String key, double[] def) {
        Object value = raw.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return def;
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) {
            Object element = list.get(i);
            if (!(element instanceof Number n)) {
                return def;
            }
            out[i] = n.doubleValue();
        }
        return out;
    }

    public List<String> getStringList(String key) {
        Object value = raw.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element != null) {
                out.add(element.toString());
            }
        }
        return List.copyOf(out);
    }

    /** A nested mapping, or {@link #EMPTY} when the key is absent or not a mapping. */
    public MechanicSettings getSection(String key) {
        Object value = raw.get(key);
        return value instanceof Map<?, ?> map ? new MechanicSettings(castMap(map)) : EMPTY;
    }

    /** A list of nested mappings, e.g. the weighted pool of {@code randomize}. Non-mapping entries are skipped. */
    public List<MechanicSettings> getSectionList(String key) {
        Object value = raw.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<MechanicSettings> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                out.add(new MechanicSettings(castMap(map)));
            }
        }
        return List.copyOf(out);
    }

    public Object raw(String key) {
        return raw.get(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
