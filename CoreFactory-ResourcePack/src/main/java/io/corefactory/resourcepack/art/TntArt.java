package io.corefactory.resourcepack.art;

import java.util.List;

/**
 * The block texture of every TNT type: a painted casing, a band at the top and bottom, and a label panel with a
 * pictogram and a short code. The texture is used on all six faces of the block.
 */
public final class TntArt {

    /** The pattern of the bands at the top and bottom of the casing. */
    public enum Band {
        /** Alternating diagonal warning stripes in the accent colour. */
        HAZARD,
        /** A dark band with a row of rivets. */
        RIVETS,
        /** A solid accent band with dark pinstripes. */
        STRIPE,
        /** A plain dark band. */
        PLAIN
    }

    /** The colour of the label panel. */
    public enum Panel {
        /** Warm paper, like the classic TNT label. */
        CREAM,
        /** Brushed steel. */
        STEEL,
        /** A dark panel in the casing's own colour. */
        DARK
    }

    /**
     * One TNT type's look.
     *
     * @param id      the TNT id from tnts.yml
     * @param label   a short code printed on the panel, at most five letters
     * @param icon    the pictogram on the panel
     * @param base    the casing colour, {@code #RRGGBB}
     * @param accent  the band and frame colour, {@code #RRGGBB}
     * @param band    the band pattern
     * @param panel   the panel colour
     */
    public record Spec(String id, String label, Icon icon, String base, String accent, Band band, Panel panel) {
    }

