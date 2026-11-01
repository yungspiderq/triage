package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.data.Milestones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;

/**
 * The last few seconds of somebody's life, handed to whoever is nearby.
 *
 * <p>An emptied head or chest used to be the end of the story. Now it puts the player on
 * the floor with a clock running: they cannot move, cannot fight, and cannot save
 * themselves. A surgical kit in somebody else's hands can - which is the whole reason
 * the mod has a second player in it at all.
 */
public final class Downed {
    private Downed() {
    }

    /**
     * Called the moment a vital part empties. Puts the player down instead of killing
     * them, if the rules allow it.
     *
     * @return true when death was deferred and the caller must not kill anybody
     */
    public static boolean claim(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        if (!config.downed || body.downed()) {
            // Already on the floor: a second emptied part does not restart the clock.
            return body.downed();
        }
        body.setDownedTicks(config.downedSeconds * 20);
        // Air is still getting in at the moment somebody falls. What happens to it after
        // that depends on whether anybody comes.
        body.openAirway();
        // Whoever they were carrying goes down with them, in the literal sense.
        Carry.release(player);
        // And down is a shape, not a pose, wherever something can draw one. Here rather
        // than waiting for the next body tick: the second somebody falls is the one
        // second of this that anybody is looking at.
        Ragdolls.drop(player);
        Body.notify(player, Component.translatable("triage.msg.downed"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
        return true;
    }

    /**
     * One second of lying there. Everything that was already wrong keeps happening - the
     * bleeding does not politely wait - so the clock is rarely the only thing running.
     *
     * @return the damage type to die of, or null while there is still time
     */
    public static ResourceKey<DamageType> tick(ServerPlayer player, BodyHealth body, int interval,
                                               ResourceKey<DamageType> cause) {
        if (!body.downed()) {
            return null;
        }

        // The heart has stopped and the clock it had is spent. What is left is not time
        // this body has - it is time somebody else is buying by hand, one round after
        // another, and it runs out the moment they stop.
        if (body.arrested()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, interval * 2, 0, true, false, false));
            lieDown(player);
            // The airway counts double here, not half. Pushing blood round a body that
            // no air is reaching is pushing the same blood round, and the first version
            // of this quietly made the airway worth nothing the moment the heart
            // stopped - which is precisely when it is worth the most. It also gives the
            // second pair of hands something to do: one on the chest, one on the head.
            int step = interval;
            if (Aid.blocked(body, Config.values())) {
                step = Math.round(step * Config.values().airwayDownedRate);
            }
            if (!body.tickArrest(step)) {
                return null;
            }
            release(player);
            return cause;
        }

        // Somebody has packed the wound. That is not a rescue and it heals nothing, but
        // it is the difference between a medic sixty seconds away being a medic and
        // being a mourner. The clock stops until the packing gives out.
        if (body.stabilised()) {
            if (body.tickStabilised(interval)) {
                Body.notify(player, Component.translatable("triage.msg.hold_gave_out"),
                        ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
            }
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, interval * 2, 0, true, false, false));
            lieDown(player);
            return null;
        }

