package io.corefactory.core.process;

import java.util.Objects;
import java.util.Optional;

/**
 * A reference to an item in a process: a component id from materials.yml or items.yml ({@code item:<id>}), or a
 * vanilla material ({@code material:<NAME>}).
 *
 * @param kind whether the name is a component id or a material name
 * @param name the component id or the material name
 */
public record ItemRef(Kind kind, String name) {

    /** The two kinds of reference. */
    public enum Kind {
        ITEM("item"),
        MATERIAL("material");

        private final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }

    public ItemRef {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
    }

    public static ItemRef item(String id) {
        return new ItemRef(Kind.ITEM, id);
    }

    public static ItemRef material(String name) {
        return new ItemRef(Kind.MATERIAL, name);
    }

    /** Reads {@code item:<id>} or {@code material:<NAME>}. Empty when the text has neither form. */
    public static Optional<ItemRef> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        for (Kind kind : Kind.values()) {
            String start = kind.prefix() + ":";
            if (trimmed.startsWith(start) && trimmed.length() > start.length()) {
                return Optional.of(new ItemRef(kind, trimmed.substring(start.length()).trim()));
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return kind.prefix() + ":" + name;
    }
}
