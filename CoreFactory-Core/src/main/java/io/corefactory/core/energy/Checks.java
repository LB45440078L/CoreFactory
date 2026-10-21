package io.corefactory.core.energy;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Argument checks shared by the energy records. */
final class Checks {

    private Checks() {
    }

    static String id(String id, String what) {
        Objects.requireNonNull(id, what);
        if (id.isBlank()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return id;
    }

    static double finiteNonNegative(double value, String what) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(what + " must be finite and not negative: " + value);
        }
        return value;
    }

    static int nonNegative(int value, String what) {
        if (value < 0) {
            throw new IllegalArgumentException(what + " must not be negative: " + value);
        }
        return value;
    }

    static <T> List<T> sortedUnique(List<T> items, Function<? super T, String> id, String what) {
        Objects.requireNonNull(items, what);
        Set<String> seen = new HashSet<>();
        for (T item : items) {
            Objects.requireNonNull(item, what + " entry");
            if (!seen.add(id.apply(item))) {
                throw new IllegalArgumentException("duplicate " + what + " id: " + id.apply(item));
            }
        }
        return items.stream().sorted(Comparator.comparing(id)).toList();
    }
}
