package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

/**
 * Broken, and then broken in a particular way.
 *
 * <p>For eight versions this mod had one kind of fracture and one answer to it. A bone
 * broke, a splint went on, the bone was set. That is the easy half of what a fracture is,
 * and it left out the half that everybody who has seen one remembers: <b>the ends move.</b>
 *
 * <p>So there are three rungs now, and each one is a different job:
 *
 * <ul>
 *   <li><b>Broken, in line.</b> The bone is in two pieces and the pieces are where they
 *       were. A splint is the whole answer, exactly as it always was.</li>
 *   <li><b>Out of line.</b> The limb is the wrong shape, and anybody can see it from
 *       across a room without reading anything. A splint here is worse than useless: it
 *       holds the bone precisely where it should not be, and it knits like that. What it
 *       needs first is somebody to pull it straight, which is done with bare hands, hurts
 *       more than anything else in this mod, and is loud.</li>
 *   <li><b>Through the skin.</b> Also a wound, and the fastest road to infection here -
 *       the inside of a leg has been introduced to the floor. Nothing can be set and no
 *       splint goes on until a surgeon has cleaned it out and closed it.</li>
 * </ul>
 *
 * <p>And under all three, the thing that makes them matter a week later: a bone left out
 * of line <b>knits out of line</b>, for good. See {@link PartState#crooked}.
 */
public final class Bones {
    /** What a bone end that has gone through something carrying blood looks like. */
    private static final int ARTERY_TICKS = 0;

    private Bones() {
    }

    // ---- how they arrive ----------------------------------------------------

    /**
     * A bone has just broken. Decide whether it stayed where it was.
     *
     * <p>Called from the one place breaks are made, so every road into a fracture - a
     * blow, a fall, a crack leaned on - comes through here and gets the same question
     * asked of it.
     */
    public static void broke(ServerPlayer player, BodyHealth body, BodyPart part, float amount) {
        Config.Values config = Config.values();
        PartState state = body.part(part);
        if (!config.displacement || state.displaced || amount < config.displacedBlow) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.displacedChance) {
            return;
        }
        state.displaced = true;
        body.setDirty(true);
        Reactions.say(player, "break");
        Body.notify(player, Component.translatable("triage.msg.displaced", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.BONE_BLOCK_BREAK);

        // And, rarely, the end of it arrives outside. Only ever from a break that has
        // already moved, because that is the only way it happens: a bone in line has
        // nowhere to go.
        if (!config.openFractures || player.getRandom().nextFloat() >= config.openChance) {
            return;
        }
        state.openBreak = true;
        state.startBleeding(PartState.BLEED_HEAVY, ARTERY_TICKS);
        body.addBloodLoss(config.openBloodLoss);
        if (config.shock) {
            body.catchDisease(Disease.SHOCK);
        }
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.open_fracture", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
    }

    /** How much faster a wound with bone in it goes bad. */
    public static float infectionFactor(PartState state) {
        Config.Values config = Config.values();
        return config.displacement && state.openFracture() ? Math.max(1.0F, config.openInfection) : 1.0F;
    }

    // ---- what they cost -----------------------------------------------------

    /**
     * What the part is actually worth, after every bone in it that healed crooked.
     *
     * <p>The only permanent loss of health in the mod. Everything else here heals to the
     * number it started at, and this one does not, because a malunion is not a wound that
     * has not closed yet - it is a bone that is the wrong length and will stay that way.
     *
     * <p>Floored well above nothing: a crooked leg is a worse leg and never a missing one,
     * and no amount of bad luck should quietly take a limb away by arithmetic.
     */
    public static float maxShare(PartState state) {
        Config.Values config = Config.values();
        if (!config.displacement || state.crooked <= 0) {
            return 1.0F;
        }
        return Math.max(0.5F, 1.0F - config.crookedCost * Math.min(3, state.crooked));
    }

    /** And what it costs to use, which is felt in a leg and in a hand differently. */
    public static float penalty(PartState state) {
        Config.Values config = Config.values();
        if (!config.displacement || state.crooked <= 0) {
            return 0.0F;
        }
        return Math.min(0.6F, config.crookedPenalty * Math.min(3, state.crooked));
    }

    // ---- putting it back ----------------------------------------------------

    /**
     * Whether there is anything here for a pair of hands to pull straight.
     *
     * <p>An open one is refused: pulling on a bone that is sticking out of somebody drags
     * whatever it has touched back inside them, and the mod is not going to offer that as
     * a treatment. The surgeon goes first.
     */
    public static boolean worthSetting(PartState state) {
        return Config.values().displacement && state.outOfLine() && !state.openFracture()
                && !state.gone();
    }

