package io.moretntplus.core.mechanics;

import io.moretntplus.api.Mechanic;

import java.util.ArrayList;
import java.util.List;

/**
 * The catalogue of built-in mechanics. The core registers these under the ids used in {@code tnts.yml}.
 */
public final class Mechanics {

    private final Toolkit kit;

    public Mechanics(Toolkit kit) {
        this.kit = kit;
    }

    public List<Mechanic> all() {
        List<Mechanic> out = new ArrayList<>();
        out.add(new Standard(kit));
        out.addAll(StructuralMechanics.all(kit));
        out.addAll(TerrainMechanics.all(kit));
        out.addAll(EntityMechanics.all(kit));
        out.addAll(LogicMechanics.all(kit));
        out.addAll(UtilityMechanics.all(kit));
        out.addAll(MilitaryMechanics.all(kit));
        out.add(new NuclearMechanic(kit));
        return out;
    }

    /**
     * The plain blast. Shockwave, debris, echo and silent TNT are this mechanic with effect modifiers set in
     * {@code tnts.yml}, so they need no code of their own.
     */
    static final class Standard extends AbstractMechanic {

        Standard(Toolkit kit) {
            super("standard", "A sphere of blast: destroys blocks, damages nearby entities.", kit);
        }

        @Override
        public void detonate(io.moretntplus.api.DetonationContext context) {
            standardBlast(context, 12.0);
        }
    }
}
