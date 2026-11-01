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

//<<SECTIONS>>

    static {
//<<CALLS>>
    }

//<<CARRIED>>
}