    /**
     * The hands are done.
     *
     * <p>What decides it is whether the pull went along the bone. Done well, the ends go
     * back together and everything after it is the ordinary business of a splint and a
     * week of rest. Done badly, something in there is worse than it was - and in the worst
     * case the end of the bone finds a vessel on the way past, which is the thing that
     * makes a displaced femur frightening to people who know what they are looking at.
     *
     * @param quality 0 to 1, or at or below zero for a pull that went the wrong way
     */
    public static void set(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                           BodyPart part, float quality) {
        Config.Values config = Config.values();
        PartState state = body.part(part);
        boolean self = helper == patient;

        // Seated, and seated wrong. The ends are together and they are together out of
        // line, which is neither a failure nor a reduction: nothing is damaged, nothing
        // is fixed, and the hands can try again. It is by far the commonest outcome of a
        // first attempt and it should cost nothing but the attempt.
        if (quality > 0.0F && quality < 0.35F) {
            Body.notify(helper, Component.translatable("triage.msg.set_not_quite", part.title()),
                    ChatFormatting.GOLD, SoundEvents.BONE_BLOCK_HIT);
            scream(patient, body, config);
            return;
        }

        if (quality <= 0.0F) {
            Injuries.apply(patient, body, part, config.setTear, false);
            if (config.arterial && helper.getRandom().nextFloat() < config.setArteryChance) {
                state.startBleeding(PartState.BLEED_ARTERIAL, 0);
                Reactions.say(patient, "artery");
                Body.notify(helper, Component.translatable("triage.msg.set_artery", part.title()),
                        ChatFormatting.DARK_RED, SoundEvents.PLAYER_ATTACK_CRIT);
            } else {
                Body.notify(helper, Component.translatable("triage.msg.set_failed", part.title()),
                        ChatFormatting.RED, SoundEvents.PLAYER_HURT);
            }
            body.setDirty(true);
            scream(patient, body, config);
            return;
        }

        state.reduce();
        body.setDirty(true);
        Body.notify(helper, Component.translatable(self ? "triage.msg.set_self" : "triage.msg.set_them",
                        patient.getName(), part.title()),
                ChatFormatting.GREEN, SoundEvents.BONE_BLOCK_PLACE);
        if (!self) {
            Body.notify(patient, Component.translatable("triage.msg.set_you",
                    helper.getName(), part.title()), ChatFormatting.GREEN, null);
        }
        // Straight, and still broken. What it wants now is the stick, and after that the
        // one thing nobody wants to do, which is nothing at all for a while.
        Body.notify(helper, Component.translatable("triage.msg.set_now_splint"),
                ChatFormatting.GRAY, null);
        scream(patient, body, config);
    }

    /**
     * The noise.
     *
     * <p>Nothing else in this mod does anything to the room. Every other treatment is a
     * transaction between two people and the world does not care - and this one is the
     * exception, because a bone being pulled back into line through a limb that can feel
     * it is not a quiet thing, and there is no version of it that is.
     *
     * <p>Which is the first reason to carry the mask that has nothing to do with quality:
     * a patient who is under does not scream. Nor does one who has had enough morphine,
     * though they will still remember it.
     */
    private static void scream(ServerPlayer patient, BodyHealth body, Config.Values config) {
        if (!config.scream || config.screamRange <= 0.0F) {
            return;
        }
        // Asleep, or far enough under that the noise never starts.
        if (body.under() || body.painkilled() || body.downed()) {
            return;
        }
        Reactions.say(patient, "set");
        if (!(patient.level() instanceof ServerLevel level)) {
            return;
        }
        patient.level().playSound(null, patient.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 1.4F, 0.7F);
        double range = config.screamRange;
        // Everything with ears and a reason to come. Not a summon and not a debuff: it
        // is a loud noise, and things that hunt by noise are told where it came from.
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                new AABB(patient.blockPosition()).inflate(range), mob -> mob instanceof Enemy)) {
            if (mob.getTarget() == null) {
                mob.setTarget(patient);
            }
        }
    }

    /**
     * A surgeon on a bone that came through the skin.
     *
     * <p>Cleaning it out and closing it, which is the whole of what a surgeon does for one
     * and the reason nothing else can be done first. What is left afterwards is an
     * ordinary displaced fracture, and an ordinary displaced fracture still wants hands.
     *
     * @return true when there was an open fracture to close
     */
    public static boolean close(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        PartState state = body.part(part);
        if (!state.openFracture()) {
            return false;
        }
        state.closeBone();
        state.disinfect();
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.bone_closed", part.title()),
                ChatFormatting.GREEN, SoundEvents.PLAYER_LEVELUP);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.bone_closed_you",
                    healer.getName(), part.title()), ChatFormatting.GREEN, null);
        }
        return true;
    }

    /**
     * And a surgeon on a bone that healed crooked, which is the only way out of one.
     *
     * <p>It is the same operation it is in life and it sounds exactly as bad: the bone is
     * broken again, on purpose, so that this time somebody can set it straight. The mod
     * offers no shortcut and is not going to invent one - what fixes a bone that healed
     * wrong is doing the week again, properly.
     *
     * @return true when there was a crooked bone to break
     */
    public static boolean rebreak(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        PartState state = body.part(part);
        if (!state.bent() || state.gone()) {
            return false;
        }
        state.rebreak(part.bone().count());
        // The health that was lost to the crooked bone comes back the moment the bone
        // stops being crooked - which is now, because there is no crooked bone any more.
        // There is a broken one, which is a different and much more temporary problem.
        body.reconcile();
        body.setDirty(true);
        Reactions.say(patient, "break");
        Body.notify(healer, Component.translatable("triage.msg.rebroken", part.title()),
                ChatFormatting.GOLD, SoundEvents.BONE_BLOCK_BREAK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.rebroken_you",
                    healer.getName(), part.title()), ChatFormatting.GOLD, null);
        }
        return true;
    }

    /**
     * A break has finished knitting. Which kind of mark it leaves depends on one thing.
     *
     * @return true when it knitted out of line
     */
    public static boolean knitted(ServerPlayer player, BodyHealth body, BodyPart part) {
        PartState state = body.part(part);
        if (!Config.values().displacement || !state.displaced) {
            Scars.knitted(player, body, part);
            return false;
        }
        state.knittedCrooked();
        // The part is worth less than it was, for good, and the body has to be told so
        // the number on the chart stops being a lie.
        body.reconcile();
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.knit_crooked", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.BONE_BLOCK_FALL);
        return true;
    }
}
