package io.corefactory.paper.machine;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.process.Machine;
import org.bukkit.Material;

/**
 * The menu layout of each machine. Every machine has its own size, title, glass and arrangement, so a crusher, a roaster
 * and a separator look and work differently. Slot numbers are rows of nine, counted from the top left.
 */
public final class MachineLayouts {

    private MachineLayouts() {
    }

    /** The layout of a machine. A machine id with no layout of its own gets a plain one sized to its slots. */
    public static MachineLayout of(Machine machine) {
        return switch (machine.id()) {
            case "crusher" -> crusher();
            case "roaster" -> roaster();
            case "separator" -> separator();
            case "purifier" -> purifier();
            case "converter" -> converter();
            case "enrichment_cascade" -> enrichmentCascade();
            case "transmuter" -> transmuter();
            case "spectrometer" -> spectrometer();
            case "particle_accelerator" -> particleAccelerator();
            case "antimatter_condenser" -> antimatterCondenser();
            case "research_console" -> researchConsole();
            case "crude_oil_refinery" -> crudeOilRefinery();
            case "induction_furnace" -> inductionFurnace();
            case "powder_mill" -> powderMill();
            case "cartridge_press" -> cartridgePress();
            case "bullet_swaging_machine" -> bulletSwagingMachine();
            case "ammunition_assembly" -> ammunitionAssembly();
            case "metal_lathe" -> metalLathe();
            case "component_assembly" -> componentAssembly();
            case "heavy_weapons_assembly" -> heavyWeaponsAssembly();
            default -> plain(machine);
        };
    }

    /** A feeder: one hopper-shaped input above a row of outputs. */
    private static MachineLayout crusher() {
        return MachineLayout.builder("&7Crusher", 27)
                .frame(Material.BLACK_STAINED_GLASS_PANE)
                .input(4, "Raw ore. Put a stack in; it is crushed one piece at a time.")
                .decor(Material.IRON_BARS, 3, 5)
                .progress(Material.WHITE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, 11, 12, 13, 14, 15)
                .output(19, "Crushed ore. Take it out with a click.")
                .output(20, "Crushed ore. Take it out with a click.")
                .output(21, "Crushed ore. Take it out with a click.")
                .status(22)
                .build();
    }

    /** A kiln: two feed slots, a firebox indicator that is lit while the kiln burns fuel, and the fuel slot beside it. */
    private static MachineLayout roaster() {
        return MachineLayout.builder("&6Roasting Kiln", 27)
                .frame(Material.BROWN_STAINED_GLASS_PANE)
                .input(10, "Crushed ore, two pieces per roast.")
                .input(11, "Crushed ore, two pieces per roast.")
                .firebox(13)
                .fuel(16, "Coal or charcoal. One item burns per roast.")
                .progress(Material.ORANGE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE, 18, 19, 20, 21)
                .status(22)
                .output(23, "Calcine or slag from the roast.")
                .output(24, "Calcine or slag from the roast.")
                .output(25, "Calcine or slag from the roast.")
                .build();
    }

