package io.corefactory.spigot.logistics;

import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;

import java.util.Objects;

/**
 * The fuel of a generator as a port: a belt or a picker gives it the fuel items its kind burns, while the generator keeps fewer
 * than {@link GeneratorFuel#CAP}. It has no output. The tank is the generator's own count, so the fuel is stored once. Pure: the
 * tank is an interface, so the rules are tested with an in-memory tank.
 */
public final class FuelPort implements Port {

    /** The fuel count of a generator, and a way to add one item to it. */
    public interface Tank {

        /** The fuel items the generator keeps now. */
        int count();

        /** Adds one fuel item. Called only when {@link #count()} is below the cap. */
        void addOne();
    }

    private final GeneratorKind kind;
    private final Tank tank;

    /**
     * @param kind the generator's kind; a kind with no fuel accepts nothing
     * @param tank the generator's fuel count
     */
    public FuelPort(GeneratorKind kind, Tank tank) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.tank = Objects.requireNonNull(tank, "tank");
    }

    @Override
    public boolean accepts(Item item) {
        if (tank.count() >= GeneratorFuel.CAP) {
            return false;
        }
        return GeneratorFuel.sourceOf(kind)
                .map(source -> GeneratorFuel.accepts(kind, sourceOf(item), item.id()))
                .orElse(false);
    }

    @Override
    public void accept(Item item) {
        if (!accepts(item)) {
            throw new IllegalStateException("the generator does not take " + item.id());
        }
        tank.addOne();
    }

    @Override
    public Item peekOutput() {
        return null;
    }

    @Override
    public Item takeOutput() {
        return null;
    }

    @Override
    public int storedCount() {
        return tank.count();
    }

    private static GeneratorFuel.Source sourceOf(Item item) {
        return Item.COMPONENT.equals(item.kind()) ? GeneratorFuel.Source.COMPONENT : GeneratorFuel.Source.MATERIAL;
    }
}
