package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The arms give out.
 *
 * <p>Two things in this mod are physically heavy and the mod invented both of them:
 * carrying somebody who cannot walk, and bracing somebody who is being cut into. Until
 * now a medic could do either forever. A body over the shoulder was a slower way of
 * walking and nothing else, so "get them out first" had no limit on it - and a move with
 * no limit is not a decision, it is a formality.
 *
 * <p>What this is <b>not</b> is a stamina bar. Nothing an ordinary player does builds it:
 * sprinting does not, mining does not, fighting does not. Vanilla already charges for
 * those and charging twice would be a tax on playing rather than a rule about medicine.
 * It comes from the two acts above, and it lands on the two places they belong - how far
 * you can carry, and how steady your hands are afterwards.
 *
 * <p>The two are charged differently because they are different shapes. Carrying is a
 * drain, because it goes on for as long as you keep walking. Holding somebody still is a
 * lump paid once when it actually counted, because that is one job with a beginning and
 * an end, and because there is no honest way to tell a man bracing a patient from a man
 * crouching.
 *
 * <p>And it is the arms doing the work, so it is the arms that decide. Two sound ones
 * tire at the written rate; a broken, dead or missing one tires at up to three times it.
 * Which quietly makes a wounded medic a worse stretcher-bearer without a single new rule
 * about it, because that is what a wounded medic is.
 *
 * <p>The answer, as with mending, is to stand still for a moment. This mod has two rules
 * now whose answer is to do nothing, and they are the two it is proudest of.
 *
 * <p>It is deliberately not saved with the body. Everything else in here is an injury and
 * outlives a disconnect; this is a minute of breath, and a minute of breath is roughly
 * what reconnecting costs anyway.
 */
public final class Fatigue {
    /** Below this the arms are fresh and the readout says nothing at all. */
    private static final float TIRING = 0.15F;
    /** Above this they are burning, and they are told so. */
    private static final float BURNING = 0.5F;
    /** And above this they are nearly gone. */
    private static final float GOING = 0.85F;
    /** How much faster a wrecked pair of arms tires than a sound one. */
    private static final float WORST_ARMS = 3.0F;

    private Fatigue() {
    }

    /**
     * One second of it.
     *
     * <p>Load first, then rest, and never both in the same second: a carrier standing
     * still is still carrying, and their arms know it.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.fatigue) {
            body.fatigue = 0.0F;
            return;
        }
        float seconds = interval / 20.0F;
        float before = body.fatigue;

        if (Carry.carrying(player) != null) {
            // The arms doing the lifting are the arms you have, not the arms you wish for.
            float arms = 1.0F + PartEffects.arms(body) * (WORST_ARMS - 1.0F);
            body.fatigue = Mth.clamp(body.fatigue
                    + seconds * arms / Math.max(1, config.fatigueSeconds), 0.0F, 1.0F);
        } else {
            // Standing still is worth double, which is the whole of the advice.
            float rest = seconds / Math.max(1, config.fatigueRecoverSeconds);
            if (isStill(player)) {
                rest *= 2.0F;
            }
            body.fatigue = Mth.clamp(body.fatigue - rest, 0.0F, 1.0F);
        }
        announce(player, before, body.fatigue);

        if (body.fatigue >= 1.0F && Carry.carrying(player) != null) {
            gaveOut(player);
        }
    }

    /**
     * Somebody braced a patient through a treatment, and it cost them.
     *
     * <p>Paid at the end rather than charged by the second, because holding a patient
     * still is one job: it starts when the work does and it is over when the work is.
     * It is also the reason an assistant cannot be an assistant indefinitely, which
     * matters - otherwise the answer to every operation would be "have a friend crouch
     * here for the rest of the session".
     */
    public static void braced(ServerPlayer assistant) {
        Config.Values config = Config.values();
        if (!config.fatigue) {
            return;
        }
        BodyHealth body = Body.of(assistant);
        float before = body.fatigue;
        body.fatigue = Mth.clamp(body.fatigue + config.fatigueHold, 0.0F, 1.0F);
        announce(assistant, before, body.fatigue);
    }

    /**
     * Not walking, not sprinting, not swimming. Being somewhere rather than going.
     *
     * <p>Measured off where they were a tick ago rather than off their velocity. A server
     * only learns a player has moved when the position packet says so, and the velocity
     * it keeps for them is not reliably updated by that - ask it and almost everybody is
     * standing still, including the man sprinting past. The last position is the thing
     * the server actually knows. Walking is about 0.2 of a block a tick, so anything
     * under a hundredth is somebody who has stopped.
     */
    private static boolean isStill(ServerPlayer player) {
        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;
        return dx * dx + dz * dz < 1.0E-4 && player.onGround();
    }

    /**
     * How far off fine work this pair of arms is, 0 to 1.
     *
     * <p>Read by the theatre, where it is the fourth thing that decides a treatment after
     * the room, the instrument and the second pair of hands. A body that has just carried
     * somebody half a mile is not the body you want closing you up, and anyone who has
     * carried anything knows it.
     */
    public static float of(BodyHealth body) {
        return Config.values().fatigue ? Mth.clamp(body.fatigue, 0.0F, 1.0F) : 0.0F;
    }

    /** The word for it, for the readout. */
    public static String word(float fatigue) {
        if (fatigue >= GOING) {
            return "gone";
        }
        if (fatigue >= BURNING) {
            return "burning";
        }
        return fatigue >= TIRING ? "tiring" : "fresh";
    }

    /** Whether there is anything worth printing. Fresh arms are not news. */
    public static boolean worthSaying(float fatigue) {
        return fatigue >= TIRING;
    }

    public static ChatFormatting colour(float fatigue) {
        if (fatigue >= GOING) {
            return ChatFormatting.RED;
        }
        return fatigue >= BURNING ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
    }

    /**
     * Said once on the way up and not on the way down.
     *
     * <p>Nothing about tired arms is visible from outside, and a rule nobody can see is a
     * rule nobody learns. Two lines are enough: one saying this is going to end, and one
     * saying it is ending now.
     */
    private static void announce(ServerPlayer player, float before, float now) {
        // The word said here has to be the word the readout would print at the same
        // moment, or the two of them are describing different arms.
        if (before < GOING && now >= GOING) {
            Body.notify(player, Component.translatable("triage.msg.arms_going"),
                    ChatFormatting.RED, null);
        } else if (before < BURNING && now >= BURNING) {
            Body.notify(player, Component.translatable("triage.msg.arms_burning"),
                    ChatFormatting.YELLOW, null);
        }
    }

    /**
     * They go, and whoever was over the shoulder is on the floor.
     *
     * <p>Not damage and not a punishment: the patient ends up exactly where they would be
     * if the carrier had set them down deliberately. The cost is that it happened here
     * rather than where they were going.
     */
    private static void gaveOut(ServerPlayer player) {
        if (!Carry.putDown(player)) {
            return;
        }
        Body.notify(player, Component.translatable("triage.msg.arms_gave_out"),
                ChatFormatting.RED, SoundEvents.PLAYER_BIG_FALL);
    }
}