    /** A centrifuge: two feeds beside a rotor, with the products in a ring around it. */
    private static MachineLayout separator() {
        return MachineLayout.builder("&bCentrifugal Separator", 45)
                .frame(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .progress(Material.CYAN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, 1, 2, 3, 4, 5, 6, 7)
                .decor(Material.IRON_BARS, 12, 14, 30, 32)
                .input(20, "Calcine or crushed ore. Two pieces per run.")
                .input(21, "Calcine or crushed ore. Two pieces per run.")
                .decor(Material.PRISMARINE_CRYSTALS, 22)
                .output(13, "Product, from the top of the ring.")
                .output(23, "Product, from the side of the ring.")
                .output(31, "Waste, from the bottom of the ring.")
                .status(40)
                .build();
    }

    /** A still with two tanks, one each side of the progress row. */
    private static MachineLayout purifier() {
        return MachineLayout.builder("&3Purification Still", 27)
                .frame(Material.CYAN_STAINED_GLASS_PANE)
                .output(2, "Product from the still.")
                .output(6, "Product from the still.")
                .status(4)
                .decor(Material.GLASS, 10, 12, 14, 16)
                .input(11, "Tank A: water bucket, iron ingots or coal blocks.")
                .input(15, "Tank B: a second feed for the same recipes.")
                .progress(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** A fluorination line: two feeds, an arrow of progress, one product and a fuel slot below. */
    private static MachineLayout converter() {
        return MachineLayout.builder("&bFluorination Converter", 27)
                .frame(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .input(10, "Yellowcake, one piece.")
                .input(11, "Crushed fluorite, two pieces.")
                .progress(Material.CYAN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, 12, 13, 14)
                .output(15, "UF6 cylinder. Take it out with a click.")
                .status(22)
                .fuel(19, "Coal or charcoal. One item burns per run.")
                .build();
    }

    /** A bank of eight centrifuges fed by a row of eight cylinders, with four products below. */
    private static MachineLayout enrichmentCascade() {
        return MachineLayout.builder("&aEnrichment Cascade", 45)
                .frame(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .input(9, "UF6 cylinder. Eight are used, one per slot.")
                .input(10, "UF6 cylinder. Eight are used, one per slot.")
                .input(11, "UF6 cylinder. Eight are used, one per slot.")
                .input(12, "UF6 cylinder. Eight are used, one per slot.")
                .input(13, "UF6 cylinder. Eight are used, one per slot.")
                .input(14, "UF6 cylinder. Eight are used, one per slot.")
                .input(15, "UF6 cylinder. Eight are used, one per slot.")
                .input(16, "UF6 cylinder. Eight are used, one per slot.")
                .decor(Material.IRON_BARS, 17)
                .progress(Material.LIME_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .output(30, "Enriched uranium, U-235 metal.")
                .output(31, "Depleted uranium.")
                .output(32, "Depleted uranium.")
                .output(33, "Depleted uranium.")
                .status(40)
                .build();
    }

    /** A flux chamber: the feed on the left, the neutron core in the middle, three products on the right. */
    private static MachineLayout transmuter() {
        return MachineLayout.builder("&dNeutron Transmuter", 27)
                .frame(Material.MAGENTA_STAINED_GLASS_PANE)
                .status(4)
                .input(10, "Feed: thorium concentrate, rare earths or yellowcake.")
                .input(13, "Neutron core: a neutron flux cell goes here.")
                .decor(Material.AMETHYST_BLOCK, 12, 14, 22)
                .output(15, "Fissile product: U-233 or plutonium.")
                .output(16, "Fission waste. Keep it sealed.")
                .output(17, "Spare output.")
                .progress(Material.PINK_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 23, 24, 25, 26)
                .build();
    }

    /** A sample slot, a reagent slot, a screen in the middle and two research packs out. */
    private static MachineLayout spectrometer() {
        return MachineLayout.builder("&aSpectrometer", 27)
                .frame(Material.GRAY_STAINED_GLASS_PANE)
                .input(10, "Sample: crushed ore, or a sample to read.")
                .input(19, "Reagent for a two-part reading. Leave it empty for a one-part reading.")
                .status(13)
                .decor(Material.LECTERN, 4)
                .progress(Material.LIME_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE, 21, 22, 23)
                .output(16, "Research pack. Take it to a console.")
                .output(25, "Research pack. Take it to a console.")
                .build();
    }

    /** A beamline: two feeds, a row of progress cells that lights up as the beam runs, and two products. */
    private static MachineLayout particleAccelerator() {
        return MachineLayout.builder("&9Particle Accelerator", 27)
                .frame(Material.BLUE_STAINED_GLASS_PANE)
                .input(10, "Beam feed: deuterium cell or neutron flux cell.")
                .decor(Material.GLASS, 11, 16)
                .input(12, "Magnet: a superconducting magnet.")
                .progress(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, 13, 14, 15)
                .output(21, "Antiproton capsule or neutronium slug.")
                .status(22)
                .output(23, "Antiproton capsule or neutronium slug.")
                .build();
    }

    /** Two capsule slots, a cold indicator, and a condensate slot. */
    private static MachineLayout antimatterCondenser() {
        return MachineLayout.builder("&5Antimatter Condenser", 27)
                .frame(Material.PURPLE_STAINED_GLASS_PANE)
                .status(4)
                .decor(Material.GLASS, 12, 14)
                .input(11, "Antiproton capsule, two are used.")
                .input(15, "Neutron flux cell.")
                .output(13, "Antimatter condensate. Handle it with care.")
                .progress(Material.CYAN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE,
                        19, 20, 21, 22, 23, 24, 25)
                .build();
    }

    /**
     * The research console: a page of 35 node slots (rows one to five, columns two to eight, written by the console)
     * and six pack slots along the bottom row.
     */
    private static MachineLayout researchConsole() {
        MachineLayout.Builder builder = MachineLayout.builder("&bResearch Console", 54)
                .frame(Material.BLUE_STAINED_GLASS_PANE)
                .decor(Material.BOOKSHELF, 52);
        for (int i = 0; i < 6; i++) {
            builder.input(46 + i, "Research pack. Feeds the node you click.");
        }
        return builder.build();
    }

    /**
     * The crude oil refinery: no item slots, since it takes petroleum and gives diesel through the oil network. Its info item shows the
     * petroleum and diesel in the network and its state, and its status item shows whether it runs or is stuck.
     */
    private static MachineLayout crudeOilRefinery() {
        return MachineLayout.builder("&8Crude Oil Refinery", 27)
                .frame(Material.GRAY_STAINED_GLASS_PANE)
                .status(4)
                .info(13)
                .build();
    }

    /** A melt crucible: two metal feeds, a magma core between them, one alloy product, and a melt bar along the bottom. */
    private static MachineLayout inductionFurnace() {
        return MachineLayout.builder("&6Induction Furnace", 27)
                .frame(Material.BLACK_STAINED_GLASS_PANE)
                .status(4)
                .input(10, "Copper ingot. Brass takes two copper and one zinc ingot.")
                .input(11, "Zinc ingot. Brass takes two copper and one zinc ingot.")
                .decor(Material.MAGMA_BLOCK, 13)
                .output(15, "Alloy ingot from the melt. Take it out with a click.")
                .progress(Material.ORANGE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** A grinding mill: three feeds in a row, the drum's housing beside them, and the gunpowder out on the right. */
    private static MachineLayout powderMill() {
        return MachineLayout.builder("&eGunpowder Mill", 27)
                .frame(Material.GRAY_STAINED_GLASS_PANE)
                .status(4)
                .input(10, "Charcoal, one piece per pass.")
                .input(11, "Sulfur, one piece per pass.")
                .input(12, "High purity potassium nitrate, one piece per pass.")
                .decor(Material.IRON_BARS, 13, 14)
                .output(16, "High grade gunpowder. Take it out with a click.")
                .progress(Material.YELLOW_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** A stamping press, the largest lab tool: a ram housing over the feed, two brass feeds, and a shell tray out. */
    private static MachineLayout cartridgePress() {
        return MachineLayout.builder("&6Cartridge Press", 45)
                .frame(Material.BLACK_STAINED_GLASS_PANE)
                .status(4)
                .decor(Material.IRON_BARS, 11, 13, 15)
                .input(19, "Brass ingot. Two are used per run.")
                .decor(Material.PISTON, 20)
                .input(21, "Brass ingot. Two are used per run.")
                .decor(Material.IRON_BLOCK, 22, 23)
                .output(24, "Empty ammunition shell. Take it out with a click.")
                .decor(Material.IRON_BLOCK, 30, 31, 32)
                .progress(Material.LIME_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        37, 38, 39, 40, 41, 42, 43)
                .build();
    }

    /** A small swage press: the lead feed and the core feed on either side of the die, and the bullet out on the right. */
    private static MachineLayout bulletSwagingMachine() {
        return MachineLayout.builder("&7Bullet Swaging Machine", 27)
                .frame(Material.GRAY_STAINED_GLASS_PANE)
                .status(4)
                .input(10, "Refined lead, one piece per pass.")
                .input(12, "Core: iron ingot, blaze powder or depleted uranium, by bullet.")
                .decor(Material.ANVIL, 13)
                .decor(Material.IRON_BARS, 14, 15)
                .output(16, "Bullet. Take it out with a click.")
                .progress(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** A belt-fed loader: a shell, a bullet and the powder feed in a row, and the finished round out on the right. */
    private static MachineLayout ammunitionAssembly() {
        return MachineLayout.builder("&cAmmunition Assembly", 27)
                .frame(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .status(4)
                .input(9, "Empty ammunition shell.")
                .input(10, "Bullet of the same kind as the round.")
                .input(11, "High grade gunpowder, one per round.")
                .decor(Material.IRON_BARS, 13)
                .output(15, "Round. Take it out with a click.")
                .progress(Material.LIME_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** A lathe: the iron feed and the steel-grade feed, a chuck between them, and the finished barrel out on the right. */
    private static MachineLayout metalLathe() {
        return MachineLayout.builder("&bPrecision Machining Station", 27)
                .frame(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .status(4)
                .input(10, "Iron ingot, for the barrel.")
                .input(12, "Steel-grade iron, from zone refining.")
                .decor(Material.IRON_BARS, 13, 14)
                .output(16, "Precision barrel. Take it out with a click.")
                .progress(Material.CYAN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /** An assembly bench: four part feeds in a row, the bench's gear housing beside them, and the sub-assembly out. */
    private static MachineLayout componentAssembly() {
        return MachineLayout.builder("&bMechanical Component Assembly", 27)
                .frame(Material.WHITE_STAINED_GLASS_PANE)
                .status(4)
                .input(9, "Part: iron ingot, or the part the recipe names.")
                .input(10, "Part: iron ingot, or the part the recipe names.")
                .input(11, "Part: a circuit or mechanism part, by recipe.")
                .input(12, "Part: a circuit or mechanism part, by recipe.")
                .decor(Material.IRON_BARS, 13)
                .output(15, "Assembled component. Take it out with a click.")
                .progress(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        18, 19, 20, 21, 22, 23, 24, 25, 26)
                .build();
    }

    /**
     * The turret line: the largest menu, six part feeds along the second row, a gantry rail above the progress row and the
     * furnace glow beneath it, and the finished turret in the middle of the fifth row.
     */
    private static MachineLayout heavyWeaponsAssembly() {
        MachineLayout.Builder builder = MachineLayout.builder("&4Heavy Weapons Assembly", 54)
                .frame(Material.BLACK_STAINED_GLASS_PANE)
                .status(4)
                .decor(Material.IRON_BARS, 1, 2, 3, 5, 6, 7, 18, 26)
                .decor(Material.MAGMA_BLOCK, 21, 22, 23);
        for (int i = 0; i < 6; i++) {
            builder.input(10 + i, "Turret part: a steel frame, barrel, targeting unit, mechanism, feed, or brass ingot.");
        }
        return builder.output(40, "Machine gun turret. Take it out with a click.")
                .progress(Material.RED_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        27, 28, 29, 30, 31, 32, 33, 34, 35)
                .build();
    }

    /**
     * A drill: the info item on top, the resources book beside it, the cycle bar at the start of the second row. A petroleum drill has
     * the fuel slot beside its grid, which takes charcoal, and no outputs: the petroleum it pumps goes into the oil network, so the grid
     * shows decoration. An ore drill has no fuel slot, since its petroleum comes from the oil network, and the outputs of its grid. The
     * petroleum drill is black and the ore drill grey, so the two read apart in a row of drills.
     */
    public static MachineLayout drill(String title, DrillKind kind) {
        boolean ore = kind == DrillKind.ORE;
        String product = "Ore from the bit. Take it out with a click.";
        MachineLayout.Builder builder = MachineLayout.builder(title, 36)
                .frame(ore ? Material.GRAY_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE)
                .info(4)
                .button(6, "Ores in range, what this drill mined, and which ores it mines.")
                .progress(ore ? Material.LIME_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE,
                        Material.GRAY_STAINED_GLASS_PANE, 9, 10, 11, 12)
                .decor(Material.IRON_BARS, 13, 20, 21, 22);
        if (ore) {
            return builder.output(14, product)
                    .output(15, product)
                    .output(16, product)
                    .output(23, product)
                    .output(24, product)
                    .output(25, product)
                    .output(32, product)
                    .output(33, product)
                    .output(34, product)
                    .build();
        }
        return builder.fuel(19, "Charcoal. One burns every 8 cycles, only when no solar panel or redstone power is stronger.")
                .decor(Material.BLACK_STAINED_GLASS_PANE, 14, 15, 16, 23, 24, 25, 32, 33, 34)
                .build();
    }

    private static MachineLayout plain(Machine machine) {
        int size = machine.inputSlots() + machine.outputSlots() > 9 ? 54 : 27;
        MachineLayout.Builder builder = MachineLayout.builder(machine.name(), size);
        int slot = 9;
        for (int i = 0; i < machine.inputSlots(); i++) {
            builder.input(slot++, "Input for " + machine.id() + ".");
        }
        slot = 18;
        for (int i = 0; i < machine.outputSlots(); i++) {
            builder.output(slot++, "Output of " + machine.id() + ".");
        }
        if (machine.fuelSlots() > 0) {
            builder.fuel(size - 1, "Coal or charcoal.");
        }
        return builder.build();
    }
}
