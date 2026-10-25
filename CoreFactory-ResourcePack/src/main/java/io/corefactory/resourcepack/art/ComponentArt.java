package io.corefactory.resourcepack.art;

import java.util.List;
import java.util.stream.Stream;

/**
 * The item sprite of every crafting component: a silhouette, lit with {@link Sprite}, and details painted on top.
 * Each component has its own shape, so the set reads at a glance in the inventory and the catalogue.
 */
public final class ComponentArt {

    /** Paints one component's silhouette and details. */
    @FunctionalInterface
    public interface Painter {
        void paint(Canvas c, int base, int accent, long seed);
    }

    /**
     * One component's look.
     *
     * @param id      the component id from items.yml
     * @param base    the main colour, {@code #RRGGBB}
     * @param accent  the detail colour, {@code #RRGGBB}
     * @param painter draws the silhouette and details
     */
    public record Spec(String id, String base, String accent, Painter painter) {
    }

    /**
     * The components listed here: the items.yml components first, then the materials.yml components, the machines and
     * the logistics.
     */
    private static final List<Spec> LISTED = List.of(
            new Spec("fuse_linker", "#3A3F4A", "#E0302A", ComponentArt::fuseLinker),
            new Spec("fuse_wire", "#C07040", "#FFB070", ComponentArt::fuseWire),
            new Spec("shaped_charge_lens", "#A8E0F0", "#2F7F9F", ComponentArt::shapedLens),
            new Spec("gravity_core", "#1E1030", "#9A4DFF", ComponentArt::gravityCore),
            new Spec("vitrifier_flux", "#E8D8A0", "#FFFFFF", ComponentArt::flux),
            new Spec("sorting_magnet", "#505A70", "#A0B0D0", ComponentArt::magnet),
            new Spec("anchor_crystal", "#7B5CC4", "#D8C8FF", ComponentArt::crystal),
            new Spec("pulse_capacitor", "#7A1010", "#FF4040", ComponentArt::capacitor),
            new Spec("quantum_detonator", "#222222", "#FFD000", ComponentArt::detonator),
            new Spec("biohazard_culture", "#3B7A2A", "#A8FF60", ComponentArt::culture),
            new Spec("napalm_gel", "#B84A10", "#FFB030", ComponentArt::canister),
            new Spec("anchor_plate", "#9AA0A8", "#D0D4DA", ComponentArt::anchorPlate),
            new Spec("enriched_uranium", "#4E5A3A", "#A6FF3A", ComponentArt::pellets),
            new Spec("plutonium_pit", "#5A3A70", "#E8A0FF", ComponentArt::pit),
            new Spec("tritium_booster", "#1E6A70", "#7FFFE0", ComponentArt::tube),
            new Spec("neutron_initiator", "#6A4A9A", "#FFE070", ComponentArt::initiator),
            new Spec("nuclear_casing", "#2E3238", "#FFB020", ComponentArt::casing),
            new Spec("thermonuclear_core", "#7A1020", "#FF6A6A", ComponentArt::core),
            new Spec("hazmat_fabric", "#D8C23A", "#2A2A2A", ComponentArt::fabric),
            new Spec("radioactive_helmet", "#D8C23A", "#7FE0FF", ComponentArt::helmet),
            new Spec("radioactive_chestplate", "#D8C23A", "#2A2A2A", ComponentArt::chestplate),
            new Spec("radioactive_leggings", "#D8C23A", "#2A2A2A", ComponentArt::leggings),
            new Spec("radioactive_boots", "#D8C23A", "#2A2A2A", ComponentArt::boots),
            new Spec("geiger_counter", "#4A4E57", "#FFD000", ComponentArt::counter),
            // Raw ore drops: the mineral as it comes out of the rock.
            new Spec("raw_uraninite", "#1B1D20", "#7CFF5A", ComponentArt::rawUraninite),
            new Spec("raw_pitchblende", "#3B2A1C", "#8A6A3A", ComponentArt::rawPitchblende),
            new Spec("raw_monazite", "#C98A2A", "#FFD87A", ComponentArt::rawMonazite),
            new Spec("raw_thorite", "#161616", "#FF8A20", ComponentArt::rawThorite),
            new Spec("raw_galena", "#7E8794", "#E4E9F0", ComponentArt::rawGalena),
            new Spec("raw_cinnabar", "#A8201E", "#FF6A55", ComponentArt::rawCinnabar),
            new Spec("raw_fluorite", "#8A4DC4", "#62E89A", ComponentArt::rawFluorite),
            // Crushed ore: a heap of powder in a steel dish, speckled with the mineral's glints.
            new Spec("crushed_uraninite", "#2A2E30", "#8CFF70", ComponentArt::crushed),
            new Spec("crushed_pitchblende", "#5E4124", "#C89A5A", ComponentArt::crushed),
            new Spec("crushed_monazite", "#D9A040", "#FFE8A0", ComponentArt::crushed),
            new Spec("crushed_thorite", "#2B2622", "#FF9A3A", ComponentArt::crushed),
            new Spec("crushed_galena", "#9AA0AA", "#FFFFFF", ComponentArt::crushed),
            new Spec("crushed_cinnabar", "#B8382A", "#FF9080", ComponentArt::crushed),
            new Spec("crushed_fluorite", "#7A5AB0", "#7AF0A0", ComponentArt::crushed),
            // Roasted, separated and converted products.
            new Spec("uranium_calcine", "#4A4E3A", "#C8D060", ComponentArt::calcine),
            new Spec("slag", "#353030", "#7A9A9A", ComponentArt::slag),
            new Spec("refined_lead", "#5C6172", "#A8B4D0", ComponentArt::ingot),
            new Spec("mercury_flask", "#9AA8B8", "#E8F0FF", ComponentArt::flask),
            new Spec("yellowcake", "#E0C23A", "#FFF6B0", ComponentArt::sack),
            new Spec("tailings", "#8C8478", "#B8AE9A", ComponentArt::heap),
            new Spec("rare_earth_concentrate", "#3E5AA0", "#A0C0FF", ComponentArt::jar),
            new Spec("phosphate_tailings", "#9AA070", "#D8DC9A", ComponentArt::slabs),
            new Spec("thorium_concentrate", "#6A8A6A", "#D0F0C8", ComponentArt::bucket),
            new Spec("silicate_tailings", "#B8C4CC", "#F0F8FF", ComponentArt::cullet),
            new Spec("uf6_cylinder", "#5E7A9A", "#FFD040", ComponentArt::gasCylinder),
            new Spec("depleted_uranium", "#4A5A46", "#D8C23A", ComponentArt::coins),
            new Spec("deuterium_cell", "#2A4A8A", "#7FD0FF", ComponentArt::cell),
            new Spec("purified_iron", "#C8D0DA", "#FFFFFF", ComponentArt::ibeam),
            new Spec("high_purity_graphite", "#2A2A2C", "#6A6E78", ComponentArt::sheet),
            new Spec("u233_oxide", "#3EA070", "#B8FFD8", ComponentArt::bottle),
            new Spec("plutonium_oxide", "#3A4A6A", "#7AB0FF", ComponentArt::tin),
            new Spec("fission_waste", "#2A4A7A", "#FFD000", ComponentArt::drum),
            new Spec("neutron_flux_cell", "#2E6A5A", "#C0FF80", ComponentArt::fluxCell),
            new Spec("superconducting_magnet", "#B86A3A", "#7FD8FF", ComponentArt::solenoid),
            new Spec("antiproton_capsule", "#5ACBE0", "#F0FFFF", ComponentArt::capsule),
            new Spec("antimatter_condensate", "#2A1040", "#E0B0FF", ComponentArt::droplet),
            new Spec("neutronium_slug", "#1C1C28", "#7A7AFF", ComponentArt::slug),
            new Spec("tesseract_lattice", "#3A1A5A", "#FF70FF", ComponentArt::lattice),
            // Research packs: white and green cards, and coloured cartridges.
            new Spec("research_pack_basic", "#EEF0F4", "#3A4048", ComponentArt.card(null)),
            new Spec("research_pack_chemical", "#3FB85A", "#E0FFE8", ComponentArt.card(Icon.DROP)),
            new Spec("research_pack_nuclear", "#B8D83A", "#2A2A10", ComponentArt.cartridge(Icon.RADIATION)),
            new Spec("research_pack_plasma", "#2A7AE0", "#A8E8FF", ComponentArt.cartridge(Icon.WAVE)),
            new Spec("research_pack_exotic", "#7A2AE0", "#E0A0FF", ComponentArt.cartridge(Icon.ORB)),
            // Survey and oil: the density scanner, the solar panel and the crude petroleum.
            new Spec("ore_density_scanner", "#B08A3A", "#3CFF7A", ComponentArt::densityScanner),
            new Spec("solar_panel", "#C4CBD4", "#2A6FD8", ComponentArt::solarPanel),
            // Power: the wire, the pylons and accumulators, the wind turbine, the diesel and the nuclear fuel.
            new Spec("energy_wire", "#C07040", "#FFB070", ComponentArt::energyWire),
            new Spec("pylon_1", "#7C8590", "#C87533", ComponentArt.pylon(1)),
            new Spec("pylon_2", "#7C8590", "#E0B040", ComponentArt.pylon(2)),
            new Spec("pylon_3", "#7C8590", "#9B5CFF", ComponentArt.pylon(3)),
            new Spec("pylon_4", "#7C8590", "#E0443A", ComponentArt.pylon(4)),
            new Spec("pylon_5", "#7C8590", "#F2B233", ComponentArt.pylon(5)),
            new Spec("accumulator_1", "#8A929C", "#5BC8F0", ComponentArt.accumulator(1)),
            new Spec("accumulator_2", "#8A929C", "#3F7BFF", ComponentArt.accumulator(2)),
            new Spec("accumulator_3", "#8A929C", "#8A5CFF", ComponentArt.accumulator(3)),
            new Spec("accumulator_4", "#8A929C", "#2FC7B8", ComponentArt.accumulator(4)),
            new Spec("accumulator_5", "#8A929C", "#D65CD6", ComponentArt.accumulator(5)),
            new Spec("wind_generator", "#E4E8EE", "#3A8FD8", ComponentArt::wind),
            new Spec("nuclear_fuel_rod", "#8E949C", "#B8FF5A", ComponentArt::fuelRod),
            // Placeable machines: the inventory sprite. The 3D model is in MachineArt.
            new Spec("crusher", "#5A6470", "#FFB000", ComponentArt.machine(Icon.CONE)),
            new Spec("roaster", "#6A3A24", "#FF7A20", ComponentArt.machine(Icon.FLAME)),
            new Spec("separator", "#3A5A6A", "#9FF0FF", ComponentArt.machine(Icon.SPIRAL)),
            new Spec("purifier", "#2F6A5A", "#7FFFD0", ComponentArt.machine(Icon.DROP)),
            new Spec("converter", "#5A5A6A", "#FFD040", ComponentArt.machine(Icon.TARGET)),
            new Spec("enrichment_cascade", "#3A4A60", "#C0F060", ComponentArt.machine(Icon.BARS)),
            new Spec("transmuter", "#2A2038", "#B56BFF", ComponentArt.machine(Icon.RINGS)),
            new Spec("spectrometer", "#2A3A44", "#55FFFF", ComponentArt.machine(Icon.WAVE)),
            new Spec("research_console", "#4A5058", "#66FF99", ComponentArt.machine(Icon.ATOM)),
            new Spec("particle_accelerator", "#4A4E60", "#7AB8FF", ComponentArt.machine(Icon.PORTAL)),
            new Spec("antimatter_condenser", "#1A1E3A", "#C080FF", ComponentArt.machine(Icon.ORB)),
            new Spec("charcoal_generator", "#4A3A30", "#FF7A20", ComponentArt::charcoalGenerator),
            new Spec("petroleum_generator", "#2A2F38", "#E0A030", ComponentArt::petroleumGenerator),
            new Spec("diesel_generator", "#3A4048", "#E89020", ComponentArt::dieselGenerator),
            new Spec("nuclear_generator", "#2A3A4A", "#3AA8FF", ComponentArt::nuclearGenerator),
            new Spec("crude_oil_refinery", "#4A4A52", "#FF8C2A", ComponentArt::refinery),
            new Spec("petroleum_drill_1", "#2B2B2F", "#B5482A", ComponentArt.drill(Icon.DROP, 1)),
            new Spec("petroleum_drill_2", "#2B2B2F", "#B5482A", ComponentArt.drill(Icon.DROP, 2)),
            new Spec("petroleum_drill_3", "#2B2B2F", "#B5482A", ComponentArt.drill(Icon.DROP, 3)),
            new Spec("petroleum_drill_4", "#2B2B2F", "#B5482A", ComponentArt.drill(Icon.DROP, 4)),
            new Spec("petroleum_drill_5", "#2B2B2F", "#B5482A", ComponentArt.drill(Icon.DROP, 5)),
            new Spec("ore_drill_1", "#6E7884", "#3AA8FF", ComponentArt.drill(Icon.CHISEL, 1)),
            new Spec("ore_drill_2", "#6E7884", "#3AA8FF", ComponentArt.drill(Icon.CHISEL, 2)),
            new Spec("ore_drill_3", "#6E7884", "#3AA8FF", ComponentArt.drill(Icon.CHISEL, 3)),
            new Spec("ore_drill_4", "#6E7884", "#3AA8FF", ComponentArt.drill(Icon.CHISEL, 4)),
            new Spec("ore_drill_5", "#6E7884", "#3AA8FF", ComponentArt.drill(Icon.CHISEL, 5)),
            // Logistics: belts carry items, pickers move them from one face to the other, splitters send them three ways.
            new Spec("belt_1", "#5A6470", "#E8ECF2", ComponentArt::belt),
            new Spec("belt_2", "#5A6470", "#FFD040", ComponentArt::belt),
            new Spec("belt_3", "#5A6470", "#FF5A20", ComponentArt::belt),
            new Spec("picker_1", "#B87333", "#3A4048", ComponentArt::picker),
            new Spec("picker_2", "#E0B030", "#3A4048", ComponentArt::picker),
            new Spec("picker_3", "#9B5CFF", "#3A4048", ComponentArt::picker),
            new Spec("splitter_1", "#5A6470", "#C87533", ComponentArt::splitter),
            new Spec("splitter_2", "#7A6A3A", "#D8A83A", ComponentArt::splitter),
            new Spec("splitter_3", "#5E4E8A", "#C9A227", ComponentArt::splitter),
            // Process products: the raw drops, the crushed and processed salts and metals, the powder and the ammunition.
            new Spec("raw_saltpeter", "#D9D5C4", "#FFFFFF", ComponentArt::rawSaltpeter),
            new Spec("raw_sulfur", "#E8C81E", "#FFF5A0", ComponentArt::lumps),
            new Spec("raw_sphalerite", "#3B3026", "#6FE0EC", ComponentArt::rawSphalerite),
            new Spec("crude_saltpeter", "#E8E6DA", "#FFFFFF", ComponentArt::crushed),
            new Spec("sulfur", "#E8C21A", "#FFF7B0", ComponentArt::crushed),
            new Spec("zinc_ore_dust", "#5A5246", "#6FDCE8", ComponentArt::crushed),
            new Spec("potassium_nitrate", "#F2F4F6", "#B8CCDC", ComponentArt::saltCrystals),
            new Spec("high_purity_potassium_nitrate", "#E6F6FF", "#9FD4F0", ComponentArt::saltCrystals),
            new Spec("zinc_ingot", "#7D90A4", "#C9D8E8", ComponentArt::ingot),
            new Spec("brass_ingot", "#C9A03A", "#F5E2A0", ComponentArt::ingot),
            new Spec("high_grade_gunpowder", "#1A1A1E", "#6A707A", ComponentArt::granules),
            new Spec("empty_shell", "#C9A24A", "#8A6A22", ComponentArt::shell),
            new Spec("bullet_regular", "#C9A24A", "#9AA3B0", ComponentArt::bullet),
            new Spec("bullet_armor_piercing", "#C9A24A", "#3A3F48", ComponentArt::bullet),
            new Spec("bullet_incendiary", "#C9A24A", "#FF7A20", ComponentArt::bullet),
            new Spec("bullet_depleted_uranium", "#C9A24A", "#3E5A44", ComponentArt::bullet),
            new Spec("round_regular", "#C9A24A", "#9AA3B0", ComponentArt::round),
            new Spec("round_armor_piercing", "#A8843E", "#3A3F48", ComponentArt::round),
            new Spec("round_incendiary", "#C9A24A", "#FF7A20", ComponentArt::round),
            new Spec("round_depleted_uranium", "#6E7F62", "#3E5A44", ComponentArt::round),
            // Artillery stock and containment: the casings and the propellant packed for a shell, and the antimatter trap.
            new Spec("shell_casing_stock", "#C9A24A", "#7A5A1E", ComponentArt::shellCasingStock),
            new Spec("shell_powder_stock", "#6E7480", "#2A2C32", ComponentArt::shellPowderStock),
            new Spec("antimatter_containment", "#1A1430", "#B56BFF", ComponentArt::antimatterContainment),
            // Turret parts and the turret.
            new Spec("precision_barrel", "#8F98A3", "#D0D6DE", ComponentArt::barrel),
            new Spec("steel_frame", "#6D7782", "#C5CCD6", ComponentArt::frame),
            new Spec("targeting_unit", "#3A4A5A", "#55D8FF", ComponentArt::targeting),
            new Spec("turret_mechanism", "#5F6A76", "#D8B040", ComponentArt::mechanism),
            new Spec("ammunition_feed", "#5A6470", "#C98A3A", ComponentArt::feed),
            new Spec("machine_gun_turret", "#4B5A44", "#E0A030", ComponentArt::turret),
            // Hand-made parts.
            new Spec("iron_plate", "#BFC6CE", "#7E8690", ComponentArt::plate),
            new Spec("iron_gear", "#AEB6C0", "#6E7782", ComponentArt::smallGear),
            new Spec("circuit_board", "#1F6A3A", "#D9A040", ComponentArt::circuit),
            new Spec("charcoal", "#262220", "#5A524B", ComponentArt::lumps));

