package com.claudemods.triage.body;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Which way a body is going, worked out from two readings of the same thing.
 *
 * <p>The mod has always been able to say what a body <em>is</em>. This is the first thing
 * in it that can say what a body is <b>doing</b>, and the difference is most of medicine:
 * a pulse of a hundred and twelve is a number, and a pulse of a hundred and twelve on
 * somebody who was at ninety four minutes ago is an answer.
 *
 * <p>It cannot be bought, faked or read off a healthy-looking person. A direction costs a
 * second visit, and there is no other way to get one - which makes it the only finding in
 * the mod whose price is patience rather than an item or a minigame.
 *
 * <p>Nothing here interprets. A rising pulse is not labelled "internal bleeding", because
 * a rising pulse is also a frightened person, a sprinting person and somebody whose fever
 * is climbing. The mod says the arithmetic and stops; the player does the rest, and that
 * is the entire design of the second difficulty applied to a number.
 */
public record Trend(int delta, long seconds) {
    /** How much has to move before it is worth saying, by kind. */
    private static int worth(int kind) {
        return switch (kind) {
            case Reading.PULSE -> 8;
            // Tenths of a degree: three of them is the difference between a warm person
            // and a person with something starting.
            case Reading.TEMP -> 3;
            case Reading.BREATH -> 4;
            case Reading.PAIN -> 2;
            default -> 1;
        };
    }

    /**
     * The newest reading of this kind against the one before it, or null when there is
     * only one - which is the usual state of affairs and the point of the whole class.
     */
    public static @Nullable Trend of(BodyHealth body, int kind) {
        Reading now = body.latest(kind);
        Reading before = body.previous(kind);
        if (now == null || before == null) {
            return null;
        }
        return new Trend(now.value() - before.value(), Math.max(0L, now.at() - before.at()) / 20L);
    }

    public boolean worthSaying(int kind) {
        return Math.abs(delta) >= worth(kind);
    }

    /** Up, down, or level, as one character that survives being read at a glance. */
    public String arrow() {
        return delta > 0 ? "\u2191" : delta < 0 ? "\u2193" : "\u2192";
    }

    public ChatFormatting colour(int kind) {
        if (!worthSaying(kind)) {
            return ChatFormatting.GRAY;
        }
        // Which direction is the bad one depends on what is being measured, and getting
        // this wrong would be the mod colouring a recovery red. A falling pain score is
        // good news; a falling temperature on somebody who is bleeding is not, and the
        // mod does not pretend to know which - so only pain gets a direction it likes,
        // and everything else is coloured for having moved at all.
        if (kind == Reading.PAIN) {
            return delta < 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        }
        return ChatFormatting.GOLD;
    }

    /**
     * Said to whoever took the second one, in the moment they took it.
     *
     * <p>On the chart the arrow is there to be looked up; here it arrives without being
     * asked for, because the person holding the wrist has just done the work and this is
     * what the work was for. Nothing is said when nothing moved: a chart that announces
     * "no change" every four minutes teaches people to stop reading it.
     */
    public static void say(ServerPlayer helper, BodyHealth body, int kind) {
        Trend trend = of(body, kind);
        if (trend == null || !trend.worthSaying(kind) || trend.seconds() <= 0L) {
            return;
        }
        String name = switch (kind) {
            case Reading.PULSE -> "triage.trend.pulse";
            case Reading.TEMP -> "triage.trend.temp";
            case Reading.BREATH -> "triage.trend.breath";
            default -> "triage.trend.pain";
        };
        // Tenths print as a decimal and everything else as a whole number, because that
        // is how each of them is written everywhere else in this mod.
        String moved = kind == Reading.TEMP
                ? String.format("%+.1f", trend.delta() / 10.0F)
                : String.format("%+d", trend.delta());
        Body.notify(helper, Component.translatable("triage.msg.trend",
                        Component.translatable(name), moved, trend.minutes()),
                trend.colour(kind), null);
    }

    /** Rounded up, because "one minute ago" is a better lie than "zero minutes ago". */
    public long minutes() {
        return Math.max(1L, (seconds + 59L) / 60L);
    }
}
