package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

/**
 * Illness that goes somewhere, and illness that goes to somebody else.
 *
 * <p>Two things were missing. The first is that every illness here has been a private
 * fact: you caught it from the weather or from your own wound, it ran its clock, and
 * nobody else in the world was any different for it. A cold is not like that. A cold is
 * the one thing in medicine that is mostly about other people.
 *
 * <p>The second is that the two illnesses with somewhere to go had nowhere to go. A cold
 * sat at "mild, passes" forever, however long somebody spent wet and freezing with it,
 * and a torn gut rolled straight into sepsis - from the wound to the worst thing in the
 * mod in one step, with nothing in between to notice or to act on. Both of those are the
 * middle of an illness missing, and the middle is where the decisions are.
 *
 * <p>So: a cold that is left in a body that stays cold settles into the lungs, and a gut
 * that is leaking gives you peritonitis before it gives you sepsis. Neither is a new way
 * to die. They are the warnings that were not there.
 *
 * <p>Where each of them ends up is not decided here. The mod already knew how to say that
 * one illness becomes another - it is how a fever becomes sepsis - so both of these carry
 * a duration and a {@code then()} like everything else. What is left in this file is the
 * part that is actually about a body: what they do while they last, and what starts them.
 */
public final class Sick {
    /** How far a sneeze carries to somebody who can catch it. */
    private static final double CATCHING_RANGE = 4.0;
    /** How close is close enough to be in the crowd. */
    private static final double CROWD_RANGE = 8.0;

    private Sick() {
    }

    // ---- the cold ---------------------------------------------------------

    /**
     * A sneeze, and what one is actually for.
     *
     * <p>It used to be a sound and a line of text addressed to the person sneezing, which
     * is the one audience a sneeze is not for. Now it does the three things a sneeze does:
     * it is loud, it is catching, and it hurts if your chest is already wrong.
     */
    public static void sneeze(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        if (player.getRandom().nextFloat() >= config.sneezeChance) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 1.0F, 1.6F);
        Body.notify(player, Component.translatable("triage.msg.sneeze"), ChatFormatting.GRAY, null);

