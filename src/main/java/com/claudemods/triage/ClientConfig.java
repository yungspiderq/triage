package com.claudemods.triage;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The handful of things that belong to the person looking rather than to the world.
 *
 * <p>Separate from {@link Config} on purpose, and not merely for tidiness: everything in
 * that one decides how much damage a body takes, so it travels with the save and the
 * server has the last word on it. Which sheet the chart is printed on decides nothing at
 * all, and a server telling a player what colour their own screen is would be the wrong
 * shape of setting.
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue DARK;
    private static final ForgeConfigSpec.BooleanValue SYMPTOMS;
    private static final ForgeConfigSpec.DoubleValue SYMPTOM_STRENGTH;
    private static final ForgeConfigSpec.BooleanValue SWAY;

    /**
     * Baked out of the spec like everything in {@link Config}, and for a duller reason:
     * this is read once per frame by everything that draws, and a spec lookup there is
     * a map lookup per colour.
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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("What the mod's screens look like. Nothing here changes the game.",
                        "",
                        "The chart is a printed casualty card either way - the same rules, the",
                        "same boxes, the same one accent. This only decides which paper it is",
                        "printed on: pale ink on dark card, or dark ink on a cream sheet.",
                        "",
                        "There is a switch beside the question mark on the chart, so this is",
                        "only here for packs that want to set it and for anybody who would",
                        "rather edit a file than click.")
                .push("look");
        DARK = builder.comment("Dark card and pale ink. Off is the cream sheet.")
                .define("dark", true);
        builder.pop();

        builder.comment("What being in a bad way looks like from inside it.",
                        "",
                        "The mod used to say all of this with vanilla potion effects: a bottle",
                        "of blindness for losing blood, nausea for a rattled head. Those are",
                        "blunt, they stack badly with everything else a pack is doing, and they",
                        "put icons in the corner for things that are not potions.",
                        "",
                        "What is drawn instead is the thing itself - the edges closing in as",
                        "volume goes, the grey that comes before somebody drops, a pulse in the",
                        "corners of the eyes, a head that will not quite hold still. None of it",
                        "changes what happens to the body. It changes what the body can see.")
                .push("symptoms");
        SYMPTOMS = builder.define("symptoms", true);
        SYMPTOM_STRENGTH = builder.comment("How strong all of it is. Zero is the same as off.")
                .defineInRange("strength", 1.0, 0.0, 1.0);
        SWAY = builder.comment("Whether a head that has been hit, or one that is running out of",
                        "blood, moves the camera as well as the picture. Separate because this",
                        "is the half that can make people ill, and nobody should have to give",
                        "up the rest of it to turn this off.")
                .define("sway", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static void bake() {
        dark = DARK.get();
        symptoms = SYMPTOMS.get();
        symptomStrength = SYMPTOM_STRENGTH.get().floatValue();
        sway = SWAY.get();
    }

    /**
     * Flips it from the chart and remembers the answer.
     *
     * <p>The field moves whether or not the write lands. A player who has just clicked
     * the switch should see the screen change; if the file cannot be written - a config
     * that is not loaded yet, a read-only game directory - the worst that happens is
     * that it is the other sheet again next time.
     */
    public static void setDark(boolean value) {
        dark = value;
        try {
            DARK.set(value);
        } catch (IllegalStateException ignored) {
            // Not loaded. The screen still changes; the file simply keeps its old answer.
        }
    }
}
