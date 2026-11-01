package com.claudemods.triage.body;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * What kind of round a wound is holding, as far as the wound is concerned.
 *
 * <p>A bullet was one word and one shape for as long as the mod had one gun mod to talk
 * to. That was already a lie at the point it was written: a 9mm pistol round and a 7.62
 * rifle round do not look alike, do not go in alike, and do not come out alike, and the
 * screen that asks somebody to fish one out of a leg should not draw the same stub for
 * both. The count stays a count - two rounds are two rounds - but every round now keeps
 * the ammunition it came from, and the drawing follows it.
 *
 * <p>TaCZ's built-in ammunition is listed here by the id it actually reports, which is a
 * resource location such as {@code tacz:762x39}. The list is the twenty-five rounds the
 * default pack ships. A gun pack can add its own; those keep their own id and fall back
 * to the nearest shape by name rather than being turned into a ninth 9mm.
 *
 * <p>Nothing in this class touches a TaCZ class. The compatibility layer reads the id by
 * reflection for the same reason it reads the damage source that way: the gun mod is
 * optional, and a build this was not written against must still put a round in a leg.
 */
public final class BulletKinds {
    /** A round from a pack with no shape of its own. Still a round, just not a named one. */
    public static final String UNKNOWN = "triage:unknown";

    /**
     * The part of a round that faces the surgeon: the nose.
     *
     * <p>The body of every round is a cylinder with a jacket and, usually, a driving band.
     * What changes between a pistol round and a rifle round is how that cylinder ends, and
     * that is most of what makes one recognisable at a glance.
     */
    public enum Nose {
        /** A pistol round: a hemisphere, wide and blunt. */
        ROUND,
        /** A rifle round: a long ogive that comes almost to a point. */
        SPITZER,
        /** A flat-nosed or wadcutter round, which leaves a full-width face. */
        FLAT,
        /** A hollow point: a round nose with a cavity in the middle. */
        HOLLOW,
        /** One pellet of shot. */
        PELLET,
        /** A rocket, with fins at the base. */
        ROCKET
    }

    /**
     * One kind of round, in the terms the drawing needs.
     *
     * @param length how many times its own size long the round is
     * @param half   how many times its own size wide the half-width is
     * @param jacket the main colour of the projectile
     * @param band   the colour of the driving band, when there is one
     * @param banded whether a driving band is drawn at all
     */
    public record Kind(String id, String label, float length, float half,
                       Nose nose, int jacket, int band, boolean banded) {
    }

    private static final int COPPER = 0xFFB87333;
    private static final int BRASS = 0xFFC9A227;
    private static final int STEEL = 0xFFC3C9D0;
    private static final int LEAD = 0xFF949494;
    private static final int OLIVE = 0xFF6B6F3A;

    private static final Map<String, Kind> KINDS = new LinkedHashMap<>();
    private static final List<String> SUGGESTIONS = new ArrayList<>();

    /**
     * Shapes for rounds from another pack. They are not in {@link #KINDS} because their
     * ids belong to whoever shipped them; only the outline is borrowed.
     */
    private static final Kind CUSTOM_PISTOL = new Kind("triage:custom_pistol",
            "triage.bullet.unknown", 1.70F, 0.36F, Nose.ROUND, LEAD, LEAD, false);
    private static final Kind CUSTOM_RIFLE = new Kind("triage:custom_rifle",
            "triage.bullet.unknown", 2.05F, 0.36F, Nose.SPITZER, COPPER, COPPER, false);
    private static final Kind CUSTOM_SHOT = new Kind("triage:custom_shot",
            "triage.bullet.unknown", 0.70F, 0.46F, Nose.PELLET, LEAD, LEAD, false);
    private static final Kind CUSTOM_LAUNCHER = new Kind("triage:custom_launcher",
            "triage.bullet.unknown", 1.30F, 0.62F, Nose.ROUND, OLIVE, OLIVE, false);

    static {
        // Pistol and revolver rounds: short, wide, and mostly blunt.
        add("9mm", 1.70F, 0.36F, Nose.ROUND, COPPER, true);
        add("22wmr", 1.35F, 0.30F, Nose.ROUND, COPPER, false);
        add("357mag", 1.75F, 0.38F, Nose.FLAT, COPPER, true);
        add("45acp", 1.65F, 0.44F, Nose.ROUND, COPPER, true);
        add("46x30", 1.55F, 0.30F, Nose.SPITZER, STEEL, false);
        add("50ae", 1.80F, 0.48F, Nose.FLAT, COPPER, true);
        add("500mag", 2.10F, 0.50F, Nose.FLAT, COPPER, true);
        add("57x28", 1.65F, 0.30F, Nose.SPITZER, STEEL, false);
        add("762x25", 1.60F, 0.36F, Nose.ROUND, COPPER, true);

        // Intermediate and rifle rounds: long, narrow, and pointed.
        add("300blk", 1.90F, 0.36F, Nose.SPITZER, COPPER, false);
        add("308", 2.25F, 0.37F, Nose.SPITZER, COPPER, false);
        add("30_06", 2.40F, 0.37F, Nose.SPITZER, COPPER, false);
        add("338", 2.60F, 0.42F, Nose.SPITZER, COPPER, false);
        add("545x39", 2.00F, 0.32F, Nose.SPITZER, STEEL, false);
        add("556x45", 2.00F, 0.32F, Nose.SPITZER, COPPER, false);
        add("58x42", 2.00F, 0.33F, Nose.SPITZER, STEEL, false);
        add("68x51fury", 2.15F, 0.38F, Nose.SPITZER, COPPER, false);
        add("762x39", 2.05F, 0.37F, Nose.SPITZER, COPPER, false);
        add("762x54", 2.40F, 0.38F, Nose.SPITZER, COPPER, false);
        add("792x57", 2.40F, 0.40F, Nose.SPITZER, COPPER, false);
        add("50bmg", 3.00F, 0.48F, Nose.SPITZER, COPPER, false);

        // Shotgun and big-bore: the pellets and the flat-nosed thumpers.
        add("12g", 0.70F, 0.46F, Nose.PELLET, LEAD, false);
        add("45_70", 2.10F, 0.48F, Nose.FLAT, LEAD, false);

        // Launchers: fat, olive, and not really a bullet at all.
        add("40mm", 1.30F, 0.62F, Nose.ROUND, OLIVE, false);
        add("rpg_rocket", 2.80F, 0.52F, Nose.ROCKET, OLIVE, false);

        KINDS.put(UNKNOWN, new Kind(UNKNOWN, "triage.bullet.unknown",
                1.70F, 0.36F, Nose.ROUND, STEEL, COPPER, true));
        SUGGESTIONS.add("unknown");
    }

