package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * What happens to a limb nothing is reaching, and to the body it is still attached to.
 *
 * <p>Everything else the mod does to a part is a wound: it can be closed, cleaned, set,
 * healed, waited out or carried around. This is the one road with nothing at the end of
 * it, and the mod needed one. Until now every decision in it could be walked back given
 * enough bandages and enough patience, which quietly made all of them smaller than they
 * looked - a tourniquet was a free stop, a festering arm was a chore.
 *
 * <p>Both of those now have a clock. Neither clock is a surprise and neither is a trap:
 * the strap says on the chart that it is on, the rot says on the chart that it is
 * spreading, the warning arrives out loud with a third of the time left, and the count
 * runs back down the moment somebody deals with the reason for it. The only way to reach
 * the end of it is to keep not dealing with it.
 *
 * <p>And what is at the end is not death. It is a limb that has died on a living person,
 * which is a worse thing to be holding than the wound was - it takes no treatment, it
 * mends nothing, and it poisons what it is attached to. The answer to it is the saw, and
 * the saw is the first treatment in this mod that makes the patient permanently smaller
 * than they were.
 */
public final class Necrosis {
    /** Share of the clock at which the patient is told out loud, once. */
    private static final float WARN_AT = 0.66F;

    private Necrosis() {
    }

    /**
     * The price of letting a strap go.
     *
     * <p>The real complication of a tourniquet and the one nobody expects. A limb with no
     * blood moving through it does not simply stop; things build up in it, and the moment
     * the strap comes off they go everywhere. The longer it was on, the more there is.
     *
     * <p>Which is what finally makes the strap the argument it should always have been.
     * Taken off early it costs nothing at all. Left until the clock is nearly out it is
     * dangerous to take off, and the limb under it is dying anyway - so the question stops
     * being "when do I release this" and becomes "am I releasing this or cutting below
     * it". That is the road the real one goes down, and it is the road this mod already
     * had a saw at the end of.
     *
     * <p>Charged wherever a strap comes off, which includes a bandage going over the
     * wound: releasing is releasing, and a bandage that quietly dodged this would make
     * the whole thing avoidable by using the commonest item in the game.
     */
    public static void released(ServerPlayer player, BodyHealth body, PartState state) {
        Config.Values config = Config.values();
        if (!config.reperfusion || config.reperfusionDamage <= 0.0F) {
            return;
        }
        int limit = config.strapDeadSeconds * 20;
        if (limit <= 0) {
            return;
        }
        // Nothing at all for the first third. A strap put on and taken off again is a
        // strap that did its job, and it should cost what it always cost: nothing.
        float share = Mth.clamp((state.deadTicks / (float) limit - 0.33F) / 0.67F, 0.0F, 1.0F);
        if (share <= 0.0F) {
            return;
        }
        body.hurtAll(config.reperfusionDamage * share);
        if (config.shock && player.getRandom().nextFloat() < share * 0.6F) {
            body.catchDisease(Disease.SHOCK);
        }
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.reperfusion"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
    }

