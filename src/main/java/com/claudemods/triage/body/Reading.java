package com.claudemods.triage.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One reading somebody took, with the time on it and the hand that took it.
 *
 * <p>The mod already wrote down the <em>last</em> of each of these. What it never kept was
 * the one before - and the one before is the whole of what a reading is for. A number on
 * its own is a fact about a body; two numbers are a fact about <b>where that body is
 * going</b>, which is the thing every ward in the world is built to notice and the thing
 * this mod has never once been able to say.
 *
 * <p>Deliberately dumb: a kind, an integer, a tick and a name. No interpretation is stored,
 * because interpretation is the player's job - the chart prints what was found and how long
 * ago, and the arrow beside it is arithmetic rather than an opinion.
 */
public record Reading(int kind, int value, long at, String by) {
    /** Beats a minute. */
    public static final int PULSE = 0;
    /** Tenths of a degree, the way the thermometer already stores one. */
    public static final int TEMP = 1;
    /** Breaths a minute, off a stethoscope. */
    public static final int BREATH = 2;
    /** Out of ten, and an opinion rather than a measurement - see {@link Pain}. */
    public static final int PAIN = 3;

    public static final Codec<Reading> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("kind").forGetter(Reading::kind),
            Codec.INT.fieldOf("value").forGetter(Reading::value),
            Codec.LONG.fieldOf("at").forGetter(Reading::at),
            Codec.STRING.optionalFieldOf("by", "").forGetter(Reading::by)
    ).apply(i, Reading::new));

    /** How long ago this was, in seconds, against the clock the caller is holding. */
    public long secondsBefore(long now) {
        return Math.max(0L, now - at) / 20L;
    }
}
