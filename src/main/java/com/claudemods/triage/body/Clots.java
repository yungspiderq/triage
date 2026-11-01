package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Blood that stopped moving, and what happens when it starts again.
 *
 * <p>Everything else in this mod arrives on the end of something: a blow, a blade, a fire,
 * a mouthful of the wrong dinner. This one is the opposite of an event. <b>It is what a
 * body does to itself when nothing is happening to it at all</b> - blood that is not moving
 * sets, and the things that stop it moving are a strap, a splint, a broken leg, and an hour
 * spent lying on the floor while somebody decides whether to come for you.
 *
 * <p>Which makes it the first injury in this mod <b>caused by treatment</b>, and that is
 * deliberate rather than clever: the tourniquet that saved the limb, the splint that set
 * the bone and the stillness that let both of them work are the three commonest ways to
 * get one in life too. Nothing here punishes a mistake. It charges for a correct decision,
 * which is a thing the mod has never done.
 *
 * <p>The leg is not the danger. The leg is swollen and warm and aches, and that is the
 * whole of the warning: nothing to see, nothing to put a hand on, no bleeding, no mark on
 * the chart that anybody would walk across a field for. <b>What it is waiting for is you
 * to stand up.</b> A clot that has been sitting in a still limb goes when the limb starts
 * working - and it does not stop in the leg. It stops in the lungs.
 *
 * <p>After that there are about ninety seconds and one answer, and the answer is not a
 * bandage. See {@link Disease#EMBOLISM}.
 */
public final class Clots {
    /** How many ways there are of saying the leg aches, in the language files. */
    public static final int LINES = 3;

    private Clots() {
    }

    // ---- forming --------------------------------------------------------------

    /**
     * One second of blood either moving or not.
     *
     * <p>Asked of everybody, and answered no for anybody who is walking about - which is
     * the entire prevention and the reason it is never explained in a message. A player
     * who moves does not get these.
     */
    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (!config.clots) {
            return;
        }
        boolean still = still(player, body);
        int stillTicks = Math.max(1, config.clotStillSeconds * 20);
        body.tickStillness(still ? interval : -interval * 2, stillTicks * 2);

        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.clotted()) {
                sitting(player, body, state, part, config, interval, still);
            } else if (part.limb()) {
                forming(player, body, state, part, config, interval, still, stillTicks);
            }
        }
    }

    /**
     * Whether this body is doing the thing that makes them.
     *
     * <p>Three ways to be still, and two of them are not a choice: on the floor, or under
     * an anaesthetic. The third is standing about, which is a choice and is the one every
     * player will make while they read their own chart.
     */
    private static boolean still(ServerPlayer player, BodyHealth body) {
        // How far they have walked since this was last asked. Vanilla keeps the figure
        // for hunger, it is updated where movement actually happens, and it is the only
        // honest answer to "is this body doing anything": a player's velocity read from
        // the server is nearly always zero however hard they are running.
        float walked = player.walkDist;
        float step = walked - body.walked;
        body.walked = walked;
        if (body.floored() || body.under()) {
            return true;
        }
        // About a third of a block a second. Turning on the spot does not count, which
        // is correct and slightly cruel.
        return step < 0.35F;
    }

    /**
     * A vein in a limb that has not moved for a while.
     *
     * <p>Everything that multiplies the chance is something the mod already models, and
     * every one of them is the reason it happens in life: the strap, the stick, the bone
     * that will not take weight, and blood that is thick because there is less of it.
     */
    private static void forming(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                Config.Values config, int interval, boolean still, int stillTicks) {
        if (state.gone() || body.stillness() < stillTicks) {
            return;
        }
        // A clot needs something to explain it. The mod already models every one of
        // these, and every one is a reason it happens in life; a body with none of them
        // and nothing else wrong simply does not make one, however long it stands there.
        boolean atRisk = state.tourniquet || state.splinted() || state.fractured() || state.cracked
                || body.bloodLoss() >= 0.05F || body.floored() || body.under();
        if (!atRisk) {
            return;
        }
        float chance = config.clotChance * (interval / 20.0F);
        // A strap is the worst of them by a distance: nothing below it is moving at all,
        // which is exactly what the strap is for.
        if (state.tourniquet) {
            chance *= 4.0F;
        }
        if (state.splinted()) {
            chance *= 2.5F;
        }
        if (state.fractured() || state.cracked) {
            chance *= 2.0F;
        }
        // Thicker blood, because there is less of it. The same number that makes a pulse
        // race makes this likelier, which is the sort of thing worth being true twice.
        chance *= 1.0F + body.bloodLoss();
        if (!part.isLeg()) {
            // Arms get them and get them far less. The size of the vein decides it.
            chance *= 0.35F;
        }
        // The meter outlives the stillness by design, because a body that has just got
        // up is still a body that was sitting down. What it must not do is keep rolling
        // at the old rate while the player walks: moving is the prevention, and it has
        // to be the prevention for the whole of the recovery as well.
        if (!still) {
            chance *= 0.1F;
        }
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        state.clot = true;
        state.clotTicks = 0;
        body.setDirty(true);
        Reactions.say(player, "clot");
        Body.notify(player, Component.translatable("triage.msg.clot", part.title()),
                ChatFormatting.DARK_PURPLE, null);
    }

    // ---- and going -------------------------------------------------------------

    /**
     * A clot that is already there, and the moment it stops being there.
     *
     * <p>The whole design is in the condition: it goes when the limb <b>starts working</b>.
     * Lie still with one and nothing happens for as long as you like; stand up and run and
     * the thing that has been quietly sitting in your calf for four minutes is in your
     * chest in one beat.
     *
     * <p>So the danger is not the injury. The danger is the recovery.
     */
    private static void sitting(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                Config.Values config, int interval, boolean still) {
        state.clotTicks += interval;
        if (state.gone()) {
            state.clearClot();
            body.setDirty(true);
            return;
        }
        // Old ones go by themselves eventually, which is what happens to most of them and
        // is the only reason having one is survivable at all.
        if (config.clotClearSeconds > 0 && state.clotTicks >= config.clotClearSeconds * 20) {
            state.clearClot();
            body.setDirty(true);
            Body.notify(player, Component.translatable("triage.msg.clot_gone", part.title()),
                    ChatFormatting.GREEN, null);
            return;
        }
        if (still) {
            return;
        }
        float chance = config.embolismChance * (interval / 20.0F);
        // Getting up is one thing. Running is another.
        if (player.isSprinting()) {
            chance *= 3.0F;
        }
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        breaks(player, body, state, part, config);
    }

    /** It has gone to the lungs. */
    private static void breaks(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                               Config.Values config) {
        state.clearClot();
        body.setDirty(true);
        if (!body.catchDisease(Disease.EMBOLISM)) {
            return;
        }
        Reactions.say(player, "embolism");
        Body.notify(player, Component.translatable("triage.msg.embolism", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
    }

    /**
     * One second of a lung with something in it.
     *
     * <p>Not an illness that gets worse over an afternoon. It is the fastest thing in this
     * mod after an artery, it takes the chest apart rather than the limb, and no bandage,
     * no drip and no course of anything touches it. What touches it is the one drug that
     * breaks the thing up, given in the minute and a half that is on offer.
     */
    public static void embolism(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.EMBOLISM)) {
            return;
        }
        Injuries.apply(player, body, BodyPart.THORAX, config.embolismDamage, false);
    }

    /**
     * What the thinner does, which depends entirely on when it was given.
     *
     * <p>To a limb, it is a course: the clot in there breaks up over the next while and
     * the danger is over. To a chest with one already in it, it is the only thing in this
     * mod that undoes an emergency rather than treating the damage from one - and it is
     * the difference between a minute and a half and a grave.
     *
     * @return true when there was anything for it to do
     */
    public static boolean thin(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                               Config.Values config, float quality) {
        boolean anything = false;
        if (body.has(Disease.EMBOLISM)) {
            // Quality decides how much of the damage was avoided rather than whether it
            // works. A drug that half-worked in a chest is not a thing anybody survives
            // by being told they should have aimed better.
            body.cure(Disease.EMBOLISM);
            body.setDirty(true);
            Body.notify(healer, Component.translatable("triage.msg.embolism_cleared",
                    patient.getName()), ChatFormatting.GREEN, SoundEvents.PLAYER_LEVELUP);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.embolism_cleared_you"),
                        ChatFormatting.GREEN, null);
            }
            anything = true;
        }
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.clotted()) {
                continue;
            }
            // A poor dose leaves it sitting there a while longer rather than clearing it.
            if (quality < 0.4F) {
                state.clotTicks = Math.max(0, state.clotTicks - 20 * 30);
                Body.notify(healer, Component.translatable("triage.msg.clot_slowed", part.title()),
                        ChatFormatting.GOLD, null);
            } else {
                state.clearClot();
                Body.notify(healer, Component.translatable("triage.msg.clot_cleared", part.title()),
                        ChatFormatting.GREEN, null);
            }
            body.setDirty(true);
            anything = true;
        }
        return anything;
    }

    /** Whether anything on this body is a job for the thinner. */
    public static boolean worthThinning(BodyHealth body) {
        if (body.has(Disease.EMBOLISM)) {
            return true;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).clotted()) {
                return true;
            }
        }
        return false;
    }
}
