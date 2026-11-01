package com.claudemods.triage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/**
 * The handful of things that belong to the person looking rather than to the world.
 *
 * <p>Separate from {@link Config} on purpose, and not merely for tidiness: everything in
 * that one decides how much damage a body takes, so it travels with the world and the
 * server has the last word on it. Which sheet the chart is printed on decides nothing at
 * all, and a server telling a player what colour their own screen is would be the wrong
 * shape of setting. Nothing here is ever sent over the wire, in either direction.
 *
 * <p>Four settings, written by hand rather than generated like the server file, because
 * four is fewer than the generator is worth and because this one is written back the
 * moment somebody flips the switch on the chart rather than only at startup.
 */
public final class ClientConfig {
    private static final String FILE_NAME = "triage-client.toml";

    /**
     * Baked into a plain field for a dull reason: this is read once per frame by
     * everything that draws, and a lookup there is a map lookup per colour.
     */
    public static boolean dark = true;

    /**
     * Whether the mod is allowed to touch the view itself.
     *
     * <p>Here rather than in the server file, and the line is the same one: what a body
     * does is the server's business, and what a screen does to the person in front of it
     * is not. Somebody who gets motion sick has a real reason to turn the sway off, and
     * that reason has nothing to do with the game being played on the other end.
     */
    public static boolean symptoms = true;
    public static float symptomStrength = 1.0F;
    public static boolean sway = true;

    private ClientConfig() {
    }

    public static void load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    int equals = line.indexOf('=');
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("[") || equals < 0) {
                        continue;
                    }
                    String key = line.substring(0, equals).trim();
                    String value = line.substring(equals + 1).trim();
                    switch (key) {
                        case "dark" -> dark = Boolean.parseBoolean(value);
                        case "symptoms" -> symptoms = Boolean.parseBoolean(value);
                        case "strength" -> symptomStrength = Mth.clamp(Float.parseFloat(value), 0.0F, 1.0F);
                        case "sway" -> sway = Boolean.parseBoolean(value);
                        default -> { }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                TriageMod.LOGGER.error("Triage: could not read {}, using defaults", path, e);
            }
        }
        save();
    }

    /**
     * Flips the card from the chart and remembers the answer.
     *
     * <p>The field moves whether or not the write lands. A player who has just clicked the
     * switch should see the screen change; if the file cannot be written - a read-only
     * game directory, say - the worst that happens is that it is the other sheet again
     * next time.
     */
    public static void setDark(boolean value) {
        dark = value;
        save();
    }

    private static void save() {
        List<String> out = new ArrayList<>();
        out.add("# What the mod's screens look like. Nothing here changes the game.");
        out.add("#");
        out.add("# The chart is a printed casualty card either way - the same rules, the same");
        out.add("# boxes, the same one accent. This only decides which paper it is printed on:");
        out.add("# pale ink on dark card, or dark ink on a cream sheet.");
        out.add("#");
        out.add("# There is a switch beside the question mark on the chart, so this is only here");
        out.add("# for packs that want to set it and for anybody who would rather edit a file.");
        out.add("[look]");
        out.add("\t# Dark card and pale ink. Off is the cream sheet.");
        out.add("\tdark = " + dark);
        out.add("");
        out.add("# What being in a bad way looks like from inside it.");
        out.add("#");
        out.add("# The mod used to say all of this with vanilla potion effects: a bottle of");
        out.add("# blindness for losing blood, nausea for a rattled head. Those are blunt, they");
        out.add("# stack badly with everything else a pack is doing, and they put icons in the");
        out.add("# corner for things that are not potions.");
        out.add("#");
        out.add("# What is drawn instead is the thing itself - the edges closing in as volume");
        out.add("# goes, the grey that comes before somebody drops, a pulse in the corners of the");
        out.add("# eyes, a head that will not quite hold still. None of it changes what happens");
        out.add("# to the body. It changes what the body can see.");
        out.add("[symptoms]");
        out.add("\tsymptoms = " + symptoms);
        out.add("\t# How strong all of it is. Zero is the same as off.");
        out.add("\tstrength = " + symptomStrength);
        out.add("\t# Whether a head that has been hit, or one that is running out of blood, moves");
        out.add("\t# the camera as well as the picture. Separate because this is the half that can");
        out.add("\t# make people ill, and nobody should have to give up the rest of it to turn");
        out.add("\t# this off.");
        out.add("\tsway = " + sway);
        out.add("");
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.write(path, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            TriageMod.LOGGER.error("Triage: could not write {}", FILE_NAME, e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
