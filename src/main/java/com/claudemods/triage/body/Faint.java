package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;

/**
 * Going out.
 *
 * <p>Not the same thing as going down, and the difference is the entire reason this
 * exists. Being downed in this mod is the end of your luck: a clock, a rescue, or a
 * grave. A faint is nothing of the kind. It is a body deciding, without consulting
 * anybody, that it cannot keep the head supplied <em>while standing up</em> - so it
 * stops standing up, which fixes the problem, which is why fainting is a thing bodies
 * do rather than a thing that kills them.
 *
 * <p>Ten seconds later you are fine. Nothing is damaged, nothing is permanent and
 * nothing needs treating, and that is deliberate: this is the mod's only warning shot.
 * Everything else here tells you something is wrong by taking something away.
 *
 * <p>What it costs is the ten seconds, and wherever you happened to be standing. Fainting
 * in a field is an inconvenience. Fainting on a ledge, in water, or in front of something
 * that was already swinging at you is the whole of the danger, and none of it had to be
 * invented - it is simply where you were when your body stopped asking.
 *
 * <p>Three roads in, all of them real:
 *
 * <ul>
 *   <li><b>Not enough volume left</b>, which is the one most players will meet. Standing
 *       costs a body pressure it no longer has, and sprinting costs it more - which is
 *       why it so often happens to somebody who had been fine a second ago and then got
 *       up quickly.</li>
 *   <li><b>Pain</b>, on its own, with no blood lost at all. This is the common faint, the
 *       one at the sight of a broken arm, and it is usually somebody else's arm.</li>
 *   <li><b>The bill for adrenaline</b>, arriving on a body that had nothing to spend.</li>
 * </ul>
 */
public final class Faint {
    /** Sprinting on an empty tank is the classic way to find the floor. */
    private static final float SPRINT_FACTOR = 2.5F;
    /** How many ways there are of saying it, in the language files. */
    public static final int LINES = 4;

    private Faint() {
    }

    /**
     * One second of asking whether this body is still able to stand up.
     *
     * <p>Asked of everybody every second, which is cheap, and answered no almost always.
     */
    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (!config.faint) {
            if (body.fainted()) {
                wake(player, body);
            }
            return;
        }
        if (body.fainted()) {
            if (body.tickFaint(interval)) {
                wake(player, body);
            }
            return;
        }
        if (body.faintGrace() > 0) {
            body.tickFaintGrace(interval);
            return;
        }
        // Somebody already on the floor cannot faint onto it, and somebody under
        // anaesthetic is further out than a faint goes.
        if (body.floored() || body.under() || body.arrested() || player.isSleeping()) {
            return;
        }
        float chance = risk(player, body, config, interval);
        if (chance <= 0.0F || player.getRandom().nextFloat() >= chance) {
            return;
        }
        drop(player, body, config);
    }

    /**
     * How close this body is to putting itself on the floor, per tick of this interval.
     *
     * <p>Nothing about being wounded is in here on purpose. A body with a broken arm and
     * a full tank does not faint, and one that fainted every time it was hurt would be a
     * body nobody could play. The three things that do it are the three things that
     * actually stop blood reaching a head.
     */
    private static float risk(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        float per = config.faintChance * (interval / 20.0F);
        float chance = 0.0F;
        if (body.bloodLoss() >= config.faintBloodLoss) {
            // Steeper the emptier it gets, so that the last of it is the dangerous part
            // rather than a flat tax across the whole range.
            float over = (body.bloodLoss() - config.faintBloodLoss)
                    / Math.max(0.01F, 1.0F - config.faintBloodLoss);
            chance += per * (1.0F + over * 3.0F);
        }
        // The bill for adrenaline, landing on a body that had nothing to spend.
        if (Adrenaline.crashing(body) && body.bloodLoss() > config.faintBloodLoss * 0.5F) {
            chance += per;
        }
        // And pain, which needs no blood loss at all. Painkillers work on this, because
        // what they take away is exactly the thing that is doing it.
        if (config.faintPain <= Pain.WORST && !body.painkilled()) {
            int said = Pain.said(body, player.getRandom());
            if (said >= config.faintPain) {
                chance += per * 0.6F;
            }
        }
        if (chance > 0.0F && player.isSprinting()) {
            chance *= SPRINT_FACTOR;
        }
        return chance;
    }

    /**
     * Down.
     *
     * <p>The pose is the downed one because it is the same picture from outside - a
     * person on the ground - and there is no second animation worth building for seven
     * seconds. What is not the same is everything else: no clock to run out, no bleeding
     * out, nothing anybody has to do, and nobody can pick them up or treat them for it.
     */
    public static void drop(ServerPlayer player, BodyHealth body, Config.Values config) {
        body.setFaint(config.faintSeconds * 20);
        // The pose, and deliberately not the ragdoll.
        //
        // Measured rather than assumed: a ragdolled player stops being ticked while the
        // physics has hold of them, which froze everything about the body for as long as
        // the doll lasted - the faint clock, and the bleeding that caused it. A faint
        // that stops the bleeding while it lasts would be a rest rather than a danger,
        // and the whole point of going over with a wound open is that the wound is still
        // open while you are down there.
        com.claudemods.triage.compat.Poses.force(player, Pose.SWIMMING);
        player.setSprinting(false);
        Body.notify(player, Component.translatable("triage.msg.fainted"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
        Reactions.say(player, "faint");
        Body.sync(player);
    }

    /**
     * And back.
     *
     * <p>With a grace period after it, because a body that comes round into exactly the
     * conditions that put it down would go straight back over, and a player spending a
     * minute alternating between standing and lying is not being told anything new. The
     * grace is the mod saying it once and then letting them act on it.
     */
    private static void wake(ServerPlayer player, BodyHealth body) {
        body.clearFaint(Config.values().faintClearSeconds * 20);
        if (!body.downed()) {
            com.claudemods.triage.compat.Poses.force(player, null);
        }
        Body.notify(player, Component.translatable("triage.msg.came_round"),
                ChatFormatting.GRAY, null);
        Body.sync(player);
    }

    /**
     * The eyes closing on their own, which is not the same as going over.
     *
     * <p>Shares every mechanism with a faint because from the outside it is one - a body
     * on the floor for a moment - and differs in the only way that matters to whoever it
     * happens to: nothing is wrong with them. They have simply been awake for four days,
     * and the body has taken the decision out of their hands for a second and a half.
     */
    public static void microsleep(ServerPlayer player, BodyHealth body, int seconds) {
        body.setFaint(Math.max(20, seconds * 20));
        com.claudemods.triage.compat.Poses.force(player, Pose.SWIMMING);
        player.setSprinting(false);
        Body.notify(player, Component.translatable("triage.msg.microsleep"),
                ChatFormatting.GRAY, SoundEvents.PLAYER_BREATH);
        Body.sync(player);
    }

    /** Put back on their feet by something else entirely - a death, or a reset. */
    public static void release(ServerPlayer player, BodyHealth body) {
        if (!body.fainted()) {
            return;
        }
        body.clearFaint(0);
        if (!body.downed()) {
            com.claudemods.triage.compat.Poses.force(player, null);
        }
    }
}
