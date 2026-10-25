package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The fuel items of the generators: which items each kind takes, how many a generator keeps, and how a kept count is split into
 * stacks when the generator gives its fuel back. Only charcoal and nuclear fuel rods are items. Wind, solar, petroleum and diesel take
 * no item: petroleum and diesel burn fluid from the oil network. Pure: the caller reads the items and their identity and passes the
 * source and id in.
 */
public final class GeneratorFuel {

    /** The most fuel items one generator keeps, and the most in one stack when it gives them back. */
    public static final int CAP = 64;

    /** Where a fuel item comes from: a plain vanilla material, or a plugin component. */
    public enum Source {
        /** A vanilla item with no plugin identity, such as charcoal. */
        MATERIAL,
        /** A plugin component, such as a nuclear fuel rod. */
        COMPONENT
    }

    private GeneratorFuel() {
    }

    /**
     * Whether a generator of this kind takes an item.
     *
     * @param kind   the generator's kind
     * @param source where the item comes from
     * @param itemId the vanilla material name, or the component id
     * @return true for charcoal (a plain CHARCOAL) and a nuclear fuel rod (its component); false otherwise.
     *         Whether a nuclear generator stands in water is a production rule, not checked here.
     */
    public static boolean accepts(GeneratorKind kind, Source source, String itemId) {
        return sourceOf(kind).filter(found -> found.equals(source)).isPresent() && itemIdOf(kind).filter(found -> found.equals(itemId)).isPresent();
    }

    /** The source of the item a kind burns, or empty for a kind that burns no item. */
    public static Optional<Source> sourceOf(GeneratorKind kind) {
        return switch (kind) {
            case WIND, SOLAR, PETROLEUM, DIESEL -> Optional.empty();
            case CHARCOAL -> Optional.of(Source.MATERIAL);
            case NUCLEAR -> Optional.of(Source.COMPONENT);
        };
    }

    /** The id of the item a kind burns, or empty for a kind that burns no item. */
    public static Optional<String> itemIdOf(GeneratorKind kind) {
        return switch (kind) {
            case WIND, SOLAR, PETROLEUM, DIESEL -> Optional.empty();
            case CHARCOAL -> Optional.of("CHARCOAL");
            case NUCLEAR -> Optional.of("nuclear_fuel_rod");
        };
    }

    /**
     * The fuel count a slot or a saved value gives: its amount, from 0 up to {@link #CAP}.
     *
     * @param amount the items held
     */
    public static int countOf(int amount) {
        return Math.min(CAP, Math.max(0, amount));
    }

    /**
     * The room left for more items in a generator that keeps the given count.
     *
     * @param count the items kept now
     */
    public static int room(int count) {
        return CAP - countOf(count);
    }

    /**
     * The stacks a kept count is given back in: full stacks of {@link #CAP}, then the rest. The sizes add up to the count, so no
     * item is lost or made when the generator is broken.
     *
     * @param count the items kept
     */
    public static List<Integer> stacks(int count) {
        List<Integer> stacks = new ArrayList<>();
        int left = countOf(count);
        while (left > 0) {
            int size = Math.min(CAP, left);
            stacks.add(size);
            left -= size;
        }
        return List.copyOf(stacks);
    }
}