    /** Once a second, per player. */
    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (BodyPart part : BodyPart.VALUES) {
            if (!part.limb()) {
                continue;
            }
            PartState state = body.part(part);
            if (state.gone()) {
                phantom(player, body, state, part, config);
                wearing(player, body, state, part, config, interval);
                continue;
            }
            if (state.necrotic()) {
                rot(player, body, config);
                continue;
            }
            if (config.necrosis) {
                dying(player, body, state, part, config, interval);
            }
        }
    }

    /**
     * The clock on a limb that is not being fed.
     *
     * <p>Two things start it and the shorter of them wins, which matters more than it
     * looks: a strapped limb that is also festering is not on two clocks, it is on the
     * one that runs out first. Anything else would mean the player has to work out which
     * of two invisible counters they are racing.
     */
    private static void dying(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                              Config.Values config, int interval) {
        int limit = Integer.MAX_VALUE;
        if (state.tourniquet) {
            limit = Math.min(limit, config.strapDeadSeconds * 20);
        }
        if (state.infection >= PartState.INFECTION_SEVERE) {
            limit = Math.min(limit, config.rotDeadSeconds * 20);
        }

        if (limit == Integer.MAX_VALUE) {
            // Nothing is starving it any more. The time comes back at the rate it went,
            // which is the difference between a deadline and a punishment: a strap taken
            // off in time costs the limb nothing at all.
            if (state.deadTicks > 0) {
                state.deadTicks = Math.max(0, state.deadTicks - interval);
                body.setDirty(true);
            }
            return;
        }

        int before = state.deadTicks;
        state.deadTicks += interval;
        body.setDirty(true);

        int warn = Math.round(limit * WARN_AT);
        if (before < warn && state.deadTicks >= warn) {
            Body.notify(player, Component.translatable("triage.msg.limb_dying", part.title()),
                    ChatFormatting.DARK_RED, SoundEvents.WARDEN_HEARTBEAT);
        }
        if (state.deadTicks >= limit) {
            state.die();
            Body.notify(player, Component.translatable("triage.msg.limb_dead", part.title()),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
        }
    }

    /**
     * What a dead limb does to the rest of the body while nobody takes it off.
     *
     * <p>Several times what a blacked-out one costs, and on top of that it is a source
     * rather than a weight: sooner or later the blood goes bad. That is the argument for
     * the saw, and it is the argument the real operation is made of - the limb is not
     * the patient.
     */
    private static void rot(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (config.deadLimbDrain > 0.0F) {
            Injuries.apply(player, body, BodyPart.THORAX, config.deadLimbDrain, false);
        }
        if (!config.diseases || body.has(Disease.SEPSIS)) {
            return;
        }
        if (player.getRandom().nextFloat() < config.deadLimbSepsis && body.catchDisease(Disease.SEPSIS)) {
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.SEPSIS.title()),
                    Disease.SEPSIS.colour(), SoundEvents.WITHER_SPAWN);
        }
    }

    /**
     * A fitted limb getting looser.
     *
     * <p>No clock to run out and nothing to fail: it simply becomes less of a leg, all
     * the way down to a bit over half of what it was, and stays there until somebody
     * fits a fresh one. The one message it sends is at the point where it has stopped
     * being the thing that was fitted and started being a thing that needs seeing to.
     */
    private static void wearing(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                Config.Values config, int interval) {
        if (!state.prosthetic || config.prostheticWearSeconds <= 0) {
            return;
        }
        int full = config.prostheticWearSeconds * 20;
        if (state.wear >= full) {
            return;
        }
        int before = state.wear;
        state.wear = Math.min(full, state.wear + interval);
        body.setDirty(true);
        int warn = Math.round(full * 0.7F);
        if (before < warn && state.wear >= warn) {
            Body.notify(player, Component.translatable("triage.msg.prosthetic_worn", part.title()),
                    ChatFormatting.GOLD, SoundEvents.ITEM_BREAK);
        }
    }

    /**
     * The limb is gone and still hurts.
     *
     * <p>Nearly everybody who loses one has this, and it does nothing here - no damage,
     * no penalty, no clock. It is in the mod for the same reason it is in every account
     * of the operation: the chart says the arm is not there and the person keeps
     * reporting the arm. Painkillers work on it, which is also true, and is the only
     * thing anybody has ever been able to offer for it.
     */
    private static void phantom(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                Config.Values config) {
        if (!config.phantomPain || body.painkilled() || config.phantomPainSeconds <= 0) {
            return;
        }
        // A stump that still has something wrong with it is not the quiet kind of ache
        // this is about, and the player already has a line on the chart telling them so.
        if (state.bleeding()) {
            return;
        }
        if (player.getRandom().nextFloat() >= 1.0F / config.phantomPainSeconds) {
            return;
        }
        Body.notify(player, Component.translatable("triage.msg.phantom_pain", part.title()),
                ChatFormatting.DARK_GRAY, null);
    }
}
