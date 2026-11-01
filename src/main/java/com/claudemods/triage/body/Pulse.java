package com.claudemods.triage.body;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * What a heart is doing, as the one number you can get with nothing but two fingers.
 *
 * <p>Every other reading in this mod comes off a chart. A pulse does not: it is the
 * first thing anybody does to anybody, it needs no item and no light and no time, and
 * it is the reason trauma medicine can triage a field full of people in a minute.
 *
 * <p>And it is the only sign this mod has for the injury it deliberately made invisible.
 * A torn spleen puts nothing on the skin - no drip, no stain, nothing a dressing could
 * be put on - so until now the only way to find one was to read the chart and be told.
 * A pulse changes that: a body losing blood answers by speeding the heart up, so a fast
 * thin pulse on somebody with nothing open on them is the tell, and it is the same tell
 * it is in life. The mod says where the blood went. The pulse says that it went.
 *
 * <p>Pure arithmetic over a {@link BodyHealth}, so the server and the screen and the
 * game the helper is playing all read the same heart without anybody syncing anything.
 */
public record Pulse(int rate, int character) {
    /** Nothing under the fingers. The heart has stopped, and that is the whole message. */
    public static final int ABSENT = 0;
    /** Fast and thin - the one that means somebody is running out of blood. */
    public static final int THREADY = 1;
    public static final int WEAK = 2;
    public static final int STRONG = 3;

    /** What a heart at rest does when nothing is wrong with the body around it. */
    private static final int RESTING = 70;

    /**
     * Reads the heart off the body.
     *
     * <p>The big term by far is blood, and upwards: a body that is losing volume holds
     * its pressure up by beating faster, and it keeps doing that right until it cannot,
     * which is what makes a racing pulse the earliest honest sign of a bleed and a
     * falling one the last. Everything else nudges.
     */
    public static Pulse of(BodyHealth body) {
        if (body.arrested()) {
            return new Pulse(0, ABSENT);
        }
        float loss = body.bloodLoss();
        float rate = RESTING;

        // Compensation. This is the whole reason a pulse is worth taking.
        rate += 85.0F * loss;

        if (body.has(Disease.SHOCK)) {
            rate += 25.0F;
        }
        // The one reading that gives the surge away. Somebody who says they are fine,
        // standing on a leg that is broken, with a heart doing a hundred and twenty, is
        // not fine - they are twenty seconds from finding out.
        rate += Adrenaline.pulse(body);
        if (body.downed()) {
            rate += 15.0F;
        }
        // Pain drives it up, and there is a lot of pain in a body with several broken
        // things in it. Capped, because past a point a heart is already going as fast
        // as it is going to and the next fracture does not add to it.
        int hurting = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.fractured() || state.burned || state.necrotic()) {
                hurting++;
            }
        }
        rate += Math.min(20, hurting * 5);

        // And the things that hold it down. Cold blood is the strongest of them and the
        // most counterintuitive: a body going cold slows, which is exactly why a slow
        // pulse on somebody who should be racing is worse news than a fast one.
        if (body.has(Disease.HYPOTHERMIA)) {
            rate -= 25.0F;
        } else if (body.has(Disease.COLD)) {
            rate -= 8.0F;
        }
        if (body.under()) {
            rate -= 15.0F;
        } else if (body.painkillerTicks() > 0) {
            rate -= 8.0F;
        }

        int beats = Mth.clamp(Math.round(rate), 25, 210);
        return new Pulse(beats, character(loss, body));
    }

    /**
     * How it feels, which is the half of a pulse that a number cannot carry.
     *
     * <p>A hundred and thirty strong and a hundred and thirty you can barely hold onto
     * are two different patients, and the second one is the emergency.
     */
    private static int character(float loss, BodyHealth body) {
        if (loss >= 0.45F || body.has(Disease.SHOCK)) {
            return THREADY;
        }
        if (loss >= 0.22F || body.downed()) {
            return WEAK;
        }
        return STRONG;
    }

    /** Whether this is worth saying out loud without being asked. */
    public boolean alarming() {
        return character == ABSENT || character == THREADY || rate >= 120 || rate <= 45;
    }

    public Component characterTitle() {
        return Component.translatable(switch (character) {
            case ABSENT -> "triage.pulse.absent";
            case THREADY -> "triage.pulse.thready";
            case WEAK -> "triage.pulse.weak";
            default -> "triage.pulse.strong";
        });
    }

    public ChatFormatting colour() {
        return switch (character) {
            case ABSENT -> ChatFormatting.DARK_RED;
            case THREADY -> ChatFormatting.RED;
            case WEAK -> ChatFormatting.GOLD;
            default -> ChatFormatting.GREEN;
        };
    }

    /**
     * The rate in words, for a reading that was not good enough to count.
     *
     * <p>Somebody with two fingers on a wrist for three seconds does not come away with
     * a number. They come away with "fast", and fast is most of what the number was for.
     */
    public Component band() {
        String key = rate <= 0 ? "triage.pulse.absent"
                : rate >= 140 ? "triage.pulse.racing"
                : rate >= 105 ? "triage.pulse.fast"
                : rate >= 55 ? "triage.pulse.steady"
                : "triage.pulse.slow";
        return Component.translatable(key);
    }

    /** Beats to milliseconds, for anything that has to keep time with it. */
    public int interval() {
        return rate <= 0 ? 0 : Math.max(1, 60000 / rate);
    }
}
