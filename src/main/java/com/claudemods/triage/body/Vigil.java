package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;

/**
 * How long since this one slept, and what it is costing them.
 *
 * <p>Minecraft has always counted this and has always done exactly one thing with it,
 * which is to send birds. That is a threat from outside. What being awake for three days
 * actually does is from the inside, and it is the best documented thing on this list:
 * a tired pair of hands is a worse pair of hands, and the person they belong to is the
 * last to know it.
 *
 * <p>So this is the fifth thing that decides a treatment, after the room, the instrument,
 * the second pair of hands and what this pair has just been doing. All five are about the
 * surgeon rather than the patient, and this one is the only one that cannot be fixed in
 * the next thirty seconds.
 *
 * <p>It costs two other things as well, and both are real. A body that has not slept
 * catches what is going around - which this mod now has, because a cold travels between
 * players - and it mends worse, because mending is mostly something a body does while it
 * is asleep.
 *
 * <p>Nothing is stored for any of it. The count is vanilla's own, the one the phantoms
 * read, so it already persists, already survives a relog, and is already reset by the one
 * thing that should reset it. Keeping a second clock beside it would only be a way for
 * the two to disagree.
 */
public final class Vigil {
    /** A day, in ticks. The unit everything here is written in. */
    private static final int DAY = 24000;
    /** Below this there is nothing to say: one night up is not an affliction. */
    private static final float QUIET = 0.15F;

    private Vigil() {
    }

    /**
     * How far gone they are, 0 to 1.
     *
     * <p>Nothing at all for the first day, because one late night is not a medical fact,
     * and full by whatever the config calls the end of it. Read straight off the counter
     * the phantoms use.
     */
    public static float share(ServerPlayer player) {
        Config.Values config = Config.values();
        if (!config.sleepless) {
            return 0.0F;
        }
        int awake = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        int full = Math.max(1, config.sleeplessDays * DAY);
        return Mth.clamp((awake - DAY) / (float) (full - DAY), 0.0F, 1.0F);
    }

    /** Whole days awake, for the readout. */
    public static int days(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) / DAY;
    }

    /** Whether it is worth a line. One night up is not news. */
    public static boolean worthSaying(float share) {
        return share >= QUIET;
    }

    public static ChatFormatting colour(float share) {
        if (share >= 0.66F) {
            return ChatFormatting.RED;
        }
        return share >= 0.33F ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
    }

    /**
     * The eyes closing, and not opening again for a second or two.
     *
     * <p>What "sleepy" actually does, past the point where it is a mood. It is not a
     * faint and it is not a nap: it is the thing that happens to lorry drivers, where the
     * world goes away for a second and a half and comes back somewhere else.
     *
     * <p>Mechanically it is the shortest faint there is, and it borrows that whole path -
     * the lights go out, nothing moves, and it is over before anybody could treat it.
     * What it costs is a second and a half of not being there, chosen for you, and where
     * you happened to be standing.
     *
     * <p>Only at the far end of the count, only rarely, and never while already on the
     * floor: somebody who is down has enough happening.
     */
    private static void microsleep(ServerPlayer player, BodyHealth body, Config.Values config,
                                   float share, int interval) {
        if (config.microsleepChance <= 0.0F || share < config.microsleepFrom) {
            return;
        }
        if (body.floored() || body.under() || player.isSleeping()) {
            return;
        }
        // Steeper at the end, like everything else here: the difference between three
        // days and four is not linear and never has been.
        float over = (share - config.microsleepFrom) / Math.max(0.01F, 1.0F - config.microsleepFrom);
        float chance = config.microsleepChance * (interval / 20.0F) * (0.4F + over * 1.6F);
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        Faint.microsleep(player, body, config.microsleepSeconds);
    }

    /** What it takes off anything they treat. */
    public static float hands(ServerPlayer healer) {
        return share(healer) * Config.values().sleeplessHands;
    }

    /**
     * What it does to catching things.
     *
     * <p>A multiplier on the chance of picking up what is going around, because the one
     * thing everybody knows about not sleeping is that it is how you get ill. The mod only
     * has one illness that travels, and this is the body that gets it.
     */
    public static float illness(ServerPlayer player) {
        return 1.0F + share(player) * Config.values().sleeplessIllness;
    }

    /**
     * And what it does to a wound that is trying to close.
     *
     * <p>A multiplier on how long a part takes to knit. Mending is mostly a thing a body
     * does while it is asleep, so a body that is not sleeping is a body doing less of it.
     */
    public static float mending(ServerPlayer player) {
        return 1.0F + share(player) * Config.values().sleeplessMending;
    }

    /**
     * Said when they cross into it, and once.
     *
     * <p>Two lines, at the two places it starts mattering. Nothing about being tired is
     * visible from the outside and nothing about it is on the chart, so a rule nobody is
     * ever told about is a rule nobody learns.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.sleepless) {
            body.setTired(0.0F);
            return;
        }
        // Down to the client, because this is the one affliction in the mod whose whole
        // expression is on the inside of somebody's eyes.
        float share = share(player);
        body.setTired(share);
        microsleep(player, body, config, share, interval);
        int awake = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        int was = awake - interval;
        for (int day = 2; day <= config.sleeplessDays; day++) {
            int mark = day * DAY;
            if (was < mark && awake >= mark) {
                Body.notify(player, Component.translatable("triage.msg.awake_days", day),
                        day >= config.sleeplessDays ? ChatFormatting.RED : ChatFormatting.YELLOW, null);
            }
        }
    }
}