    /** Every TNT type, in the order of tnts.yml. */
    public static final List<Spec> ALL = List.of(
            new Spec("chain_reaction_tnt", "CHN", Icon.CHAIN, "#B5362A", "#FFB347", Band.HAZARD, Panel.CREAM),
            new Spec("shaped_charge_tnt", "SHP", Icon.CONE, "#C9A227", "#3A3010", Band.RIVETS, Panel.STEEL),
            new Spec("gravity_well_tnt", "GRV", Icon.SPIRAL, "#2A1442", "#B57BFF", Band.PLAIN, Panel.DARK),
            new Spec("implosion_tnt", "IMP", Icon.ARROWS_IN, "#1A1A1E", "#5C5C66", Band.RIVETS, Panel.STEEL),
            new Spec("fuse_linked_tnt", "LNK", Icon.LINK, "#8C1D1D", "#FF5555", Band.STRIPE, Panel.CREAM),
            new Spec("delayed_cascade_tnt", "DLY", Icon.HOURGLASS, "#4A4F55", "#D0D6DC", Band.RIVETS, Panel.STEEL),
            new Spec("four_dimensional_tnt", "4D", Icon.TESSERACT, "#7A0A5A", "#FF6BE0", Band.HAZARD, Panel.DARK),
            new Spec("exotic_neutron_bomb", "NTR", Icon.SKULL, "#A9D9F2", "#2A4A6A", Band.STRIPE, Panel.STEEL),
            new Spec("exotic_antimatter_bomb", "ANTI", Icon.FLARE, "#14082A", "#F4F0FF", Band.PLAIN, Panel.DARK),
            new Spec("vitrify_tnt", "VIT", Icon.CRYSTAL, "#A8E8F0", "#2A1A40", Band.PLAIN, Panel.CREAM),
            new Spec("liquefy_tnt", "LIQ", Icon.DROP, "#5A1A00", "#FF8A1A", Band.HAZARD, Panel.CREAM),
            new Spec("terraform_tnt", "TER", Icon.MOUNTAIN, "#6B4A2A", "#3E7A2A", Band.PLAIN, Panel.CREAM),
            new Spec("randomize_tnt", "RND", Icon.DIE, "#505050", "#E040E0", Band.RIVETS, Panel.STEEL),
            new Spec("sculpt_tnt", "SCL", Icon.CHISEL, "#2A6E8A", "#A8E0FF", Band.PLAIN, Panel.CREAM),
            new Spec("concussion_tnt", "CON", Icon.BURST, "#D8C84A", "#3A3A10", Band.HAZARD, Panel.CREAM),
            new Spec("launch_tnt", "LCH", Icon.UP, "#3AAA6A", "#D8FFE8", Band.STRIPE, Panel.CREAM),
            new Spec("teleport_tnt", "TLP", Icon.PORTAL, "#1A0A2E", "#8A4DFF", Band.PLAIN, Panel.DARK),
            new Spec("gravity_flip_tnt", "FLP", Icon.FLIP, "#1E4A6A", "#E8F8FF", Band.STRIPE, Panel.STEEL),
            new Spec("conditional_tnt", "IF", Icon.QUESTION, "#3A3A3A", "#FF3A3A", Band.RIVETS, Panel.CREAM),
            new Spec("proximity_tnt", "PRX", Icon.TARGET, "#4A0A0A", "#FFD000", Band.HAZARD, Panel.DARK),
            new Spec("pulse_tnt", "PLS", Icon.WAVE, "#8A1A1A", "#FF6060", Band.PLAIN, Panel.STEEL),
            new Spec("mirror_tnt", "MIR", Icon.SPLIT, "#C8E8F0", "#3A6A7A", Band.STRIPE, Panel.CREAM),
            new Spec("shockwave_tnt", "SHK", Icon.RINGS, "#2A2A3A", "#FFD8A0", Band.PLAIN, Panel.DARK),
            new Spec("debris_tnt", "DBR", Icon.SHARDS, "#5A5A5A", "#9A8A6A", Band.RIVETS, Panel.STEEL),
            new Spec("echo_tnt", "ECH", Icon.ECHO, "#3A1A5A", "#D0A0FF", Band.PLAIN, Panel.DARK),
            new Spec("silent_tnt", "SIL", Icon.MUTE, "#262626", "#4A4A4A", Band.PLAIN, Panel.STEEL),
            new Spec("harvest_tnt", "HRV", Icon.SPROUT, "#2A6A3A", "#E8D050", Band.STRIPE, Panel.CREAM),
            new Spec("sorting_tnt", "SRT", Icon.BARS, "#34507A", "#A0C8FF", Band.RIVETS, Panel.CREAM),
            new Spec("marker_tnt", "MRK", Icon.CROSSHAIR, "#1E3A5A", "#55FFFF", Band.STRIPE, Panel.DARK),
            new Spec("anchor_tnt", "ANC", Icon.ANCHOR, "#2A1A50", "#9AF0FF", Band.PLAIN, Panel.DARK),
            new Spec("biological_tnt", "BIO", Icon.BIOHAZARD, "#2A5A1A", "#9AFF40", Band.HAZARD, Panel.CREAM),
            new Spec("napalm_tnt", "NPM", Icon.FLAME, "#8A2A0A", "#FFB020", Band.HAZARD, Panel.STEEL),
            new Spec("nuke_davy_crockett", "W54", Icon.RADIATION, "#3A4A2A", "#E8FF60", Band.STRIPE, Panel.CREAM),
            new Spec("nuke_little_boy", "LB", Icon.MUSHROOM, "#4A5A2A", "#F0FF70", Band.RIVETS, Panel.STEEL),
            new Spec("nuke_fat_man", "FM", Icon.BOMB, "#4A4A2A", "#FFE860", Band.RIVETS, Panel.CREAM),
            new Spec("nuke_joe_1", "RDS1", Icon.SUN, "#4A3A20", "#FFD060", Band.STRIPE, Panel.STEEL),
            new Spec("nuke_ivy_king", "IVY", Icon.ATOM, "#5A3A18", "#FFA030", Band.HAZARD, Panel.DARK),
            new Spec("nuke_b83", "B83", Icon.ORB, "#5A2A18", "#FF8040", Band.RIVETS, Panel.DARK),
            new Spec("nuke_ivy_mike", "MIKE", Icon.MUSHROOM, "#6A2A1A", "#FF6030", Band.STRIPE, Panel.CREAM),
            new Spec("nuke_castle_bravo", "BRV", Icon.SUN, "#6A1A1A", "#FF4A20", Band.HAZARD, Panel.STEEL),
            new Spec("nuke_mk17", "MK17", Icon.WARNING, "#5A0A0A", "#FF3A3A", Band.HAZARD, Panel.CREAM),
            new Spec("nuke_tsar_bomba", "TSAR", Icon.MUSHROOM, "#2A0A0A", "#FF2A2A", Band.RIVETS, Panel.DARK));

