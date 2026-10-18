package io.moretntplus.api;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/**
 * Immutable description of one TNT type, as loaded from {@code tnts.yml}.
 *
 * @param id          stable lower snake case id, also the item's key ({@code moretntplus:<id>})
 * @param displayName legacy-coloured display name
 * @param lore        legacy-coloured lore lines
 * @param material    base material of the item (its look comes from the resource pack model)
 * @param fuseTicks   fuse length when ignited, in ticks
 * @param radius      blast radius in blocks; meaning is mechanic specific
 * @param mechanic    id of the {@link Mechanic} that runs this TNT
 * @param settings    mechanic specific parameters
 * @param effects     cosmetic and audio modifiers: {@code shockwave_ring}, {@code debris}, {@code echo}, {@code silent}
 */
public record TntDefinition(
        String id,
        String displayName,
        List<String> lore,
        Material material,
        int fuseTicks,
        double radius,
        String mechanic,
        MechanicSettings settings,
        Set<String> effects
) {

    public TntDefinition {
        lore = List.copyOf(lore);
        effects = Set.copyOf(effects);
    }

    /** True when the named cosmetic or audio modifier is enabled for this TNT. */
    public boolean hasEffect(String effect) {
        return effects.contains(effect);
    }
}
