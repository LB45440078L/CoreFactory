package io.corefactory.spigot.survey;

import java.util.Map;

/**
 * The map colour of each ore: its base colour, the rock colour the resource pack paints it with (see OreArt in the
 * ResourcePack module). The Spigot module does not load that module at runtime, so the colours are kept here; change
 * both when an ore's look changes.
 */
public final class OreColours {

    /** The colour of an ore id that has no entry, such as one an admin added to ores.yml. */
    public static final int UNKNOWN_RGB = 0x7A8CA0;

    private static final Map<String, Integer> BASE = Map.ofEntries(
            Map.entry("uraninite_ore", 0x1B1D20),
            Map.entry("pitchblende_ore", 0x2A1D12),
            Map.entry("monazite_ore", 0xE0B040),
            Map.entry("thorite_ore", 0x2C2C30),
            Map.entry("galena_ore", 0x9AA2AC),
            Map.entry("cinnabar_ore", 0xA8201E),
            Map.entry("fluorite_ore", 0x8A4DC4),
            Map.entry("crude_petroleum_deposit", 0x2B1F17),
            Map.entry("saltpeter_deposit", 0xC4C0B4),
            Map.entry("sulfur_deposit", 0x4A4638),
            Map.entry("zinc_deposit", 0x4A3524),
            Map.entry("coal_ore", 0x5C5C60));

    private OreColours() {
    }

    /** The base colour of an ore id, as RGB. */
    public static int of(String oreId) {
        return BASE.getOrDefault(oreId, UNKNOWN_RGB);
    }
}