        hurts(player, body, config);
        catching(player, config);
    }

    /**
     * What a sneeze costs a chest that is already in trouble.
     *
     * <p>Anybody who has sneezed with a cracked rib knows this one. And a belly that was
     * closed up an hour ago is held together by the one thing a sneeze is designed to
     * defeat, which makes this the only way in the mod to come open without doing
     * anything at all.
     */
    private static void hurts(ServerPlayer player, BodyHealth body, Config.Values config) {
        for (BodyPart part : new BodyPart[]{BodyPart.THORAX, BodyPart.STOMACH}) {
            PartState state = body.part(part);
            if (state.mending()) {
                Mending.jolted(player, body, state, part);
                return;
            }
        }
        PartState chest = body.part(BodyPart.THORAX);
        if (chest.fractured() || chest.cracked) {
            Injuries.apply(player, body, BodyPart.THORAX, config.sneezeDamage, false);
            Body.notify(player, Component.translatable("triage.msg.sneeze_ribs"),
                    ChatFormatting.RED, null);
        }
    }

    /**
     * Whoever was standing too close.
     *
     * <p>The first thing in this mod that travels from one player to another. It is a
     * cold rather than anything worse on purpose: an illness that spreads and also
     * matters would make standing near a wounded friend the wrong move, and the entire
     * mod is built on standing near wounded friends.
     */
    private static BodyHealth body(ServerPlayer player) {
        return Body.of(player);
    }

    private static void catching(ServerPlayer player, Config.Values config) {
        if (config.coldCatching <= 0.0F || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double range = CATCHING_RANGE * CATCHING_RANGE;
        for (ServerPlayer other : level.players()) {
            if (other == player || other.isSpectator() || other.distanceToSqr(player) > range) {
                continue;
            }
            // A body that has not slept catches what is going around, which is the one
            // thing everybody already knows about not sleeping.
            if (player.getRandom().nextFloat() >= config.coldCatching * Vigil.illness(other)) {
                continue;
            }
            BodyHealth theirs = Body.of(other);
            // Whatever the sneezing one has is what travels. A cold gives a cold and the
            // flu gives the flu, which is the only honest way for one sneeze to work.
            Disease going = body(player).has(Disease.FLU) ? Disease.FLU : Disease.COLD;
            if (theirs.has(going) || theirs.has(Disease.PNEUMONIA)) {
                continue;
            }
            if (theirs.catchDisease(going)) {
                Reactions.say(other, "ill");
                Body.notify(other, Component.translatable("triage.msg.caught_from",
                        player.getName(), going.title()), going.colour(), null);
            }
        }
    }

    // ---- and where one of them comes from ----------------------------------

    /**
     * Influenza, and the crowd it comes out of.
     *
     * <p>Every other illness in this mod has a source that is not a person: the weather
     * gives a cold, a dirty wound gives a fever, something eaten gives the rest. The flu
     * had only the sneeze - which means it could be passed on by somebody who had it and
     * caught by nobody who did not, so in a world with one player in it the virus did not
     * exist. A disease that cannot start is a disease that is not in the game.
     *
     * <p>So it starts where influenza actually starts, which is a crowd. This game has
     * exactly one, and it is a village. Nothing about it is aimed at the player - the
     * villagers are not ill and do not become ill and nothing is tracked about them -
     * it is simply that standing among a lot of bodies for a long time is how this is
     * caught, and a village is the only place in Minecraft where that is true.
     *
     * <p>It is also what makes the sleepless rule mean anything to somebody playing
     * alone. Being three days awake has always multiplied the chance of catching what is
     * going around, and until now nothing was going around unless a second player brought
     * it.
     */
    private static void crowd(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (config.fluRisk <= 0.0F || body.has(Disease.FLU) || body.has(Disease.PNEUMONIA)) {
            return;
        }
        // Whatever is already burning is the thing being treated. Somebody in a fever
        // does not need the mod to hand them a second reason to be hot.
        if (body.has(Disease.FEVER) || body.has(Disease.SEPSIS) || body.has(Disease.COLD)) {
            return;
        }
        AABB near = player.getBoundingBox().inflate(CROWD_RANGE);
        int bodies = player.level().getEntitiesOfClass(Villager.class, near).size();
        if (bodies < config.fluCrowd) {
            return;
        }
        // A bigger crowd is a worse one, but only up to a point: living in a village
        // should not be a standing illness, and the ceiling is what keeps it from being.
        float crowded = Math.min(2.0F, bodies / (float) config.fluCrowd);
        float chance = config.fluRisk * crowded * Vigil.illness(player);
        if (player.getRandom().nextFloat() < chance && body.catchDisease(Disease.FLU)) {
            Reactions.say(player, "ill");
            Body.notify(player, Component.translatable("triage.msg.caught_crowd",
                    Disease.FLU.title()), Disease.FLU.colour(), null);
        }
    }

    // ---- and where illnesses go if nobody stops them -----------------------

    /**
     * One second of the two illnesses that have somewhere to be going.
     *
     * <p>Called from the tick rather than from the disease loop because both of these are
     * about a body rather than about a clock: what decides them is how cold somebody is
     * and whether the hole is still open.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.diseases) {
            return;
        }
        crowd(player, body, config);
        settles(player, body, config);
        complicates(player, body, config);
        pneumonia(player, body, config);
        peritonitis(player, body, config);
    }

    /**
     * A cold that goes down into the chest.
     *
     * <p>It needs two things and both are the player's business: time, and a body that is
     * still cold. Somebody who caught a chill and then got dry and warm gets better, which
     * is what happens; somebody who stayed out in it does not.
     */
    private static void settles(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.COLD) || body.has(Disease.PNEUMONIA)) {
            return;
        }
        if (body.diseaseTicks(Disease.COLD) < config.pneumoniaAfterSeconds * 20) {
            return;
        }
        boolean still = body.has(Disease.HYPOTHERMIA) || player.isInWaterOrRain()
                || player.isFullyFrozen() || player.isInPowderSnow;
        if (!still || player.getRandom().nextFloat() >= config.pneumoniaChance) {
            return;
        }
        body.cure(Disease.COLD);
        if (body.catchDisease(Disease.PNEUMONIA)) {
            Body.notify(player, Component.translatable("triage.msg.settled"),
                    Disease.PNEUMONIA.colour(), SoundEvents.PLAYER_HURT);
        }
    }

    /**
     * The flu going into a chest, which is the one thing the flu is here for.
     *
     * <p>It used to happen by the clock: the virus ran its three hundred seconds and
     * turned into pneumonia, every time, for everybody. That is a worse idea than it
     * looks. It makes the course mandatory rather than a decision, it makes the virus a
     * pneumonia timer rather than an illness, and it takes away the only interesting
     * sentence in the whole feature - that most flu is just flu.
     *
     * <p>So it is a roll, and what it is rolled against is the body. Cold, wet or badly
     * short of sleep, and the chest is likely to open before the virus is done; warm, dry
     * and rested, and it will not open at all. Both of those are things the player is
     * doing, which is the same shape as a cold settling, and the answer to both is the
     * answer that costs no item.
     *
     * <p>The chance climbs as the illness runs, because a chest is opened by a week of
     * being ill and not by catching something an hour ago.
     */
    private static void complicates(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.FLU) || body.has(Disease.PNEUMONIA)) {
            return;
        }
        float tired = Vigil.share(player);
        boolean worn = body.has(Disease.HYPOTHERMIA) || player.isInWaterOrRain()
                || player.isFullyFrozen() || player.isInPowderSnow || tired >= 0.33F;
        if (!worn) {
            return;
        }
        float through = Math.min(1.0F,
                body.diseaseTicks(Disease.FLU) / (float) Math.max(1, config.fluSeconds * 20));
        if (player.getRandom().nextFloat() >= config.fluPneumonia * through) {
            return;
        }
        body.cure(Disease.FLU);
        if (body.catchDisease(Disease.PNEUMONIA)) {
            Body.notify(player, Component.translatable("triage.msg.settled_flu"),
                    Disease.PNEUMONIA.colour(), SoundEvents.PLAYER_HURT);
        }
    }

    /**
     * What pneumonia is: a lung that is filling up.
     *
     * <p>It bruises the lungs rather than tearing them, which is the honest difference
     * between an illness and a wound - there is no hole for a surgeon to find, and opening
     * the chest would achieve nothing at all. The only answer is a course, and the reason
     * to hurry is the same as it is for everything else here.
     */
    private static void pneumonia(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.PNEUMONIA)) {
            return;
        }
        PartState chest = body.part(BodyPart.THORAX);
        for (Organ lung : new Organ[]{Organ.LEFT_LUNG, Organ.RIGHT_LUNG}) {
            if (chest.organ(lung) < PartState.ORGAN_BRUISED) {
                chest.tearOrgan(lung, PartState.ORGAN_BRUISED);
                body.setDirty(true);
            }
        }
        Injuries.apply(player, body, BodyPart.THORAX, config.pneumoniaDamage, false);
    }

    /**
     * A belly full of what was supposed to stay in the gut.
     *
     * <p>This is the step that was missing. A torn gut used to roll a chance of sepsis
     * every second and otherwise say nothing, so the whole illness was one dice roll in
     * the dark. Now it announces itself, runs hot, eats the belly, and gives whoever is
     * working on it a few minutes to close the hole.
     */
    private static void peritonitis(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.PERITONITIS)) {
            return;
        }
        Injuries.apply(player, body, BodyPart.STOMACH, config.peritonitisDamage, false);
    }

    /**
     * Whether a course of antibiotics can finish this one off.
     *
     * <p>Peritonitis is the first illness here that antibiotics cannot simply cure, and
     * the reason is not difficulty: the cause is still leaking. A course against an open
     * gut buys time and nothing else, which is exactly what a course against an open gut
     * does. The surgeon ends it; the bottle only means the surgeon still has somebody to
     * work on when they get there.
     */
    public static boolean curable(BodyHealth body, Disease disease) {
        // Lockjaw is treatable while it is silent and not afterwards, which is the same
        // bargain the thing in a dog's mouth offers. The difference is that this clock
        // starts at a wound somebody dressed without washing, so it is a clock the medic
        // started themselves.
        if (disease == Disease.TETANUS) {
            return body.diseaseTicks(Disease.TETANUS) < Config.values().tetanusSeconds * 20;
        }
        if (disease != Disease.PERITONITIS) {
            return true;
        }
        return !body.part(BodyPart.STOMACH).organTorn();
    }
}
