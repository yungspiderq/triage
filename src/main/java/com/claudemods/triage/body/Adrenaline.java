package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The minute after, when it does not hurt yet.
 *
 * <p>This mod has always charged for an injury from the instant it happened. A leg breaks
 * and the limp starts on the same tick, which is the one part of breaking a leg that
 * nobody who has broken one recognises. What actually happens is that for a short while
 * afterwards you can run on it, and then you cannot, and the second part arrives without
 * warning and is worse than the first.
 *
 * <p>So the rule is a real one and it has a shape: <b>it hides pain, it does not mend
 * bone.</b> People finish races on stress fractures and walk away from car crashes on
 * broken ankles; nobody runs on a femur that is in two pieces, because what is missing
 * there is not the will to ignore it, it is the thing that carries the weight. A part
 * that is gone, dead or beaten past using at all is untouched by this. Everything else
 * goes quiet for a bit.
 *
 * <p>It is not free and it is not a buff. Three things come with it, and all three are
 * true: the heart is already going, the hands shake - which makes this the worst possible
 * moment to operate on anybody, including yourself - and when it ends the whole of what
 * was being held back arrives at once, worse for a while than it would have been if the
 * surge had never happened. That is the honest trade, and it is why there is no way to
 * ask for this on purpose.
 *
 * <p>And you cannot have it twice in a row. The next surge waits until the crash from the
 * last one has passed, which stops "keep getting hurt" from being a way to never feel
 * anything.
 *
 * <p>Kept out of the codec for the same reason fatigue is: this is a minute of chemistry,
 * not an injury, and nothing about it should outlive a disconnect.
 */
public final class Adrenaline {
    private Adrenaline() {
    }

    /**
     * Something just happened that a body answers this way.
     *
     * <p>Called from the one place every wound goes through, for the three things that
     * are sudden and serious enough: a bone going, a limb going out, and a limb going
     * altogether. Not for ordinary damage - a body that did this every time it was
     * scratched would be a body permanently on it.
     */
    public static void surge(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        if (!config.adrenaline || body.crashTicks > 0) {
            // Still paying for the last one. This is the whole of why it cannot be farmed.
            return;
        }
        boolean fresh = body.adrenalineTicks <= 0;
        body.adrenalineTicks = config.adrenalineSeconds * 20;
        if (fresh) {
            Body.notify(player, Component.translatable("triage.msg.adrenaline"),
                    ChatFormatting.YELLOW, null);
        }
    }

    /**
     * The same thing, out of a needle.
     *
     * <p>Separate from the injury version only because the refractory rule has to be
     * different: a wound cannot give you a second surge while the first is still being
     * paid for, and a syringe plainly can. That is the whole danger of the syringe, and
     * pretending otherwise would take it away.
     */
    public static void shot(ServerPlayer patient, BodyHealth body) {
        Config.Values config = Config.values();
        if (!config.adrenaline) {
            return;
        }
        boolean fresh = body.adrenalineTicks <= 0;
        body.crashTicks = 0;
        body.adrenalineTicks = config.adrenalineSeconds * 20;
        if (fresh) {
            Body.notify(patient, Component.translatable("triage.msg.adrenaline"),
                    ChatFormatting.YELLOW, null);
        }
    }

    /**
     * One second of it, and of what follows it.
     *
     * <p>The crash is started here rather than where the surge is, because the surge does
     * not know how long it is going to last - another injury while it is running puts the
     * clock back to the top.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.adrenaline) {
            body.adrenalineTicks = 0;
            body.crashTicks = 0;
            return;
        }
        if (body.adrenalineTicks > 0) {
            body.adrenalineTicks = Math.max(0, body.adrenalineTicks - interval);
            if (body.adrenalineTicks == 0) {
                body.crashTicks = config.adrenalineCrashSeconds * 20;
                Body.notify(player, Component.translatable("triage.msg.adrenaline_crash"),
                        ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
            }
            return;
        }
        if (body.crashTicks > 0) {
            body.crashTicks = Math.max(0, body.crashTicks - interval);
        }
    }

    /** Whether it is running right now. */
    public static boolean up(BodyHealth body) {
        return Config.values().adrenaline && body.adrenalineTicks > 0;
    }

    /** And whether this is the part afterwards. */
    public static boolean crashing(BodyHealth body) {
        return Config.values().adrenaline && body.adrenalineTicks <= 0 && body.crashTicks > 0;
    }

    /**
     * What share of the pain is not being felt, 0 to 1.
     *
     * <p>Negative is the crash: the same number read the other way, because what comes
     * back has to be worse than the baseline or the surge was simply free.
     */
    public static float relief(BodyHealth body) {
        Config.Values config = Config.values();
        if (up(body)) {
            return Mth.clamp(config.adrenalineRelief, 0.0F, 1.0F);
        }
        if (crashing(body)) {
            return -Mth.clamp(config.adrenalineCrashBite, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    /**
     * How much the hands are shaking, 0 to 1.
     *
     * <p>Only during the surge. This is the detail that keeps the rule from being a gift:
     * the minute in which you cannot feel the injury is the same minute in which you are
     * the worst surgeon in the room.
     */
    public static float shake(BodyHealth body) {
        return up(body) ? Mth.clamp(Config.values().adrenalineShake, 0.0F, 1.0F) : 0.0F;
    }

    /** What it adds to a heart that is already going. */
    public static float pulse(BodyHealth body) {
        return up(body) ? Config.values().adrenalinePulse : 0.0F;
    }
}