    private TntArt() {
    }

    /** Paints one TNT block texture onto a fresh canvas. */
    public static void paint(Canvas c, Spec spec) {
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        long seed = spec.id().hashCode();

        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> {
            double falloff = 1.1 - 0.25 * y / 31.0;
            double grain = (Noise.hash(x, y, seed) - 0.5) * 0.10;
            double mottle = (Noise.smooth(x / 5.0, y / 5.0, seed) - 0.5) * 0.14;
            return Color.scale(base, falloff * (1 + grain + mottle));
        });
        paintBands(c, spec.band(), base, accent);
        c.paint(Area.rect(0, 0, 1, Canvas.SIZE), Color.withAlpha(Color.BLACK, 0.25));
        c.paint(Area.rect(Canvas.SIZE - 1, 0, Canvas.SIZE, Canvas.SIZE), Color.withAlpha(Color.BLACK, 0.25));

        Area panel = Area.roundRect(4, 6, 28, 26, 2.5);
        int panelTop = panelTop(spec.panel(), base);
        int panelBottom = panelBottom(spec.panel(), base);
        c.shade(panel, (x, y, current) -> Color.mix(panelTop, panelBottom, (y - 6) / 20.0));
        c.paint(panel.minus(Area.roundRect(5, 7, 27, 25, 2)), accent);

        int ink = switch (spec.panel()) {
            case CREAM -> Color.scale(base, 0.6);
            case STEEL -> Color.hex("#F4F5F7");
            case DARK -> accent;
        };
        int second = switch (spec.panel()) {
            case CREAM -> Color.mix(ink, accent, 0.5);
            case STEEL -> accent;
            case DARK -> Color.mix(accent, Color.WHITE, 0.5);
        };
        int text = switch (spec.panel()) {
            case CREAM -> Color.scale(base, 0.45);
            case STEEL, DARK -> Color.WHITE;
        };
        spec.icon().draw(c, 16, 12.5, 5.5, ink, second);
        Font.draw(c, spec.label(), 16 - Font.width(spec.label()) / 2, 19, text);
    }

    private static void paintBands(Canvas c, Band band, int base, int accent) {
        Area bands = Area.rect(0, 0, Canvas.SIZE, 3).or(Area.rect(0, 29, Canvas.SIZE, Canvas.SIZE));
        switch (band) {
            case HAZARD -> c.shade(bands, (x, y, current) ->
                    ((x + y) / 3) % 2 == 0 ? accent : Color.scale(accent, 0.3));
            case RIVETS -> {
                c.paint(bands, Color.scale(base, 0.5));
                for (int x = 2; x < Canvas.SIZE; x += 6) {
                    c.set(x, 1, Color.mix(base, Color.WHITE, 0.4));
                    c.set(x, 30, Color.mix(base, Color.WHITE, 0.4));
                }
            }
            case STRIPE -> {
                c.paint(bands, accent);
                c.paint(Area.rect(0, 1, Canvas.SIZE, 2), Color.scale(accent, 0.45));
                c.paint(Area.rect(0, 30, Canvas.SIZE, 31), Color.scale(accent, 0.45));
            }
            case PLAIN -> c.paint(bands, Color.scale(base, 0.55));
        }
    }

    private static int panelTop(Panel panel, int base) {
        return switch (panel) {
            case CREAM -> Color.hex("#F7EDD2");
            case STEEL -> Color.hex("#A9AFB7");
            case DARK -> Color.scale(base, 0.75);
        };
    }

    private static int panelBottom(Panel panel, int base) {
        return switch (panel) {
            case CREAM -> Color.hex("#E2D2A8");
            case STEEL -> Color.hex("#50565F");
            case DARK -> Color.scale(base, 0.35);
        };
    }
}
