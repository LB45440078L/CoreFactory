package io.corefactory.core.config;

import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the plain maps and lists the configuration readers take, for tests. YAML text is parsed into the same data a file
 * gives in the plugin; a dotted path sets a value inside nested maps.
 */
public final class TestConfig {

    private TestConfig() {
    }

    /**
     * The root of a YAML document as plain maps and lists. Keys become text, as Bukkit's configuration gives them, so a key
     * written as a number, such as {@code 4:}, is read by its text {@code "4"}.
     */
    public static Map<String, Object> yaml(String text) {
        return loaded(new Yaml().load(text));
    }

    private static Map<String, Object> loaded(Object value) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, element) -> out.put(String.valueOf(key), plainValue(element)));
        }
        return out;
    }

    private static Object plainValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return loaded(value);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(TestConfig::plainValue).toList();
        }
        return value;
    }

    /** An empty, mutable configuration to fill with {@link #set}. */
    public static Map<String, Object> config() {
        return new LinkedHashMap<>();
    }

    /** Sets the value at a dotted path, creating the nested maps on the way. */
    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> config, String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = config;
        for (int i = 0; i < keys.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(keys[i], key -> new LinkedHashMap<String, Object>());
        }
        current.put(keys[keys.length - 1], value);
    }
}