    /**
     * Every component: the listed components, then the process machines' inventory sprites from
     * {@link ProcessMachineArt#SPRITES}, then the turrets' sprites from {@link TurretArt#SPRITES}, then the artillery's
     * sprites from {@link ArtilleryArt#SPRITES} and the shells and charges from {@link ShellArt#SPRITES}.
     */
    public static final List<Spec> ALL = Stream.of(LISTED, ProcessMachineArt.SPRITES, TurretArt.SPRITES,
                    TurretLevelArt.SPRITES, LevelArt.SPRITES, WaterArt.SPRITES, OilVariants.SPRITES, VerticalBeltArt.SPRITES,
                    DuplexArt.SPRITES, ArtilleryArt.SPRITES, RailgunArt.SPRITES, ShellArt.SPRITES)
            .flatMap(List::stream).toList();

    private ComponentArt() {
    }

    /** Paints one component sprite onto a transparent canvas. */
    public static void paint(Canvas c, Spec spec) {
        c.clear(0x00000000);
        spec.painter().paint(c, Color.hex(spec.base()), Color.hex(spec.accent()), spec.id().hashCode());
    }

    private static void fuseLinker(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(11, 9, 9, 4, 1.4), Color.scale(base, 0.6));
        c.paint(Area.line(21, 9, 23, 4, 1.4), Color.scale(base, 0.6));
        c.paint(Area.disc(9, 4, 1.3), accent);
        c.paint(Area.disc(23, 4, 1.3), accent);
        Sprite.body(c, Area.roundRect(6, 9, 26, 25, 3), base, seed);
        c.paint(Area.roundRect(9, 12, 23, 18, 1.5), Color.hex("#1B2A22"));
        c.paint(Area.line(11, 14, 21, 14, 0.8), Color.hex("#8CF5A0"));
        c.paint(Area.disc(11, 21.5, 1.3), accent);
        c.paint(Area.disc(16, 21.5, 1.3), Color.WHITE);
        c.paint(Area.disc(21, 21.5, 1.3), Color.hex("#8CF5A0"));
    }

    private static void fuseWire(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(16, 16, 13), Color.scale(base, 0.55), seed);
        Sprite.body(c, Area.disc(16, 16, 9), base, seed + 1);
        for (int k = 0; k < 3; k++) {
            c.paint(Area.ring(16, 16, 4.5 + k * 1.5, 0.7), Color.scale(base, 0.6));
        }
        c.paint(Area.disc(16, 16, 2.2), Color.scale(base, 0.25));
        c.paint(Area.line(23, 20, 27, 27, 1.6), base);
        c.paint(Area.disc(27.5, 27.5, 1.2), accent);
    }

    private static void shapedLens(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ellipse(16, 16, 15, 11), Color.withAlpha(accent, 0.25));
        Sprite.body(c, Area.ellipse(16, 16, 13, 9), base, seed);
        c.paint(Area.orbit(16, 16, 9, 5, 0, 0.09), accent);
        c.paint(Area.orbit(16, 16, 5, 2.8, 0, 0.12), accent);
        c.paint(Area.ellipse(11, 12, 3, 1.5), Color.withAlpha(Color.WHITE, 0.6));
    }

    private static void gravityCore(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 14, 2.2), Color.withAlpha(accent, 0.45));
        Sprite.body(c, Area.disc(16, 16, 12), base, seed);
        c.paint(Area.arc(16, 16, 4, 11, -Math.PI / 2, Math.PI / 2), accent);
        c.paint(Area.arc(16, 16, 4, 11, Math.PI / 2, 3 * Math.PI / 2), Color.scale(accent, 0.7));
        c.paint(Area.disc(16, 16, 2.5), Color.withAlpha(Color.WHITE, 0.8));
    }

    private static void flux(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(12, 2, 20, 5), Color.hex("#8B5A2B"));
        Sprite.body(c, Area.rect(13, 4, 19, 12).or(Area.disc(16, 20, 10)), base, seed);
        c.paint(Area.disc(16, 21, 8).and(Area.rect(0, 16, 32, 32)), Color.withAlpha(accent, 0.85));
        c.paint(Area.ellipse(11, 19, 1.4, 4), Color.withAlpha(Color.WHITE, 0.5));
    }

    private static void magnet(Canvas c, int base, int accent, long seed) {
        Area horseshoe = Area.arc(16, 14, 5, 11, 0, Math.PI);
        Area legs = Area.rect(5, 14, 10, 20).or(Area.rect(22, 14, 27, 20));
        Sprite.body(c, horseshoe.or(legs), base, seed);
        c.paint(Area.rect(5, 14, 10, 18), Color.hex("#E0302A"));
        c.paint(Area.rect(22, 14, 27, 18), Color.hex("#3A7BFF"));
        c.paint(Area.rect(5, 14, 10, 18).minus(Area.rect(6, 15, 9, 17)), accent);
    }

    private static void crystal(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(16, 3, 25, 11, 22, 27, 10, 27, 7, 11), base, seed);
        c.paint(Area.polygon(16, 3, 7, 11, 10, 27, 16, 27), Color.withAlpha(Color.WHITE, 0.22));
        c.paint(Area.polygon(18, 8, 22, 13, 19, 22, 17, 13), Color.withAlpha(accent, 0.5));
        c.paint(Area.line(14, 9, 13, 20, 1.2), Color.withAlpha(Color.WHITE, 0.6));
    }

    private static void capacitor(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(14, 3, 18, 7), Color.hex("#B8BEC6"));
        Sprite.body(c, Area.rect(10, 8, 22, 24).or(Area.ellipse(16, 8, 6, 2.6)).or(Area.ellipse(16, 24, 6, 2.6)),
                base, seed);
        c.paint(Area.rect(10, 13, 22, 15), accent);
        c.paint(Area.rect(10, 19, 22, 21), accent);
        c.paint(Area.ellipse(14, 7.5, 3, 1), Color.withAlpha(Color.WHITE, 0.4));
    }

    private static void detonator(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(22, 9, 24, 3, 1.3), Color.scale(base, 0.6));
        c.paint(Area.disc(24, 3, 1.4), accent);
        Sprite.body(c, Area.roundRect(5, 9, 27, 25, 3), base, seed);
        c.paint(Area.disc(16, 18, 4.6), Color.scale(base, 0.3));
        c.paint(Area.line(16, 18, 19, 15, 1.2), accent);
        c.paint(Area.disc(9.5, 22, 1.3), Color.hex("#FF3030"));
        c.paint(Area.rect(20, 21, 25, 22), Color.scale(base, 0.4));
    }

    private static void culture(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.rect(13, 3, 19, 13).or(Area.disc(16, 20, 9)), base, seed);
        c.paint(Area.disc(16, 21, 7).and(Area.rect(0, 15, 32, 32)), Color.withAlpha(accent, 0.85));
        double[][] specks = {{13, 19}, {18, 23}, {16, 17}, {19, 20}};
        for (double[] p : specks) {
            c.paint(Area.disc(p[0], p[1], 0.9), Color.withAlpha(Color.WHITE, 0.7));
        }
        c.paint(Area.rect(12, 2, 20, 4), Color.scale(base, 0.5));
    }

    private static void canister(Canvas c, int base, int accent, long seed) {
        c.paint(Area.arc(16, 9, 4, 6.5, Math.PI, 2 * Math.PI), Color.scale(base, 0.7));
        Sprite.body(c, Area.roundRect(7, 9, 25, 27, 2.5), base, seed);
        c.paint(Area.rect(7, 14, 25, 21), accent);
        Icon.FLAME.draw(c, 16, 17.5, 3.6, Color.scale(base, 0.35), Color.WHITE);
        c.paint(Area.rect(12, 4, 20, 9), Color.scale(base, 0.6));
    }

    private static void anchorPlate(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(5, 5, 27, 27, 3), base, seed);
        double[][] rivets = {{8, 8}, {24, 8}, {8, 24}, {24, 24}};
        for (double[] p : rivets) {
            c.paint(Area.disc(p[0], p[1], 1.1), Color.scale(base, 0.55));
        }
        Icon.ANCHOR.draw(c, 16, 16, 8, Color.scale(base, 0.3), accent);
    }

    private static void pellets(Canvas c, int base, int accent, long seed) {
        double[][] centres = {{11, 12}, {21, 12}, {16, 21}};
        Area all = (x, y) -> false;
        for (double[] p : centres) {
            all = all.or(Area.disc(p[0], p[1], 6));
        }
        Sprite.body(c, all, base, seed);
        for (double[] p : centres) {
            c.paint(Area.ring(p[0], p[1], 3.2, 0.9), accent);
            c.paint(Area.disc(p[0], p[1], 1), accent);
        }
    }

    private static void pit(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 13.5), Color.withAlpha(accent, 0.18));
        Sprite.body(c, Area.disc(16, 16, 11), base, seed);
        c.paint(Area.orbit(16, 16, 11, 3.4, 0, 0.1), accent);
        c.paint(Area.ellipse(11, 11, 3, 2), Color.withAlpha(Color.WHITE, 0.5));
    }

    private static void tube(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(11, 4, 21, 28, 4.5), base, seed);
        c.paint(Area.roundRect(13, 7, 19, 25, 3), Color.withAlpha(accent, 0.7));
        c.paint(Area.rect(12.5, 6, 14, 27), Color.withAlpha(Color.WHITE, 0.4));
        c.paint(Area.rect(9, 1, 23, 4), Color.hex("#8A9099"));
        c.paint(Area.rect(9, 28, 23, 31), Color.hex("#8A9099"));
    }

    private static void initiator(Canvas c, int base, int accent, long seed) {
        Area spikes = Area.disc(16, 16, 9);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            spikes = spikes.or(Area.polygon(
                    16 + Math.cos(a - 0.22) * 8, 16 + Math.sin(a - 0.22) * 8,
                    16 + Math.cos(a) * 14.5, 16 + Math.sin(a) * 14.5,
                    16 + Math.cos(a + 0.22) * 8, 16 + Math.sin(a + 0.22) * 8));
        }
        Sprite.body(c, spikes, base, seed);
        c.paint(Area.disc(16, 16, 4), accent);
        c.paint(Area.disc(16, 16, 1.6), Color.WHITE);
    }

    private static void casing(Canvas c, int base, int accent, long seed) {
        Area body = Area.roundRect(4, 6, 28, 26, 3);
        Sprite.body(c, body, base, seed);
        c.shade(Area.rect(4, 12, 28, 20).and(body), (x, y, current) ->
                ((x + y) / 3) % 2 == 0 ? accent : Color.scale(accent, 0.35));
        double[][] bolts = {{7, 9}, {25, 9}, {7, 23}, {25, 23}};
        for (double[] p : bolts) {
            c.paint(Area.disc(p[0], p[1], 1.1), Color.scale(base, 0.45));
        }
    }

    private static void core(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 14.2, 2.6), Color.withAlpha(accent, 0.4));
        Sprite.body(c, Area.disc(16, 16, 12), base, seed);
        c.paint(Area.orbit(16, 16, 12, 4, 0.5, 0.09), accent);
        c.paint(Area.orbit(16, 16, 12, 4, -0.5, 0.09), Color.withAlpha(accent, 0.8));
        c.paint(Area.disc(16, 16, 3.2), Color.mix(accent, Color.WHITE, 0.6));
    }

    private static void fabric(Canvas c, int base, int accent, long seed) {
        Area sheet = Area.roundRect(5, 7, 27, 25, 4);
        Sprite.body(c, sheet, base, seed);
        c.shade(Area.rect(5, 7, 27, 25).and(sheet), (x, y, current) ->
                ((x + y) / 4) % 2 == 0 ? Color.mix(current, accent, 0.35) : current);
        c.paint(Area.rect(5, 21, 27, 25).and(sheet), Color.scale(base, 0.6));
    }

    private static void helmet(Canvas c, int base, int accent, long seed) {
        Area shell = Area.arc(16, 17, 11, 12, Math.PI, 2 * Math.PI).or(Area.rect(5, 17, 27, 22));
        Sprite.body(c, shell, base, seed);
        c.paint(Area.roundRect(9, 13, 23, 19, 2), Color.withAlpha(accent, 0.85));
        c.paint(Area.ellipse(12, 15.5, 3, 1), Color.withAlpha(Color.WHITE, 0.5));
        c.paint(Area.disc(16, 25, 1.5), Color.scale(base, 0.4));
    }

    private static void chestplate(Canvas c, int base, int accent, long seed) {
        Area torso = Area.roundRect(7, 6, 25, 28, 5);
        Sprite.body(c, torso, base, seed);
        c.paint(Area.rect(12, 4, 20, 8), Color.scale(base, 0.6));
        c.paint(Area.rect(7, 16, 25, 19).and(torso), accent);
        c.paint(Area.rect(10, 10, 15, 14), Color.scale(base, 0.7));
    }

    private static void leggings(Canvas c, int base, int accent, long seed) {
        Area legs = Area.rect(7, 4, 25, 12).or(Area.rect(7, 12, 15, 28)).or(Area.rect(17, 12, 25, 28));
        Sprite.body(c, legs, base, seed);
        c.paint(Area.rect(7, 6, 25, 9), accent);
        c.paint(Area.rect(7, 12, 15, 28).minus(Area.rect(8, 13, 14, 27)), Color.scale(base, 0.75));
        c.paint(Area.rect(17, 12, 25, 28).minus(Area.rect(18, 13, 24, 27)), Color.scale(base, 0.75));
    }

    private static void boots(Canvas c, int base, int accent, long seed) {
        Area boots = Area.roundRect(5, 8, 14, 28, 2).or(Area.roundRect(18, 8, 27, 28, 2));
        Sprite.body(c, boots, base, seed);
        c.paint(Area.rect(5, 24, 14, 28), accent);
        c.paint(Area.rect(18, 24, 27, 28), accent);
    }

    private static void counter(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(24, 10, 28, 3, 1.4), Color.scale(base, 0.6));
        c.paint(Area.disc(28, 3, 1.3), accent);
        Sprite.body(c, Area.roundRect(4, 10, 28, 27, 3), base, seed);
        c.paint(Area.roundRect(8, 13, 24, 19, 1.5), Color.hex("#1B2A22"));
        c.paint(Area.line(10, 17, 20, 14, 0.8), accent);
        c.paint(Area.disc(10, 23, 1.6), accent);
        c.paint(Area.disc(22, 23, 1.6), Color.scale(base, 0.5));
    }

    private static void densityScanner(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 7, 6), Color.hex("#8A94A0"));
        c.paint(Area.disc(16, 7, 4), Color.withAlpha(Color.hex("#BFEFFF"), 0.85));
        c.paint(Area.disc(14.5, 5.5, 1.1), Color.withAlpha(Color.WHITE, 0.9));
        Sprite.body(c, Area.roundRect(5, 11, 27, 28, 6), base, seed);
        c.paint(Area.roundRect(9, 14, 23, 25, 2), Color.hex("#05140B"));
        c.paint(Area.ring(16, 19.5, 4, 0.5), Color.withAlpha(accent, 0.5));
        c.paint(Area.line(16, 19.5, 19.2, 16.6, 0.7), accent);
        c.paint(Area.disc(18.6, 17.9, 0.7), accent);
        c.paint(Area.disc(22, 23, 1.2), Color.hex("#8A94A0"));
    }

    /** A photovoltaic array of blue cells in a silver frame, on a stand, with the sun in the top corner. */
    private static void solarPanel(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(14, 22, 18, 29), Color.scale(base, 0.6));
        c.paint(Area.rect(9, 27, 23, 30), Color.scale(base, 0.5));
        Icon.SUN.draw(c, 26, 6, 3.6, Color.hex("#FFD040"), Color.hex("#FFF2B0"));
        Sprite.body(c, Area.roundRect(3, 11, 25, 25, 1.5), base, seed);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                double x = 5 + 5 * col;
                double y = 13 + 4 * row;
                c.paint(Area.rect(x, y, x + 4, y + 3), accent);
                c.paint(Area.rect(x, y, x + 4, y + 1), Color.withAlpha(Color.WHITE, 0.25));
            }
        }
    }

    /** An erlenmeyer flask of dark oil with a gold cap and a bluish sheen. */
    /** A spool of copper wire between two steel flanges, with the loose end drawn out to a terminal. */
    private static void energyWire(Canvas c, int base, int accent, long seed) {
        int steel = Color.hex("#9AA0A8");
        Sprite.body(c, Area.rect(9, 8, 23, 24), base, seed);
        for (int y = 10; y <= 22; y += 3) {
            c.paint(Area.rect(9, y, 23, y + 1), Color.scale(base, 0.6));
        }
        Sprite.body(c, Area.roundRect(4, 4, 28, 9, 1.5), steel, seed + 1);
        Sprite.body(c, Area.roundRect(4, 23, 28, 28, 1.5), steel, seed + 2);
        c.paint(Area.line(23, 16, 27, 19, 1.2), accent);
        c.paint(Area.disc(27.5, 19.5, 1.4), accent);
    }

    // ---- Raw ore drops -------------------------------------------------------------------------------------------

    private static void rawUraninite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(6, 24, 4, 14, 10, 6, 21, 4, 28, 12, 26, 24, 16, 28), base, seed);
        c.paint(Area.disc(12, 14, 1.3), accent);
        c.paint(Area.disc(20, 11, 1.1), accent);
        c.paint(Area.line(15, 20, 22, 23, 0.9), accent);
    }

    private static void rawPitchblende(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(11, 14, 6).or(Area.disc(21, 13, 6)).or(Area.disc(16, 21, 6)), base, seed);
        c.paint(Area.disc(9, 11, 1.8), Color.withAlpha(accent, 0.7));
        c.paint(Area.disc(20, 10, 1.4), Color.withAlpha(accent, 0.6));
        c.paint(Area.disc(15, 19, 1.2), Color.withAlpha(accent, 0.6));
    }

    private static void rawMonazite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(16, 3, 26, 16, 16, 29, 6, 16), base, seed);
        c.paint(Area.polygon(16, 3, 16, 16, 6, 16), Color.withAlpha(Color.WHITE, 0.25));
        c.paint(Area.line(16, 16, 16, 29, 0.8), accent);
        c.paint(Area.line(16, 16, 26, 16, 0.8), accent);
    }

    private static void rawThorite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(8, 8, 24, 6, 28, 18, 22, 28, 8, 26, 4, 16), base, seed);
        c.paint(Area.line(10, 12, 16, 18, 1), accent);
        c.paint(Area.line(16, 18, 24, 14, 1), accent);
        c.paint(Area.line(16, 18, 20, 26, 1), accent);
    }

    private static void rawGalena(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.rect(4, 12, 15, 25).or(Area.rect(14, 15, 27, 27)).or(Area.rect(11, 4, 21, 13)), base, seed);
        c.paint(Area.rect(6, 14, 9, 15), accent);
        c.paint(Area.rect(16, 17, 19, 18), accent);
        c.paint(Area.rect(13, 6, 16, 7), accent);
    }

    private static void rawCinnabar(Canvas c, int base, int accent, long seed) {
        Area shards = Area.polygon(5, 28, 9, 9, 14, 28).or(Area.polygon(13, 28, 18, 4, 23, 28))
                .or(Area.polygon(19, 28, 26, 13, 28, 28));
        Sprite.body(c, shards, base, seed);
        c.paint(Area.line(9, 12, 9, 22, 1), accent);
        c.paint(Area.line(18, 8, 18, 18, 1), accent);
        c.paint(Area.line(26, 17, 26, 24, 1), accent);
    }

    private static void rawFluorite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(9, 5, 15, 15, 9, 27, 3, 15), base, seed);
        Sprite.body(c, Area.polygon(23, 9, 28, 17, 23, 27, 18, 17), accent, seed + 1);
        c.paint(Area.line(9, 9, 9, 22, 0.8), Color.withAlpha(Color.WHITE, 0.4));
        c.paint(Area.line(23, 13, 23, 22, 0.8), Color.withAlpha(Color.WHITE, 0.4));
    }

    // ---- Crushed ore and products --------------------------------------------------------------------------------

    /** Powder heaped in a steel dish. The base colour is the powder. */
    private static void crushed(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(16, 17, 13).and(Area.rect(0, 17, 32, 32)), Color.hex("#6A707A"), seed + 7);
        Sprite.body(c, Area.ellipse(16, 16, 12, 6), base, seed);
        double[][] specks = {{11, 14}, {17, 12}, {21, 16}, {14, 18}};
        for (double[] p : specks) {
            c.paint(Area.disc(p[0], p[1], 0.9), accent);
        }
    }

    private static void calcine(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(6, 14, 26, 14, 23, 28, 9, 28), Color.hex("#9A5A3A"), seed);
        Sprite.body(c, Area.ellipse(16, 15, 9, 4), base, seed + 1);
        c.paint(Area.disc(12, 15, 1), accent);
        c.paint(Area.disc(19, 14, 1), accent);
    }

    private static void slag(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.ellipse(16, 17, 12, 8), base, seed);
        c.paint(Area.ellipse(12, 13, 4, 1.5), Color.withAlpha(accent, 0.8));
        c.paint(Area.disc(21, 20, 1), accent);
    }

    private static void ingot(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(4, 24, 8, 12, 24, 12, 28, 24), base, seed);
        c.paint(Area.polygon(8, 12, 24, 12, 28, 8, 12, 8), Color.mix(base, Color.WHITE, 0.4));
        c.paint(Area.line(10, 18, 22, 18, 0.9), accent);
    }

    private static void flask(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(16, 21, 10).or(Area.rect(13, 4, 19, 13)), base, seed);
        c.paint(Area.disc(16, 22, 6), Color.withAlpha(accent, 0.9));
        c.paint(Area.disc(13, 19, 1.2), Color.withAlpha(Color.WHITE, 0.7));
    }

    private static void sack(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.ellipse(16, 19, 10, 10).or(Area.rect(12, 7, 20, 11)), base, seed);
        c.paint(Area.rect(12, 9, 20, 10), accent);
        c.paint(Area.disc(16, 21, 3), Color.withAlpha(accent, 0.6));
    }

    private static void heap(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 27, 32, 30), Color.scale(base, 0.5));
        Sprite.body(c, Area.polygon(2, 27, 13, 12, 19, 12, 30, 27), base, seed);
        c.paint(Area.disc(12, 20, 1), accent);
        c.paint(Area.disc(19, 23, 1), accent);
        c.paint(Area.disc(9, 24, 0.9), accent);
    }

    private static void jar(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(8, 9, 24, 28, 4), base, seed);
        c.paint(Area.roundRect(10, 15, 22, 26, 2), Color.withAlpha(accent, 0.8));
        c.paint(Area.rect(7, 5, 25, 9), Color.hex("#8A9099"));
    }

    private static void slabs(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.rect(5, 21, 27, 27).or(Area.rect(7, 14, 25, 20)).or(Area.rect(9, 7, 23, 13)), base, seed);
        c.paint(Area.rect(7, 20, 25, 21), Color.scale(base, 0.5));
        c.paint(Area.rect(9, 9, 23, 10), accent);
        c.paint(Area.rect(7, 16, 25, 17), accent);
    }

    private static void bucket(Canvas c, int base, int accent, long seed) {
        c.paint(Area.arc(16, 12, 7, 9, Math.PI, 2 * Math.PI), Color.scale(base, 0.7));
        Sprite.body(c, Area.polygon(7, 9, 25, 9, 22, 28, 10, 28), base, seed);
        c.paint(Area.rect(7, 9, 25, 12), accent);
        c.paint(Area.rect(9, 17, 23, 18), Color.withAlpha(accent, 0.5));
    }

    private static void cullet(Canvas c, int base, int accent, long seed) {
        Area pebbles = Area.ellipse(10, 20, 6, 5).or(Area.ellipse(21, 19, 6, 5)).or(Area.ellipse(16, 25, 7, 4));
        Sprite.body(c, pebbles, base, seed);
        c.paint(Area.disc(8, 18, 1.4), Color.withAlpha(accent, 0.9));
        c.paint(Area.disc(19, 17, 1.2), Color.withAlpha(accent, 0.9));
        c.paint(Area.disc(15, 24, 1), Color.withAlpha(accent, 0.9));
    }

    private static void gasCylinder(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(13, 2, 19, 6), Color.hex("#8A9099"));
        Sprite.body(c, Area.roundRect(9, 5, 23, 29, 5), base, seed);
        c.paint(Area.rect(9, 13, 23, 17), accent);
        c.paint(Area.rect(9, 24, 23, 26), Color.scale(base, 0.5));
    }

    private static void coins(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.ellipse(16, 22, 10, 4.5).or(Area.rect(6, 12, 26, 22)).or(Area.ellipse(16, 12, 10, 4.5)),
                base, seed);
        c.paint(Area.rect(6, 16, 26, 17), accent);
        c.paint(Area.rect(6, 20, 26, 21), accent);
    }

    private static void cell(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(3, 11, 29, 21, 5), base, seed);
        c.paint(Area.rect(23, 11, 27, 21).and(Area.roundRect(3, 11, 29, 21, 5)), accent);
        c.paint(Area.line(7, 14, 19, 14, 1), Color.withAlpha(Color.WHITE, 0.6));
    }

    private static void ibeam(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(5, 7, 27, 7, 27, 11, 18, 11, 18, 21, 27, 21, 27, 25, 5, 25, 5, 21, 14, 21, 14, 11, 5, 11),
                base, seed);
        c.paint(Area.rect(7, 8, 25, 9), accent);
    }

    private static void sheet(Canvas c, int base, int accent, long seed) {
        Area plate = Area.roundRect(5, 5, 27, 27, 3);
        Sprite.body(c, plate, base, seed);
        for (int x0 = 2; x0 <= 26; x0 += 6) {
            c.paint(Area.line(x0, 4, x0 + 18, 22, 0.7).and(plate), accent);
        }
    }

    private static void bottle(Canvas c, int base, int accent, long seed) {
        c.paint(Area.roundRect(12, 3, 20, 7, 1.5), Color.hex("#8A9099"));
        Sprite.body(c, Area.roundRect(8, 9, 24, 28, 4).or(Area.rect(13, 6, 19, 10)), base, seed);
        c.paint(Area.rect(10, 16, 22, 26), Color.withAlpha(accent, 0.85));
    }

    private static void tin(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 12, 28, 27, 4), base, seed);
        c.paint(Area.ellipse(16, 12, 12, 3), Color.scale(base, 1.3));
        c.paint(Area.rect(4, 17, 28, 19), accent);
        c.paint(Area.disc(9, 23, 1), Color.withAlpha(Color.WHITE, 0.6));
    }

    private static void drum(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(7, 4, 25, 28, 5), base, seed);
        c.paint(Area.rect(7, 8, 25, 9), Color.scale(base, 0.6));
        c.paint(Area.rect(7, 23, 25, 24), Color.scale(base, 0.6));
        Icon.RADIATION.draw(c, 16, 16, 5, accent, accent);
    }

    private static void fluxCell(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(28, 16, 22, 26.4, 10, 26.4, 4, 16, 10, 5.6, 22, 5.6), base, seed);
        c.paint(Area.ring(16, 16, 6, 1.6), accent);
        c.paint(Area.disc(16, 16, 3), Color.withAlpha(Color.WHITE, 0.9));
    }

    private static void solenoid(Canvas c, int base, int accent, long seed) {
        Area coil = Area.roundRect(3, 10, 29, 22, 4);
        Sprite.body(c, coil, base, seed);
        for (int x = 9; x <= 23; x += 4) {
            c.paint(Area.rect(x, 10, x + 1, 22).and(coil), Color.scale(base, 0.55));
        }
        c.paint(Area.rect(3, 10, 7, 22).and(coil), accent);
        c.paint(Area.rect(25, 10, 29, 22).and(coil), accent);
    }

    private static void capsule(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(9, 23, 23, 9, 14), Color.withAlpha(accent, 0.2));
        Sprite.body(c, Area.line(9, 23, 23, 9, 10), base, seed);
        c.paint(Area.line(12, 20, 20, 12, 1.2), accent);
        c.paint(Area.disc(12, 14, 1.2), Color.withAlpha(Color.WHITE, 0.8));
    }

    private static void droplet(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(16, 20, 9).or(Area.polygon(8, 18, 16, 3, 24, 18)), base, seed);
        c.paint(Area.disc(16, 21, 4), Color.withAlpha(accent, 0.55));
        c.paint(Area.disc(12, 16, 1.4), Color.withAlpha(Color.WHITE, 0.8));
    }

    private static void slug(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(9, 29, 9, 14, 16, 3, 23, 14, 23, 29), base, seed);
        c.paint(Area.rect(9, 20, 23, 22), accent);
        c.paint(Area.disc(13, 12, 1.1), Color.withAlpha(Color.WHITE, 0.6));
    }

    private static void lattice(Canvas c, int base, int accent, long seed) {
        double[][] nodes = {{5, 11}, {19, 11}, {19, 25}, {5, 25}, {11, 5}, {25, 5}, {25, 19}, {11, 19}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            double[] a = nodes[e[0]];
            double[] b = nodes[e[1]];
            c.paint(Area.line(a[0], a[1], b[0], b[1], 2.6), base);
        }
        for (int[] e : edges) {
            double[] a = nodes[e[0]];
            double[] b = nodes[e[1]];
            c.paint(Area.line(a[0], a[1], b[0], b[1], 1), accent);
        }
        for (double[] n : nodes) {
            c.paint(Area.disc(n[0], n[1], 1.3), Color.withAlpha(Color.WHITE, 0.9));
        }
    }

    // ---- Research packs ------------------------------------------------------------------------------------------

    /** A flat card: a title strip and text lines, or an icon when one is given. */
    private static Painter card(Icon icon) {
        return (c, base, accent, seed) -> {
            Sprite.body(c, Area.roundRect(5, 4, 27, 28, 2), base, seed);
            c.paint(Area.rect(7, 6, 25, 9), accent);
            if (icon == null) {
                c.paint(Area.rect(8, 13, 24, 14), accent);
                c.paint(Area.rect(8, 17, 24, 18), accent);
                c.paint(Area.rect(8, 21, 19, 22), accent);
            } else {
                icon.draw(c, 16, 18, 6, Color.scale(base, 0.3), accent);
            }
        };
    }

    /** A cartridge: a notched body with a pale label and an icon. */
    private static Painter cartridge(Icon icon) {
        return (c, base, accent, seed) -> {
            Sprite.body(c, Area.polygon(8, 3, 24, 3, 27, 7, 27, 29, 5, 29, 5, 7), base, seed);
            c.paint(Area.rect(8, 6, 24, 9), Color.scale(base, 0.5));
            c.paint(Area.rect(8, 12, 24, 23), Color.mix(base, Color.WHITE, 0.6));
            icon.draw(c, 16, 17, 5, Color.scale(base, 0.35), accent);
        };
    }

    // ---- Machines (inventory sprites) ----------------------------------------------------------------------------

    /** A lit housing with a coloured top panel and a pictogram. The 3D model is in {@link MachineArt}. */
    private static Painter machine(Icon icon) {
        return (c, base, accent, seed) -> {
            Sprite.body(c, Area.roundRect(4, 5, 28, 28, 4), base, seed);
            c.paint(Area.rect(7, 8, 25, 10), accent);
            c.paint(Area.rect(7, 22, 25, 25), Color.scale(base, 0.5));
            icon.draw(c, 16, 16, 6, Color.hex("#F4F5F7"), accent);
        };
    }

    /**
     * A drill's inventory sprite: the machine sprite with five lamps along its base, the first {@code level} of them lit.
     * The 3D model of the drill is in {@link MachineArt}.
     */
    static Painter drill(Icon icon, int level) {
        Painter housing = machine(icon);
        return (c, base, accent, seed) -> {
            housing.paint(c, base, accent, seed);
            for (int k = 0; k < 5; k++) {
                c.paint(Area.disc(9.5 + 3.5 * k, 23.5, 1.1), k < level ? Color.hex("#FFD040") : Color.scale(base, 0.35));
            }
        };
    }

    // ---- Power machines (inventory sprites) ----------------------------------------------------------------------

    /**
     * A lattice tower seen from the front: two steel legs, cross-bars, and an accent cap. Higher levels are taller, and
     * the third has three accent cross-arms. The fourth is wider, with four arms; the fifth has four arms and a crown.
     * The 3D model is in {@link MachineArt}.
     */
    static Painter pylon(int level) {
        return (c, base, accent, seed) -> {
            double top = switch (level) {
                case 1 -> 9;
                case 2 -> 5;
                case 3 -> 3;
                case 4 -> 2;
                default -> 6;
            };
            double half = level >= 4 ? 11 : 8;
            c.paint(Area.rect(5, 27, 27, 29), Color.scale(base, 0.6));
            c.paint(Area.line(16, top, 16 - half, 28, 1.5).or(Area.line(16, top, 16 + half, 28, 1.5)), base);
            for (double y = top + 5; y < 26; y += 5) {
                double reach = half * (y - top) / (28 - top);
                c.paint(Area.line(16 - reach, y, 16 + reach, y, 1.1), Color.scale(base, 0.75));
            }
            if (level >= 3) {
                int arms = level == 3 ? 3 : 4;
                for (int k = 0; k < arms; k++) {
                    double y = top + 4 + 5 * k;
                    double reach = half * (y - top) / (28 - top) + 1.5;
                    c.paint(Area.line(16 - reach, y, 16 + reach, y, 1.3), accent);
                    c.paint(Area.disc(16 - reach, y, 1.3), accent);
                    c.paint(Area.disc(16 + reach, y, 1.3), accent);
                }
            }
            if (level == 5) {
                c.paint(Area.line(11, top - 2, 21, top - 2, 1.2), accent);
                c.paint(Area.disc(11, top - 2, 1.6), accent);
                c.paint(Area.disc(16, top - 4, 1.8), accent);
                c.paint(Area.disc(21, top - 2, 1.6), accent);
            }
            c.paint(Area.disc(16, top, 2.2), accent);
        };
    }

    /**
     * A steel cell with a cap and a level meter: three bars on the front, the first {@code level} of them lit. The fourth
     * and fifth are wider cells, with five bars and coils round the sides.
     */
    static Painter accumulator(int level) {
        return (c, base, accent, seed) -> {
            boolean large = level >= 4;
            double edge = large ? 6 : 8;
            Area body = Area.rect(edge, 7, 32 - edge, 26).or(Area.ellipse(16, 7, 16 - edge, 3))
                    .or(Area.ellipse(16, 26, 16 - edge, 3));
            Sprite.body(c, body, base, seed);
            c.paint(Area.ellipse(16, 7, 16 - edge, 3), accent);
            if (large) {
                for (double y : new double[]{11, 16, 21}) {
                    c.paint(Area.rect(edge, y, 32 - edge, y + 1), Color.scale(base, 0.7));
                }
            }
            c.paint(Area.rect(10, 9, 22, 25), Color.hex("#0E1A14"));
            int bars = large ? 5 : 3;
            for (int k = 0; k < bars; k++) {
                double y = large ? 23 - 3 * k : 21 - 5 * k;
                c.paint(Area.rect(12, y, 20, y + (large ? 1 : 3)), k < level ? Color.hex("#FFD040") : Color.scale(base, 0.4));
            }
        };
    }

    /** A turbine: a mast, a hub and three blades, with a blue tip on each blade. */
    private static void wind(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(11, 29, 21, 31), Color.scale(base, 0.5));
        c.paint(Area.line(16, 12, 16, 29, 2.2), Color.scale(base, 0.7));
        for (int k = 0; k < 3; k++) {
            double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
            double tipX = 16 + Math.cos(a) * 11;
            double tipY = 12 + Math.sin(a) * 11;
            c.paint(Area.line(16, 12, tipX, tipY, 2.6), base);
            c.paint(Area.disc(tipX, tipY, 1.4), accent);
        }
        c.paint(Area.disc(16, 12, 2.6), Color.scale(base, 0.8));
        c.paint(Area.disc(16, 12, 1.2), accent);
    }

    /** A dark amber canister with a handle, a spout and a drop on the label. */
    /** A glowing core in a grey sleeve, with spacer bands and a halo of light. */
    private static void fuelRod(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ellipse(16, 16, 8, 15), Color.withAlpha(accent, 0.25));
        Sprite.body(c, Area.roundRect(11, 3, 21, 29, 3), base, seed);
        c.paint(Area.rect(14, 6, 18, 26), accent);
        c.paint(Area.rect(14, 6, 15.5, 26), Color.withAlpha(Color.WHITE, 0.5));
        for (int y = 9; y <= 23; y += 7) {
            c.paint(Area.rect(11, y, 21, y + 1), Color.scale(base, 0.55));
        }
    }

    /** A furnace-like box with a chimney that smokes, and a fire door glowing at its front. */
    private static void charcoalGenerator(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(20, 10, 25, 14), Color.scale(base, 0.6));
        c.paint(Area.rect(20, 3, 25, 11), Color.scale(base, 0.8));
        c.paint(Area.disc(22, 2, 2.2), Color.withAlpha(Color.hex("#D0D0D0"), 0.55));
        c.paint(Area.disc(26, 0.5, 1.6), Color.withAlpha(Color.hex("#D0D0D0"), 0.4));
        Sprite.body(c, Area.roundRect(4, 11, 28, 29, 3), base, seed);
        c.paint(Area.roundRect(9, 16, 23, 25, 2), Color.hex("#140804"));
        c.paint(Area.roundRect(11, 19, 21, 24, 1.5), Color.withAlpha(accent, 0.9));
        c.paint(Area.rect(11, 21, 21, 21.8), Color.scale(base, 0.3));
    }

    /** A pump-jack: a tapered pillar, a walking beam with its head, a counterweight and a frame. */
    private static void petroleumGenerator(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(3, 26, 29, 29), Color.scale(base, 0.6));
        Sprite.body(c, Area.polygon(12, 26, 15, 11, 17, 11, 20, 26), base, seed);
        Sprite.body(c, Area.rect(5, 12, 27, 14), base, seed + 1);
        c.paint(Area.rect(24, 14, 27, 21), accent);
        c.paint(Area.disc(7, 17, 3), Color.scale(base, 0.5));
        c.paint(Area.disc(16, 22, 2.2), accent);
    }

    /** An engine with a fuel tank on the right side and a wide exhaust stack. */
    private static void dieselGenerator(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(7, 1, 12, 12), Color.scale(base, 0.5));
        c.paint(Area.rect(6, 1, 13, 3), Color.hex("#2A2A2A"));
        Sprite.body(c, Area.roundRect(3, 11, 21, 28, 2.5), base, seed);
        for (int y = 15; y <= 25; y += 3) {
            c.paint(Area.rect(6, y, 17, y + 1), Color.scale(base, 0.45));
        }
        Sprite.body(c, Area.roundRect(19, 15, 30, 28, 4), base, seed + 1);
        c.paint(Area.rect(20, 20, 29, 23), accent);
    }

    /** A reactor: a steel cylinder with a blue Cherenkov window and a cooling fin on each side. */
    private static void nuclearGenerator(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(1, 6, 3, 26), Color.scale(base, 0.7));
        c.paint(Area.rect(29, 6, 31, 26), Color.scale(base, 0.7));
        Area body = Area.rect(7, 5, 25, 27).or(Area.ellipse(16, 5, 9, 3)).or(Area.ellipse(16, 27, 9, 3));
        Sprite.body(c, body, base, seed);
        c.paint(Area.roundRect(11, 11, 21, 22, 2), accent);
        c.paint(Area.roundRect(13, 13, 15, 20, 1), Color.withAlpha(Color.WHITE, 0.45));
        c.paint(Area.rect(7, 24, 25, 25), Color.scale(base, 0.5));
    }

    /** A column with two condenser tanks on either side, linked by a pipe. */
    private static void refinery(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(12, 2, 20, 29, 3), base, seed);
        Sprite.body(c, Area.roundRect(3, 14, 12, 29, 4), base, seed + 1);
        Sprite.body(c, Area.roundRect(20, 14, 29, 29, 4), base, seed + 2);
        c.paint(Area.line(8, 13, 24, 13, 1.5), accent);
        c.paint(Area.rect(12, 10, 20, 12), accent);
        c.paint(Area.rect(12, 21, 20, 23), accent);
        c.paint(Area.rect(15, 6, 17, 19), Color.withAlpha(Color.WHITE, 0.35));
    }

    // ---- Logistics (inventory sprites) ---------------------------------------------------------------------------

    /** A conveyor: a long steel band on two rollers, with two arrows in the level colour running toward the right. */
    private static void belt(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(2, 10, 30, 22, 5), base, seed);
        c.paint(Area.disc(7, 16, 4.2), Color.scale(base, 0.5));
        c.paint(Area.disc(25, 16, 4.2), Color.scale(base, 0.5));
        c.paint(Area.disc(7, 16, 2.2), Color.hex("#A8B0BA"));
        c.paint(Area.disc(25, 16, 2.2), Color.hex("#A8B0BA"));
        for (double x : new double[]{12, 18}) {
            c.paint(Area.line(x - 2.5, 12.5, x + 1, 16, 1.8).or(Area.line(x - 2.5, 19.5, x + 1, 16, 1.8)), accent);
        }
    }

    /** A cube housing in the level colour, a claw arm reaching up and out of its top, and a lens on its front. */
    private static void picker(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(16, 14, 24, 6, 2.6), accent);
        c.paint(Area.line(24, 6, 29, 3, 1.6).or(Area.line(24, 6, 29, 10, 1.6)), accent);
        Sprite.body(c, Area.roundRect(5, 13, 27, 28, 3), base, seed);
        c.paint(Area.disc(16, 20, 3.4), accent);
        c.paint(Area.disc(14.8, 18.8, 1.1), Color.withAlpha(Color.WHITE, 0.8));
        c.paint(Area.rect(7, 25, 25, 26), Color.scale(base, 0.5));
    }

    /** A fork: a wide housing with three outlets on top, one straight up and one to each side, fed from a pipe below. */
    private static void splitter(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(14, 22, 18, 30), Color.scale(base, 0.6));
        Sprite.body(c, Area.roundRect(3, 14, 29, 24, 3), base, seed);
        Sprite.body(c, Area.rect(13, 3, 19, 15), base, seed + 1);
        Sprite.body(c, Area.rect(3, 7, 8, 15), base, seed + 2);
        Sprite.body(c, Area.rect(24, 7, 29, 15), base, seed + 3);
        c.paint(Area.rect(13, 3, 19, 5), accent);
        c.paint(Area.rect(3, 7, 8, 9), accent);
        c.paint(Area.rect(24, 7, 29, 9), accent);
        c.paint(Area.rect(6, 18, 26, 20), accent);
    }

    // ---- Process products and turret parts -----------------------------------------------------------------------

    /** Crude saltpeter as it comes out of the rock: a grainy white crust with crystal glints. */
    private static void rawSaltpeter(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(5, 25, 6, 14, 12, 8, 20, 9, 27, 15, 27, 25, 19, 28, 9, 28), base, seed);
        glint(c, 11, 15, 2.6, accent);
        glint(c, 20, 13, 2.2, accent);
        glint(c, 21, 21, 2.6, accent);
        glint(c, 11, 22, 2.0, accent);
    }

    /** A small crystal diamond with a pale highlight on its upper left. */
    private static void glint(Canvas c, double cx, double cy, double r, int colour) {
        c.paint(Area.polygon(cx, cy - r, cx + 0.6 * r, cy, cx, cy + r, cx - 0.6 * r, cy), colour);
        c.paint(Area.disc(cx - 0.2 * r, cy - 0.3 * r, 0.3 * r), Color.withAlpha(Color.WHITE, 0.8));
    }

    /** Native sulfur: yellow lumps heaped together, each with a pale sheen. */
    private static void lumps(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.ellipse(10, 18, 6, 5).or(Area.ellipse(21, 16, 6, 5)).or(Area.ellipse(16, 24, 8, 4.5)),
                base, seed);
        c.paint(Area.ellipse(8, 15, 2.2, 1.2), accent);
        c.paint(Area.ellipse(19, 13, 2.2, 1.2), accent);
        c.paint(Area.ellipse(14, 22, 2.4, 1.2), accent);
    }

    /** Sphalerite as it comes out of the rock: a dark, waxy chunk speckled with cyan. */
    private static void rawSphalerite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(4, 22, 7, 10, 16, 5, 26, 9, 28, 21, 22, 28, 9, 28), base, seed);
        c.paint(Area.line(9, 11, 15, 7, 0.8), Color.withAlpha(Color.WHITE, 0.35));
        double[][] specks = {{10, 15}, {18, 11}, {23, 18}, {14, 21}, {20, 25}, {8, 23}};
        for (double[] p : specks) {
            c.paint(Area.disc(p[0], p[1], 1.2), accent);
        }
    }

    /** A cluster of white crystal prisms standing up from a crust, with pale facet lines. */
    private static void saltCrystals(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(3, 26, 29, 29), Color.scale(base, 0.8));
        Area prisms = prism(9, 10, 3.5, 27).or(prism(17, 5, 4.5, 27)).or(prism(24, 12, 3.5, 27))
                .or(prism(13, 18, 2.5, 27));
        Sprite.body(c, prisms, base, seed);
        c.paint(Area.line(17, 7, 17, 25, 0.7), accent);
        c.paint(Area.line(9, 12, 9, 25, 0.6), accent);
        c.paint(Area.line(24, 14, 24, 25, 0.6), accent);
    }

    /** A crystal prism on a flat bottom: its top tapers to a point, its sides are straight. */
    private static Area prism(double cx, double top, double w, double bottom) {
        double shoulder = top + 0.3 * (bottom - top);
        return Area.polygon(cx, top, cx + w, shoulder, cx + w, bottom, cx - w, bottom, cx - w, shoulder);
    }

    /** A steel tin with a black window of powder, the grains glinting in the accent colour. */
    private static void granules(Canvas c, int base, int accent, long seed) {
        int steel = Color.hex("#8A9099");
        Sprite.body(c, Area.roundRect(5, 7, 27, 28, 3), steel, seed);
        c.paint(Area.rect(5, 7, 27, 11), Color.scale(steel, 0.6));
        c.paint(Area.rect(8, 13, 24, 24), base);
        scatter(c, 40, 9, 14, 23, 23, 0.8, accent, seed);
    }

    /** Dots of one colour at seeded positions inside a box. */
    private static void scatter(Canvas c, int count, double x0, double y0, double x1, double y1, double r, int colour,
                                long seed) {
        for (int i = 0; i < count; i++) {
            double x = x0 + Noise.hash(i, 0, seed) * (x1 - x0);
            double y = y0 + Noise.hash(i, 1, seed) * (y1 - y0);
            c.paint(Area.disc(x, y, r), colour);
        }
    }

    /** An empty brass case, open at the mouth, with a band and a primer at the base. */
    private static void shell(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.polygon(9, 29, 10, 12, 13, 6, 19, 6, 22, 12, 23, 29), base, seed);
        c.paint(Area.ellipse(16, 6.5, 3.4, 1.4), Color.hex("#2A1A08"));
        c.paint(Area.rect(9, 22, 23, 24), accent);
        c.paint(Area.disc(16, 27, 2.2), Color.scale(accent, 0.8));
    }

    /** A bullet: a slim brass case with a grooved neck, and the lead tip in the kind's colour. */
    private static void bullet(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(11, 12, 21, 28, 1.5), base, seed);
        c.paint(Area.rect(11, 20, 21, 21), Color.scale(base, 0.6));
        Sprite.body(c, Area.polygon(11, 13, 13, 6, 16, 2, 19, 6, 21, 13), accent, seed + 1);
    }

    /** A loaded round: a full cartridge with a crimped mouth, a rim at the base and the tip in the kind's colour. */
    private static void round(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(8, 12, 24, 27, 2), base, seed);
        c.paint(Area.rect(8, 12, 24, 14), Color.scale(base, 0.7));
        c.paint(Area.rect(7, 26, 25, 29), Color.scale(base, 0.5));
        Sprite.body(c, Area.polygon(10, 13, 12, 7, 16, 3, 20, 7, 22, 13), accent, seed + 1);
    }

    /** Three brass casings stacked one above another, with grooves between them and a steel strap binding them. */
    private static void shellCasingStock(Canvas c, int base, int accent, long seed) {
        Area casings = Area.roundRect(4, 5, 28, 11, 3).or(Area.roundRect(4, 12, 28, 18, 3))
                .or(Area.roundRect(4, 19, 28, 25, 3));
        Sprite.body(c, casings, base, seed);
        c.paint(Area.rect(5, 11, 27, 12), Color.scale(base, 0.45));
        c.paint(Area.rect(5, 18, 27, 19), Color.scale(base, 0.45));
        for (double y : new double[]{8, 15, 22}) {
            c.paint(Area.ellipse(6, y, 1.2, 2.6), accent);
        }
        c.paint(Area.rect(13, 4, 16, 26), Color.hex("#7A7F88"));
    }

    /** Three grey cloth bags of propellant, two above and one below, tied with a dark cord, and grains in the bags. */
    private static void shellPowderStock(Canvas c, int base, int accent, long seed) {
        Area bags = Area.ellipse(10, 12, 6, 5).or(Area.ellipse(22, 12, 6, 5)).or(Area.ellipse(16, 21, 9, 6));
        Sprite.body(c, bags, base, seed);
        c.paint(Area.line(7, 9, 25, 9, 1.2).and(bags), accent);
        c.paint(Area.ellipse(16, 9, 1.6, 1.4), accent);
        scatter(c, 6, 11, 18, 21, 24, 0.8, Color.scale(base, 0.45), seed);
    }

    /** A dark crystal in a steel ring, with a violet core that glows through it and a glint on its face. */
    private static void antimatterContainment(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 12.5, 2), Color.hex("#7A8090"));
        c.paint(Area.disc(16, 16, 9), Color.withAlpha(accent, 0.25));
        Sprite.body(c, Area.polygon(16, 7, 23, 16, 16, 25, 9, 16), base, seed);
        c.paint(Area.disc(16, 16, 3.2), Color.withAlpha(accent, 0.9));
        c.paint(Area.disc(14.5, 13.5, 1), Color.withAlpha(Color.WHITE, 0.7));
    }

    /** A precision barrel: a long steel tube with a collar at the breech and a dark bore at the muzzle. */
    private static void barrel(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(2, 13, 29, 19, 1.5), base, seed);
        Sprite.body(c, Area.roundRect(4, 11, 10, 21, 1.5), Color.scale(base, 0.8), seed + 1);
        c.paint(Area.rect(4, 15, 29, 16), accent);
        c.paint(Area.ellipse(29, 16, 1.4, 3), Color.hex("#0E1012"));
        c.paint(Area.ellipse(29, 16, 0.7, 1.4), Color.hex("#3A4048"));
    }

    /** A structural frame: a square steel plate with a window, two braces across it and rivets at the corners. */
    private static void frame(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 4, 28, 28, 2).minus(Area.rect(8, 8, 24, 24)), base, seed);
        c.paint(Area.line(8, 8, 24, 24, 1.4), accent);
        c.paint(Area.line(24, 8, 8, 24, 1.4), accent);
        double[][] rivets = {{6, 6}, {26, 6}, {6, 26}, {26, 26}};
        for (double[] p : rivets) {
            c.paint(Area.disc(p[0], p[1], 1.2), Color.scale(base, 0.5));
        }
    }

    /** A targeting unit: a dark control box with a green board, a crosshair sensor and an antenna. */
    private static void targeting(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(10, 9, 10, 3, 1.2), Color.scale(base, 0.6));
        c.paint(Area.disc(10, 3, 1.4), accent);
        Sprite.body(c, Area.roundRect(4, 9, 28, 28, 3), base, seed);
        c.paint(Area.roundRect(7, 12, 25, 25, 1.5), Color.hex("#0F2A22"));
        c.paint(Area.ring(16, 18, 4.5, 0.8), accent);
        c.paint(Area.line(16, 12.5, 16, 23.5, 0.6), accent);
        c.paint(Area.line(10.5, 18, 21.5, 18, 0.6), accent);
        c.paint(Area.disc(16, 18, 1.4), Color.withAlpha(Color.WHITE, 0.9));
    }

    /** A gear wheel centred on ({@code cx}, {@code cy}): a disc with evenly spaced teeth, a bore and an accent ring. */
    private static void gear(Canvas c, double cx, double cy, int base, int accent, long seed, int teeth, double rim,
                             double bore) {
        Area wheel = Area.disc(cx, cy, rim);
        for (int k = 0; k < teeth; k++) {
            double a = 2 * Math.PI * k / teeth;
            wheel = wheel.or(Area.disc(cx + Math.cos(a) * (rim + 0.5), cy + Math.sin(a) * (rim + 0.5), 1.8));
        }
        Sprite.body(c, wheel, base, seed);
        c.paint(Area.disc(cx, cy, bore), Color.scale(base, 0.3));
        c.paint(Area.ring(cx, cy, bore + 1.2, 0.7), accent);
    }

    /** A turret mechanism: a large gear with a smaller one meshed into it. */
    private static void mechanism(Canvas c, int base, int accent, long seed) {
        gear(c, 13, 13, base, accent, seed, 9, 8.5, 3.2);
        gear(c, 24, 24, Color.scale(base, 1.15), accent, seed + 1, 7, 5, 2);
    }

    /** A small iron gear: a single wheel of eight teeth. */
    private static void smallGear(Canvas c, int base, int accent, long seed) {
        gear(c, 16, 16, base, accent, seed, 8, 8.5, 3.2);
    }

    /** An ammunition feed: a chain of links, every other one in the accent colour. */
    private static void feed(Canvas c, int base, int accent, long seed) {
        for (int k = 0; k < 6; k++) {
            double cx = 4 + 5 * k;
            Area link = Area.ellipse(cx, 16, 2.6, 6).minus(Area.ellipse(cx, 16, 1.1, 3.4));
            Sprite.body(c, link, k % 2 == 0 ? base : accent, seed + k);
        }
    }

    /** A machine gun turret: a round mount on a steel base, a barrel out to the right and an ammunition box. */
    private static void turret(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 24, 28, 29, 2), Color.scale(base, 0.6), seed + 1);
        Sprite.body(c, Area.rect(7, 17, 14, 24), Color.scale(base, 0.8), seed + 2);
        Sprite.body(c, Area.rect(14, 12, 29, 17), base, seed + 3);
        c.paint(Area.rect(18, 12, 22, 13), accent);
        c.paint(Area.rect(27, 13, 30, 16), Color.hex("#101010"));
        Sprite.body(c, Area.disc(14, 15, 6.5), base, seed);
        c.paint(Area.disc(14, 15, 2.2), accent);
    }

    /** A plate of sheet iron: a flat rounded slab with a bright edge and four rivet holes. */
    private static void plate(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 9, 28, 23, 2.5), base, seed);
        double[][] holes = {{8, 13}, {24, 13}, {8, 19}, {24, 19}};
        for (double[] p : holes) {
            c.paint(Area.disc(p[0], p[1], 1.2), Color.scale(base, 0.4));
        }
        c.paint(Area.rect(6, 22, 26, 23), accent);
    }

    /** A circuit board: a green board with copper traces, a chip with its pins and two resistors. */
    private static void circuit(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 6, 28, 26, 1.5), base, seed);
        c.paint(Area.line(6, 10, 13, 10, 0.8).or(Area.line(13, 10, 13, 21, 0.8)).or(Area.line(13, 21, 27, 21, 0.8)),
                accent);
        c.paint(Area.line(19, 24, 27, 24, 0.8), accent);
        Sprite.body(c, Area.rect(16, 10, 23, 17), Color.hex("#2A2A2A"), seed + 1);
        for (double y : new double[]{11.5, 13.5, 15.5}) {
            c.paint(Area.rect(14, y, 16, y + 0.8), Color.hex("#C8C8C8"));
            c.paint(Area.rect(23, y, 25, y + 0.8), Color.hex("#C8C8C8"));
        }
        c.paint(Area.rect(6, 15, 11, 17), Color.hex("#D8C4A0"));
        c.paint(Area.rect(7, 15, 8, 17), Color.hex("#7A3A1A"));
        c.paint(Area.rect(9, 15, 10, 17), Color.hex("#2F4A9A"));
    }
}