    private BulletKinds() {
    }

    private static void add(String path, float length, float half, Nose nose, int jacket, boolean banded) {
        Kind kind = new Kind("tacz:" + path, "triage.bullet." + path,
                length, half, nose, jacket, banded ? BRASS : jacket, banded);
        KINDS.put(kind.id(), kind);
        SUGGESTIONS.add(path);
    }

    /**
     * The shape and name for an id, with a useful guess for a round this list has never
     * seen. A pack's own id is kept by {@link #canonical(String)}; this only decides how
     * it is drawn and what it is called.
     */
    public static Kind of(String id) {
        if (id == null || id.isBlank()) {
            return KINDS.get(UNKNOWN);
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (key.equals("unknown")) {
            return KINDS.get(UNKNOWN);
        }
        Kind exact = KINDS.get(key);
        if (exact != null) {
            return exact;
        }
        if (!key.contains(":")) {
            exact = KINDS.get("tacz:" + key);
            if (exact != null) {
                return exact;
            }
        }
        if (key.endsWith(":empty") || key.equals("empty")) {
            return KINDS.get(UNKNOWN);
        }
        return guess(key);
    }

    /**
     * The id to store: a known round gets its full {@code tacz:} name, a pack's round
     * keeps whatever it was called. Short names are accepted because typing
     * {@code 762x39} at a command prompt is what a person will do.
     */
    public static String canonical(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (key.equals("unknown")) {
            return UNKNOWN;
        }
        if (KINDS.containsKey(key)) {
            return key;
        }
        if (!key.contains(":")) {
            String tacz = "tacz:" + key;
            if (KINDS.containsKey(tacz)) {
                return tacz;
            }
        }
        if (key.endsWith(":empty") || key.equals("empty")) {
            return UNKNOWN;
        }
        return key;
    }

    /** The short names a command should offer, without the namespace every one shares. */
    public static List<String> suggestions() {
        return List.copyOf(SUGGESTIONS);
    }

    /** A translated name for one round, e.g. "7.62×39 мм". */
    public static Component label(String id) {
        return Component.translatable(of(id).label());
    }

    /**
     * The names of what is in a wound, for a title or a command line.
     *
     * <p>Two names at most, because a title has one line and five calibres in it stops
     * being a title. The rest are still counted, so nothing is hidden.
     */
    public static Component summary(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return label(UNKNOWN);
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (String id : ids) {
            labels.add(of(id).label());
        }
        List<String> keys = new ArrayList<>(labels);
        int shown = Math.min(2, keys.size());
        var out = Component.empty();
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                out.append(Component.literal(", "));
            }
            out.append(Component.translatable(keys.get(i)));
        }
        if (keys.size() > shown) {
            out.append(Component.literal(" +" + (keys.size() - shown)));
        }
        return out;
    }

    /**
     * A shape for an id nobody listed. The checks are ordered from the most specific
     * word to the least, so a pack that calls a rifle round {@code 50bmg_ap} is not
     * mistaken for a .50 pistol round.
     */
    private static Kind guess(String key) {
        String path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        if (path.contains("shot") || path.contains("buck") || path.contains("bird")
                || path.contains("12g") || path.contains("20g")) {
            return CUSTOM_SHOT;
        }
        if (path.contains("rpg") || path.contains("rocket") || path.contains("40mm")
                || path.contains("grenade") || path.contains("launcher")) {
            return CUSTOM_LAUNCHER;
        }
        if (path.contains("bmg") || path.contains("lapua") || path.contains("338")
                || path.contains("308") || path.contains("30_") || path.contains("rifle")
                || path.contains("nato") || path.contains("x39") || path.contains("x45")
                || path.contains("x51") || path.contains("x54") || path.contains("x57")) {
            return CUSTOM_RIFLE;
        }
        if (path.contains("9mm") || path.contains("45") || path.contains("357")
                || path.contains("44") || path.contains("50ae") || path.contains("500")
                || path.contains("22") || path.contains("25") || path.contains("32")
                || path.contains("380") || path.contains("mag") || path.contains("acp")) {
            return CUSTOM_PISTOL;
        }
        return CUSTOM_RIFLE;
    }
}