        // Being carried is not treatment and heals nothing either, but somebody has hold
        // of you and is doing something about it. The clock gives ground for that, or
        // carrying anybody anywhere would cost more time than it could ever buy.
        Config.Values config = Config.values();
        int step = Carry.carried(player)
                ? Math.round(interval * config.downedCarriedRate)
                : interval;
        // Shock is why two people with the same wound have different amounts of time,
        // and it is the whole reason the mod can be sorted rather than merely triaged in
        // the order people fell over.
        if (config.shock && body.has(Disease.SHOCK)) {
            step = Math.round(step * config.shockDownedRate);
        }
        // Nothing else on this list takes time away as fast as not breathing, and
        // nothing else on it can be undone by somebody with empty hands.
        if (Aid.blocked(body, config)) {
            step = Math.round(step * config.airwayDownedRate);
        }
        body.setDownedTicks(body.downedTicks() - step);

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, interval * 2, 0, true, false, false));
        lieDown(player);

        // The countdown is drawn on the HUD rather than shouted into the action bar
        // once a second: it is the only thing on screen that matters just now.
        if (body.downedTicks() > 0) {
            return null;
        }
        // The clock running out used to be the death itself. Now it is the heart
        // stopping, which is a different event: there is still a body worth working on,
        // and whether anybody is standing over it is the entire question.
        if (arrest(player, body, config)) {
            return null;
        }
        release(player);
        return cause;
    }

    /**
     * The clock is spent, so the heart stops.
     *
     * <p>Nothing about this saves anybody by itself. It is a window, and a window is
     * only worth something to a player who is not alone - which is the same bargain the
     * downed state has always made, made once more at the point where it used to stop
     * making it.
     *
     * @return true when there is a window and the caller must not kill anybody
     */
    /**
     * Breathing stops, from something given rather than from something lost.
     *
     * <p>The one way into this state that is nobody's injury: a dose that worked too
     * well. It lands in the same place as the clock running out because it is the same
     * emergency and has the same answer - hands on the chest - and putting it anywhere
     * else would mean inventing a second one that behaved identically.
     */
    public static void stopBreathing(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        body.wake();
        if (!body.downed()) {
            body.setDownedTicks(config.downedSeconds * 20);
        }
        arrest(player, body, config);
        body.setDirty(true);
        Body.sync(player);
    }

    private static boolean arrest(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.cpr || config.arrestSeconds <= 0) {
            return false;
        }
        body.setArrestTicks(config.arrestSeconds * 20);
        Body.notify(player, Component.translatable("triage.msg.arrest"),
                ChatFormatting.DARK_RED, null);
        // The last beat, and the only one anybody else has ever been able to hear. After
        // this the body is silent, and the silence is the thing that says what happened.
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    /**
     * Puts them on the floor, unless somebody has picked them up.
     *
     * <p>A passenger is never posed. The server sends no position for one - every
     * client works it out from the vehicle, and that sum subtracts the passenger's own
     * vehicle attachment point, which comes from their dimensions, which come from
     * their pose. Force a pose that only some of the clients know about and the two
     * views measure the same offset from two different bodies: the carried player rides
     * the shoulder while everyone else watches them lie where they fell.
     */
    private static void lieDown(ServerPlayer player) {
        floor(player);
    }

    /**
     * Puts somebody on the floor and leaves them there.
     *
     * <p>Public because there are two ways to end up on the ground now and only one of
     * them is a clock. Somebody with no legs is not dying - there is nothing to revive
     * them from and no time running out - they simply have nothing to stand on, and the
     * shape of that is the shape of this.
     */
    public static void floor(ServerPlayer player) {
        if (player.isPassenger()) {
            com.claudemods.triage.compat.Poses.force(player, null);
            return;
        }
        // A real ragdoll beats a pose whenever one is available, and asking for it every
        // second rather than remembering it means anything that takes a player off their
        // doll - the physics mod, an addon, a carry that has just ended - is undone by
        // itself within a second. There is no second copy of this state to go stale.
        if (Ragdolls.drop(player)) {
            return;
        }
        com.claudemods.triage.compat.Poses.force(player, Pose.SWIMMING);
    }

    /**
     * And back up.
     *
     * <p>Called at the moment somebody gets something to stand on rather than once a
     * second, because there is no way to ask a player whether a pose is being forced on
     * them - only to force one or to stop. Polling would mean clearing a pose every
     * second for every player alive on the off chance it was ours.
     */
    public static void stand(ServerPlayer player) {
        com.claudemods.triage.compat.Poses.force(player, null);
        Ragdolls.getUp(player);
    }

    /**
     * A blow landing on somebody who is already down. It cannot wound them any
     * further - the part that matters is already empty - so it costs them time
     * instead.
     *
     * @return true when that was the last of their time and the caller should let
     *         the killing blow through
     */
    public static boolean hasten(ServerPlayer player, BodyHealth body, float damage) {
        Config.Values config = Config.values();
        if (damage <= 0.0F || config.downedDamageSeconds <= 0.0F) {
            return false;
        }
        // A hit undoes the packing. Whatever was holding the wound closed no longer is,
        // which is the other half of "get them out of the fire first" - a hold bought
        // under fire is a hold you are about to lose.
        body.unstabilise();
        int cost = Math.round(damage * config.downedDamageSeconds * 20.0F);
        // A blow to somebody whose heart has already stopped comes off the only thing
        // they have left, which is the window somebody else is holding open by hand.
        if (body.arrested()) {
            if (!body.tickArrest(cost)) {
                return false;
            }
            release(player);
            return true;
        }
        body.setDownedTicks(body.downedTicks() - cost);
        if (body.downedTicks() > 0) {
            return false;
        }
        if (arrest(player, body, config)) {
            return false;
        }
        release(player);
        return true;
    }

    /**
     * Stop waiting. Nobody is obliged to spend their last minute face down on the
     * floor hoping - and on your own, nobody is coming by definition.
     */
    public static void giveUp(ServerPlayer player) {
        BodyHealth body = Body.of(player);
        if (!body.downed()) {
            return;
        }
        body.clearDowned();
        release(player);
        Body.killIfEmpty(player, Body.deathCause(body));
    }

    /**
     * Back on your feet, one way or the other - which also means off whoever's shoulder
     * you were on. Both endings go through here, so neither can leave somebody riding a
     * player who has stopped being their rescue.
     */
    public static void release(ServerPlayer player) {
        com.claudemods.triage.compat.Poses.force(player, null);
        Carry.release(player);
        Ragdolls.getUp(player);
    }

    /**
     * Called after any treatment: if the vital part that put them down is no longer
     * empty, they are back. Nothing else clears it - a downed player cannot be walked
     * off or waited out.
     */
    public static void checkRevived(ServerPlayer healer, ServerPlayer patient, BodyHealth body) {
        if (!body.downed() || body.isDead()) {
            return;
        }
        if (healer != patient) {
            Milestones.onRevived(healer);
        }
        body.clearDowned();
        release(patient);
        Body.notify(patient, Component.translatable("triage.msg.revived"),
                ChatFormatting.GREEN, SoundEvents.TOTEM_USE);
    }
}
