package com.claudemods.triage;

import com.claudemods.triage.med.MedicineKind;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side tuning for the whole mod.
 *
 * <p>This file is generated - see tools/config_port.py. The table below is the same table
 * the Forge build builds out of a config spec, with the same keys, defaults, ranges and
 * comments, so config/triage-server.toml is the same file on both loaders and a pack that
 * tunes the mod tunes it once. Edit the Forge spec and run the script; editing this by
 * hand only holds until the next run.
 *
 * <p>Two differences from Forge worth knowing. The file lives in config/ rather than in
 * the save, because Fabric has no per-world config folder to put it in. And nothing syncs
 * it by itself, so {@link #encode} and {@link #decode} hand the server's copy to joining
 * players: what a body does has to be the server's decision, or two people in the same
 * world would be playing different games.
 *
 * <p>Every value is read through {@link #values()} rather than off the table, because this
 * runs in the damage path and once a second per player.
 */
public final class Config {
    private static final String FILE_NAME = "triage-server.toml";

    /** The settings in the order they are written, section headers included. */
    private static final List<Object> LAYOUT = new ArrayList<>();

    private static Values values = new Values();
    private static String section = "";

    private Config() {
    }

    /** The current numbers. Never null, and safe to read before the config has loaded. */
    public static Values values() {
        return values;
    }

    /**
     * Reads config/triage-server.toml, writing it first if it is not there.
     *
     * <p>The file is always written back afterwards. That is what adds the settings a new
     * version brought with it to a file somebody has already edited, rather than leaving
     * them silently at their defaults and out of sight.
     */
    public static void load() {
        Values fresh = new Values();
        for (Object item : LAYOUT) {
            if (item instanceof Setting setting) {
                setting.reset(fresh);
            }
        }
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(path)) {
            try {
                read(Files.readAllLines(path, StandardCharsets.UTF_8), fresh);
            } catch (IOException e) {
                TriageMod.LOGGER.error("Triage: could not read {}, using defaults", path, e);
            }
        }
        values = fresh;
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, write(fresh), StandardCharsets.UTF_8);
        } catch (IOException e) {
            TriageMod.LOGGER.error("Triage: could not write {}", path, e);
        }
    }

    /** Puts the server's numbers in place of the local ones; see ModNetwork. */
    public static void accept(Values sent) {
        values = sent;
    }

    // ---- the wire ---------------------------------------------------------

    /** Writes every setting in table order, so both ends agree without naming anything. */
    public static void encode(FriendlyByteBuf buf, Values from) {
        for (Object item : LAYOUT) {
            if (item instanceof Setting setting) {
                setting.encode(buf, from);
            }
        }
    }

    public static Values decode(FriendlyByteBuf buf) {
        Values got = new Values();
        for (Object item : LAYOUT) {
            if (item instanceof Setting setting) {
                setting.decode(buf, got);
            }
        }
        return got;
    }

    // ---- reading and writing the file -------------------------------------

    private static void read(List<String> lines, Values into) {
        String at = "";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[")) {
                int close = line.indexOf(']');
                at = close > 0 ? line.substring(1, close).trim() : "";
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[") && !value.endsWith("]")) {
                StringBuilder gathered = new StringBuilder(value);
                while (++i < lines.size()) {
                    String more = lines.get(i).trim();
                    gathered.append(' ').append(more);
                    if (more.endsWith("]")) {
                        break;
                    }
                }
                value = gathered.toString();
            }
            Setting setting = find(at, key);
            if (setting == null) {
                TriageMod.LOGGER.warn("Triage: {} has a setting called {} that this version does not know", FILE_NAME,
                        at.isEmpty() ? key : at + "." + key);
                continue;
            }
            try {
                setting.parse(value, into);
            } catch (RuntimeException e) {
                TriageMod.LOGGER.warn("Triage: could not read {}.{} = {}, leaving it at its default", at, key, value);
            }
        }
    }

    private static @Nullable Setting find(String at, String key) {
        for (Object item : LAYOUT) {
            if (item instanceof Setting setting && setting.section.equals(at) && setting.key.equals(key)) {
                return setting;
            }
        }
        return null;
    }

    private static List<String> write(Values from) {
        List<String> out = new ArrayList<>();
        for (Object item : LAYOUT) {
            if (item instanceof Header header) {
                if (!out.isEmpty()) {
                    out.add("");
                }
                for (String line : header.comment) {
                    out.add(line.isEmpty() ? "#" : "# " + line);
                }
                out.add("[" + header.path + "]");
                continue;
            }
            Setting setting = (Setting) item;
            if (!setting.comment.isEmpty()) {
                out.add("");
                for (String line : setting.comment) {
                    out.add(line.isEmpty() ? "\t#" : "\t# " + line);
                }
            }
            out.add("\t" + setting.key + " = " + setting.print(from));
        }
        out.add("");
        return out;
    }

    // ---- the shapes a setting can have ------------------------------------

    private record Header(String path, List<String> comment) {
    }

    private abstract static class Setting {
        final String section;
        final String key;
        final List<String> comment;

        Setting(String section, String key, String[] comment) {
            this.section = section;
            this.key = key;
            this.comment = List.of(comment);
        }

        abstract void reset(Values into);

        abstract String print(Values from);

        abstract void parse(String text, Values into);

        abstract void encode(FriendlyByteBuf buf, Values from);

        abstract void decode(FriendlyByteBuf buf, Values into);
    }

    /**
     * What a number is underneath, which decides how it is written down.
     *
     * <p>Not fussiness: a float widened to a double prints as 0.15000000596046448, because
     * that is genuinely the nearest float to 0.15. Asking Float for the text instead gets
     * back the shortest string that reads as the same float, which is 0.15 - and the file is
     * meant to be read and edited by people.
     */
    private enum Kind {
        WHOLE,
        SINGLE,
        DOUBLE
    }

    private static final class Number extends Setting {
        private final double fallback;
        private final double low;
        private final double high;
        private final Kind kind;
        private final ToDoubleFunction<Values> reader;
        private final ObjDoubleConsumer<Values> writer;

        Number(String section, String key, double fallback, double low, double high, Kind kind,
                ToDoubleFunction<Values> reader, ObjDoubleConsumer<Values> writer, String[] comment) {
            super(section, key, comment);
            this.fallback = fallback;
            this.low = low;
            this.high = high;
            this.kind = kind;
            this.reader = reader;
            this.writer = writer;
        }

        @Override
        void reset(Values into) {
            writer.accept(into, fallback);
        }

        @Override
        String print(Values from) {
            double value = reader.applyAsDouble(from);
            if (kind == Kind.WHOLE) {
                return Long.toString((long) value);
            }
            String text = kind == Kind.SINGLE
                    ? Float.toString((float) value)
                    : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
            if (text.endsWith(".0")) {
                return text;
            }
            text = new BigDecimal(text).stripTrailingZeros().toPlainString();
            return text.indexOf('.') < 0 ? text + ".0" : text;
        }

        @Override
        void parse(String text, Values into) {
            double value = Double.parseDouble(text);
            // Out-of-range is clamped rather than refused. Somebody who wrote 5 in a field
            // that stops at 1 meant "as much as it goes", and throwing their whole file
            // away over it would be a poor trade.
            writer.accept(into, Math.min(high, Math.max(low, value)));
        }

        @Override
        void encode(FriendlyByteBuf buf, Values from) {
            buf.writeDouble(reader.applyAsDouble(from));
        }

        @Override
        void decode(FriendlyByteBuf buf, Values into) {
            writer.accept(into, buf.readDouble());
        }
    }

    private static final class Flag extends Setting {
        private final boolean fallback;
        private final Predicate<Values> reader;
        private final BiConsumer<Values, Boolean> writer;

        Flag(String section, String key, boolean fallback, Predicate<Values> reader,
                BiConsumer<Values, Boolean> writer, String[] comment) {
            super(section, key, comment);
            this.fallback = fallback;
            this.reader = reader;
            this.writer = writer;
        }

        @Override
        void reset(Values into) {
            writer.accept(into, fallback);
        }

        @Override
        String print(Values from) {
            return Boolean.toString(reader.test(from));
        }

        @Override
        void parse(String text, Values into) {
            writer.accept(into, Boolean.parseBoolean(text.trim()));
        }

        @Override
        void encode(FriendlyByteBuf buf, Values from) {
            buf.writeBoolean(reader.test(from));
        }

        @Override
        void decode(FriendlyByteBuf buf, Values into) {
            writer.accept(into, buf.readBoolean());
        }
    }

    private static final class Items extends Setting {
        private final MedicineKind kind;

        Items(String section, String key, MedicineKind kind, String[] comment) {
            super(section, key, comment);
            this.kind = kind;
        }

        @Override
        void reset(Values into) {
            into.medicine.put(kind, List.of());
        }

        @Override
        String print(Values from) {
            List<String> parts = new ArrayList<>();
            for (ItemMatcher matcher : from.medicine(kind)) {
                parts.add("\"" + name(matcher) + "\"");
            }
            return "[" + String.join(", ", parts) + "]";
        }

        @Override
        void parse(String text, Values into) {
            String body = text.trim();
            if (body.startsWith("[")) {
                body = body.substring(1);
            }
            if (body.endsWith("]")) {
                body = body.substring(0, body.length() - 1);
            }
            List<String> entries = new ArrayList<>();
            for (String piece : body.split(",")) {
                String entry = piece.trim();
                if (entry.length() >= 2 && entry.startsWith("\"") && entry.endsWith("\"")) {
                    entry = entry.substring(1, entry.length() - 1);
                }
                if (!entry.isBlank()) {
                    entries.add(entry);
                }
            }
            into.medicine.put(kind, matchers(entries));
        }

        @Override
        void encode(FriendlyByteBuf buf, Values from) {
            List<ItemMatcher> list = from.medicine(kind);
            buf.writeVarInt(list.size());
            for (ItemMatcher matcher : list) {
                buf.writeUtf(name(matcher));
            }
        }

        @Override
        void decode(FriendlyByteBuf buf, Values into) {
            int count = buf.readVarInt();
            List<String> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(buf.readUtf());
            }
            into.medicine.put(kind, matchers(entries));
        }

        private static String name(ItemMatcher matcher) {
            return matcher.tag() != null ? "#" + matcher.tag().location() : String.valueOf(matcher.item());
        }
    }

    private static List<ItemMatcher> matchers(List<String> entries) {
        List<ItemMatcher> out = new ArrayList<>();
        for (String entry : entries) {
            boolean isTag = entry.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
            if (id == null) {
                TriageMod.LOGGER.warn("Triage: '{}' in the medicine config is not a valid id, skipping it", entry);
                continue;
            }
            out.add(isTag ? new ItemMatcher(TagKey.create(Registries.ITEM, id), null) : new ItemMatcher(null, id));
        }
        return List.copyOf(out);
    }

    // ---- what the generated table calls -----------------------------------

    private static void section(String path, String... comment) {
        section = path;
        LAYOUT.add(new Header(path, List.of(comment)));
    }

    private static void num(String key, double fallback, double low, double high, Kind kind,
            ToDoubleFunction<Values> reader, ObjDoubleConsumer<Values> writer, String... comment) {
        LAYOUT.add(new Number(section, key, fallback, low, high, kind, reader, writer, comment));
    }

    private static void flag(String key, boolean fallback, Predicate<Values> reader,
            BiConsumer<Values, Boolean> writer, String... comment) {
        LAYOUT.add(new Flag(section, key, fallback, reader, writer, comment));
    }

    private static void items(String key, MedicineKind kind, String... comment) {
        LAYOUT.add(new Items(section, key, kind, comment));
    }

    // ---- the table, generated from the NeoForge spec -----------------------

    private static void complications() {
        section("complications",
                "Three illnesses that come out of the shape of a wound or out of a",
                "treatment rather than out of dirt, and the numbers behind them.");
        num("tetanusSeconds", 420, 30, 100000, Kind.WHOLE, v -> v.tetanusSeconds, (v, x) -> v.tetanusSeconds = (int) x,
                "How long lockjaw stays silent and treatable. When this runs",
                "out a course stops working, and nothing after that does.");
        num("tetanusChance", 0.0016, 0.0, 1.0, Kind.SINGLE, v -> v.tetanusChance, (v, x) -> v.tetanusChance = (float) x,
                "Chance per second that a dressed, dirty puncture - an arrow or a",
                "fragment closed over - becomes lockjaw. Small on purpose: this is",
                "the price of one bad decision, not a tax on being shot.");
        num("crushSeconds", 75, 10, 100000, Kind.WHOLE, v -> v.crushSeconds, (v, x) -> v.crushSeconds = (int) x,
                "How long there is, once the strap is off a limb that was under it",
                "too long, before the heart answers for it.");
        num("crushStrapSeconds", 180, 10, 100000, Kind.WHOLE, v -> v.crushStrapSeconds, (v, x) -> v.crushStrapSeconds = (int) x,
                "How long a tourniquet has to have been on before releasing it",
                "is dangerous. Below this, nothing happens at all.");
        num("relapseChance", 0.18, 0.0, 1.0, Kind.SINGLE, v -> v.relapseChance, (v, x) -> v.relapseChance = (float) x,
                "Chance that a cured illness comes back in waves instead of ending.",
                "What it is really measuring is a course that was not finished.");
    }

    private static void difficulty() {
        section("difficulty",
                "Which of the two the mod is playing.",
                "",
                "Off is arcade, which is what this mod has always been: the body",
                "chart is a readout. Open it and it tells you the left leg has two",
                "breaks and an infection, and you go and treat that.",
                "",
                "On is realistic, and it changes exactly one thing - where that",
                "knowledge comes from. The body is identical: same injuries, same",
                "rates, same treatments, same minigames. What goes is the free",
                "answer. A part nobody has examined reads as a blank rather than as",
                "a healthy one, and the difference between 'nothing there' and 'did",
                "not look' is the whole of the mode.",
                "",
                "Everything needed to fill that chart in is already in the mod and",
                "was built one version at a time: a pulse, asking where it hurts,",
                "asking for a number out of ten, pressing on a burn, a thermometer,",
                "an X-ray - and two new ones, feeling along a bone and listening to",
                "a chest. In arcade those are colour. Here they are the only channel.",
                "",
                "The server decides this for everybody in the world. Two people in",
                "one world cannot be playing two different games.");
        flag("realistic", false, v -> v.realistic, (v, x) -> v.realistic = x);
    }

    private static void health() {
        section("health",
                "How much health each part of the body has. These are not hearts:",
                "the seven parts add up to a pool of their own, and the vanilla bar",
                "only mirrors what is left of it.",
                "",
                "Head and thorax are vital - empty either one and the player dies.",
                "The rest black out instead.");
        num("headMax", 10.0, 1.0, 1000.0, Kind.SINGLE, v -> v.headMax, (v, x) -> v.headMax = (float) x);
        num("thoraxMax", 22.0, 1.0, 1000.0, Kind.SINGLE, v -> v.thoraxMax, (v, x) -> v.thoraxMax = (float) x);
        num("stomachMax", 16.0, 1.0, 1000.0, Kind.SINGLE, v -> v.stomachMax, (v, x) -> v.stomachMax = (float) x);
        num("armMax", 12.0, 1.0, 1000.0, Kind.SINGLE, v -> v.armMax, (v, x) -> v.armMax = (float) x,
                "Each arm.");
        num("legMax", 14.0, 1.0, 1000.0, Kind.SINGLE, v -> v.legMax, (v, x) -> v.legMax = (float) x,
                "Each leg.");
        num("damageScale", 2.0, 0.1, 100.0, Kind.SINGLE, v -> v.damageScale, (v, x) -> v.damageScale = (float) x,
                "Incoming damage is multiplied by this before it is taken off a part.",
                "Damage arrives here after armour and resistance, so armour still works.",
                "Raise it to make the game more lethal, lower it to make bodies tougher.");
        num("healScale", 2.0, 0.1, 100.0, Kind.SINGLE, v -> v.healScale, (v, x) -> v.healScale = (float) x,
                "Same, for healing: potions, food regeneration and the like.");
        flag("regeneration", true, v -> v.regeneration, (v, x) -> v.regeneration = x,
                "",
                "Whether the body mends itself at all.",
                "",
                "Off turns off every source of natural healing - food, potions,",
                "golden apples, beacons and anything else that calls vanilla's",
                "heal - and leaves only what somebody does with their hands.",
                "Bandages, medkits, splints and surgery heal the body directly",
                "and keep working; this is about the passive recovery that",
                "quietly undoes a wound while the player waits.",
                "",
                "The default is on. Turn it off for a world where every injury",
                "has to be treated, or leave it on and use the healing scale",
                "above to decide how fast a body comes back.");
        num("overflowToThorax", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.overflowToThorax, (v, x) -> v.overflowToThorax = (float) x,
                "Share of a hit that a blacked-out limb cannot absorb and passes",
                "on to the thorax. 0 makes dead limbs a free shield.");
        num("blackedLimbDrain", 0.15, 0.0, 10.0, Kind.SINGLE, v -> v.blackedLimbDrain, (v, x) -> v.blackedLimbDrain = (float) x,
                "Damage per second done to the thorax by every blacked-out limb.",
                "This is what eventually kills a player who never treats anything.");
        num("oneShotProtectionAbove", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.oneShotProtectionAbove, (v, x) -> v.oneShotProtectionAbove = (float) x,
                "",
                "No single blow may empty a head or a chest that was healthier than this",
                "share of its maximum. A drawn bow to the chest is realistic and it is",
                "also the end of the game before it started: the arrow you never saw",
                "leaves no decision to make, and this mod is about decisions.",
                "",
                "Below the threshold a hit can still finish you outright - being already",
                "hurt is supposed to be dangerous. Set to 0 to allow one-shots again.");
        num("oneShotFloor", 1.0, 0.0, 100.0, Kind.SINGLE, v -> v.oneShotFloor, (v, x) -> v.oneShotFloor = (float) x,
                "What such a blow leaves behind, in health.");
    }

    private static void routing() {
        section("routing",
                "Relative chance of an ordinary physical hit landing on each part.",
                "Only the ratios matter, not the totals. Falls always go to the legs,",
                "and damage with no point of impact - drowning, poison, fire - is",
                "spread over the whole body instead.");
        num("head", 8, 0, 1000, Kind.WHOLE, v -> v.headWeight, (v, x) -> v.headWeight = (int) x);
        num("thorax", 25, 0, 1000, Kind.WHOLE, v -> v.thoraxWeight, (v, x) -> v.thoraxWeight = (int) x);
        num("stomach", 15, 0, 1000, Kind.WHOLE, v -> v.stomachWeight, (v, x) -> v.stomachWeight = (int) x);
        num("arm", 13, 0, 1000, Kind.WHOLE, v -> v.armWeight, (v, x) -> v.armWeight = (int) x,
                "Per arm.");
        num("leg", 13, 0, 1000, Kind.WHOLE, v -> v.legWeight, (v, x) -> v.legWeight = (int) x,
                "Per leg.");
    }

    private static void bleeding() {
        section("bleeding",
                "Bleeding damages the part it is on, once a second, and does not stop",
                "on its own unless lightBleedSeconds says so. Bandages stop it.");
        num("lightPerSecond", 0.25, 0.0, 100.0, Kind.SINGLE, v -> v.lightBleedRate, (v, x) -> v.lightBleedRate = (float) x);
        num("heavyPerSecond", 0.7, 0.0, 100.0, Kind.SINGLE, v -> v.heavyBleedRate, (v, x) -> v.heavyBleedRate = (float) x);
        num("chancePerDamage", 0.05, 0.0, 1.0, Kind.SINGLE, v -> v.bleedChancePerDamage, (v, x) -> v.bleedChancePerDamage = (float) x,
                "Chance of a cut per point of damage the part took. Capped at 60%.");
        num("heavyThreshold", 10.0, 0.1, 1000.0, Kind.SINGLE, v -> v.heavyBleedThreshold, (v, x) -> v.heavyBleedThreshold = (float) x,
                "Damage to one part in a single hit that makes the bleed a heavy one.");
        num("lightBleedSeconds", 60, 0, 100000, Kind.WHOLE, v -> v.lightBleedSeconds, (v, x) -> v.lightBleedSeconds = (int) x,
                "How long a light bleed runs before it clots. 0 means never.");
    }

    private static void bleedingBlood() {
        section("bleeding.blood",
                "",
                "Blood lost while bleeding, as a share of all of it. This is what a bleed",
                "leaves behind once it is bandaged: the wound closes, the blood does not",
                "come back for a long while, and a body short of it is slow and dizzy.",
                "Rest and food put it back slowly; a drip puts it back at once, but only",
                "once nothing is still open.");
        num("lightPerSecond", 0.004, 0.0, 1.0, Kind.SINGLE, v -> v.bloodPerLight, (v, x) -> v.bloodPerLight = (float) x);
        num("heavyPerSecond", 0.010, 0.0, 1.0, Kind.SINGLE, v -> v.bloodPerHeavy, (v, x) -> v.bloodPerHeavy = (float) x);
        num("recoveryPerSecond", 0.0030, 0.0, 1.0, Kind.SINGLE, v -> v.bloodRecovery, (v, x) -> v.bloodRecovery = (float) x,
                "Put back each second, but only while there is food in the tank.");
        num("recoveryHunger", 7, 0, 20, Kind.WHOLE, v -> v.bloodRecoveryHunger, (v, x) -> v.bloodRecoveryHunger = (int) x,
                "Hunger needed before blood is made at all, out of 20.");
        flag("bloodLossKills", true, v -> v.bloodLossKills, (v, x) -> v.bloodLossKills = x,
                "Whether running out of blood entirely is fatal. With this off, an",
                "empty player is merely slow and dizzy forever, which rather undercuts",
                "the word 'bleeding'.");
        flag("particles", true, v -> v.bloodParticles, (v, x) -> v.bloodParticles = x,
                "Blood running off a bleeding player and staining the floor under them.",
                "Nothing is placed in the world and nothing is saved: stains are redrawn",
                "as particles for a while and then dry up.");
        num("stainSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.bloodStainSeconds, (v, x) -> v.bloodStainSeconds = (int) x,
                "",
                "How long blood stays on the floor before it has dried, in seconds.",
                "",
                "This is how long a trail is worth following. Set short it is not a trail",
                "at all - by the time somebody has worked out which way the marks lead,",
                "the far end of them has gone and the direction went with it.");
        num("tourniquetDamagePerSecond", 0.05, 0.0, 100.0, Kind.SINGLE, v -> v.tourniquetDamage, (v, x) -> v.tourniquetDamage = (float) x,
                "Damage a strapped limb takes each second while the tourniquet is on.",
                "",
                "Zero, and that is a correction rather than a nerf. This used to be the",
                "whole cost of a strap and it was right when it was: something had to",
                "make keeping one on a decision. Then necrosis arrived and gave the strap",
                "a three minute clock - and left this running underneath it, at a rate",
                "that emptied the limb in about thirty seconds. The clock was never",
                "reached by anybody, because there was nothing left to reach it with.",
                "",
                "A real tourniquet is fine for a while and then it is not, which is what",
                "the clock says and this cannot.",
                "",
                "It is back at a rate the clock outlives: about nine over three minutes,",
                "so a healthy limb is badly used by the time it dies and never quite",
                "empties on its own. And it goes onto that limb ONLY - what a part cannot",
                "absorb no longer runs into the chest, because a strap round an arm does",
                "nothing whatever to a chest and billing one for the other was how three",
                "minutes under a strap became unsurvivable.");
    }

    private static void fractures() {
        section("fractures",
                "Broken bones. A fracture does not heal on its own - it needs a splint.");
        flag("enabled", true, v -> v.fractures, (v, x) -> v.fractures = x);
        num("fallThreshold", 4.0, 0.1, 1000.0, Kind.SINGLE, v -> v.fractureFall, (v, x) -> v.fractureFall = (float) x,
                "Fall damage (before scaling) that can break a leg.");
        num("blowThreshold", 14.0, 0.1, 1000.0, Kind.SINGLE, v -> v.fractureBlow, (v, x) -> v.fractureBlow = (float) x,
                "Damage to one part in a single hit that can break the bone under it.");
        num("chance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.fractureChance, (v, x) -> v.fractureChance = (float) x);
        num("painPerSecond", 1.5, 0.0, 100.0, Kind.SINGLE, v -> v.fracturePain, (v, x) -> v.fracturePain = (float) x,
                "Damage done to a fractured leg each second the player sprints on it.");
        num("crackThreshold", 8.0, 0.1, 1000.0, Kind.SINGLE, v -> v.crackBlow, (v, x) -> v.crackBlow = (float) x,
                "Damage to one part in a single hit that can crack the bone without",
                "breaking it. A crack is a warning: left alone it knits by itself, but",
                "another hit or a sprint on it turns it into a real fracture.");
        num("crackChance", 0.45, 0.0, 1.0, Kind.SINGLE, v -> v.crackChance, (v, x) -> v.crackChance = (float) x);
        num("crackHealSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.crackHealSeconds, (v, x) -> v.crackHealSeconds = (int) x,
                "Seconds of not being leaned on before a crack knits. Every hit on",
                "the part, and every second of sprinting on a cracked leg, starts it over.");
        num("crackBreakChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.crackBreakChance, (v, x) -> v.crackBreakChance = (float) x,
                "Chance that leaning on a crack breaks it properly.");
    }

    private static void armour() {
        section("armour",
                "Vanilla armour has already done its work by the time a hit is routed, and",
                "it protects the whole body evenly. This is the part it cannot express:",
                "a hit that lands where you are wearing nothing hurts more.",
                "",
                "Helmet covers the head, chestplate the thorax and stomach, leggings and",
                "boots the legs. Arms are never covered - there is no slot for them.",
                "Set to 1.0 to switch this off.");
        num("uncoveredMultiplier", 1.3, 1.0, 10.0, Kind.SINGLE, v -> v.uncoveredMultiplier, (v, x) -> v.uncoveredMultiplier = (float) x);
        flag("aimedHits", true, v -> v.aimedHits, (v, x) -> v.aimedHits = x,
                "",
                "Whether a blow lands where it was actually aimed. An arrow is asked",
                "where it is; a melee swing is traced from the attacker's line of sight",
                "into the victim. Aim at a head, hit a head.",
                "",
                "Turn this off to go back to the weights in [routing] deciding everything,",
                "which is kinder to anyone being shot at by a skeleton.");
    }

    private static void infection() {
        section("infection",
                "A wound that is not cleaned goes bad. Infection eats the part it is on,",
                "stops that part healing by itself, and at its worst makes the whole body",
                "ill - see [diseases]. Antiseptic clears it.");
        flag("enabled", true, v -> v.infection, (v, x) -> v.infection = x);
        num("bleedingSecondsBeforeRisk", 45, 1, 100000, Kind.WHOLE, v -> v.bleedSecondsBeforeRisk, (v, x) -> v.bleedSecondsBeforeRisk = (int) x,
                "How long a part bleeds before it can start going bad.");
        num("blackedSecondsBeforeRisk", 60, 1, 100000, Kind.WHOLE, v -> v.blackedSecondsBeforeRisk, (v, x) -> v.blackedSecondsBeforeRisk = (int) x,
                "Same, for a limb that has been blacked out and left.");
        num("riskPerSecond", 0.012, 0.0, 1.0, Kind.SINGLE, v -> v.infectionRisk, (v, x) -> v.infectionRisk = (float) x,
                "Chance per second, once past those thresholds.");
        num("worsenSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.infectionWorsenSeconds, (v, x) -> v.infectionWorsenSeconds = (int) x,
                "Seconds a mild infection runs before it turns severe.");
        num("feverSeconds", 60, 1, 100000, Kind.WHOLE, v -> v.infectionFeverSeconds, (v, x) -> v.infectionFeverSeconds = (int) x,
                "Seconds a severe infection runs before it becomes a fever.");
        num("mildDamagePerSecond", 0.08, 0.0, 100.0, Kind.SINGLE, v -> v.infectionMildDamage, (v, x) -> v.infectionMildDamage = (float) x);
        num("severeDamagePerSecond", 0.2, 0.0, 100.0, Kind.SINGLE, v -> v.infectionSevereDamage, (v, x) -> v.infectionSevereDamage = (float) x);
    }

    private static void burns() {
        section("burns",
                "Fire and lava leave burns, and a burn has a depth.",
                "",
                "Red heals itself. Blistered wants a salve. Through the skin will not",
                "close at all and no salve reaches it - that one wants the knife. Which",
                "of the three it ends up being is not decided when the fire touches you:",
                "every burn arrives shallow and then cooks, and what happens during that",
                "window is the only part of a burn anybody can change.",
                "",
                "The worst of the three does not hurt, because the nerve endings that",
                "would have done the hurting are part of what burnt. So the loudest burn",
                "is the one that will be fine and the quiet patch beside it is the one",
                "that needs help, which is why a burn is tested by hand rather than",
                "looked at.");
        flag("enabled", true, v -> v.burns, (v, x) -> v.burns = x);
        num("chancePerDamage", 0.08, 0.0, 1.0, Kind.SINGLE, v -> v.burnChance, (v, x) -> v.burnChance = (float) x,
                "Chance per point of fire damage taken. Capped at 70%.");
        num("cookSeconds", 30, 0, 100000, Kind.WHOLE, v -> v.burnCookSeconds, (v, x) -> v.burnCookSeconds = (int) x,
                "",
                "How long a burn goes on deciding how deep it is, after the fire is out.",
                "",
                "This is the window, and it is the whole of the first aid: water in it",
                "stops the burn where it is, and nothing outside it changes anything.");
        num("deepenPerSecond", 0.05, 0.0, 1.0, Kind.SINGLE, v -> v.burnDeepenChance, (v, x) -> v.burnDeepenChance = (float) x,
                "Chance per second in that window of going a step deeper.");
        num("fadeSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.burnFadeSeconds, (v, x) -> v.burnFadeSeconds = (int) x,
                "",
                "How long a superficial burn takes to close on its own.",
                "",
                "Red skin heals, and it is the only one of the three that does. A mod",
                "that made every brush with a campfire into an errand for a jar would be",
                "wrong about burns in the direction that annoys rather than the one that",
                "teaches.");
        num("coolBackChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.burnCoolBack, (v, x) -> v.burnCoolBack = (float) x,
                "",
                "And the chance that a burn held under water for the whole window comes",
                "back a step. The only thing in this mod that undoes an injury rather",
                "than treating it, and the one place where that is true in life too.");
        num("iceFactor", 2.0, 0.0, 100.0, Kind.SINGLE, v -> v.burnIceFactor, (v, x) -> v.burnIceFactor = (float) x,
                "",
                "What packing a burn in something freezing does to it, as a multiplier on",
                "the deepening chance. Snow and ice are not water: they shut down what",
                "little blood is still reaching the burn and take it deeper, which is a",
                "mistake common enough that every first aid course warns about it.");
        num("fluidFloor", 0.15, 0.0, 1.0, Kind.SINGLE, v -> v.burnFluidFloor, (v, x) -> v.burnFluidFloor = (float) x,
                "",
                "How much of somebody has to be burnt before it costs volume.",
                "",
                "The rule of nines, added up over the parts burnt past the skin: head",
                "nine, each arm nine, each leg eighteen, chest and belly eighteen apiece.",
                "Under this it is an injury; over it, it is a person emptying out through",
                "their own surface, and the answer is fluid faster than it is going.");
        num("fluidDrainPerSecond", 0.02, 0.0, 1.0, Kind.SINGLE, v -> v.burnFluidDrain, (v, x) -> v.burnFluidDrain = (float) x,
                "Volume lost per second, per share of surface over that floor.");
        num("chillPerSecond", 0.004, 0.0, 1.0, Kind.SINGLE, v -> v.burnChill, (v, x) -> v.burnChill = (float) x,
                "",
                "And how fast a burnt body cools, per share of surface per second. Skin",
                "is the thing that keeps heat in. This feeds the same figure an operation",
                "feeds, so the circle the mod already has closes around burns as well:",
                "cold does not clot, and a big burn is a cold patient.");
        num("smokeChancePerSecond", 0.08, 0.0, 1.0, Kind.SINGLE, v -> v.smokeChance, (v, x) -> v.smokeChance = (float) x,
                "",
                "Breathing it, which is the half of fire that kills quickly.",
                "",
                "A burn on the outside is measured in hours. An airway that has breathed",
                "fire is measured in minutes, it happens to people who walked out looking",
                "fine, and the tell is the voice rather than the skin.",
                "",
                "Chance per second of it while standing in fire or lava with nothing over",
                "your face. 0 turns it off.");
        num("smokeSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.smokeSeconds, (v, x) -> v.smokeSeconds = (int) x,
                "How long it takes to come right, out of the smoke.");
        num("smokeBreath", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.smokeBreath, (v, x) -> v.smokeBreath = (float) x,
                "What it takes off the breathing rate while it lasts.");
    }

    private static void reactions() {
        section("reactions",
                "Whether people say anything when the worst of it happens to them.",
                "",
                "Everything in this mod has been told to the player in the voice of a",
                "chart: a bone breaks and a line names the bone. That is the right voice",
                "for a readout and the wrong one for the moment itself, because the moment",
                "is not information - it is a person making a noise.",
                "",
                "Whoever is within about fourteen blocks hears it, which is the only thing",
                "in the mod a bystander gets without walking over and opening a chart. It",
                "is also the first warning a medic ever gets.",
                "",
                "None of it is a mechanic. No line does anything.");
        flag("enabled", true, v -> v.reactions, (v, x) -> v.reactions = x);
    }

    private static void allergies() {
        section("allergies",
                "The one thing in the bag that will kill this particular person.",
                "",
                "Rolled with the blood type, at the same moment and for the same reason:",
                "it is a fact about somebody rather than an event in their day, and it is",
                "wanted before a bag is opened over them rather than after.",
                "",
                "What happens is anaphylaxis, which this mod already had, and the answer",
                "is the needle it already keeps for exactly that. The dose is spent either",
                "way - it is not a refusal, it is a treatment that went in and did the",
                "wrong thing.",
                "",
                "And it can be asked about, which is the whole of what makes it",
                "interesting: a conscious patient tells you, and an unconscious one does",
                "not, so the same dose is a treatment on a man who can talk and a killing",
                "on the same man ten minutes later.");
        flag("enabled", true, v -> v.allergies, (v, x) -> v.allergies = x);
        num("chance", 0.18, 0.0, 1.0, Kind.SINGLE, v -> v.allergyChance, (v, x) -> v.allergyChance = (float) x,
                "Share of bodies that arrive with one. Even odds between the three.");
    }

    private static void grafts() {
        section("grafts",
                "Whether a limb that comes off lands on the floor, and can go back on.",
                "",
                "Losing one here was final in the flattest way: the arm stopped existing",
                "the moment it came off. That is true of an amputation somebody chose and",
                "not of the other way limbs come off - something takes one off you, and",
                "the thing it took is lying four feet away.",
                "",
                "So it drops, with a clock on it. What decides whether it can go back on",
                "happened before anybody opened a screen: how long it has been off, and",
                "whether anyone thought to keep it cold.",
                "",
                "It makes nothing survivable that was not. The stump bleeds the same and",
                "the shock arrives the same - what it adds is a second clock running",
                "beside the first, and a decision about which to spend the minute on.");
        flag("enabled", true, v -> v.grafts, (v, x) -> v.grafts = x);
        num("graftSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.graftSeconds, (v, x) -> v.graftSeconds = (int) x,
                "How long a limb is worth sewing back on, warm.");
        num("graftCold", 0.33, 0.01, 1.0, Kind.SINGLE, v -> v.graftColdFactor, (v, x) -> v.graftColdFactor = (float) x,
                "",
                "And how much slower that clock runs in water or snow. A third is the",
                "real advice: a severed hand travels to hospital on ice, and it is the",
                "one piece of first aid about this that everybody has heard and nobody",
                "expects a game to know.");
        num("graftRestore", 0.45, 0.05, 1.0, Kind.SINGLE, v -> v.graftRestore, (v, x) -> v.graftRestore = (float) x,
                "",
                "What a limb comes back on, as a share of what it was, before the",
                "surgery is taken into account. It comes back stiff and has to be rested",
                "like a set bone, and it comes back dirty - it has been on the floor.");
    }

    private static void parasites() {
        section("parasites",
                "Worms, from what was eaten.",
                "",
                "The commonest illness in the world and the one nobody puts in a game,",
                "because there is no scene in it. It does not burn, it opens nothing and",
                "it will not kill anybody this afternoon. It takes: it eats before you do",
                "and drinks off the blood while it is there, and what it leaves is",
                "somebody hungry and short and tired for weeks with no dramatic moment in",
                "the whole business.",
                "",
                "Which is why it belongs here. Nothing else in this mod is slow.",
                "",
                "It does not pass on its own - nothing about a parasite is self-limiting -",
                "and a bottle of antibiotics does nothing for it. What answers it is the",
                "other card, which answers nothing else.");
        num("chance", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.parasiteChance, (v, x) -> v.parasiteChance = (float) x,
                "Chance that something eaten badly was carrying them.");
        num("hungerPerSecond", 0.08, 0.0, 10.0, Kind.SINGLE, v -> v.parasiteHunger, (v, x) -> v.parasiteHunger = (float) x,
                "Food burnt per second while they are in there. They eat first.");
        num("drainPerSecond", 0.0006, 0.0, 1.0, Kind.SINGLE, v -> v.parasiteDrain, (v, x) -> v.parasiteDrain = (float) x,
                "And volume taken per second, which is what makes it anaemia.");
    }

    private static void diseases() {
        section("diseases",
                "Illness, as opposed to injury: it belongs to the whole body and no",
                "bandage will touch it. A fever left alone becomes blood poisoning, and",
                "that one does not pass by itself. Antibiotics cure any of them.");
        flag("enabled", true, v -> v.diseases, (v, x) -> v.diseases = x);
        num("coldSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.coldSeconds, (v, x) -> v.coldSeconds = (int) x,
                "How long a cold lasts before it passes on its own.");
        num("poisoningSeconds", 180, 1, 100000, Kind.WHOLE, v -> v.poisoningSeconds, (v, x) -> v.poisoningSeconds = (int) x);
        num("feverSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.feverSeconds, (v, x) -> v.feverSeconds = (int) x,
                "How long a fever runs before it becomes sepsis. This is the deadline",
                "the whole infection chain exists to impose.");
        num("sepsisDamagePerSecond", 0.3, 0.0, 100.0, Kind.SINGLE, v -> v.sepsisDamage, (v, x) -> v.sepsisDamage = (float) x,
                "Damage done to the thorax each second by blood poisoning.");
        num("poisoningDamagePerSecond", 0.12, 0.0, 100.0, Kind.SINGLE, v -> v.poisoningDamage, (v, x) -> v.poisoningDamage = (float) x,
                "Damage done to the stomach each second while poisoned.");
        num("coldRiskPerSecond", 0.02, 0.0, 1.0, Kind.SINGLE, v -> v.coldRisk, (v, x) -> v.coldRisk = (float) x,
                "Chance per second of catching a cold while freezing or soaked in a",
                "cold place. A cold is mild, but it doubles the chance of a wound going bad.");
        num("foodPoisonChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.foodPoisonChance, (v, x) -> v.foodPoisonChance = (float) x,
                "Chance of poisoning from eating something that deserved it -",
                "rotten flesh, raw chicken, spider eyes, pufferfish.");
    }

    private static void diseasesConcussion() {
        section("diseases.concussion",
                "",
                "A hard knock to the head. Nothing about the head itself changes - what it",
                "costs is knowing your own condition: while it lasts, every number the mod",
                "would show you is a question mark and the colours are all you have.",
                "No medicine touches it and none needs to; it wears off.");
        num("concussionSeconds", 75, 1, 100000, Kind.WHOLE, v -> v.concussionSeconds, (v, x) -> v.concussionSeconds = (int) x);
        num("concussionBlow", 6.0, 0.0, 1000.0, Kind.SINGLE, v -> v.concussionBlow, (v, x) -> v.concussionBlow = (float) x,
                "Damage to the head in one blow that can rattle it. 0 turns this off.");
        num("concussionChance", 0.4, 0.0, 1.0, Kind.SINGLE, v -> v.concussionChance, (v, x) -> v.concussionChance = (float) x,
                "Chance that a blow that hard actually does it.");
    }

    private static void diseasesShock() {
        section("diseases.shock",
                "",
                "Circulation giving out, which is the half of bleeding this mod did not",
                "have. Losing blood used to be a penalty you could walk off; shock is",
                "what kills people who have already been bandaged.",
                "",
                "It arrives on volume, not on damage, and it does not run down a clock.",
                "It lifts when the reason for it is gone - nothing still open, volume back",
                "under the lower mark - and only while the patient stays still. That is",
                "the argument it exists to have: they will want to get up and keep going,",
                "and getting up is what stops them recovering.");
        flag("enabled", true, v -> v.shock, (v, x) -> v.shock = x);
        num("bloodLoss", 0.30, 0.05, 1.0, Kind.SINGLE, v -> v.shockBloodLoss, (v, x) -> v.shockBloodLoss = (float) x,
                "Share of blood gone that tips a body into shock.",
                "",
                "Real shock starts around a fifth to a third of it, and the first number",
                "tried here was nearly half - which meant a heavy chest bleed emptied the",
                "chest and killed the player about fifteen seconds before shock could",
                "arrive. A threshold nothing ever reaches is not a threshold.");
        num("clearBloodLoss", 0.18, 0.0, 1.0, Kind.SINGLE, v -> v.shockClearBloodLoss, (v, x) -> v.shockClearBloodLoss = (float) x,
                "And the share it has to come back under before it can lift.",
                "Deliberately lower than the one above: a body that comes out of shock",
                "the instant it crosses the same line would flicker in and out of it.");
        num("restSeconds", 30, 1, 100000, Kind.WHOLE, v -> v.shockRestSeconds, (v, x) -> v.shockRestSeconds = (int) x,
                "Seconds of lying still, with nothing bleeding, before it lifts.",
                "Sprinting and jumping start the count over.");
        num("blackedParts", 2, 0, 7, Kind.WHOLE, v -> v.shockBlackedParts, (v, x) -> v.shockBlackedParts = (int) x,
                "How many parts have to be gone at once for the body to go into shock",
                "on their own, with no blood loss at all. This is the crush injury: enough",
                "of somebody destroyed and the circulation gives up whether or not any of",
                "it is leaking. 0 turns that route off.");
        num("downedRate", 1.5, 1.0, 10.0, Kind.SINGLE, v -> v.shockDownedRate, (v, x) -> v.shockDownedRate = (float) x,
                "How much faster the clock runs on a downed player who is in shock.",
                "This is where it bites hardest, and it should: shock is the reason two",
                "people with the same wound have different amounts of time.");
    }

    private static void diseasesHypothermia() {
        section("diseases.hypothermia",
                "",
                "Cold that has stopped being a nuisance. It does little by itself; what it",
                "does is stop blood setting, so every light cut that would have clotted on",
                "its own keeps running instead.",
                "",
                "Blood loss makes it far easier to catch, which is not a lever - a body",
                "short of blood cannot keep itself warm. The two feed each other, and that",
                "is why the first thing to do with somebody on a cold floor is get them",
                "off the cold floor.");
        flag("enabled", true, v -> v.hypothermia, (v, x) -> v.hypothermia = x);
        num("riskPerSecond", 0.015, 0.0, 1.0, Kind.SINGLE, v -> v.hypothermiaRisk, (v, x) -> v.hypothermiaRisk = (float) x,
                "Chance per second while freezing, or while soaked somewhere cold.",
                "Doubled again for every quarter of their blood a player has lost.");
        num("warmSeconds", 45, 1, 100000, Kind.WHOLE, v -> v.hypothermiaWarmSeconds, (v, x) -> v.hypothermiaWarmSeconds = (int) x,
                "Seconds of being warm and dry before it lifts. Standing in the",
                "light of a fire halves it; being cold or wet starts it over.");
    }

    private static void diseasesReaction() {
        section("diseases.reaction",
                "",
                "What the wrong blood does. Every player is given a blood type, rerolled",
                "every time they respawn, and blood may only go into somebody who already",
                "has every antigen it carries - the real ABO and Rh rules, which make O-",
                "worth keeping and AB+ a lucky thing to be.",
                "",
                "Getting it wrong has to be worse than doing nothing at all, or reading",
                "the label before hanging the bag would be optional.");
        num("reactionSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.reactionSeconds, (v, x) -> v.reactionSeconds = (int) x);
        num("reactionDamagePerSecond", 0.45, 0.0, 100.0, Kind.SINGLE, v -> v.reactionDamage, (v, x) -> v.reactionDamage = (float) x,
                "Damage done to the thorax each second while it runs.");
        num("reactionBloodLossPerSecond", 0.006, 0.0, 1.0, Kind.SINGLE, v -> v.reactionBloodLoss, (v, x) -> v.reactionBloodLoss = (float) x,
                "Blood lost each second on top of that: the transfusion running",
                "backwards as the body destroys what was put into it.");
    }

    private static void chest() {
        section("chest",
                "A broken rib pushed the wrong way, and now there is air where the lung",
                "wants to be. It is the only injury here with nothing to see: nothing",
                "bleeds and nothing is bent, and the only way to find it is to read a",
                "chart. It is also the only one that gets worse the longer it is left",
                "rather than at a steady rate, because the pressure keeps building.",
                "",
                "Nothing heals it and nothing waits it out. A needle between the ribs lets",
                "the air out, and that is the whole treatment.");
        flag("enabled", true, v -> v.pneumothorax, (v, x) -> v.pneumothorax = x);
        num("blowThreshold", 9.0, 0.1, 1000.0, Kind.SINGLE, v -> v.pneumoBlow, (v, x) -> v.pneumoBlow = (float) x,
                "Damage to the chest in one hit that can do it. Only ever after the",
                "ribs have already gone - this is what a broken one does next.");
        num("chance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.pneumoChance, (v, x) -> v.pneumoChance = (float) x);
        num("damagePerSecond", 0.25, 0.0, 100.0, Kind.SINGLE, v -> v.pneumoDamage, (v, x) -> v.pneumoDamage = (float) x,
                "Damage to the chest each second when it starts.");
        num("rampSeconds", 75, 1, 100000, Kind.WHOLE, v -> v.pneumoRampSeconds, (v, x) -> v.pneumoRampSeconds = (int) x,
                "Seconds over which that damage climbs to three times itself. This is",
                "the deadline: left alone it stops being survivable well before the",
                "infection chain would have got anywhere.");
    }

    private static void effects() {
        section("effects",
                "What a wrecked body does to the player it belongs to.");
        flag("enabled", true, v -> v.effects, (v, x) -> v.effects = x,
                "Turn off to keep the health system and drop all the penalties.");
        num("thoraxRegenPenalty", 1.0, 0.0, 1.0, Kind.SINGLE, v -> v.regenPenalty, (v, x) -> v.regenPenalty = (float) x,
                "How strongly a damaged thorax slows healing down.",
                "0 disables it, 1 means a thorax at half health heals at half speed.");
        num("painkillerSeconds", 45, 1, 100000, Kind.WHOLE, v -> v.painkillerSeconds, (v, x) -> v.painkillerSeconds = (int) x,
                "How long one painkiller hides the penalties. It heals nothing.");
        num("painkillerFalloff", 0.6, 0.05, 1.0, Kind.SINGLE, v -> v.painkillerFalloff, (v, x) -> v.painkillerFalloff = (float) x,
                "What each further course is worth, as a share of the one before,",
                "while the last is still wearing off.",
                "",
                "Without this the pills are a button with no cost - swallow one whenever",
                "anything hurts. At the default the fourth in a row is barely worth",
                "swallowing, which makes them a thing you spend rather than a thing you",
                "hold down. 1.0 turns the whole idea off.");
        num("painkillerToleranceSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.painkillerToleranceSeconds, (v, x) -> v.painkillerToleranceSeconds = (int) x,
                "How long it takes to shake off one step of that, once the last",
                "course has run out. Tolerance does not fall while pills are still in.");
        flag("heartbeat", true, v -> v.heartbeat, (v, x) -> v.heartbeat = x,
                "Whether a body low on blood can be heard as well as read. Only the",
                "player it belongs to ever hears it, and it quickens as it gets worse.");
        flag("assess", true, v -> v.assess, (v, x) -> v.assess = x,
                "Whether right-clicking another player with an empty hand shows their",
                "condition without treating them. Sorting the wounded is the thing the",
                "mod is named after, and it needs somewhere to read them from.");
        flag("tags", true, v -> v.tags, (v, x) -> v.tags = x,
                "",
                "Whether a player who has been looked over can be marked, and whether",
                "those marks show on nameplates along with who is on the floor.",
                "",
                "A look is worth nothing to anybody but the person doing the looking",
                "until there is somewhere to put the answer. This is that somewhere: one",
                "of four colours, written on a tag, read across a field by everybody",
                "else. The mod never decides which colour is correct and never will -",
                "that decision is the thing the whole feature exists to make somebody",
                "make.",
                "",
                "Off, tags are inert paper and nobody's nameplate says anything.");
        num("tagStaleSeconds", 120, 5, 100000, Kind.WHOLE, v -> v.tagStaleSeconds, (v, x) -> v.tagStaleSeconds = (int) x,
                "How long a mark reads as current. After this it keeps its colour and",
                "gains a question mark, and the mod stops claiming to know how old it is.",
                "",
                "The point of the number is that a mark is a snapshot of a body that has",
                "carried on being a body. Set it long and the group will trust old notes;",
                "set it short and somebody has to keep walking back.");
        flag("minigame", true, v -> v.minigame, (v, x) -> v.minigame = x,
                "Whether treating a wound opens the screen where you have to do it by hand.",
                "Turn it off and medicine works the moment it is used, on whichever",
                "part needs it most. Painkillers never open it either way.");
        num("interruptDamage", 3.0, 0.0, 100.0, Kind.SINGLE, v -> v.interruptDamage, (v, x) -> v.interruptDamage = (float) x,
                "How much damage it takes to knock a player off a treatment, measured",
                "on the vanilla health bar. A scratch or a tick of fire should not cost",
                "a bandage; being set upon should.",
                "",
                "0 means treatments are never interrupted. Swallowing pills - painkillers",
                "and antibiotics - is never interrupted either way, because being ill is",
                "exactly the state you cannot fight your way out of first.");
        flag("downed", true, v -> v.downed, (v, x) -> v.downed = x,
                "",
                "Whether emptying a vital part puts the player on the floor with a clock",
                "running instead of killing them outright. Somebody else can open them up",
                "with a surgical kit and get them back; nobody coming is what turns it",
                "into a death.",
                "",
                "On your own this is a dramatic last few seconds rather than a rescue,",
                "so a solo player may well want it off.");
        num("downedSeconds", 45, 1, 100000, Kind.WHOLE, v -> v.downedSeconds, (v, x) -> v.downedSeconds = (int) x,
                "How long there is to save them.");
        flag("downedIgnoredByMobs", true, v -> v.downedIgnoredByMobs, (v, x) -> v.downedIgnoredByMobs = x,
                "Whether mobs lose interest in a player who is already down.",
                "With this off, whatever put them there simply finishes the job and the",
                "rescue the whole feature exists for never gets to happen.");
        num("downedDamageSeconds", 3.0, 0.0, 1000.0, Kind.SINGLE, v -> v.downedDamageSeconds, (v, x) -> v.downedDamageSeconds = (float) x,
                "Seconds taken off the clock per point of damage dealt to a player who",
                "is already down. Without this, lying there is the safest place in the",
                "game: nothing can finish you and only the timer can.");
        num("stabiliseSeconds", 30, 0, 100000, Kind.WHOLE, v -> v.stabiliseSeconds, (v, x) -> v.stabiliseSeconds = (int) x,
                "",
                "How long a dressing holds the clock on somebody who is down.",
                "",
                "Packing a wound does not stand them up - only surgery does that - but",
                "it stops the count while it holds. Without it a medic sixty seconds",
                "away is a medic who arrives to a body, and the whole rescue only ever",
                "worked at knife range.",
                "",
                "A hit undoes the packing. 0 turns the whole thing off.");
        num("stabiliseFalloff", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.stabiliseFalloff, (v, x) -> v.stabiliseFalloff = (float) x,
                "What each further hold on the same downing is worth, as a share of",
                "the one before. This is what stops bandages being immortality: at the",
                "default the second hold is half as long and the fourth is barely worth",
                "the bandage.");
        flag("ragdoll", true, v -> v.ragdoll, (v, x) -> v.ragdoll = x,
                "",
                "Whether a downed player goes limp for real, when Sable: Ragdolls is",
                "installed. Does nothing at all without it, and Triage never asks for it.",
                "",
                "Triage's own version of lying down is a forced swimming pose: the same",
                "shape whether you were blown off a roof or bled out standing still. With",
                "the physics mod there, the body falls instead - and it cannot be got up",
                "from, which is the rule this mod has always had about being down, only",
                "now it is the same rule the player can see.",
                "",
                "Off, the pose comes back and the physics mod is left alone.");
        flag("carry", true, v -> v.carry, (v, x) -> v.carry = x,
                "",
                "Whether a downed player can be picked up and carried: Shift + right click",
                "with an empty hand, and the give-up key sets them down again.",
                "",
                "Without it the only surgery available is surgery where they fell - in the",
                "fire, in the open, in front of whatever put them there. It costs the",
                "carrier their hands and most of their speed, so it is a decision rather",
                "than a free extra.");
        num("carrySlow", 0.45, 0.0, 0.95, Kind.DOUBLE, v -> v.carrySlow, (v, x) -> v.carrySlow = x,
                "How much slower the carrier moves. A body is heavy.");
        num("downedCarriedRate", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.downedCarriedRate, (v, x) -> v.downedCarriedRate = (float) x,
                "How fast the clock runs while somebody is carrying them, as a share",
                "of normal. Being carried is not treatment and heals nothing, but somebody",
                "has hold of them, and at the full rate carrying anybody anywhere would",
                "cost more time than it could ever buy.",
                "",
                "0 stops the clock entirely while they are being carried.");
        flag("airway", true, v -> v.airway, (v, x) -> v.airway = x,
                "",
                "Whether an unconscious body stops breathing for itself.",
                "",
                "A player on the floor is not holding their own airway open, and after a",
                "while air stops getting past. It costs nothing to fix and cannot be",
                "fixed once: somebody has to kneel down, open it, and come back and do it",
                "again. It is the only thing in the mod that asks for a person rather",
                "than for an item, which is why it is here.",
                "",
                "You can hear it from a few blocks away. That is deliberate - everything",
                "else about a body has to be gone and read on purpose, and somebody who",
                "cannot breathe is a noise rather than a fact.");
        num("airwaySeconds", 20, 1, 100000, Kind.WHOLE, v -> v.airwaySeconds, (v, x) -> v.airwaySeconds = (int) x,
                "How long air keeps getting in with nobody holding it open. The count",
                "starts when they go down and goes back to nothing every time somebody",
                "opens it.");
        num("airwayDownedRate", 2.0, 1.0, 100.0, Kind.SINGLE, v -> v.airwayDownedRate, (v, x) -> v.airwayDownedRate = (float) x,
                "How much faster the clock runs while they are not breathing. This is",
                "the largest single thing that can be done about somebody dying on the",
                "floor, and it is free, which is exactly how it is in life.",
                "",
                "It counts against the arrest window at the same rate, so it matters more",
                "after the heart stops rather than less: compressions on a body no air is",
                "reaching only move the same blood round.");
        flag("cpr", true, v -> v.cpr, (v, x) -> v.cpr = x,
                "",
                "Whether the clock running out stops the heart instead of ending the",
                "player.",
                "",
                "In arrest there is no clock left of their own - what is left is a window",
                "somebody else holds open by hand, one round of compressions at a time.",
                "Compressions never stand anybody up: they buy the minute in which",
                "somebody with a surgical kit can arrive, and nothing else.",
                "",
                "Off, the clock running out kills as it used to.");
        num("arrestSeconds", 25, 1, 100000, Kind.WHOLE, v -> v.arrestSeconds, (v, x) -> v.arrestSeconds = (int) x,
                "How long one round of compressions buys.");
        num("cprFalloff", 0.6, 0.0, 1.0, Kind.SINGLE, v -> v.cprFalloff, (v, x) -> v.cprFalloff = (float) x,
                "What each further round is worth, as a share of the one before. This is",
                "what stops one pair of patient hands being immortality: at the default",
                "the fourth round is worth about a fifth of the first.");
        num("cprRibChance", 0.3, 0.0, 1.0, Kind.SINGLE, v -> v.cprRibChance, (v, x) -> v.cprRibChance = (float) x,
                "Chance per round of breaking one of their ribs. A chest pushed hard",
                "enough to move blood is a chest that cracks, and the patient wakes up",
                "with the evidence - which is the price of the answer, not a punishment",
                "for giving it.");
        flag("pressure", true, v -> v.pressure, (v, x) -> v.pressure = x,
                "",
                "Whether a bare hand pressed on a wound stops it bleeding while it is",
                "there.",
                "",
                "The first thing anybody is taught about bleeding, and the last thing",
                "this mod had: before it, a bleeding teammate and no bandage was a",
                "bleeding teammate and nothing at all. It closes nothing and heals",
                "nothing - it buys the walk to the dressing, at the price of standing",
                "there. A light bleed goes on clotting underneath, so one held long",
                "enough does close on its own.");
        num("pressureSeconds", 12, 1, 100000, Kind.WHOLE, v -> v.pressureSeconds, (v, x) -> v.pressureSeconds = (int) x,
                "How long one hand stays on before it has to be put back.");
        flag("schizophrenia", true, v -> v.schizophrenia, (v, x) -> v.schizophrenia = x,
                "",
                "Whether some bodies arrive with it.",
                "",
                "The only condition in the mod that nobody catches and nothing",
                "caused: it is rolled with the blood type, at the same moment, and",
                "for the same reason - it is a fact about a person rather than an",
                "event in their day.",
                "",
                "What it does is not what films do with it. The voices comment and",
                "criticise; they never give orders, because the version that does is",
                "the most repeated untruth about this illness. The heaviest part of",
                "it here is the part with no drama in it: a body that does not want",
                "to do anything, and mends more slowly for it.",
                "",
                "It is treatable and not curable. A course holds it down while it is",
                "being taken and costs enough that stopping is a real temptation.",
                "",
                "On by default, as it was when it was added. Some players do not",
                "want a condition that cannot be caught, prevented or examined",
                "away; turn it off if the group does not want that story in their",
                "world.");
        num("schizophreniaChance", 0.1, 0.0, 1.0, Kind.SINGLE, v -> v.schizophreniaChance, (v, x) -> v.schizophreniaChance = (float) x,
                "The share of bodies that have it, when it is enabled.",
                "",
                "The real figure is under one in a hundred, which in a game of four",
                "people means nobody ever meets it. The default here is deliberately",
                "higher than that: it is a condition some people have, and a rate",
                "nobody ever meets is not a feature.",
                "",
                "1.0 gives it to everybody. 0.0 is the same as turning it off.");
        num("antipsychoticSeconds", 600, 1, 100000, Kind.WHOLE, v -> v.antipsychoticSeconds, (v, x) -> v.antipsychoticSeconds = (int) x,
                "How long one course holds the symptoms down.");
        num("reboundSeconds", 60, 0, 100000, Kind.WHOLE, v -> v.reboundSeconds, (v, x) -> v.reboundSeconds = (int) x,
                "How long the symptoms come back worse for after a course runs out.",
                "",
                "This is the reason people stop taking it and then wish they had not,",
                "and it is why the choice in this mod is not 'treat or do not' but",
                "'which of the two do you put up with'.");
        flag("delirium", true, v -> v.delirium, (v, x) -> v.delirium = x,
                "",
                "Whether a body that has stopped supplying its head starts getting",
                "things wrong.",
                "",
                "Not an illness anybody catches and not one anybody treats. It is as",
                "true as its causes - blood gone, air not getting in, a temperature",
                "that has stopped the works, or the body poisoning itself - and it",
                "goes when they do. The point of it is that it is the only thing in",
                "the mod that makes your own chart lie to you: concussion makes the",
                "numbers coarse, this makes them wrong. On somebody else it reads as",
                "'whatever is wrong with them has reached their head'.",
                "",
                "You will also hear things that are not there. Only you.");
        num("deliriumBloodLoss", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.deliriumBloodLoss, (v, x) -> v.deliriumBloodLoss = (float) x,
                "How much volume has to be gone before the head starts missing things.");
        num("deliriumClearSeconds", 20, 0, 100000, Kind.WHOLE, v -> v.deliriumClearSeconds, (v, x) -> v.deliriumClearSeconds = (int) x,
                "How long after the cause is dealt with before somebody is back in",
                "their own senses. Short, and not zero: a volume sitting on the",
                "threshold would otherwise switch them in and out once a second.");
        num("whisperSeconds", 25, 0, 100000, Kind.WHOLE, v -> v.whisperSeconds, (v, x) -> v.whisperSeconds = (int) x,
                "Roughly how many seconds between things that are not said, while not on",
                "a course. Twice as often in the stretch after one runs out.",
                "",
                "Four kinds, in equal measure: a noise from where nothing is, a line",
                "of chat with nobody attached to it, a word across the middle of the",
                "screen, and a message with another player's name on it that they did",
                "not write. None of them is an instruction - real voices comment and",
                "criticise, and the ones that give orders are a thing films made up.",
                "",
                "All of it happens on the one client. Nobody else hears, reads or",
                "sees any of it, least of all the player whose name gets borrowed.",
                "",
                "0 turns them off and leaves the figures.");
        num("phantomSeconds", 180, 0, 100000, Kind.WHOLE, v -> v.phantomSeconds, (v, x) -> v.phantomSeconds = (int) x,
                "Roughly how many seconds between somebody who is not there, while not",
                "on a course. Twice as often in the stretch after one runs out.",
                "",
                "It is a person and not a monster, it looks like somebody really on",
                "the server, it never comes closer and it is gone the moment you turn",
                "towards it. Nobody else can see it, because there is nothing to see.",
                "",
                "0 turns them off and leaves the voices.");
        num("catatoniaChance", 0.0008, 0.0, 1.0, Kind.SINGLE, v -> v.catatoniaChance, (v, x) -> v.catatoniaChance = (float) x,
                "Chance per second of the illness stopping somebody outright, while",
                "they are not on a course. Three times that in the stretch after one",
                "has run out.",
                "",
                "It takes movement away completely and passes on its own. Nothing in",
                "the box touches it, which makes it the second thing in this mod",
                "whose only answer is somebody else being there.");
        num("catatoniaSeconds", 30, 1, 100000, Kind.WHOLE, v -> v.catatoniaSeconds, (v, x) -> v.catatoniaSeconds = (int) x,
                "How long it lasts.");
        num("neurolepticChance", 0.03, 0.0, 1.0, Kind.SINGLE, v -> v.neurolepticChance, (v, x) -> v.neurolepticChance = (float) x,
                "",
                "Chance that a course causes the reaction that kills.",
                "",
                "Rare, real, and the sharpest form of this mod's oldest argument:",
                "every treatment is a trade, and this is the one where the trade can",
                "be the thing that ends you. There is no antidote - stop the course",
                "and get cold.");
        num("neurolepticDamage", 0.5, 0.0, 100.0, Kind.SINGLE, v -> v.neurolepticDamage, (v, x) -> v.neurolepticDamage = (float) x,
                "What it costs the whole body per second while it runs.");
        num("neurolepticCoolSeconds", 25, 1, 100000, Kind.WHOLE, v -> v.neurolepticCoolSeconds, (v, x) -> v.neurolepticCoolSeconds = (int) x,
                "How long off the course and cold - in water, in the rain, in the",
                "snow - before it breaks.");
        flag("organs", true, v -> v.organs, (v, x) -> v.organs = x,
                "",
                "Whether a hard blow to a torso can tear something inside it.",
                "",
                "The injury this mod did not have and trauma medicine is mostly about:",
                "the one that does not bleed anywhere anybody can see. From the moment",
                "it happens the patient is losing blood into their own abdomen or chest,",
                "and nothing outside them shows it. No wound on the figure. No dressing",
                "that helps. No clot, ever, because there is nothing outside for a clot",
                "to close.",
                "",
                "It is on the chart, because the chart has never lied - it is simply not",
                "on the FIGURE, the same as a crushed lung. This is the injury found by",
                "reading rather than by looking, and it is the one that kills somebody",
                "who was walking and talking a minute ago.",
                "",
                "One thing stops it, and it is a surgeon.");
        num("organBlow", 11.0, 0.0, 1000.0, Kind.SINGLE, v -> v.organBlow, (v, x) -> v.organBlow = (float) x,
                "How hard a single blow has to be before it can tear something.");
        num("organChance", 0.30, 0.0, 1.0, Kind.SINGLE, v -> v.organChance, (v, x) -> v.organChance = (float) x,
                "And the chance that it does.");
        num("heartStopChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.organHeartStopChance, (v, x) -> v.organHeartStopChance = (float) x,
                "",
                "Chance that tearing the heart stops it, rather than merely",
                "bleeding into the chest.",
                "",
                "The heart is the one organ in the mod that does not give",
                "anybody three minutes. It tears at the same threshold as the",
                "rest, and when it does this is the chance the player goes",
                "down with an arrest instead of simply losing volume. CPR",
                "and a surgeon are the only answers, so with CPR disabled this",
                "does nothing and the tear is an ordinary - which is to say",
                "still very bad - internal bleed.");
        num("organBleedPerSecond", 0.007, 0.0, 1.0, Kind.SINGLE, v -> v.organBleed, (v, x) -> v.organBleed = (float) x,
                "Share of volume lost each second while something is torn.",
                "",
                "Between a light bleed and a heavy one on purpose: it is slower than an",
                "artery and it never, ever stops, which over a few minutes is worse.");
        num("organDamagePerSecond", 0.12, 0.0, 100.0, Kind.SINGLE, v -> v.organDamage, (v, x) -> v.organDamage = (float) x,
                "What a torn organ costs its own part each second, on top of that.");
        num("organSepsisChance", 0.012, 0.0, 1.0, Kind.SINGLE, v -> v.organSepsis, (v, x) -> v.organSepsis = (float) x,
                "Chance per second that a torn gut turns into blood poisoning.",
                "",
                "The one organ whose danger is not the bleeding. What comes out of a gut",
                "is not blood, and the thing that kills people is three minutes later.");
        num("organHealSeconds", 150, 1, 100000, Kind.WHOLE, v -> v.organHealSeconds, (v, x) -> v.organHealSeconds = (int) x,
                "How long a bruised one takes to settle on its own.",
                "Only the bruised kind. The torn kind waits for a surgeon and nothing else.");
        flag("reperfusion", true, v -> v.reperfusion, (v, x) -> v.reperfusion = x,
                "",
                "Whether letting a long-strapped limb go costs the rest of the body.",
                "",
                "The real complication of a tourniquet, and the one nobody expects: what",
                "has been building up in a limb with no blood moving through it goes",
                "everywhere the moment the strap comes off. The longer it was on, the",
                "more there is of it.",
                "",
                "Which turns the strap into the argument it should have been all along.",
                "Taken off early it costs nothing. Left until the clock is nearly out, it",
                "is dangerous to take off at all - and the limb under it is dying anyway.",
                "That is the whole road from a strap to a saw, and it is the road the",
                "real one goes down.",
                "",
                "Nothing at all under a third of the clock. A bandage over the wound",
                "releases the strap too, so it is charged the same: releasing is",
                "releasing.");
        num("reperfusionDamage", 7.0, 0.0, 1000.0, Kind.SINGLE, v -> v.reperfusionDamage, (v, x) -> v.reperfusionDamage = (float) x,
                "What a strap left the whole clock costs the body when it comes off.",
                "Spread over everything, and scaled by how much of the clock had run.");
        num("dressingStuckSeconds", 120, 1, 100000, Kind.WHOLE, v -> v.dressingStuckSeconds, (v, x) -> v.dressingStuckSeconds = (int) x,
                "How long before a dressing has dried onto what is under it.",
                "",
                "Fingers can take a fresh one off cleanly. Past this it is stuck to the",
                "wound by the thing that was trying to close it, and pulling it away",
                "takes that with it - so a dressing left on is not only dirty, it is",
                "harder to get off. Scissors do not care either way, which is the whole",
                "reason to carry a pair.");
        flag("anaesthesia", true, v -> v.anaesthesia, (v, x) -> v.anaesthesia = x,
                "",
                "Whether somebody can be put under, and whether it matters.",
                "",
                "Two treatments in this mod are done to a patient who is awake and can",
                "feel every part of it: a surgical kit opening them up, and a saw taking",
                "their leg off. Until now the mod did not mention that. It does now, and",
                "the mention has a price attached: operating on somebody conscious caps",
                "how well it can possibly go, hurts them on top of the operation, and can",
                "put them into shock from the pain alone.",
                "",
                "Nobody can put themselves under and then operate - being under is being",
                "unable to do anything - so this is the first item in the mod whose whole",
                "value is what it lets somebody ELSE do properly.",
                "",
                "Off, an operation is what it was: nobody minds, and nobody is asked to.");
        num("anaesthesiaSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.anaesthesiaSeconds, (v, x) -> v.anaesthesiaSeconds = (int) x,
                "How long a good dose keeps somebody under, in seconds.",
                "A poor one is worth a fraction of it.");
        num("anaesthesiaArrestChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.anaesthesiaArrest, (v, x) -> v.anaesthesiaArrest = (float) x,
                "Chance a badly given dose stops their breathing altogether.",
                "",
                "Scaled by how badly. This is the only thing in the mod that can kill a",
                "patient by working too well, and the answer to it is the answer the mod",
                "already has: hands on the chest.");
        num("awakeQualityCap", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.awakeQualityCap, (v, x) -> v.awakeQualityCap = (float) x,
                "The best an operation on a conscious patient can go, however well",
                "the hands did. They are moving, and no amount of skill fixes that.");
        num("awakeDamage", 3.0, 0.0, 1000.0, Kind.SINGLE, v -> v.awakeDamage, (v, x) -> v.awakeDamage = (float) x,
                "What that costs the part, on top of the operation itself.");
        flag("dressings", true, v -> v.dressings, (v, x) -> v.dressings = x,
                "",
                "Whether a dressing stays on, and goes bad if it is left there.",
                "",
                "A bandage used to be an event: it went on, the bleeding stopped,",
                "and that was the end of it. Now it is a thing on somebody. While it",
                "is fresh it is the best item in the box - nothing under a clean",
                "dressing can go bad at all, which is the one preventive thing in the",
                "whole mod. Left on past its time it becomes what is keeping the dirt",
                "in, which is the commonest way a treated wound turns into an infected",
                "one, and the infection risk starts running with no wait at all.",
                "",
                "The answer is another bandage: the dirty one comes off with it.",
                "It also comes off by itself once there is nothing left underneath",
                "worth dressing, so nobody is changing the bandage on a healed arm.",
                "",
                "Off, a bandage goes back to being an event and nothing is tracked.");
        num("dressingCleanSeconds", 480, 1, 100000, Kind.WHOLE, v -> v.dressingCleanSeconds, (v, x) -> v.dressingCleanSeconds = (int) x,
                "How long a dressing stays clean, in seconds.");
        flag("necrosis", true, v -> v.necrosis, (v, x) -> v.necrosis = x,
                "",
                "Whether a limb that has stopped being fed can die on the body.",
                "",
                "Two roads reach it, and a player chooses both of them. A tourniquet",
                "stops the bleeding and starts a clock; something festering in a limb",
                "nobody cleaned starts the same one. Neither is a surprise and both are",
                "reversible right up until they are not - the count runs back down as",
                "soon as the reason for it is dealt with.",
                "",
                "What is at the end of it is the first state in the mod that nothing",
                "repairs. A dead limb takes no treatment, mends nothing, and poisons",
                "the body it is attached to until it comes off.",
                "",
                "Off, a strap is what it was before: a slow cost with no deadline.");
        num("tourniquetDeadSeconds", 180, 1, 100000, Kind.WHOLE, v -> v.strapDeadSeconds, (v, x) -> v.strapDeadSeconds = (int) x,
                "How long a tourniquet can stay on before what is under it dies.",
                "",
                "This is the number that gives the strap its teeth. It is generous by the",
                "standards of the real thing and short by the standards of forgetting.");
        num("rotDeadSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.rotDeadSeconds, (v, x) -> v.rotDeadSeconds = (int) x,
                "How long a limb can carry a severe infection before it dies.");
        num("deadLimbDrain", 0.20, 0.0, 100.0, Kind.SINGLE, v -> v.deadLimbDrain, (v, x) -> v.deadLimbDrain = (float) x,
                "Damage the chest takes each second for every dead limb still attached.",
                "",
                "Half again what a merely blacked-out one costs, and the smaller half of",
                "what a dead limb does: the rest of it arrives as the blood going bad,",
                "below. Between them they are meant to be an emergency with a way out of",
                "it - long enough to find a saw, put a strap on and use both.");
        num("deadLimbSepsisChance", 0.010, 0.0, 1.0, Kind.SINGLE, v -> v.deadLimbSepsis, (v, x) -> v.deadLimbSepsis = (float) x,
                "Chance per second that a dead limb starts poisoning the blood.");
        flag("amputation", true, v -> v.amputation, (v, x) -> v.amputation = x,
                "",
                "Whether a dead limb can be taken off.",
                "",
                "The only treatment in the mod that removes something rather than",
                "repairing it, and the only one whose result is permanent. It is offered",
                "on nothing but a limb that has already died, so it is never the clever",
                "shortcut - by the time the saw is an option, everything else has already",
                "stopped being one.",
                "",
                "Off, a dead limb stays on the body and keeps costing, which is a slower",
                "death and nothing else.");
        num("amputationBloodLoss", 0.12, 0.0, 1.0, Kind.SINGLE, v -> v.amputationBloodLoss, (v, x) -> v.amputationBloodLoss = (float) x,
                "Share of volume the operation costs, done properly.");
        num("amputationOpenBloodLoss", 0.20, 0.0, 1.0, Kind.SINGLE, v -> v.amputationOpenBloodLoss, (v, x) -> v.amputationOpenBloodLoss = (float) x,
                "Extra volume gone when it is done with no strap above the cut.",
                "",
                "On top of a heavy bleed on the stump that will not clot. Sawing through",
                "an unstrapped limb is the fastest way to kill somebody in this mod and",
                "it is meant to be: the strap first is the whole rule of the operation.");
        flag("prosthetics", true, v -> v.prosthetics, (v, x) -> v.prosthetics = x,
                "",
                "Whether a stump can have something fitted to it.",
                "",
                "A wooden leg and a hook. Neither is the limb and neither pretends to be:",
                "they hand back part of what was lost and stop there. Without them the",
                "worst road in the mod ends in a wall, which is honest and is not a game.");
        num("prostheticRatio", 0.45, 0.0, 1.0, Kind.SINGLE, v -> v.prostheticRatio, (v, x) -> v.prostheticRatio = (float) x,
                "How much of a working limb a plain fitted one counts as.");
        num("prostheticRatioFine", 0.75, 0.0, 1.0, Kind.SINGLE, v -> v.prostheticRatioFine, (v, x) -> v.prostheticRatioFine = (float) x,
                "And how much one with a joint in it counts as.",
                "",
                "The difference between a peg and a leg somebody has spent money on is a",
                "knee, and what a knee gives back is most of the difference between",
                "walking and getting about. It is not a limb and this number should never",
                "reach one: the mod has exactly one permanent thing in it and softening",
                "it to nothing would be taking that back.");
        flag("rabies", true, v -> v.rabies, (v, x) -> v.rabies = x,
                "",
                "Whether a bite from an animal can give somebody rabies.",
                "",
                "Every other illness here is a race you can join late. A fever gives you",
                "minutes; sepsis gives you a fight; both can be answered after they have",
                "announced themselves.",
                "",
                "This one cannot, and that is why it is here. It is the only condition in",
                "medicine where the treatment works perfectly right up until the first",
                "symptom and then does not work at all, ever, for anybody.",
                "",
                "So it is silent. It sits there showing nothing, and a course of",
                "antibiotics during that time takes it away completely. Which makes it the",
                "only reason in the mod to treat somebody who is not ill - and the answer",
                "to a question every player asks once: what happens if I wait and see.",
                "",
                "off - a bite is a bite.");
        num("rabiesChance", 0.14, 0.0, 1.0, Kind.SINGLE, v -> v.rabiesChance, (v, x) -> v.rabiesChance = (float) x,
                "Chance a bite from a wolf, fox, bat or bear carries it.");
        num("rabiesSeconds", 300, 1, 1000000, Kind.WHOLE, v -> v.rabiesSeconds, (v, x) -> v.rabiesSeconds = (int) x,
                "How long it stays silent. This is all the time anybody gets.");
        num("frenzySeconds", 90, 1, 1000000, Kind.WHOLE, v -> v.frenzySeconds, (v, x) -> v.frenzySeconds = (int) x,
                "And how long the end of it lasts once it shows.");
        num("frenzyDamage", 0.35, 0.0, 100.0, Kind.SINGLE, v -> v.frenzyDamage, (v, x) -> v.frenzyDamage = (float) x,
                "What it takes off the head each second, growing as it goes.");
        num("deafnessSeconds", 90, 0, 100000, Kind.WHOLE, v -> v.deafnessSeconds, (v, x) -> v.deafnessSeconds = (int) x,
                "",
                "How long a blast leaves somebody deaf, in seconds.",
                "",
                "The injury everybody near an explosion has and nobody counts. No",
                "treatment and none needed - it wears off. What it costs is the one sense",
                "this mod uses to warn you about yourself: a heartbeat you cannot hear",
                "stops telling you how much blood is left, and a pulse has to be counted",
                "by eye instead.",
                "",
                "And it costs the medic. A deafened patient cannot hear the question, so",
                "asking stops working on exactly the casualty who was closest to it.",
                "",
                "0 turns it off.");
        flag("blast", true, v -> v.blast, (v, x) -> v.blast = x,
                "",
                "Whether an explosion does what an explosion does.",
                "",
                "Until this existed the mod treated a blast as a big fire: damage smeared",
                "over the body, nothing broken, nothing bleeding, nothing to treat. Blast",
                "injury is the one pattern trauma medicine has a whole vocabulary for, and",
                "it arrives in four kinds at once - the wave, the fragments, the throw and",
                "the heat.",
                "",
                "off - an explosion goes back to being a big fire.");
        num("tearOffBlow", 6.0, 0.0, 1000.0, Kind.SINGLE, v -> v.tearOffBlow, (v, x) -> v.tearOffBlow = (float) x,
                "",
                "How hard a blow takes a dead limb clean off, in damage.",
                "",
                "Dead flesh is not attached to anything. It is held on by what has already",
                "stopped working, so a hard enough knock takes it away - which turns the",
                "necrosis clock from a deadline into a warning. Past the end of it you are",
                "not only carrying something useless, you are carrying something that can",
                "be knocked off you.");
        num("tearOffChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.tearOffChance, (v, x) -> v.tearOffChance = (float) x,
                "And how likely that is, per blow that hard.");
        flag("blastTearOff", true, v -> v.blastTearOff, (v, x) -> v.blastTearOff = x,
                "",
                "Whether a blast can take a limb off outright.",
                "",
                "The worst thing in this mod and the only one that happens to somebody",
                "without anybody deciding it. Everything else that ends in a stump is a",
                "decision - a strap left on too long, a saw somebody picked up. This is a",
                "distance.",
                "",
                "So it is kept to the middle of a blast and kept unlikely even there. A",
                "rule that takes an arm off at the edge of a radius makes explosions",
                "unplayable; one that takes an arm off at the centre makes standing next",
                "to a charge a thing people do once.",
                "",
                "What it leaves is worse than a saw leaves. A cut is straight and somebody",
                "chose where; this is a tear, there is no strap above it because nobody",
                "knew it was coming, and it is the only wound here that arrives already",
                "emptying somebody.",
                "",
                "off - a blast can only break what it finds.");
        num("blastTearChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.blastTearChance, (v, x) -> v.blastTearChance = (float) x,
                "Chance of it, dead in the middle of a blast.");
        flag("blastFragments", true, v -> v.blastFragments, (v, x) -> v.blastFragments = x,
                "",
                "Whether a blast throws fragments that stay in people.",
                "",
                "Several small wounds at once, in whatever was facing it. They are the",
                "same problem an arrow is and come out the same way; the chart simply",
                "calls them what they are.");
        flag("blastWave", true, v -> v.blastWave, (v, x) -> v.blastWave = x,
                "",
                "Whether the pressure wave itself injures.",
                "",
                "This is the half worth having. Pressure does nothing at all to muscle -",
                "what it destroys is anything with gas in it, lungs first and bowel after.",
                "It leaves NO MARK. Somebody walks away from a blast talking and drowns in",
                "their own chest twenty minutes later, and the only sign in between is a",
                "breathing rate that will not settle.",
                "",
                "And armour does not touch it, which is the rule that makes the whole",
                "thing worth building: plate is a very good answer to a piece of metal",
                "travelling at somebody and no answer at all to the air around it. The",
                "hardest casualty to find is the one in full armour.",
                "",
                "off - a blast is fragments and heat and nothing else.");
        num("blastArmourStop", 0.85, 0.0, 1.0, Kind.SINGLE, v -> v.blastArmourStop, (v, x) -> v.blastArmourStop = (float) x,
                "How much of the fragments full armour stops.");
        num("blastLungChance", 0.45, 0.0, 1.0, Kind.SINGLE, v -> v.blastLungChance, (v, x) -> v.blastLungChance = (float) x,
                "Chance per lung of the wave tearing it, at the centre.");
        num("blastGutChance", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.blastGutChance, (v, x) -> v.blastGutChance = (float) x,
                "Chance of the wave tearing bowel, at the centre.");
        num("blastBurnChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.blastBurnChance, (v, x) -> v.blastBurnChance = (float) x,
                "Chance of a burn, scaled by how close.");
        flag("mending", true, v -> v.mending, (v, x) -> v.mending = x,
                "",
                "Whether a part that has been operated on has to be left alone.",
                "",
                "Everything in this mod has ended at the moment the treatment did. A",
                "surgeon closed somebody up and they stood straight back up and went to",
                "work, which is the one thing about surgery nobody who has had any",
                "believes.",
                "",
                "This is not a debuff and nothing is taken away by it. A mending arm swings",
                "exactly as well as it did an hour ago. It is a wound that has not finished",
                "being one: work the part hard - sprint on a leg, swing or mine with an arm",
                "- and it comes open again, and everything that was true before the",
                "operation is true afresh.",
                "",
                "Which makes it the first rule here whose answer is to do nothing. There is",
                "no item for it and there is not going to be one.",
                "",
                "off - a treatment ends when it ends.");
        num("mendingSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.mendingSeconds, (v, x) -> v.mendingSeconds = (int) x,
                "How long a part needs after somebody has been inside it.");
        num("mendingTearChance", 0.10, 0.0, 1.0, Kind.SINGLE, v -> v.mendingTearChance, (v, x) -> v.mendingTearChance = (float) x,
                "Chance per second of working it open, while working it.");
        flag("sleepless", true, v -> v.sleepless, (v, x) -> v.sleepless = x,
                "",
                "Whether being awake for days costs anything.",
                "",
                "The game has always counted this and has always done one thing with it,",
                "which is to send birds. That is a threat from outside. What being awake",
                "for three days actually does is from the inside, and it is the best",
                "documented thing on this list: a tired pair of hands is a worse pair of",
                "hands, and the person they belong to is the last to know.",
                "",
                "So it is the fifth thing that decides a treatment, after the room, the",
                "instrument, the second pair of hands and what this pair has just been",
                "doing - and the only one of the five with no answer available in the next",
                "half minute. It also costs two other things that are just as real: a body",
                "that has not slept catches what is going around, and it mends worse,",
                "because mending is mostly something a body does while it is asleep.",
                "",
                "Nothing is stored for any of it. The count is the game own one, the one",
                "the phantoms read, so it persists and is reset by the one thing that",
                "should reset it.",
                "",
                "off - a week awake is a week like any other.");
        num("sleeplessDays", 3, 2, 100, Kind.WHOLE, v -> v.sleeplessDays, (v, x) -> v.sleeplessDays = (int) x,
                "Days awake at which it is as bad as it gets. The first is free.");
        num("sleeplessHands", 0.2, 0.0, 1.0, Kind.SINGLE, v -> v.sleeplessHands, (v, x) -> v.sleeplessHands = (float) x,
                "What a pair of hands that far gone takes off a treatment.");
        num("sleeplessIllness", 1.0, 0.0, 10.0, Kind.SINGLE, v -> v.sleeplessIllness, (v, x) -> v.sleeplessIllness = (float) x,
                "And how much likelier they are to catch what is going around.");
        num("sleeplessMending", 0.5, 0.0, 10.0, Kind.SINGLE, v -> v.sleeplessMending, (v, x) -> v.sleeplessMending = (float) x,
                "And how much longer their own wounds take to knit.");
        num("epinephrineRounds", 2, 0, 100, Kind.WHOLE, v -> v.epinephrineRounds, (v, x) -> v.epinephrineRounds = (int) x,
                "",
                "The three needles, and why they are their own idea.",
                "",
                "Everything else in the bag is something you do with your hands over a",
                "minute. These are decisions made in a second, and every one of them can",
                "be the wrong decision.",
                "",
                "Adrenaline has two right uses and both are real. In a stopped heart it",
                "does not restart anything - nothing in a needle restarts a heart - it makes",
                "the next round of compressions worth what the first one was, which is why",
                "it is given between cycles. And it is the only answer to a body destroying",
                "the blood that was hung on it.",
                "",
                "Its wrong use looks exactly like its right one, which is what makes it the",
                "most dangerous thing here: somebody emptying out looks like somebody",
                "fading, and the instinct is to push something into them. Raising the",
                "pressure in a body with an open vessel means the bleed pours.",
                "",
                "How many rounds of compressions it hands back.");
        num("epinephrineBleed", 0.06, 0.0, 1.0, Kind.SINGLE, v -> v.epinephrineBleed, (v, x) -> v.epinephrineBleed = (float) x,
                "And what it costs in volume, per part already bleeding.");
        num("epinephrineArrestChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.epinephrineArrestChance, (v, x) -> v.epinephrineArrestChance = (float) x,
                "Chance a second shot on top of a surge stops the heart.");
        num("morphineSeconds", 75, 1, 100000, Kind.WHOLE, v -> v.morphineSeconds, (v, x) -> v.morphineSeconds = (int) x,
                "",
                "Morphine. The pills are a course: slow, safe, worth less each time. This",
                "is the other thing entirely - at once, strong, works on somebody already",
                "on the floor, and untouched by how many courses they have had, because it",
                "is not the same drug.",
                "",
                "What it costs is breathing. That is not a balance decision, it is what an",
                "opiate does: the drive to breathe is what it puts to sleep first.",
                "",
                "How long one holds.");
        flag("morphineDepresses", true, v -> v.morphineDepresses, (v, x) -> v.morphineDepresses = x,
                "",
                "Whether it can stop somebody breathing at all.",
                "",
                "Only on a chest that is already failing - crushed, broken, full of",
                "something, or on a body too empty to spare the effort. They go down and",
                "they stop breathing, which the airway clock and a pair of hands already",
                "answer, and so does the third needle.",
                "",
                "off - it is a stronger painkiller and nothing else.");
        num("morphineStopsBreathing", 0.4, 0.0, 1.0, Kind.SINGLE, v -> v.morphineStopsBreathing, (v, x) -> v.morphineStopsBreathing = (float) x,
                "Chance of it, on a chest that cannot afford it.");
        num("naloxoneShockPain", 3, 1, 7, Kind.WHOLE, v -> v.naloxoneShockPain, (v, x) -> v.naloxoneShockPain = (int) x,
                "",
                "The undo, and the only item here that is one. It answers morphine and",
                "nothing else.",
                "",
                "Its own wrong use is the reason it is a decision and not a reflex: pushed",
                "into somebody who is on morphine for a good reason it does what it says on",
                "the ampoule and takes the morphine away. All of it, at once, out of a",
                "person who was being held together by it.",
                "",
                "How many things have to be wrong with them for that to be dangerous.");
        num("naloxoneShockChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.naloxoneShockChance, (v, x) -> v.naloxoneShockChance = (float) x,
                "And the chance it puts them into shock when it is.");
        num("fluSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.fluSeconds, (v, x) -> v.fluSeconds = (int) x,
                "",
                "Viruses, and the single thing they are here for.",
                "",
                "Everything else a bottle answers in this mod is bacterial, and the mod",
                "never had to say so, because there was nothing the bottle did not answer.",
                "A virus is the sentence that makes the bottle mean something: it is not a",
                "stronger illness, it is a different kind of thing, and a course poured into",
                "one achieves nothing at all except that the course is gone.",
                "",
                "Influenza runs hot, spreads off a sneeze the way a cold does, and passes",
                "on its own given time and somewhere warm - which most of the time is all",
                "it does. What it can do on the way is leave a chest open to pneumonia -",
                "which is how influenza has always killed people, and which is the one",
                "part of it a bottle does answer. Whether it does is about the body: see",
                "fluPneumoniaPerSecond.",
                "",
                "How long it runs before that becomes a possibility.");
        num("fluRiskPerSecond", 0.0008, 0.0, 1.0, Kind.SINGLE, v -> v.fluRisk, (v, x) -> v.fluRisk = (float) x,
                "",
                "Where it comes from, which is the part a virus cannot do without.",
                "",
                "A cold is caught from the weather and a fever from your own wound.",
                "Influenza is caught from people, and it is the one illness in here that",
                "has no other honest source - so the vector is the only crowd this game",
                "has, which is a village. Stand around in one long enough and somebody in",
                "it was ill.",
                "",
                "That also makes the sleepless rule reachable by one player on their own:",
                "a body that has not slept catches what is going around, and now there is",
                "something going around.",
                "",
                "Chance per second while standing in a crowd. 0 stops it arriving on its",
                "own and leaves it travelling off a sneeze only.");
        num("fluCrowd", 3, 1, 100, Kind.WHOLE, v -> v.fluCrowd, (v, x) -> v.fluCrowd = (int) x,
                "How many villagers within eight blocks count as a crowd.");
        num("fluPneumoniaPerSecond", 0.005, 0.0, 1.0, Kind.SINGLE, v -> v.fluPneumonia, (v, x) -> v.fluPneumonia = (float) x,
                "",
                "And the chance per second that it goes down into the chest, at the end",
                "of its run. It climbs from nothing to this as the illness goes on.",
                "",
                "Only rolled at all on a body that is cold, wet or badly short of sleep.",
                "Warm, dry and rested, the flu is just the flu - which is the sentence the",
                "whole feature is for, and the reason this is a roll and not a clock.");
        num("stomachSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.stomachSeconds, (v, x) -> v.stomachSeconds = (int) x,
                "",
                "And the other one, which empties people rather than burning them.",
                "",
                "Caught off what was eaten. Antibiotics do nothing here either, and the",
                "answer is the one nobody thinks of as medicine: fluid, faster than it is",
                "going out. Which is a reason to reach for a bag of saline that is not a",
                "wound, and there was not one before.");
        num("stomachChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.stomachChance, (v, x) -> v.stomachChance = (float) x,
                "Chance that something eaten badly was alive rather than poison.");
        num("stomachDrain", 0.004, 0.0, 1.0, Kind.SINGLE, v -> v.stomachDrain, (v, x) -> v.stomachDrain = (float) x,
                "Volume lost per second to it. This is the whole danger.");
        num("stomachDamage", 0.04, 0.0, 100.0, Kind.SINGLE, v -> v.stomachDamage, (v, x) -> v.stomachDamage = (float) x,
                "And what the belly itself takes.");
        flag("thermometer", true, v -> v.thermometer, (v, x) -> v.thermometer = x,
                "",
                "Whether a figure needs an instrument.",
                "",
                "Every other reading here is taken with the hands and is there the moment",
                "you look. A temperature is not. A hot forehead is something anybody can",
                "feel - so the word stays free, and the chart still says warm - but thirty",
                "eight point six is not a thing any hand has reported, and the mod said it",
                "anyway.",
                "",
                "With this on, the figure costs a thermometer and the half minute it takes",
                "the column to settle. Which is the right price, because the figure is what",
                "separates a fever starting from one that is finished.",
                "",
                "off - the number is free again, as it used to be.");
        flag("rinse", true, v -> v.rinse, (v, x) -> v.rinse = x,
                "",
                "Whether leaving a dirty instrument in water rinses it.",
                "",
                "Water rinses; it does not sterilise, and it never has. What comes off a",
                "blade in a stream is the blood - the part you can see and the part that",
                "matters least - and the stream makes its own contribution.",
                "",
                "So this takes an instrument most of the way down and stops short. The",
                "last of it still wants a medical table or a bottle of antiseptic, which",
                "is the right shape for a field expedient: worth doing, and no substitute",
                "for doing the job properly.",
                "",
                "off - water is water and a saw is a saw.");
        num("rinseSeconds", 6, 1, 100000, Kind.WHOLE, v -> v.rinseSeconds, (v, x) -> v.rinseSeconds = (int) x,
                "Seconds in water per patient rinsed off it.");
        num("rinseFloor", 1, 0, 8, Kind.WHOLE, v -> v.rinseFloor, (v, x) -> v.rinseFloor = (int) x,
                "How clean water can get it. 0 lets a stream do the whole job.");
        flag("referredPain", true, v -> v.referredPain, (v, x) -> v.referredPain = x,
                "",
                "Whether a torn organ is felt somewhere it is not.",
                "",
                "A ruptured spleen hurts at the tip of the left shoulder. It is called",
                "Kehr's sign, it is real, and it is how that injury gets found by",
                "somebody with no machine to hand. The liver does it on the right.",
                "",
                "The answer is not marked as suspect, because the patient is not lying -",
                "they do not know anything is wrong with their shoulder. What gives it",
                "away is the chart: a shoulder that hurts more than anything else in a",
                "body, on a shoulder with nothing whatever wrong with it, is not a",
                "shoulder problem. Add a pulse that is running and it can only be one",
                "thing.",
                "",
                "It only ever fires when the named place is genuinely unhurt, so the",
                "contradiction is always real. A coincidence would not be a clue.",
                "",
                "off - they name the part the injury is actually in.");
        flag("chill", true, v -> v.chill, (v, x) -> v.chill = x,
                "",
                "Whether an open body loses heat.",
                "",
                "This mod already had two thirds of the oldest circle in trauma surgery",
                "and did not know it. Hypothermia here stops clotting outright - a bleed",
                "that would have closed by itself never does - and losing blood cools",
                "somebody, which is in the temperature reading. The missing third is the",
                "one that closes it: the operation itself is what cools the patient.",
                "",
                "Until this existed the only way to get cold was to stand in powder snow",
                "or in the rain in the wrong biome. A man lying opened up on a table in a",
                "blizzard caught nothing at all. In life he is the likeliest person in the",
                "world to, which is why an operating theatre is kept uncomfortably warm",
                "for everybody standing up in it.",
                "",
                "So where you cut decides how much it costs. A lit room out of the weather",
                "is almost free. The open, in the snow, at night, is not - and enough of",
                "it is hypothermia, which is the thing that stops the bleeding stopping.",
                "",
                "Nothing new punishes anybody for it: every consequence was already here",
                "and already right. This only supplies the cause.",
                "",
                "off - a body opened in a blizzard is a body opened indoors.");
        num("chillPerOperation", 0.3, 0.0, 1.0, Kind.SINGLE, v -> v.chillPerOperation, (v, x) -> v.chillPerOperation = (float) x,
                "What one cut costs, in the worst place there is.");
        num("chillDegrees", 2.5, 0.0, 20.0, Kind.SINGLE, v -> v.chillDegrees, (v, x) -> v.chillDegrees = (float) x,
                "And what a body that cold reads on the forehead.");
        num("chillHypothermia", 1.0, 0.0, 1.0, Kind.SINGLE, v -> v.chillHypothermia, (v, x) -> v.chillHypothermia = (float) x,
                "Past this it stops being a reading and becomes the illness.");
        num("chillWarmSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.chillWarmSeconds, (v, x) -> v.chillWarmSeconds = (int) x,
                "Seconds of being warm to get it back, halved by firelight.");
        num("sneezeChance", 0.04, 0.0, 1.0, Kind.SINGLE, v -> v.sneezeChance, (v, x) -> v.sneezeChance = (float) x,
                "",
                "A sneeze used to be a sound and a line of text addressed to the person",
                "sneezing, which is the one audience a sneeze is not for. It now does the",
                "three things a sneeze does: it is loud, it is catching, and it hurts if",
                "your chest is already wrong.",
                "",
                "Chance per second, while ill.");
        num("sneezeDamage", 0.8, 0.0, 100.0, Kind.SINGLE, v -> v.sneezeDamage, (v, x) -> v.sneezeDamage = (float) x,
                "What one costs a chest with a broken rib in it.");
        num("coldCatching", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.coldCatching, (v, x) -> v.coldCatching = (float) x,
                "",
                "Chance somebody within four blocks catches it off that sneeze.",
                "",
                "The first thing here that travels from one player to another, and it is a",
                "cold rather than anything worse on purpose: an illness that spreads and",
                "also matters would make standing near a wounded friend the wrong move,",
                "and this whole mod is built on standing near wounded friends.",
                "",
                "0 - nobody catches anything off anybody.");
        num("pneumoniaAfterSeconds", 180, 1, 100000, Kind.WHOLE, v -> v.pneumoniaAfterSeconds, (v, x) -> v.pneumoniaAfterSeconds = (int) x,
                "",
                "A cold used to sit at mild-and-passes however long somebody spent wet and",
                "freezing with it. Past this many seconds it can go down into the chest -",
                "but only while they are still cold or still wet. Getting dry and warm is",
                "the whole of the answer, which is what getting dry and warm is.",
                "",
                "Pneumonia bruises both lungs rather than tearing anything: there is no",
                "hole for a surgeon to find, so only a course answers it.");
        num("pneumoniaChance", 0.01, 0.0, 1.0, Kind.SINGLE, v -> v.pneumoniaChance, (v, x) -> v.pneumoniaChance = (float) x,
                "Chance per second past that, while still cold.");
        num("pneumoniaDamage", 0.06, 0.0, 100.0, Kind.SINGLE, v -> v.pneumoniaDamage, (v, x) -> v.pneumoniaDamage = (float) x,
                "What it costs the chest each second.");
        num("pneumoniaSepsisSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.pneumoniaSepsisSeconds, (v, x) -> v.pneumoniaSepsisSeconds = (int) x,
                "And how long before it stops being pneumonia.");
        num("peritonitisDamage", 0.12, 0.0, 100.0, Kind.SINGLE, v -> v.peritonitisDamage, (v, x) -> v.peritonitisDamage = (float) x,
                "",
                "What a belly full of gut costs each second.",
                "",
                "A torn gut used to roll straight for sepsis, which took a body from a",
                "wound to the worst thing in the mod in one step, with nothing in between",
                "to notice or to act on. This is that step.",
                "",
                "It is also the one illness antibiotics cannot finish: while the gut is",
                "still open the cause is still arriving, so a course buys time and the",
                "surgeon ends it.");
        num("peritonitisSepsisSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.peritonitisSepsisSeconds, (v, x) -> v.peritonitisSepsisSeconds = (int) x,
                "How long there is to close the hole.");
        flag("adrenaline", true, v -> v.adrenaline, (v, x) -> v.adrenaline = x,
                "",
                "Whether a fresh injury takes a moment to start hurting.",
                "",
                "This mod used to charge for a wound from the instant it happened: a leg",
                "breaks and the limp starts on the same tick, which is the one part of",
                "breaking a leg nobody who has broken one recognises.",
                "",
                "It hides pain. It does not mend bone. People finish races on stress",
                "fractures and walk away from crashes on broken ankles; nobody runs on a",
                "femur in two pieces, because what is missing there is not the will to",
                "ignore it. A part that is gone, dead, or beaten past use at all is",
                "untouched by this.",
                "",
                "It is not a buff. The heart is already going, the hands shake - which",
                "makes it the worst possible moment to operate on anybody - and when it",
                "ends the whole of what was held back arrives at once and worse. There is",
                "no way to ask for it on purpose, and no second one until the crash from",
                "the last has passed.",
                "",
                "off - a wound hurts from the tick it happens.");
        num("adrenalineSeconds", 25, 1, 100000, Kind.WHOLE, v -> v.adrenalineSeconds, (v, x) -> v.adrenalineSeconds = (int) x,
                "How long you do not feel it.");
        num("adrenalineRelief", 0.8, 0.0, 1.0, Kind.SINGLE, v -> v.adrenalineRelief, (v, x) -> v.adrenalineRelief = (float) x,
                "What share of the pain goes quiet. Structure is never hidden.");
        num("adrenalineCrashSeconds", 20, 0, 100000, Kind.WHOLE, v -> v.adrenalineCrashSeconds, (v, x) -> v.adrenalineCrashSeconds = (int) x,
                "And how long the bill takes to pay afterwards.");
        num("adrenalineCrashBite", 0.3, 0.0, 1.0, Kind.SINGLE, v -> v.adrenalineCrashBite, (v, x) -> v.adrenalineCrashBite = (float) x,
                "How much worse than normal it is while that lasts.");
        num("adrenalineShake", 0.15, 0.0, 1.0, Kind.SINGLE, v -> v.adrenalineShake, (v, x) -> v.adrenalineShake = (float) x,
                "What the shaking takes off anything you treat during it.");
        num("adrenalinePulse", 30.0, 0.0, 200.0, Kind.SINGLE, v -> v.adrenalinePulse, (v, x) -> v.adrenalinePulse = (float) x,
                "And what it adds to a heart that is already going.");
        flag("fatigue", true, v -> v.fatigue, (v, x) -> v.fatigue = x,
                "",
                "Whether arms get tired.",
                "",
                "This is not a stamina bar and nothing you normally do builds it.",
                "Sprinting does not, mining does not, fighting does not - vanilla already",
                "charges for those, and charging twice would be a tax on playing rather",
                "than a rule about medicine.",
                "",
                "It comes from the two things in this mod that are physically heavy and",
                "that this mod invented: carrying somebody who cannot walk, and bracing",
                "somebody who is being cut into. A body over the shoulder used to be a",
                "slower way of walking and nothing else, so getting them out first had no",
                "limit on it - and a move with no limit is not a decision.",
                "",
                "It costs two things: how far you can carry before your arms give out and",
                "put the patient down, and how steady your hands are for a while after.",
                "Bad arms tire up to three times faster, which makes a wounded medic a",
                "worse stretcher-bearer without a separate rule saying so.",
                "",
                "The answer is to stand still for a moment, which is the same answer",
                "mending has.",
                "",
                "off - a body weighs nothing after the first step.");
        num("fatigueSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.fatigueSeconds, (v, x) -> v.fatigueSeconds = (int) x,
                "Seconds of carrying, with sound arms, before they give out.");
        num("fatigueRecoverSeconds", 45, 1, 100000, Kind.WHOLE, v -> v.fatigueRecoverSeconds, (v, x) -> v.fatigueRecoverSeconds = (int) x,
                "Seconds of not carrying to come back, halved standing still.");
        num("fatigueQuality", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.fatigueQuality, (v, x) -> v.fatigueQuality = (float) x,
                "What fully gone arms take off a treatment.");
        num("fatigueCarrySlow", 0.5, 0.0, 4.0, Kind.SINGLE, v -> v.fatigueCarrySlow, (v, x) -> v.fatigueCarrySlow = (float) x,
                "How much heavier a body feels once the arms are gone.");
        num("fatigueHold", 0.15, 0.0, 1.0, Kind.SINGLE, v -> v.fatigueHold, (v, x) -> v.fatigueHold = (float) x,
                "What bracing a patient through one treatment costs.");
        flag("assist", true, v -> v.assist, (v, x) -> v.assist = x,
                "",
                "Whether a second person holding the patient still is worth anything.",
                "",
                "Everything in this mod is easier with somebody else, and until now",
                "somebody else could do everything except the one thing an assistant",
                "actually does, which is hold on.",
                "",
                "They have to be crouched next to the patient with an empty hand. Both are",
                "deliberate - nobody does either by walking past - so the mod never has to",
                "guess whether they meant it.",
                "",
                "It is worth most where there is nothing else: held down, somebody being",
                "operated on awake moves less, which is what an operation was before",
                "anaesthetic and why every picture of an old operating table has straps on",
                "it. It is still worse than the bottle.",
                "",
                "off - the second person in the room is scenery again.");
        num("assistQuality", 0.15, 0.0, 1.0, Kind.SINGLE, v -> v.assistQuality, (v, x) -> v.assistQuality = (float) x,
                "What a pair of hands adds to how well it goes.");
        num("assistAwakeCap", 0.7, 0.0, 1.0, Kind.SINGLE, v -> v.assistAwakeCap, (v, x) -> v.assistAwakeCap = (float) x,
                "The ceiling on an operation without anaesthetic, while held.");
        flag("theatre", true, v -> v.theatre, (v, x) -> v.theatre = x,
                "",
                "Whether where you operate and what you operate with matter.",
                "",
                "Until this existed a surgical kit opened in a lit room over a table did",
                "the same job as one opened face-down in a cave, with the same saw that",
                "took somebody else's leg off an hour ago and has not been touched since.",
                "",
                "The instrument is the famous half. For most of the history of surgery it",
                "was the thing that killed people - not the cutting, the carrying: a blade",
                "went from one patient into the next and nobody thought anything of it.",
                "That gives the two tools that go into a wound and are never spent - the",
                "saw and the forceps - the one thing they never had, which is a running cost.",
                "",
                "Neither half is a tax on playing. A clean instrument in an ordinary room",
                "is worth exactly what a treatment has always been worth; somewhere good",
                "is better than that, and a filthy saw in the dark is worse.",
                "",
                "off - a treatment is the same treatment anywhere, with anything.");
        num("grimeInfection", 0.55, 0.0, 1.0, Kind.SINGLE, v -> v.grimeInfection, (v, x) -> v.grimeInfection = (float) x,
                "Chance a filthy instrument leaves something behind.");
        num("roomInfection", 0.18, 0.0, 1.0, Kind.SINGLE, v -> v.roomInfection, (v, x) -> v.roomInfection = (float) x,
                "And what the worst possible place adds on its own.");
        num("placeQuality", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.placeQuality, (v, x) -> v.placeQuality = (float) x,
                "How much a proper theatre is worth to a pair of hands.");
        flag("pulse", true, v -> v.pulse, (v, x) -> v.pulse = x,
                "",
                "Whether a pulse can be felt for.",
                "",
                "The first thing anybody does to anybody: free, instant, needs no item,",
                "and works on somebody still standing up. It is also the only outward",
                "sign of the injury this mod deliberately made invisible - a torn spleen",
                "puts nothing on the skin, and a body losing blood answers by driving the",
                "heart faster. A fast thin pulse on somebody with nothing open on them is",
                "the tell, and it is the same tell it is in life.",
                "",
                "How well it is felt buys precision rather than healing: a good hold",
                "gives the number, a poor one gives the word.",
                "",
                "off - a heart is something you read off a chart again.");
        flag("breathing", true, v -> v.breathing, (v, x) -> v.breathing = x,
                "",
                "Whether a chest can be watched.",
                "",
                "The one vital you get without touching anybody: you watch, and you have",
                "the answer from across the room. It is also what the pulse is to internal",
                "bleeding, for the other injury this mod hides - a crushed lung has never",
                "had an outward sign, and fast and shallow is what one looks like.",
                "",
                "off - a chest stops being something you can read.");
        flag("temperature", true, v -> v.temperature, (v, x) -> v.temperature = x,
                "",
                "Whether somebody's temperature can be felt.",
                "",
                "The slowest vital and the only one that sees the future: it starts",
                "climbing while an infection is still a colour on the chart and long",
                "before it is a fever with a name. Somebody who checks a forehead every",
                "so often catches the thing that kills quietly.",
                "",
                "It runs the other way too, and that half is the one people get wrong.",
                "Lost blood and cold blood both pull it down, and cold blood does not",
                "clot - which is the loop that kills a casualty who was going to live.",
                "",
                "off - a forehead tells you nothing.");
        flag("ask", true, v -> v.ask, (v, x) -> v.ask = x,
                "",
                "Whether a conscious patient can be asked where it hurts.",
                "",
                "The only channel in this mod that comes out of a body rather than off",
                "one. It is the only thing that says WHERE - a pulse says the blood went",
                "somewhere, and a conscious person says which side of their belly it went",
                "into - and it stops working the moment they stop being able to talk.",
                "",
                "It can also be wrong, and that is deliberate. A rattled head names",
                "something that hurts rather than the worst thing. And painkillers work:",
                "a dosed patient reports less than is true - small complaints go entirely",
                "and big ones come down a grade - so the medic who dosed them keeps the",
                "location and loses the urgency. Nothing else in the mod makes a",
                "treatment cost information.",
                "",
                "off - nobody says anything.");
        num("organBrokenBonus", 2.0, 1.0, 10.0, Kind.SINGLE, v -> v.organBrokenBonus, (v, x) -> v.organBrokenBonus = (float) x,
                "",
                "How much likelier a blow is to tear something inside when the bone",
                "over it is already broken.",
                "",
                "A ribcage is a shield, and a broken one is not. This mod has said from",
                "the start that a rib going in and a spleen going are the same kick from",
                "different angles - so a kick that finds a chest already caved in finds",
                "what is behind it far more easily.",
                "",
                "Which turns a broken rib from a thing that hurts into a thing that has",
                "stopped protecting somebody, and gives the splint a second reason to",
                "exist that nobody has to be told about: the second blow is the",
                "dangerous one. A crack counts for half of it.",
                "",
                "1.0 turns it off and bone stops mattering to what is under it.");
        num("prostheticWearSeconds", 2400, 0, 1000000, Kind.WHOLE, v -> v.prostheticWearSeconds, (v, x) -> v.prostheticWearSeconds = (int) x,
                "",
                "How long a fitted limb takes to wear out completely, in seconds.",
                "",
                "Nothing made of wood and leather and worn all day stays the shape it was",
                "cut to. A socket loosens, a strap stretches, a joint develops play - and",
                "a worn prosthesis is not a broken one, it is a worse one. It rubs, and it",
                "gives back less of what it gave back on the day it was fitted.",
                "",
                "Fitting a fresh one puts it right, which is the standing job the one",
                "permanent thing in this mod ought to come with: losing a leg is not an",
                "event somebody gets past, it is a thing they then live with, and living",
                "with one is mostly maintenance.",
                "",
                "0 turns it off and a fitted limb stays as good as the day it went on.");
        num("prostheticWornRatio", 0.55, 0.0, 1.0, Kind.SINGLE, v -> v.prostheticWornRatio, (v, x) -> v.prostheticWornRatio = (float) x,
                "What a completely worn one is worth, as a share of a fresh one.");
        flag("phantomPain", true, v -> v.phantomPain, (v, x) -> v.phantomPain = x,
                "",
                "Whether a limb that is gone still hurts.",
                "",
                "Almost everybody who loses one has this, which is the only reason it is",
                "here: it costs nothing, it changes nothing, and leaving it out would",
                "have been the mod quietly saying the body forgets. It does not. Only the",
                "player it belongs to is ever told, and painkillers do work on it.");
        num("phantomPainSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.phantomPainSeconds, (v, x) -> v.phantomPainSeconds = (int) x,
                "Roughly how many seconds between one and the next.");
        flag("arrows", true, v -> v.arrows, (v, x) -> v.arrows = x,
                "",
                "Whether arrows can stay in.",
                "",
                "A lodged arrow is not extra damage - the damage happened on the way in.",
                "It is a wound that cannot be dealt with while it is there: it will not",
                "clot, it will not mend, and a bandage over the top of it is a bandage",
                "over an arrow. Getting it out costs nothing but hands, and getting it",
                "out badly costs the patient more than leaving it would have.",
                "",
                "A blade is the other half of this, and it has its own switch below.");
        num("arrowChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.arrowChance, (v, x) -> v.arrowChance = (float) x,
                "Chance that an arrow which hits stays in.");
        num("arrowTear", 3.0, 0.0, 1000.0, Kind.SINGLE, v -> v.arrowTear, (v, x) -> v.arrowTear = (float) x,
                "Damage a badly pulled arrow does on the way out, on top of the heavy",
                "bleed it leaves. This is the only thing in the mod that can make somebody",
                "worse off by being helped, and that is deliberate: an arrow in a leg is",
                "the one injury where waiting for steadier hands is a real plan.");
        flag("weaponsStick", true, v -> v.weaponsStick, (v, x) -> v.weaponsStick = x,
                "",
                "Whether a weapon can stay in the person it was swung at.",
                "",
                "Rare, and worth much more than its rate. Three things happen at once that",
                "nothing else in this mod does: the thing in the wound is holding the wound",
                "shut, so taking it out is the injury; whoever swung it does not have it any",
                "more, which is the only disarm here; and it is visible across a field on",
                "somebody who is still walking around.",
                "",
                "Anything with a tier can do it - a sword, an axe, a pick, and modded",
                "equivalents built the ordinary way - and a trident. Not a fist.");
        num("stickChance", 0.06, 0.0, 1.0, Kind.SINGLE, v -> v.stickChance, (v, x) -> v.stickChance = (float) x,
                "Chance that a heavy enough blow leaves the weapon in.");
        num("stickDamage", 5.0, 0.0, 1000.0, Kind.SINGLE, v -> v.stickDamage, (v, x) -> v.stickDamage = (float) x,
                "How hard the blow has to be first. Below this it is a cut,",
                "not something that went in deep enough to hold.");
        num("stickJostle", 0.25, 0.0, 1000.0, Kind.SINGLE, v -> v.stickJostle, (v, x) -> v.stickJostle = (float) x,
                "Damage per second done to a part while walking about with",
                "something still in it. Sprinting costs four times this.");
        num("stickJostleBleed", 0.06, 0.0, 1.0, Kind.SINGLE, v -> v.stickJostleBleed, (v, x) -> v.stickJostleBleed = (float) x,
                "Chance per second, while moving, that the wound lets go",
                "around it anyway.");
        num("findingFreshSeconds", 120, 5, 100000, Kind.WHOLE, v -> v.findingFreshSeconds, (v, x) -> v.findingFreshSeconds = (int) x,
                "",
                "How many seconds a finding counts as current, in realistic difficulty.",
                "",
                "Past this the chart prints it faintly. It is not taken away: an old",
                "finding is still the last thing anybody knows, and throwing it out would",
                "punish the one person who did the work. What goes is the confidence.");
        flag("bedsideObs", true, v -> v.bedsideObs, (v, x) -> v.bedsideObs = x,
                "",
                "Whether a patient beside a medical table is observed automatically.",
                "",
                "A ward is not a place with better medicine in it - it is a place where",
                "somebody writes a number on a page every so often. Pulse and temperature",
                "go onto the observation sheet on a clock, quietly, with no line in chat.",
                "",
                "It is also the first reason in this mod to carry somebody somewhere:",
                "supplies travel, and a place that measures for you does not.");
        num("bedsideSeconds", 40, 5, 100000, Kind.WHOLE, v -> v.bedsideSeconds, (v, x) -> v.bedsideSeconds = (int) x,
                "Seconds between one automatic observation and the next.");
        num("bedsideRange", 3, 1, 16, Kind.WHOLE, v -> v.bedsideRange, (v, x) -> v.bedsideRange = (int) x,
                "How many blocks from the table still counts as beside it.");
        flag("panic", true, v -> v.panic, (v, x) -> v.panic = x,
                "",
                "Whether an awake patient in pain can pull away from an operation.",
                "",
                "The only interruption in this mod that comes from the patient rather than",
                "from somebody hitting the medic. It never happens to anybody who is under",
                "ether, dosed, on the floor, or being held by a second pair of hands - so",
                "it is not a tax on surgery, it is the reason those three things exist.");
        num("panicChance", 0.035, 0.0, 1.0, Kind.SINGLE, v -> v.panicChance, (v, x) -> v.panicChance = (float) x,
                "Chance per second at the worst pain there is. Scaled down by how",
                "much pain they are actually in, so a scratch never does this.",
                "",
                "Per second, which means the number that matters is not this one - it is",
                "what it comes to over a job. At this setting a fifteen-second operation on",
                "somebody in serious pain is interrupted about a third of the time, and on",
                "somebody in the worst pain there is, four times in ten. Nothing is spent",
                "when it happens: what it costs is the time and the walk back to whatever",
                "would have stopped it.");
        flag("breathSounds", true, v -> v.breathSounds, (v, x) -> v.breathSounds = x,
                "",
                "Whether a chest in trouble can be heard from a body length away.",
                "",
                "The third thing in this mod that costs nothing to find out, after the",
                "colour of somebody and the way they are standing. What it says is that",
                "something is wrong with a chest - never the rate and never the cause, so",
                "the stethoscope keeps its job and gains a better one.",
                "",
                "A chest that has stopped makes no sound at all. That is not a gap.");
        num("stickTear", 4.0, 0.0, 1000.0, Kind.SINGLE, v -> v.stickTear, (v, x) -> v.stickTear = (float) x,
                "Damage done by pulling one out with bare hands, on top of the",
                "bleed. A surgeon with forceps does neither.");
    }

    private static void effectsGuns() {
        section("effects.guns",
                "Bullets from a gun mod, and what they leave behind.",
                "",
                "A rifle round is not an arrow. It goes in deep, it does not stand",
                "out of the skin, and it cannot be drawn back the way a shaft can:",
                "the only way out is to open the wound and pick it out. Until it is",
                "out the part will not clot, will not mend and cannot be dressed.",
                "",
                "This only does anything when a gun mod is installed. TaCZ is the",
                "one this was written against; no class from it is named, so any",
                "gun mod that reports its bullets the same way works too.");
        flag("bulletsStick", true, v -> v.bulletsStick, (v, x) -> v.bulletsStick = x,
                "Whether a bullet can stay in.");
        num("bulletStickChance", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.bulletStickChance, (v, x) -> v.bulletStickChance = (float) x,
                "Chance that a bullet which hits stays in.",
                "",
                "Higher than an arrow's, because a round that has gone through a",
                "body has already spent most of what it had and stops in the meat",
                "far more often than a shaft does. Set it to 0 to keep the damage",
                "and lose the extraction.");
        num("bulletTear", 3.0, 0.0, 1000.0, Kind.SINGLE, v -> v.bulletTear, (v, x) -> v.bulletTear = (float) x,
                "Damage a badly cut-out bullet does on the way out, on top of the",
                "heavy bleed it leaves. The same trade as an arrow: a steady hand",
                "leaves an ordinary cut, and an impatient one makes the patient worse.");
    }

    private static void clots() {
        section("clots",
                "",
                "Blood that stopped moving.",
                "",
                "Everything else in this mod arrives on the end of something. This is what",
                "a body does to itself when nothing is happening to it: blood that is not",
                "moving sets, and what stops it moving is a strap, a splint, a broken leg",
                "and an hour on the floor waiting for somebody to come.",
                "",
                "Which makes it the first injury here caused by treatment - the tourniquet",
                "that saved the limb and the stillness that let it work. Nothing about it",
                "punishes a mistake; it charges for a correct decision.",
                "",
                "The limb is not the danger. It aches, and that is all it ever does. What",
                "it is waiting for is the patient to stand up and walk.");
        flag("clots", true, v -> v.clots, (v, x) -> v.clots = x);
        num("clotChance", 0.00008, 0.0, 1.0, Kind.SINGLE, v -> v.clotChance, (v, x) -> v.clotChance = (float) x,
                "Chance per second of one forming in a still limb, before the",
                "multipliers for the strap, the splint, the break and the blood.",
                "",
                "A clot needs a reason now: a tourniquet, a splint, a fracture or",
                "crack, real blood loss, or being on the floor. A player who is",
                "simply standing still with nothing wrong will not develop one.");
        num("stillSeconds", 90, 1, 100000, Kind.WHOLE, v -> v.clotStillSeconds, (v, x) -> v.clotStillSeconds = (int) x,
                "How long a limb has to be still before a clot can start.",
                "",
                "This is the clock that stops an ordinary pause from becoming an",
                "injury. The default is a minute and a half; the risk factors",
                "above still multiply the chance after that.");
        num("clotClearSeconds", 240, 1, 100000, Kind.WHOLE, v -> v.clotClearSeconds, (v, x) -> v.clotClearSeconds = (int) x,
                "How long one sits before the body deals with it by itself. Most of",
                "them do, which is the only reason having one is survivable.");
        num("embolismChance", 0.002, 0.0, 1.0, Kind.SINGLE, v -> v.embolismChance, (v, x) -> v.embolismChance = (float) x,
                "And the chance per second that a clot goes, while the limb it is in",
                "is moving. Tripled by running.");
        num("embolismDamage", 0.28, 0.0, 1000.0, Kind.SINGLE, v -> v.embolismDamage, (v, x) -> v.embolismDamage = (float) x,
                "What one in the lungs costs the chest every second it is there.");
    }

    private static void sleep() {
        section("sleep",
                "",
                "What being awake for days looks like from inside.",
                "",
                "Minecraft has always counted this and has always done one thing with",
                "it, which is to send birds. That is a threat from outside. What three",
                "days awake actually does is on the inside of somebody\\'s eyes, and it",
                "is the one affliction in this mod that nobody else can see at all.",
                "",
                "The lids come down, they come down further the longer it goes on, and",
                "the blinks get longer until one of them does not end when it should.",
                "Sleep clears all of it, and nothing else does.");
        num("sleepyFrom", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.sleepyFrom, (v, x) -> v.sleepyFrom = (float) x,
                "How far into the count before the lids start showing at all.",
                "One late night is not an affliction.");
        num("microsleepFrom", 0.7, 0.0, 1.0, Kind.SINGLE, v -> v.microsleepFrom, (v, x) -> v.microsleepFrom = (float) x,
                "And how far in before one of the blinks stops being a blink.");
        num("microsleepChance", 0.010, 0.0, 1.0, Kind.SINGLE, v -> v.microsleepChance, (v, x) -> v.microsleepChance = (float) x,
                "Chance of that per second at the very end of the count. It is the",
                "thing that happens to drivers: the world goes away for a second and a",
                "half and comes back somewhere else.");
        num("microsleepSeconds", 2, 1, 100000, Kind.WHOLE, v -> v.microsleepSeconds, (v, x) -> v.microsleepSeconds = (int) x,
                "How long the lights are out for one.");
    }

    private static void displacement() {
        section("displacement",
                "",
                "Bones that moved, and bones that healed where they were left.",
                "",
                "The mod had one kind of broken and there are two. A bone can break and",
                "stay where it was - a bad month - or the ends can move, which is the",
                "thing everybody pictures when they hear the word: the limb is the wrong",
                "shape, and anybody can see it without reading a chart.",
                "",
                "It matters because a splint stops being the whole answer. Strapping a",
                "stick to a bone that is out of line holds it exactly where it is, and",
                "where it is is wrong - so it knits like that, and that is permanent.",
                "Something has to pull it back into line first, and that something is a",
                "pair of hands, done to somebody who is awake, and a great deal of noise.",
                "",
                "And above both of them, the one that is also a wound: the end of the",
                "bone through the skin. That one is a surgeon's job before it is",
                "anybody else's.");
        flag("displacement", true, v -> v.displacement, (v, x) -> v.displacement = x);
        num("displacedChance", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.displacedChance, (v, x) -> v.displacedChance = (float) x,
                "Chance that a fresh break is out of line rather than merely broken.");
        num("displacedBlow", 10.0, 0.0, 1000.0, Kind.SINGLE, v -> v.displacedBlow, (v, x) -> v.displacedBlow = (float) x,
                "And how hard a blow has to be before it can move the ends at all.",
                "Under this a bone breaks and stays where it was.");
        flag("openFractures", true, v -> v.openFractures, (v, x) -> v.openFractures = x,
                "Whether the end can come through the skin.");
        num("openChance", 0.2, 0.0, 1.0, Kind.SINGLE, v -> v.openChance, (v, x) -> v.openChance = (float) x,
                "Chance of that, on a break that is already out of line.");
        num("openInfection", 3.0, 0.0, 100.0, Kind.SINGLE, v -> v.openInfection, (v, x) -> v.openInfection = (float) x,
                "Multiplier on how fast an open fracture goes bad. The inside of a leg",
                "has been introduced to the floor, and this is the fastest road to",
                "infection there is in this mod.");
        num("openBloodLoss", 0.08, 0.0, 1.0, Kind.SINGLE, v -> v.openBloodLoss, (v, x) -> v.openBloodLoss = (float) x,
                "Share of volume an open fracture costs when it happens.");
        num("crookedCost", 0.12, 0.0, 0.9, Kind.SINGLE, v -> v.crookedCost, (v, x) -> v.crookedCost = (float) x,
                "",
                "What a bone that healed out of line costs the part, as a share of its",
                "maximum, for good.",
                "",
                "The only permanent loss of health in the mod, and it is here because",
                "that is what a malunion is: the bone is shorter or bent, and the limb",
                "does less than it did. It can be undone, and only one way - somebody",
                "breaks it again and sets it properly this time.");
        num("crookedPenalty", 0.15, 0.0, 1.0, Kind.SINGLE, v -> v.crookedPenalty, (v, x) -> v.crookedPenalty = (float) x,
                "And what it costs to use, on top of the health: speed in a leg,",
                "steadiness in an arm.");
        num("setTear", 4.0, 0.0, 1000.0, Kind.SINGLE, v -> v.setTear, (v, x) -> v.setTear = (float) x,
                "Damage a reduction does when the hands go the wrong way.");
        num("setArteryChance", 0.2, 0.0, 1.0, Kind.SINGLE, v -> v.setArteryChance, (v, x) -> v.setArteryChance = (float) x,
                "And the chance that a badly done one puts a bone end through something",
                "that was carrying blood. This is why a femur out of line is the injury",
                "that frightens people who know what they are looking at.");
        flag("scream", true, v -> v.scream, (v, x) -> v.scream = x,
                "",
                "Whether setting a bone on somebody who is awake makes a noise the world",
                "can hear.",
                "",
                "The only thing in this mod that a treatment does to the room rather than",
                "to the patient. Nothing else here has ever cost anybody a decision about",
                "where to do it - and this is the one job where standing in the open, at",
                "night, near anything with ears, is a bad plan.",
                "",
                "It is also the first reason to carry the mask that is not about quality.");
        num("screamRange", 32.0, 0.0, 256.0, Kind.SINGLE, v -> v.screamRange, (v, x) -> v.screamRange = (float) x,
                "How far it carries, in blocks.");
    }

    private static void artery() {
        section("artery",
                "",
                "The one that is not just a faster bleed.",
                "",
                "Everything else in here is a body losing blood. This is a body throwing",
                "it away: it goes with the heart rather than in a trickle, it does not",
                "clot, ever, and a dressing over the top of one soaks through and keeps",
                "soaking. What it leaves is a count in tens of seconds.",
                "",
                "Which is why it exists. The mod had two grades of bleeding and they were",
                "the same thing at two speeds, so the answer was always the same answer",
                "in less of a hurry. There are exactly two answers to this one - a hand",
                "pushed hard into it, or a strap above it - and both were already here",
                "waiting for something that needed them.");
        flag("arterial", true, v -> v.arterial, (v, x) -> v.arterial = x);
        num("arterialChance", 0.12, 0.0, 1.0, Kind.SINGLE, v -> v.arterialChance, (v, x) -> v.arterialChance = (float) x,
                "Chance that a bleed bad enough to qualify opens a vessel instead.",
                "",
                "Deliberately low. A wound this bad turning up in every other fight would",
                "not be frightening, it would be the weather.");
        num("arterialBlow", 14.0, 0.0, 1000.0, Kind.SINGLE, v -> v.arterialBlow, (v, x) -> v.arterialBlow = (float) x,
                "How hard a single blow has to land before it can reach one at all.");
        num("arterialRate", 2.0, 0.0, 1000.0, Kind.SINGLE, v -> v.arterialRate, (v, x) -> v.arterialRate = (float) x,
                "Damage to the part per second, against 0.7 for a heavy bleed.");
        num("bloodPerArterialSecond", 0.045, 0.0, 1.0, Kind.SINGLE, v -> v.bloodPerArterial, (v, x) -> v.bloodPerArterial = (float) x,
                "And volume per second, against 0.010 for a heavy bleed. This is the",
                "number that makes it a different injury rather than a worse one.");
        flag("spurts", true, v -> v.arterialSpurt, (v, x) -> v.arterialSpurt = x,
                "Whether it throws blood in time with the heart rather than dripping.",
                "Costs nothing but particles, and it is the only way anybody can tell",
                "the difference across a room.");
    }

    private static void faint() {
        section("faint",
                "",
                "Going out.",
                "",
                "Not the same thing as going down, and the difference is the whole reason",
                "it is here. Being downed is the end of your luck: a clock, a rescue, or",
                "a grave. A faint is a body deciding, without asking, that it cannot keep",
                "the head supplied while standing up - so it stops standing up. Ten",
                "seconds later it is fine, and nothing about it is permanent.",
                "",
                "What it costs is the ten seconds, and wherever you happened to be.");
        flag("faint", true, v -> v.faint, (v, x) -> v.faint = x);
        num("faintBloodLoss", 0.42, 0.0, 1.0, Kind.SINGLE, v -> v.faintBloodLoss, (v, x) -> v.faintBloodLoss = (float) x,
                "Share of blood gone before an empty body can drop on its own.");
        num("faintChance", 0.020, 0.0, 1.0, Kind.SINGLE, v -> v.faintChance, (v, x) -> v.faintChance = (float) x,
                "Chance per second while at risk. Doubled by sprinting, which is the",
                "honest version of why people faint when they stand up too fast.");
        num("faintSeconds", 7, 1, 100000, Kind.WHOLE, v -> v.faintSeconds, (v, x) -> v.faintSeconds = (int) x,
                "How long the lights are out.");
        num("faintPain", 8, 0, 10, Kind.WHOLE, v -> v.faintPain, (v, x) -> v.faintPain = (int) x,
                "Pain out of ten at which a body can go over from pain alone. This is",
                "the common faint - the one at the sight of a broken arm, usually",
                "somebody else's - and it needs no blood loss at all.");
        num("faintClearSeconds", 25, 0, 100000, Kind.WHOLE, v -> v.faintClearSeconds, (v, x) -> v.faintClearSeconds = (int) x,
                "Grace after coming round before it can happen again.");
    }

    private static void scars() {
        section("scars",
                "",
                "What a body keeps.",
                "",
                "Until this, every injury in the mod healed to the same body it started",
                "as. Somebody who had survived four amputations, a septic belly and a",
                "skull fracture was, once mended, indistinguishable from somebody who had",
                "never been touched - which is a mod about medicine quietly saying that",
                "none of it mattered once it was over.",
                "",
                "So parts now remember. A bone that has knitted knows about weather and",
                "is where the next fall lands; skin that has been opened and closed is",
                "scar tissue, which has less blood in it and is where the next infection",
                "settles; a deep burn tightens as it heals.",
                "",
                "The counts are permanent and only dying clears them. The stiffness is",
                "not: that is worked back down by hand, over days, which is the whole",
                "design - the history ratchets one way and the disability does not.");
        flag("scars", true, v -> v.scars, (v, x) -> v.scars = x);
        num("scarStiffness", 0.10, 0.0, 1.0, Kind.SINGLE, v -> v.scarStiffness, (v, x) -> v.scarStiffness = (float) x,
                "How tight each closed wound leaves the part, 0 to 1.");
        num("burnStiffness", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.burnStiffness, (v, x) -> v.burnStiffness = (float) x,
                "And a burn that healed through the skin, which is the one that really",
                "contracts: burnt skin closes by pulling the edges together.");
        num("graftStiffness", 0.40, 0.0, 1.0, Kind.SINGLE, v -> v.graftStiffness, (v, x) -> v.graftStiffness = (float) x,
                "And a limb that was sewn back on.");
        num("stiffQuality", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.stiffQuality, (v, x) -> v.stiffQuality = (float) x,
                "What a completely stiff pair of hands costs on the treatment screens,",
                "as a share. A stiff arm is not a weak arm - it is one that does not go",
                "where it is put.");
        num("stiffSlow", 0.20, 0.0, 1.0, Kind.SINGLE, v -> v.stiffSlow, (v, x) -> v.stiffSlow = (float) x,
                "And what a completely stiff leg costs off walking speed.");
        num("scarInfection", 1.7, 0.0, 100.0, Kind.SINGLE, v -> v.scarInfection, (v, x) -> v.scarInfection = (float) x,
                "Multiplier on the chance a wound goes bad where the skin has been",
                "opened before, or where something has grown once already.");
        num("knitFragility", 0.78, 0.1, 10.0, Kind.SINGLE, v -> v.knitFragility, (v, x) -> v.knitFragility = (float) x,
                "Multiplier on the blow needed to break a bone that has knitted here",
                "before. Under one, it goes more easily.",
                "",
                "Not because knitted bone is weak - it is not, and the mod will not",
                "teach that it is. It is because whatever put you on the floor hard",
                "enough to break that femur is the same thing you keep doing.");
        flag("ache", true, v -> v.ache, (v, x) -> v.ache = x,
                "Whether old breaks ache when the weather turns.",
                "",
                "Costs nothing and heals nothing. It is the single most reported thing",
                "about an old fracture by the people who have one, and it hands a player",
                "something no readout in this mod does: a reason to notice the sky.");
        num("acheSeconds", 75, 1, 100000, Kind.WHOLE, v -> v.acheSeconds, (v, x) -> v.acheSeconds = (int) x,
                "Roughly how many seconds between one twinge and the next.");
        flag("stretch", true, v -> v.stretch, (v, x) -> v.stretch = x,
                "Whether a stiff part can be worked loose by hand.");
        num("stretchGain", 0.20, 0.0, 1.0, Kind.SINGLE, v -> v.stretchGain, (v, x) -> v.stretchGain = (float) x,
                "Stiffness a perfect session takes off. One session per part per day.");
        num("stretchSelfCap", 0.5, 0.0, 1.0, Kind.SINGLE, v -> v.stretchSelfCap, (v, x) -> v.stretchSelfCap = (float) x,
                "The most of that anybody gets working on their own limb. You stop at",
                "the pain, because it is your pain, which is precisely why this is a",
                "thing done to people rather than by them.");
        num("stretchTear", 2.5, 0.0, 1000.0, Kind.SINGLE, v -> v.stretchTear, (v, x) -> v.stretchTear = (float) x,
                "Damage done by a session pushed past the joint rather than to it.");
    }

    private static void splints() {
        section("splints",
                "",
                "The stick, after it is tied on.",
                "",
                "A splint used to be a full stop: the bone was set, the item was spent,",
                "and the stick vanished. Now it stays on the limb - drawn on the body,",
                "on the chart, and holding the limb still for as long as it is there.",
                "",
                "The clock on it is not a timer, it is a bargain. Walking runs it down.",
                "Running on a set bone puts time back on, every second of it, which is",
                "the honest version of what happens to a fracture nobody will stay off:",
                "it does not start you over, it simply takes longer - and longer again",
                "the next time you do it.");
        flag("wornSplints", true, v -> v.wornSplints, (v, x) -> v.wornSplints = x);
        num("splintSeconds", 300, 1, 100000, Kind.WHOLE, v -> v.splintSeconds, (v, x) -> v.splintSeconds = (int) x,
                "How long one has to stay on before the bone is done with it.");
        num("splintRunPenaltySeconds", 3, 0, 100000, Kind.WHOLE, v -> v.splintRunPenalty, (v, x) -> v.splintRunPenalty = (int) x,
                "Seconds put back on per second of running on a splinted leg.");
        num("splintCeilingSeconds", 900, 1, 100000, Kind.WHOLE, v -> v.splintCeilingSeconds, (v, x) -> v.splintCeilingSeconds = (int) x,
                "And the most it can ever be pushed out to, so that a bad week still",
                "ends. Nothing in this mod should be able to become forever by accident.");
        num("splintSlow", 0.18, 0.0, 1.0, Kind.SINGLE, v -> v.splintSlow, (v, x) -> v.splintSlow = (float) x,
                "What a splinted leg costs off walking speed while it is on.");
        num("splintQuality", 0.25, 0.0, 1.0, Kind.SINGLE, v -> v.splintQuality, (v, x) -> v.splintQuality = (float) x,
                "And what a splinted arm costs the hands on a treatment screen.");
    }

    private static void dismemberment() {
        section("dismemberment",
                "",
                "Limbs taken off by whatever was swinging.",
                "",
                "The mod could already take an arm off with a blast or with a saw, and",
                "the one thing in between - somebody hitting you with an axe - did",
                "nothing but damage. That is the wrong gap to have: the blast is rare",
                "and the saw is a decision, so in practice nobody ever lost a limb to",
                "the thing that actually takes limbs off.",
                "",
                "It needs a blade. A zombie does not take an arm off, a skeleton does",
                "not, and a fall certainly does not - what does it is an edge, swung",
                "hard, into a limb that is already ruined.");
        flag("severing", true, v -> v.severing, (v, x) -> v.severing = x);
        num("severBlow", 7.0, 0.0, 1000.0, Kind.SINGLE, v -> v.severBlow, (v, x) -> v.severBlow = (float) x,
                "How hard the blow has to be, after armour.");
        num("severChance", 0.35, 0.0, 1.0, Kind.SINGLE, v -> v.severChance, (v, x) -> v.severChance = (float) x,
                "Chance of it, on a limb that has already been beaten to nothing.");
        num("severHealthyChance", 0.04, 0.0, 1.0, Kind.SINGLE, v -> v.severHealthyChance, (v, x) -> v.severHealthyChance = (float) x,
                "And the chance on a limb that is still in one piece, which is the",
                "clean stroke everybody pictures and nearly nobody lands. Set it to zero",
                "for a game where limbs only come off things that are already wrecked.");
    }

    private static void medicine() {
        section("medicine",
                "Extra items that count as medicine, on top of the mod's own.",
                "",
                "The mod's bandage, splint, medkit, surgical kit and painkillers already",
                "work on their own and are not listed here. These lists are for pointing",
                "the same treatments at somebody else's items - another medical mod, or",
                "plain wool if that is the game you want. Items listed here are used with",
                "Shift + right click, so an ordinary block still places normally.",
                "",
                "Entries are item ids ('minecraft:stick') or item tags ('#minecraft:wool').",
                "",
                " bandages stop bleeding on one part and patch it up a little",
                " splints set a broken bone",
                " medkits heal the part in the worst shape",
                " surgery bring a blacked-out limb back, partly",
                " painkillers hide the penalties for a while, healing nothing",
                " salines put lost blood volume back, once nothing is still open");
        items("bandages", MedicineKind.BANDAGE);
        items("tourniquets", MedicineKind.TOURNIQUET);
        items("splints", MedicineKind.SPLINT);
        items("medkits", MedicineKind.MEDKIT);
        items("surgery", MedicineKind.SURGERY);
        items("painkillers", MedicineKind.PAINKILLER);
        items("antiseptics", MedicineKind.ANTISEPTIC);
        items("salves", MedicineKind.SALVE);
        items("antibiotics", MedicineKind.ANTIBIOTICS);
        items("salines", MedicineKind.TRANSFUSION);
        num("bandageHeal", 2.0, 0.0, 1000.0, Kind.SINGLE, v -> v.bandageHeal, (v, x) -> v.bandageHeal = (float) x);
        num("medkitHeal", 12.0, 0.0, 1000.0, Kind.SINGLE, v -> v.medkitHeal, (v, x) -> v.medkitHeal = (float) x);
        num("salveHeal", 3.0, 0.0, 1000.0, Kind.SINGLE, v -> v.salveHeal, (v, x) -> v.salveHeal = (float) x,
                "What burn salve puts back, on top of clearing the burn.");
        num("surgeryRestore", 0.3, 0.05, 1.0, Kind.SINGLE, v -> v.surgeryRestore, (v, x) -> v.surgeryRestore = (float) x,
                "Share of its maximum a blacked-out part comes back at.");
        num("transfusionRestore", 0.5, 0.01, 1.0, Kind.SINGLE, v -> v.transfusionRestore, (v, x) -> v.transfusionRestore = (float) x,
                "Share of a full body of blood one drip puts back.",
                "",
                "It is refused outright while anything is still bleeding, so this cannot",
                "be used to outrun a wound - only to undo one that has been closed.");
        num("bloodRestore", 0.85, 0.01, 1.0, Kind.SINGLE, v -> v.bloodRestore, (v, x) -> v.bloodRestore = (float) x,
                "Share a bag of matching blood puts back. More than saline, because it",
                "is the real thing - and because somebody had to bleed for it.");
        num("donationCost", 0.3, 0.01, 1.0, Kind.SINGLE, v -> v.donationCost, (v, x) -> v.donationCost = (float) x,
                "Share of their own blood a donor loses filling one bag.");
        num("maxDonorLoss", 0.5, 0.05, 1.0, Kind.SINGLE, v -> v.maxDonorLoss, (v, x) -> v.maxDonorLoss = (float) x,
                "How empty a donor may be left. A draw that would take them past this",
                "is refused, so nobody can be bled to death one bag at a time.");
    }

    static {
        complications();
        difficulty();
        health();
        routing();
        bleeding();
        bleedingBlood();
        fractures();
        armour();
        infection();
        burns();
        reactions();
        allergies();
        grafts();
        parasites();
        diseases();
        diseasesConcussion();
        diseasesShock();
        diseasesHypothermia();
        diseasesReaction();
        chest();
        effects();
        effectsGuns();
        clots();
        sleep();
        displacement();
        artery();
        faint();
        scars();
        splints();
        dismemberment();
        medicine();
    }

    /** One line of the medicine config: either an item tag or a single item. */
    public record ItemMatcher(@Nullable TagKey<Item> tag, @Nullable ResourceLocation item) {
        public boolean matches(ItemStack stack) {
            if (tag != null) {
                return stack.is(tag);
            }
            return item != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }
    }

    /** A snapshot of the config, as plain fields. Defaults match the spec above. */
    public static final class Values {
        /**
         * Whether the chart is a readout or a form. See the config comment, and chapter 0
         * of DESIGN.md - this is the one flag the rest of the mod asks about before it
         * decides what a player is allowed to know.
         */
        public boolean realistic = false;

        public int tetanusSeconds = 420;
        public float tetanusChance = 0.0016F;
        public int crushSeconds = 75;
        public int crushStrapSeconds = 180;
        public float relapseChance = 0.18F;

        public float headMax = 10.0F;
        public float thoraxMax = 22.0F;
        public float stomachMax = 16.0F;
        public float armMax = 12.0F;
        public float legMax = 14.0F;
        public float damageScale = 2.0F;
        public float healScale = 2.0F;
        public boolean regeneration = true;
        public float overflowToThorax = 0.5F;
        public float blackedLimbDrain = 0.15F;
        public float oneShotProtectionAbove = 0.5F;
        public float oneShotFloor = 1.0F;

        public int headWeight = 8;
        public int thoraxWeight = 25;
        public int stomachWeight = 15;
        public int armWeight = 13;
        public int legWeight = 13;

        public float lightBleedRate = 0.25F;
        public float heavyBleedRate = 0.7F;
        public float bleedChancePerDamage = 0.05F;
        public float heavyBleedThreshold = 10.0F;
        public int lightBleedSeconds = 60;
        public boolean clots = true;
        public float clotChance = 0.00008F;
        public int clotStillSeconds = 90;
        public int clotClearSeconds = 240;
        public float embolismChance = 0.002F;
        public float embolismDamage = 0.28F;
        public float sleepyFrom = 0.25F;
        public float microsleepFrom = 0.7F;
        public float microsleepChance = 0.010F;
        public int microsleepSeconds = 2;
        public boolean displacement = true;
        public float displacedChance = 0.5F;
        public float displacedBlow = 10.0F;
        public boolean openFractures = true;
        public float openChance = 0.2F;
        public float openInfection = 3.0F;
        public float openBloodLoss = 0.08F;
        public float crookedCost = 0.12F;
        public float crookedPenalty = 0.15F;
        public float setTear = 4.0F;
        public float setArteryChance = 0.2F;
        public boolean scream = true;
        public float screamRange = 32.0F;
        public boolean arterial = true;
        public float arterialChance = 0.12F;
        public float arterialBlow = 14.0F;
        public float arterialRate = 2.0F;
        public float bloodPerArterial = 0.045F;
        public boolean arterialSpurt = true;
        public boolean faint = true;
        public float faintBloodLoss = 0.42F;
        public float faintChance = 0.020F;
        public int faintSeconds = 7;
        public int faintPain = 8;
        public int faintClearSeconds = 25;
        public boolean scars = true;
        public float scarStiffness = 0.10F;
        public float burnStiffness = 0.35F;
        public float graftStiffness = 0.40F;
        public float stiffQuality = 0.35F;
        public float stiffSlow = 0.20F;
        public float scarInfection = 1.7F;
        public float knitFragility = 0.78F;
        public boolean ache = true;
        public int acheSeconds = 75;
        public boolean stretch = true;
        public float stretchGain = 0.20F;
        public float stretchSelfCap = 0.5F;
        public float stretchTear = 2.5F;
        public boolean wornSplints = true;
        public int splintSeconds = 300;
        public int splintRunPenalty = 3;
        public int splintCeilingSeconds = 900;
        public float splintSlow = 0.18F;
        public float splintQuality = 0.25F;
        public boolean severing = true;
        public float severBlow = 7.0F;
        public float severChance = 0.35F;
        public float severHealthyChance = 0.04F;
        public float bloodPerLight = 0.004F;
        public float bloodPerHeavy = 0.010F;
        public float bloodRecovery = 0.0030F;
        public int bloodRecoveryHunger = 7;
        public boolean bloodLossKills = true;
        public boolean bloodParticles = true;
        public int bloodStainSeconds = 300;
        public float tourniquetDamage = 0.05F;

        public float uncoveredMultiplier = 1.3F;
        public boolean aimedHits = true;

        public boolean fractures = true;
        public float fractureFall = 4.0F;
        public float fractureBlow = 14.0F;
        public float fractureChance = 0.5F;
        public float fracturePain = 1.5F;
        public float crackBlow = 8.0F;
        public float crackChance = 0.45F;
        public int crackHealSeconds = 90;
        public float crackBreakChance = 0.5F;

        public boolean infection = true;
        public int bleedSecondsBeforeRisk = 45;
        public int blackedSecondsBeforeRisk = 60;
        public float infectionRisk = 0.012F;
        public int infectionWorsenSeconds = 90;
        public int infectionFeverSeconds = 60;
        public float infectionMildDamage = 0.08F;
        public float infectionSevereDamage = 0.2F;

        public boolean burns = true;
        public float burnChance = 0.08F;
        public int burnCookSeconds = 30;
        public float burnDeepenChance = 0.05F;
        public int burnFadeSeconds = 90;
        public float burnCoolBack = 0.5F;
        public float burnIceFactor = 2.0F;
        public float burnFluidFloor = 0.15F;
        public float burnFluidDrain = 0.02F;
        public float burnChill = 0.004F;
        public float smokeChance = 0.08F;
        public int smokeSeconds = 240;
        public float smokeBreath = 0.35F;
        public boolean reactions = true;
        public boolean allergies = true;
        public float allergyChance = 0.18F;
        public boolean grafts = true;
        public int graftSeconds = 300;
        public float graftColdFactor = 0.33F;
        public float graftRestore = 0.45F;
        public float parasiteChance = 0.25F;
        public float parasiteHunger = 0.08F;
        public float parasiteDrain = 0.0006F;

        public boolean diseases = true;
        public int coldSeconds = 240;
        public int poisoningSeconds = 180;
        public int feverSeconds = 300;
        public float sepsisDamage = 0.3F;
        public float poisoningDamage = 0.12F;
        public float coldRisk = 0.02F;
        public float foodPoisonChance = 0.35F;
        public int concussionSeconds = 75;
        public float concussionBlow = 6.0F;
        public float concussionChance = 0.4F;
        public boolean shock = true;
        public float shockBloodLoss = 0.30F;
        public float shockClearBloodLoss = 0.18F;
        public int shockRestSeconds = 30;
        public int shockBlackedParts = 2;
        public float shockDownedRate = 1.5F;
        public boolean hypothermia = true;
        public float hypothermiaRisk = 0.015F;
        public int hypothermiaWarmSeconds = 45;
        public boolean pneumothorax = true;
        public float pneumoBlow = 9.0F;
        public float pneumoChance = 0.35F;
        public float pneumoDamage = 0.25F;
        public int pneumoRampSeconds = 75;
        public int reactionSeconds = 90;
        public float reactionDamage = 0.45F;
        public float reactionBloodLoss = 0.006F;

        public boolean effects = true;
        public float regenPenalty = 1.0F;
        public int painkillerSeconds = 45;
        public float painkillerFalloff = 0.6F;
        public int painkillerToleranceSeconds = 240;
        public boolean heartbeat = true;
        public boolean assess = true;
        public boolean tags = true;
        public int tagStaleSeconds = 120;
        public boolean minigame = true;
        public float interruptDamage = 3.0F;
        public boolean downed = true;
        public int downedSeconds = 45;
        public boolean downedIgnoredByMobs = true;
        public float downedDamageSeconds = 3.0F;
        public int stabiliseSeconds = 30;
        public float stabiliseFalloff = 0.5F;
        public boolean ragdoll = true;
        public boolean carry = true;
        public double carrySlow = 0.45;
        public float downedCarriedRate = 0.5F;
        public boolean airway = true;
        public int airwaySeconds = 20;
        public float airwayDownedRate = 2.0F;
        public boolean cpr = true;
        public int arrestSeconds = 25;
        public float cprFalloff = 0.6F;
        public float cprRibChance = 0.3F;
        public boolean pressure = true;
        public int pressureSeconds = 12;
        public int whisperSeconds = 25;
        public int phantomSeconds = 180;
        public float catatoniaChance = 0.0008F;
        public int catatoniaSeconds = 30;
        public float neurolepticChance = 0.03F;
        public float neurolepticDamage = 0.5F;
        public int neurolepticCoolSeconds = 25;
        public boolean delirium = true;
        public float deliriumBloodLoss = 0.5F;
        public int deliriumClearSeconds = 20;
        public boolean schizophrenia = true;
        public float schizophreniaChance = 0.1F;
        public int antipsychoticSeconds = 600;
        public int reboundSeconds = 60;
        public boolean organs = true;
        public float organBlow = 11.0F;
        public float organChance = 0.30F;
        public float organHeartStopChance = 0.35F;
        public float organBleed = 0.007F;
        public float organDamage = 0.12F;
        public float organSepsis = 0.012F;
        public int organHealSeconds = 150;
        public boolean reperfusion = true;
        public float reperfusionDamage = 7.0F;
        public int dressingStuckSeconds = 120;
        public boolean anaesthesia = true;
        public int anaesthesiaSeconds = 90;
        public float anaesthesiaArrest = 0.35F;
        public float awakeQualityCap = 0.5F;
        public float awakeDamage = 3.0F;
        public boolean dressings = true;
        public int dressingCleanSeconds = 480;
        public boolean necrosis = true;
        public int strapDeadSeconds = 180;
        public int rotDeadSeconds = 240;
        public float deadLimbDrain = 0.20F;
        public float deadLimbSepsis = 0.010F;
        public boolean amputation = true;
        public float amputationBloodLoss = 0.12F;
        public float amputationOpenBloodLoss = 0.20F;
        public boolean prosthetics = true;
        public float prostheticRatio = 0.45F;
        public float prostheticRatioFine = 0.75F;
        public boolean rabies = true;
        public float rabiesChance = 0.14F;
        public int rabiesSeconds = 300;
        public int frenzySeconds = 90;
        public float frenzyDamage = 0.35F;
        public int deafnessSeconds = 90;
        public boolean blast = true;
        public float tearOffBlow = 6.0F;
        public float tearOffChance = 0.5F;
        public boolean blastTearOff = true;
        public float blastTearChance = 0.35F;
        public boolean blastFragments = true;
        public boolean blastWave = true;
        public float blastArmourStop = 0.85F;
        public float blastLungChance = 0.45F;
        public float blastGutChance = 0.25F;
        public float blastBurnChance = 0.5F;
        public boolean mending = true;
        public int mendingSeconds = 90;
        public float mendingTearChance = 0.10F;
        public boolean sleepless = true;
        public int sleeplessDays = 3;
        public float sleeplessHands = 0.2F;
        public float sleeplessIllness = 1.0F;
        public float sleeplessMending = 0.5F;
        public int epinephrineRounds = 2;
        public float epinephrineBleed = 0.06F;
        public float epinephrineArrestChance = 0.35F;
        public int morphineSeconds = 75;
        public boolean morphineDepresses = true;
        public float morphineStopsBreathing = 0.4F;
        public int naloxoneShockPain = 3;
        public float naloxoneShockChance = 0.5F;
        public int fluSeconds = 300;
        public float fluRisk = 0.0008F;
        public int fluCrowd = 3;
        public float fluPneumonia = 0.005F;
        public int stomachSeconds = 240;
        public float stomachChance = 0.35F;
        public float stomachDrain = 0.004F;
        public float stomachDamage = 0.04F;
        public boolean thermometer = true;
        public boolean rinse = true;
        public int rinseSeconds = 6;
        public int rinseFloor = 1;
        public boolean referredPain = true;
        public boolean chill = true;
        public float chillPerOperation = 0.3F;
        public float chillDegrees = 2.5F;
        public float chillHypothermia = 1.0F;
        public int chillWarmSeconds = 90;
        public float sneezeChance = 0.04F;
        public float sneezeDamage = 0.8F;
        public float coldCatching = 0.25F;
        public int pneumoniaAfterSeconds = 180;
        public float pneumoniaChance = 0.01F;
        public float pneumoniaDamage = 0.06F;
        public int pneumoniaSepsisSeconds = 300;
        public float peritonitisDamage = 0.12F;
        public int peritonitisSepsisSeconds = 240;
        public boolean adrenaline = true;
        public int adrenalineSeconds = 25;
        public float adrenalineRelief = 0.8F;
        public int adrenalineCrashSeconds = 20;
        public float adrenalineCrashBite = 0.3F;
        public float adrenalineShake = 0.15F;
        public float adrenalinePulse = 30.0F;
        public boolean fatigue = true;
        public int fatigueSeconds = 90;
        public int fatigueRecoverSeconds = 45;
        public float fatigueQuality = 0.25F;
        public float fatigueCarrySlow = 0.5F;
        public float fatigueHold = 0.15F;
        public boolean assist = true;
        public float assistQuality = 0.15F;
        public float assistAwakeCap = 0.7F;
        public boolean theatre = true;
        public float grimeInfection = 0.55F;
        public float roomInfection = 0.18F;
        public float placeQuality = 0.25F;
        public boolean pulse = true;
        public boolean breathing = true;
        public boolean temperature = true;
        public boolean ask = true;
        public float organBrokenBonus = 2.0F;
        public int prostheticWearSeconds = 2400;
        public float prostheticWornRatio = 0.55F;
        public boolean phantomPain = true;
        public int phantomPainSeconds = 90;
        public boolean arrows = true;
        public float arrowChance = 0.35F;
        public float arrowTear = 3.0F;
        public boolean bulletsStick = true;
        public float bulletStickChance = 0.25F;
        public float bulletTear = 3.0F;
        public boolean weaponsStick = true;
        public float stickChance = 0.06F;
        public float stickDamage = 5.0F;
        public float stickJostle = 0.25F;
        public float stickJostleBleed = 0.06F;
        public float stickTear = 4.0F;
        public boolean breathSounds = true;
        public int findingFreshSeconds = 120;
        public boolean bedsideObs = true;
        public int bedsideSeconds = 40;
        public int bedsideRange = 3;
        public boolean panic = true;
        public float panicChance = 0.035F;

        public float bandageHeal = 2.0F;
        public float medkitHeal = 12.0F;
        public float salveHeal = 3.0F;
        public float surgeryRestore = 0.3F;
        public float transfusionRestore = 0.5F;
        public float bloodRestore = 0.85F;
        public float donationCost = 0.3F;
        public float maxDonorLoss = 0.5F;

        public final Map<MedicineKind, List<ItemMatcher>> medicine = new EnumMap<>(MedicineKind.class);

        public List<ItemMatcher> medicine(MedicineKind kind) {
            return medicine.getOrDefault(kind, List.of());
        }
    }
}
