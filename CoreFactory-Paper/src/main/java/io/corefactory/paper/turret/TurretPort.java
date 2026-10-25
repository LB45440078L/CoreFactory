package io.corefactory.paper.turret;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.RoundType;

import java.util.Objects;
import java.util.Optional;

/**
 * The rounds of a machine gun turret as a logistics port: a belt, a picker or a hopper-like sink gives it rounds one at a time, while the
 * turret holds fewer than its capacity and no other round type. A turret gives nothing back, so its output is empty. The magazine is the
 * turret's own, so the rounds are stored once. A laser turret takes no rounds, so it accepts nothing.
 *
 * <p>The logistics ports at a block (see {@code LogisticsPorts}) return this port for a turret's block; that branch is wired by the
 * integrator, since the logistics code is not part of this package.
 */
public final class TurretPort implements Port {

    private final TurretDevice turret;
    private final Runnable save;

    /**
     * @param turret the turret whose magazine the port fills
     * @param save   what saves the turret after a round is taken in, the same save the turret service makes after every other change
     */
    public TurretPort(TurretDevice turret, Runnable save) {
        this.turret = Objects.requireNonNull(turret, "turret");
        this.save = Objects.requireNonNull(save, "save");
    }

    @Override
    public boolean accepts(Item item) {
        if (turret.kind() != TurretKind.MACHINE_GUN) {
            return false;
        }
        return roundOf(item).map(type -> turret.magazine().roomFor(type) > 0).orElse(false);
    }

    @Override
    public void accept(Item item) {
        RoundType type = roundOf(item).orElseThrow(() -> new IllegalStateException("the turret does not take " + item.id()));
        Magazine magazine = turret.magazine();
        if (magazine.roomFor(type) < 1) {
            throw new IllegalStateException("the turret has no room for " + item.id());
        }
        turret.setMagazine(magazine.add(type, 1));
        save.run();
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
        return turret.magazine().count();
    }

    private static Optional<RoundType> roundOf(Item item) {
        if (!Item.COMPONENT.equals(item.kind())) {
            return Optional.empty();
        }
        return RoundType.ofItem(item.id());
    }
}
