package io.corefactory.resourcepack;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The machine sounds of the pack. The OGG files come from {@code art/sounds}, made by {@code tools/sounds/make_sounds.py}, and
 * are copied to {@code assets/corefactory/sounds/KEY.ogg}. {@code assets/corefactory/sounds.json} names each one as the sound
 * event {@code corefactory:KEY} with its volume. Sounds longer than 2.5 seconds are streamed from disk. A pack built from an art
 * folder with no sounds folder has no sounds, and gets no sounds.json.
 */
final class SoundPack {

    private static final String NAMESPACE = "corefactory";

    /** A sound event: its key, its playback volume and whether it is streamed. */
    private record Sound(String key, double volume, boolean stream) {
    }

    private static final List<Sound> SOUNDS = List.of(
            new Sound("generator_hum", 0.6, false),
            new Sound("belt_hum", 0.6, false),
            new Sound("drill_run", 0.7, true),
            new Sound("lab_run", 0.6, false),
            new Sound("induction_hiss", 0.6, true),
            new Sound("mill_grind", 0.7, false),
            new Sound("press_stamp", 0.7, false),
            new Sound("turret_fire", 0.2, false),
            new Sound("turret_reload", 0.2, false),
            new Sound("turret_rotate", 0.12, false),
            new Sound("laser_fire", 0.15, false),
            new Sound("laser_charge", 0.12, false),
            new Sound("picker_clunk", 0.7, false),
            new Sound("wind_generator", 0.5, false),
            new Sound("solar_panel", 0.4, false),
            new Sound("charcoal_generator", 0.5, false),
            new Sound("petroleum_generator", 0.5, false),
            new Sound("diesel_generator", 0.5, false),
            new Sound("nuclear_generator", 0.5, false),
            new Sound("ore_drill", 0.7, true),
            new Sound("petroleum_drill", 0.7, true),
            new Sound("separator", 0.5, false),
            new Sound("roaster", 0.5, false),
            new Sound("purifier", 0.5, false),
            new Sound("converter", 0.5, false),
            new Sound("enrichment_cascade", 0.5, false),
            new Sound("transmuter", 0.5, false),
            new Sound("spectrometer", 0.6, false),
            new Sound("particle_accelerator", 0.6, true),
            new Sound("antimatter_condenser", 0.6, false),
            new Sound("research_console", 0.6, false),
            new Sound("crude_oil_refinery", 0.5, false),
            new Sound("cartridge_press", 0.7, false),
            new Sound("bullet_swaging_machine", 0.7, false),
            new Sound("ammunition_assembly", 0.6, false),
            new Sound("metal_lathe", 0.5, false),
            new Sound("component_assembly", 0.5, false),
            new Sound("heavy_weapons_assembly", 0.6, false),
            new Sound("pylon_hum", 0.4, false),
            new Sound("accumulator_charge", 0.5, false));

    private SoundPack() {
    }

    /** Writes the sound files and sounds.json into the zip, from the sounds folder of the art directory, if there is one. */
    static void add(ZipOutputStream zip, Path art) throws IOException {
        Path sounds = art.resolve("sounds");
        if (!Files.isDirectory(sounds)) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        for (int i = 0; i < SOUNDS.size(); i++) {
            Sound sound = SOUNDS.get(i);
            Path ogg = sounds.resolve(sound.key() + ".ogg");
            if (!Files.isRegularFile(ogg)) {
                throw new IllegalStateException("Sound " + sound.key() + " is missing: " + ogg);
            }
            zip.putNextEntry(new ZipEntry("assets/" + NAMESPACE + "/sounds/" + sound.key() + ".ogg"));
            zip.write(Files.readAllBytes(ogg));
            zip.closeEntry();
            json.append("  \"").append(sound.key()).append("\": {\n")
                    .append("    \"sounds\": [\n")
                    .append("      {\"name\": \"").append(NAMESPACE).append(':').append(sound.key())
                    .append("\", \"volume\": ").append(sound.volume())
                    .append(", \"stream\": ").append(sound.stream()).append("}\n")
                    .append("    ]\n")
                    .append("  }").append(i + 1 < SOUNDS.size() ? ",\n" : "\n");
        }
        json.append("}\n");
        writeText(zip, "assets/" + NAMESPACE + "/sounds.json", json.toString());
    }

    private static void writeText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Writer writer = new java.io.OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write(text);
        writer.flush();
        zip.closeEntry();
    }
}
