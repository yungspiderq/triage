package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.med.Treatments;
import com.claudemods.triage.data.Milestones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LightLayer;

/**
 * Everything that gets worse while you are busy with something else: bones knitting or
 * failing to, wounds going bad, and illness running its course.
 *
 * <p>This is where the mod's deadline lives. Bleeding you can ignore for a minute;
 * an infection you cannot ignore for ten, because at the end of that chain is sepsis,
 * which does not stop.
 */
public final class Ailments {
    /** Per second, on top of everything else. A course makes you hungry; it is famous for it. */
    private static final float SEDATION_HUNGER = 0.35F;

    /** Below this a biome counts as somewhere you can catch cold in the rain. */
    private static final float COLD_BIOME = 0.3F;
    /** The tick this class runs on, for the counters it keeps itself. */
    private static final int INTERVAL = 20;

    private Ailments() {
    }

    /** One pass, once a second. {@code interval} is how many ticks that was. */
    /**
     * Lockjaw, out of the one thing in this mod that cares what shape a wound is.
     *
     * <p>A deep narrow hole closed at the top is not the same wound as a gash, and until now
     * the mod treated them the same: dirt in either one went septic. This is the other thing
     * that lives at the bottom of a puncture, and the only way to stop it is the boring one
     * - take the fragment out and wash the hole before anything goes over it.
     */
    private static void tetanus(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (body.has(Disease.TETANUS)) {
            return;
        }
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            boolean puncture = state.shrapnel || state.arrows > 0 || state.bullets > 0;
            if (!puncture || !state.dressed || state.dirtyTicks <= 0) {
                continue;
            }
            if (player.getRandom().nextFloat() < config.tetanusChance
                    && body.catchDisease(Disease.TETANUS)) {
                // Nothing is said. That is the whole design of this one: it is silent for
                // as long as it is treatable, and the only thing in the world that knows
                // is the wound nobody washed.
                return;
            }
        }
    }

    /**
     * The spasms, which start when the silence ends.
     *
     * <p>The only thing in this mod that interrupts a treatment from the inside. Everything
     * else that stops a pair of hands is somebody hitting them - so a patient nobody can
     * finish working on is a new problem, and it is the correct one: this is exactly what a
     * tetanic spasm does to a room trying to help.
     */
    private static void spasms(ServerPlayer player, BodyHealth body, Config.Values config,
                               int interval) {
        if (!body.has(Disease.TETANUS)
                || body.diseaseTicks(Disease.TETANUS) < config.tetanusSeconds * 20) {
            return;
        }
        if (player.getRandom().nextFloat() > interval / 100.0F) {
            return;
        }
        Injuries.apply(player, body, BodyPart.THORAX, 1.0F, false);
        Treatments.onDamaged(player, 4.0F);
        Aid.onDamaged(player, 4.0F);
        Reactions.say(player, "spasm");
    }

    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        cracks(player, body, config, interval);
        // Before the infections, and outside them: a dressing has a clock whether or not
        // wounds in this world are allowed to go bad, and switching infection off should
        // not leave every bandage in the game welded on for good.
        dressings(player, body, config, interval);
        if (config.pneumothorax) {
            chest(player, body, config, interval);
        }
        if (config.infection) {
            infections(player, body, config, interval);
        }
        if (config.diseases) {
            catchCold(player, body, config);
            // The two that come out of a wound's shape and out of a limb rather than out
            // of the weather. Both sit here rather than with the colds because neither is
            // caught from the world: one is caught from a dressing and one from a strap.
            tetanus(player, body, config);
            spasms(player, body, config, interval);
            crushing(player, body, config, interval);
            diseases(player, body, config, interval);
            if (config.shock) {
                shock(player, body, config);
            }
            if (config.hypothermia) {
                hypothermia(player, body, config, interval);
            }
            if (config.delirium) {
                delirium(player, body, config);
            }
        }
        if (config.schizophrenia) {
            mind(player, body, config, interval);
            catatonia(player, body, config, interval);
            neuroleptic(player, body, config, interval);
        }
    }

    /**
     * A head that is not being supplied properly.
     *
     * <p>Unlike everything else here it is not caught and does not run down: it is a
     * mirror. Every second it asks whether any of the four things that starve a brain is
     * true, and it is exactly as true as they are. Nothing treats it directly and nothing
     * should - the answer to it is always the answer to something else, which is the one
     * idea this condition exists to carry.
     *
     * <p>The reason it is worth having at all is that it is the only thing in the mod
     * that makes the player doubt their own screen. Everything else the chart says is
     * true; concussion makes it coarse but never wrong. This makes it wrong, and knowing
     * that you cannot trust it is a different kind of danger from not being able to read
     * it.
     */
    private static void delirium(ServerPlayer player, BodyHealth body, Config.Values config) {
        // Four ways for a brain to be short of what it runs on: no volume to carry it,
        // no air to put in it, a temperature that has stopped it working, and a body
        // poisoning itself. Every one of them is somebody else's condition first.
        boolean starved = body.bloodLoss() >= config.deliriumBloodLoss
                || Aid.blocked(body, config)
                || body.has(Disease.SEPSIS)
                || body.has(Disease.HYPOTHERMIA);

        if (!body.has(Disease.DELIRIUM)) {
            if (starved && body.catchDisease(Disease.DELIRIUM)) {
                Body.notify(player, Component.translatable("triage.msg.caught", Disease.DELIRIUM.title()),
                        Disease.DELIRIUM.colour(), SoundEvents.AMBIENT_CAVE.value());
            }
            return;
        }
        if (starved) {
            body.resetDiseaseTicks(Disease.DELIRIUM);
            hallucinate(player);
            return;
        }
        // A mind does not come back the moment the cause does. The settling time is
        // short, and it is there so that a volume hovering on the threshold does not
        // switch somebody in and out of their own senses once a second.
        body.addDiseaseTicks(Disease.DELIRIUM, INTERVAL);
        hallucinate(player);
        if (body.diseaseTicks(Disease.DELIRIUM) >= config.deliriumClearSeconds * 20) {
            body.cure(Disease.DELIRIUM);
            Body.notify(player, Component.translatable("triage.msg.recovered", Disease.DELIRIUM.title()),
                    ChatFormatting.GREEN, null);
        }
    }

    /**
     * Something that is not there.
     *
     * <p>Sent to the one player rather than played into the world, which is the whole
     * difference between this and every other sound the mod makes: nobody standing next
     * to them hears it, and it does not exist. Ordinary world noises out of place -
     * a door, a footstep behind you, water where there is none - because that is what
     * this actually is. It is not visions.
     */
    private static void hallucinate(ServerPlayer player) {
        if (player.getRandom().nextFloat() >= 0.12F) {
            return;
        }
        SoundEvent sound = switch (player.getRandom().nextInt(5)) {
            case 0 -> SoundEvents.WOODEN_DOOR_OPEN;
            case 1 -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case 2 -> SoundEvents.AMBIENT_CAVE.value();
            case 3 -> SoundEvents.WATER_AMBIENT;
            default -> SoundEvents.GRAVEL_STEP;
        };
        player.playNotifySound(sound, SoundSource.AMBIENT,
                0.3F + player.getRandom().nextFloat() * 0.3F,
                0.7F + player.getRandom().nextFloat() * 0.6F);
    }

    /**
     * A body that has stopped.
     *
     * <p>Nothing is done to it and nothing can be done for it. Movement is taken away
     * outright, which is the only honest way to model a thing whose entire description
     * is that the person cannot start anything, and it ends by itself.
     *
     * <p>A course prevents it. That is not a balance decision either: it is the single
     * strongest argument for staying on one, and the mod would be misrepresenting the
     * illness if the treatment did nothing about its worst form.
     */
    private static void catatonia(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (body.has(Disease.CATATONIA)) {
            // Not slowed. Stopped - and blind with it, because somebody in this state is
            // not taking in the room either.
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    interval * 2, 250, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    interval * 2, 250, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,
                    interval * 2, 0, true, false, false));
            return;
        }
        if (!body.has(Disease.SCHIZOPHRENIA) || body.medicated() || config.catatoniaChance <= 0.0F) {
            return;
        }
        float chance = config.catatoniaChance * (body.rebounding() ? 3.0F : 1.0F);
        if (player.getRandom().nextFloat() < chance && body.catchDisease(Disease.CATATONIA)) {
            Body.notify(player, Component.translatable("triage.msg.catatonia"),
                    ChatFormatting.DARK_GRAY, SoundEvents.AMBIENT_CAVE.value());
        }
    }

    /**
     * The rare one that the treatment itself does.
     *
     * <p>Caught when a course is taken rather than at random afterwards, so that the
     * risk sits on the decision it belongs to. While it runs it is doing real damage to
     * the whole body at once - a temperature, not a wound, so there is nothing to
     * bandage and no limb to pick.
     *
     * <p>It ends on two conditions together: the course is out of them, and they are
     * cold. Warmth is what hypothermia wants and this is its opposite in every respect,
     * which is why the two can never be treated the same way and why having both at once
     * is the worst hand the mod can deal.
     */
    private static void neuroleptic(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (!body.has(Disease.NEUROLEPTIC)) {
            return;
        }
        body.hurtAll(config.neurolepticDamage);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                interval * 2, 1, true, false, false));

        boolean cold = player.isInWater() || player.isInPowderSnow || player.isFreezing()
                || (player.level().isRaining() && player.level().canSeeSky(player.blockPosition()));
        if (body.medicated() || !cold) {
            body.resetDiseaseTicks(Disease.NEUROLEPTIC);
            return;
        }
        body.addDiseaseTicks(Disease.NEUROLEPTIC, interval);
        if (body.diseaseTicks(Disease.NEUROLEPTIC) >= config.neurolepticCoolSeconds * 20) {
            body.cure(Disease.NEUROLEPTIC);
            Body.notify(player, Component.translatable("triage.msg.recovered", Disease.NEUROLEPTIC.title()),
                    ChatFormatting.GREEN, null);
        }
    }

    /**
     * The one condition here that nobody caught, and the course that holds it down.
     *
     * <p>Three states rather than two, and the third is the point. Held: a course is
     * running and there is almost nothing to notice. Left alone: the ordinary weight of
     * it. And the stretch after a course runs out, when it is worse than either - which
     * is not a punishment for stopping so much as the reason stopping is a decision.
     */
    private static void mind(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (body.tickCourse(interval)) {
            body.startRebound(config.reboundSeconds * 20);
            Body.notify(player, Component.translatable("triage.msg.course_over"),
                    ChatFormatting.GRAY, null);
        }
        body.tickRebound(interval);
        if (body.medicated()) {
            // What it costs while it is working. Not a balance lever and not invented:
            // being slowed down and being hungry all the time are the two side effects
            // people actually report, and between them they are the commonest reason a
            // course gets stopped. The mod would be lying if taking it were free.
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    interval * 2, 0, true, false, false));
            player.causeFoodExhaustion(SEDATION_HUNGER);
            return;
        }
    }


    // ---- the chest --------------------------------------------------------

    /**
     * Air in the wrong place, getting worse for as long as it is left there.
     *
     * <p>Everything else in this mod does its damage at a flat rate. This one climbs,
     * because that is what pressure does, and it makes this the shortest deadline in the
     * mod - shorter than the whole infection chain. A player who cannot work out why
     * their chest is emptying has one thing left to try, which is to read the chart.
     */
    private static void chest(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        PartState state = body.part(BodyPart.THORAX);
        if (!state.pneumo) {
            return;
        }
        state.pneumoTicks += interval;
        float ramp = 1.0F + 2.0F * Math.min(1.0F, state.pneumoTicks / (float) (config.pneumoRampSeconds * 20));
        Injuries.apply(player, body, BodyPart.THORAX, config.pneumoDamage * ramp, false);
        body.setDirty(true);

        // Said out loud now and then. Nothing is bleeding and nothing is bent, so without
        // a word the player is just losing chest for no reason they can name.
        if (state.pneumoTicks % (20 * 12) < interval) {
            Body.notify(player, Component.translatable("triage.msg.pneumo_worse"),
                    ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
        }
    }

    // ---- circulation and cold ---------------------------------------------

    /**
     * Shock: arrives on how much blood is gone, leaves on rest.
     *
     * <p>The line it arrives on and the line it leaves on are different on purpose. One
     * line for both would have a body flickering in and out of shock every time the
     * number wobbled across it, and a state that switches itself on and off is a state
     * nobody can plan around.
     */
    private static void shock(ServerPlayer player, BodyHealth body, Config.Values config) {
        int blacked = 0;
        boolean open = false;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.losingBlood()) {
                open = true;
            }
            if (state.blacked()) {
                blacked++;
            }
        }

        if (!body.has(Disease.SHOCK)) {
            // Two ways in. Volume gone is the obvious one; enough of a body destroyed is
            // the other, and it needs no blood on the floor at all - which is the crush
            // injury, and the reason somebody can be grey and sliding with nothing
            // visibly leaking out of them.
            boolean crushed = config.shockBlackedParts > 0 && blacked >= config.shockBlackedParts;
            if ((body.bloodLoss() >= config.shockBloodLoss || crushed) && body.catchDisease(Disease.SHOCK)) {
                Body.notify(player, Component.translatable("triage.msg.caught", Disease.SHOCK.title()),
                        Disease.SHOCK.colour(), SoundEvents.WARDEN_HEARTBEAT);
            }
            return;
        }
        // Being on the floor counts as still whatever the numbers say. Somebody down is
        // not going anywhere, and lying down is precisely what the rest is modelling.
        boolean still = body.downed()
                || (!player.isSprinting() && player.onGround()
                    && player.getDeltaMovement().horizontalDistanceSqr() < 0.005);
        boolean crushed = config.shockBlackedParts > 0 && blacked >= config.shockBlackedParts;
        if (open || crushed || body.bloodLoss() >= config.shockClearBloodLoss || !still) {
            body.resetDiseaseTicks(Disease.SHOCK);
            return;
        }
        body.addDiseaseTicks(Disease.SHOCK, INTERVAL);
        if (body.diseaseTicks(Disease.SHOCK) >= config.shockRestSeconds * 20) {
            body.cure(Disease.SHOCK);
            Body.notify(player, Component.translatable("triage.msg.recovered", Disease.SHOCK.title()),
                    ChatFormatting.GREEN, null);
        }
    }

    /**
     * Hypothermia: arrives on exposure, much faster on somebody already bleeding, because
     * a body short of blood cannot keep itself warm. Leaves on being warm and dry, twice
     * as fast in the light of a fire.
     */
    private static void hypothermia(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        boolean exposed = player.isFullyFrozen() || player.isInPowderSnow
                || (player.isInWaterOrRain() && player.level().getBiome(player.blockPosition())
                        .value().getBaseTemperature() < COLD_BIOME);

        if (!body.has(Disease.HYPOTHERMIA)) {
            if (!exposed) {
                return;
            }
            float risk = config.hypothermiaRisk * (1.0F + 4.0F * body.bloodLoss());
            if (player.getRandom().nextFloat() < risk && body.catchDisease(Disease.HYPOTHERMIA)) {
                Body.notify(player, Component.translatable("triage.msg.caught", Disease.HYPOTHERMIA.title()),
                        Disease.HYPOTHERMIA.colour(), null);
            }
            return;
        }
        if (exposed) {
            body.resetDiseaseTicks(Disease.HYPOTHERMIA);
            return;
        }
        // Block light rather than a hunt for a campfire: one lookup, right about fires,
        // torches and lava, and standing in the light of something burning is what a
        // player is going to try anyway.
        boolean byFire = player.level().getBrightness(LightLayer.BLOCK, player.blockPosition()) >= 10;
        body.addDiseaseTicks(Disease.HYPOTHERMIA, byFire ? interval * 2 : interval);
        if (body.diseaseTicks(Disease.HYPOTHERMIA) >= config.hypothermiaWarmSeconds * 20) {
            body.cure(Disease.HYPOTHERMIA);
            Body.notify(player, Component.translatable("triage.msg.recovered", Disease.HYPOTHERMIA.title()),
                    ChatFormatting.GREEN, null);
        }
    }

    // ---- bones ------------------------------------------------------------

    /**
     * A crack knits by itself, but only if it is left alone. Sprinting on a cracked leg
     * both resets that count and risks turning it into a real break - which is the
     * whole bargain a crack offers: rest, or find out.
     */
    private static void cracks(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            splints(player, body, state, part, config, interval);
            if (!state.cracked) {
                continue;
            }
            // A splint is what the stick is for: the limb is held, so the bone is left
            // alone whatever its owner is doing. Without one, resting it is on them.
            if (part.isLeg() && player.isSprinting() && !state.splinted()) {
                state.crackTicks = 0;
                if (config.fractures && player.getRandom().nextFloat() < config.crackBreakChance * 0.1F
                        && state.fracture(part.bone().count())) {
                    body.setDirty(true);
                    Reactions.say(player, "break");
                    Body.notify(player, Component.translatable("triage.msg.fractured", part.title()),
                            ChatFormatting.GOLD, SoundEvents.BONE_BLOCK_BREAK);
                    // Going over on a leg that was already cracked is exactly how one
                    // ends up out of line, so it asks the same question every other
                    // break does.
                    Bones.broke(player, body, part, config.displacedBlow);
                }
                continue;
            }
            // Under a splint the clock is the splint's, not this one: the bone is done
            // when the stick says it is done, and taking it off early is how somebody
            // finds out that it was not.
            if (state.splinted()) {
                continue;
            }
            state.crackTicks += interval;
            if (state.crackTicks >= config.crackHealSeconds * 20) {
                boolean crooked = Bones.knitted(player, body, part);
                state.mend();
                body.setDirty(true);
                if (!crooked) {
                    Body.notify(player, Component.translatable("triage.msg.knitted", part.title()),
                            ChatFormatting.GRAY, null);
                }
            }
        }
    }

    /**
     * One second of a stick strapped to a limb.
     *
     * <p>The clock is not a timer, it is a bargain, and this is where the bargain is
     * charged. Standing, walking and working run it down. Running on it puts time back
     * on - every second of it - which is the honest version of what happens to a
     * fracture nobody will stay off: it does not send you back to the start, it simply
     * takes longer, and longer again the next time.
     *
     * <p>When it finally runs out the splint comes off by itself and the bone under it
     * is done. That is the only place in the mod where a fracture actually ends, which
     * is why the mark it leaves is recorded here and nowhere else.
     */
    private static void splints(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                Config.Values config, int interval) {
        if (!state.splinted()) {
            return;
        }
        if (!config.wornSplints || state.gone()) {
            state.unsplint();
            body.setDirty(true);
            return;
        }
        boolean running = player.isSprinting() && (part.isLeg() || part == BodyPart.THORAX);
        if (running && config.splintRunPenalty > 0) {
            int ceiling = config.splintCeilingSeconds * 20;
            int before = state.splintTicks;
            state.splintTicks = Math.min(ceiling, state.splintTicks + config.splintRunPenalty * interval);
            if (state.splintTicks > before && player.getRandom().nextFloat() < 0.05F) {
                // Said sometimes rather than every second. A number that grows in silence
                // is a bug as far as the player is concerned, and a line every second is
                // a nag - so it speaks up now and then, which is how a leg speaks up.
                Body.notify(player, Component.translatable("triage.msg.splint_longer",
                        part.title(), state.splintTicks / 20), ChatFormatting.GOLD, null);
            }
            return;
        }
        state.splintTicks = Math.max(0, state.splintTicks - interval);
        if (state.splintTicks > 0) {
            return;
        }
        state.unsplint();
        // Which mark it leaves is decided before the break is cleared, because the thing
        // that decides it is whether the ends were in line while it knitted - and that is
        // the whole argument for setting a bone before strapping a stick to it.
        boolean crooked = Bones.knitted(player, body, part);
        state.mend();
        body.setDirty(true);
        if (!crooked) {
            Body.notify(player, Component.translatable("triage.msg.splint_done", part.title()),
                    ChatFormatting.GREEN, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
        }
    }

    // ---- infection --------------------------------------------------------

    private static void infections(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            // Neither end of the amputation road goes bad. A limb that is not there has
            // nothing left in it to rot - and a wooden leg certainly has not - while one
            // that has already died has stopped being a wound at all, which is the whole
            // reason die() cleared it. Both of them read as blacked out, which is how
            // they were quietly getting infected: a part at zero health was the test.
            //
            // Cleared rather than skipped, so a body carrying one from before this stops
            // carrying it the next second rather than for good.
            if (state.gone() || state.necrotic()) {
                if (state.infected() || state.dirtyTicks > 0) {
                    state.disinfect();
                    body.setDirty(true);
                }
                continue;
            }
            if (state.infected()) {
                worsen(player, body, part, state, config, interval);
            } else {
                maybeInfect(player, body, part, state, config, interval);
            }
        }
    }

    /**
     * The clock on a dressing.
     *
     * <p>Two ends to it. It comes off when there is nothing left under it, because a
     * dressing on a healed arm is not a thing anybody keeps changing. And it goes bad
     * when it has been on too long, at which point the player is told - once, out loud -
     * because the whole point of this is that it is a thing you can forget and should
     * not be able to forget quietly.
     */
    private static void dressings(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.dressed) {
                continue;
            }
            if (!config.dressings || !state.worthDressing()) {
                state.undress();
                body.setDirty(true);
                continue;
            }
            int clean = config.dressingCleanSeconds * 20;
            int before = state.dressedTicks;
            state.dressedTicks += interval;
            // Deliberately not marking the body dirty every second. The only thing any
            // screen does with this number is compare it to one threshold, so the copy
            // the client has needs to be right at the moment it crosses and nowhere
            // else - and syncing a whole body once a second for everybody wearing a
            // bandage would be a packet a second for the commonest state in the mod.
            if (before < clean && state.dressedTicks >= clean) {
                body.setDirty(true);
                Body.notify(player, Component.translatable("triage.msg.dressing_soiled", part.title()),
                        ChatFormatting.DARK_GREEN, SoundEvents.SLIME_BLOCK_STEP);
            }
        }
    }

    /**
     * An open wound left open, a limb left blacked out, or a dressing left on: all three
     * are the same thing said three ways, which is a wound sitting in dirt.
     *
     * <p>The third is new and is the one that catches people. A clean dressing is the
     * best thing on this list - while it is fresh, nothing under it can go bad at all,
     * which is what a dressing is for. Left on past that, it is the dirt.
     */
    private static void maybeInfect(ServerPlayer player, BodyHealth body, BodyPart part, PartState state,
                                    Config.Values config, int interval) {
        boolean open = state.bleeding();
        boolean dead = state.blacked();
        // Scar tissue has less blood going through it than what it replaced, which is
        // exactly why the next one settles in the place the last one did.
        float history = Scars.infectionFactor(state) * Bones.infectionFactor(state);
        boolean soiled = config.dressings && state.soiled(config.dressingCleanSeconds * 20);
        // A dressing that is still clean is doing its job, and doing it on everything
        // underneath: this is the only thing in the mod that stops a wound going bad
        // rather than dealing with it once it has.
        if (state.dressed && !soiled) {
            state.dirtyTicks = 0;
            return;
        }
        if (!open && !dead && !soiled) {
            state.dirtyTicks = 0;
            return;
        }
        state.dirtyTicks += interval;

        int threshold = 20 * (soiled ? 0
                : open ? config.bleedSecondsBeforeRisk : config.blackedSecondsBeforeRisk);
        if (state.dirtyTicks < threshold) {
            return;
        }
        // Being ill already makes it easier to get worse, and so does lying about in
        // the wet with an open wound.
        float risk = config.infectionRisk
                * (body.has(Disease.COLD) ? 2.0F : 1.0F)
                * (player.isInWaterOrRain() ? 1.75F : 1.0F)
                * history;
        if (player.getRandom().nextFloat() >= risk) {
            return;
        }
        state.infect(PartState.INFECTION_MILD);
        // And this part is now a part that has had one. It will be the place again.
        Scars.wasInfected(body, part);
        body.setDirty(true);
        Milestones.onInfected(player);
        Body.notify(player, Component.translatable("triage.msg.infected", part.title()),
                ChatFormatting.DARK_GREEN, SoundEvents.SLIME_BLOCK_STEP);
    }

    private static void worsen(ServerPlayer player, BodyHealth body, BodyPart part, PartState state,
                               Config.Values config, int interval) {
        state.infectionTicks += interval;
        float damage = state.infection >= PartState.INFECTION_SEVERE
                ? config.infectionSevereDamage : config.infectionMildDamage;
        Injuries.apply(player, body, part, damage, false);

        if (state.infection == PartState.INFECTION_MILD) {
            if (state.infectionTicks >= config.infectionWorsenSeconds * 20) {
                state.infect(PartState.INFECTION_SEVERE);
                body.setDirty(true);
                Body.notify(player, Component.translatable("triage.msg.infection_worse", part.title()),
                        ChatFormatting.DARK_GREEN, SoundEvents.SLIME_BLOCK_STEP);
            }
            return;
        }
        // Severe and left alone: it stops being a wound and becomes an illness.
        if (state.infectionTicks >= config.infectionFeverSeconds * 20 && config.diseases
                && body.catchDisease(Disease.FEVER)) {
            state.infectionTicks = 0;
            Milestones.onFever(player);
            Reactions.say(player, "ill");
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.FEVER.title()),
                    Disease.FEVER.colour(), SoundEvents.PLAYER_HURT);
        }
    }

    // ---- illness ----------------------------------------------------------

    private static void catchCold(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (body.has(Disease.COLD) || body.has(Disease.FEVER) || body.has(Disease.SEPSIS)) {
            return;
        }
        boolean freezing = player.isFullyFrozen() || player.isInPowderSnow;
        boolean soaked = player.isInWaterOrRain()
                && player.level().getBiome(player.blockPosition()).value().getBaseTemperature() < COLD_BIOME;
        if (!freezing && !soaked) {
            return;
        }
        if (player.getRandom().nextFloat() < config.coldRisk && body.catchDisease(Disease.COLD)) {
            Reactions.say(player, "ill");
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.COLD.title()),
                    Disease.COLD.colour(), null);
        }
    }

    private static void diseases(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (Disease disease : Disease.VALUES) {
            if (!body.has(disease)) {
                continue;
            }
            // These two keep their own count, and it is not a count of how long they
            // have been running: it is a stretch of rest, or a stretch of being warm.
            // Advancing it here too would let a patient recover from shock by being in
            // shock for long enough.
            if (disease == Disease.SHOCK || disease == Disease.HYPOTHERMIA) {
                continue;
            }
            body.addDiseaseTicks(disease, interval);

            switch (disease) {
                case POISONING -> Injuries.apply(player, body, BodyPart.STOMACH, config.poisoningDamage, false);
                case SEPSIS -> Injuries.apply(player, body, BodyPart.THORAX, config.sepsisDamage, false);
                // Pneumonia sneezes too, and is the more worth avoiding of the two.
                case COLD, PNEUMONIA, FLU -> Sick.sneeze(player, body);
                // A gut that is emptying itself is a body losing what is in it faster
                // than it is putting any back. Fluid is the answer and it is the only one.
                case STOMACH -> {
                    body.addBloodLoss(config.stomachDrain);
                    Injuries.apply(player, body, BodyPart.STOMACH, config.stomachDamage, false);
                }
                // They eat first and they drink off the blood while they are at it. No
                // damage at all: a parasite does not wound anybody, it takes, and what it
                // takes is the two things this mod already measures.
                case PARASITES -> {
                    player.causeFoodExhaustion(config.parasiteHunger);
                    body.addBloodLoss(config.parasiteDrain);
                }
                // The wrong blood costs volume as well as health: what was hung is being
                // destroyed, so the transfusion runs backwards while this lasts.
                case REACTION -> {
                    Injuries.apply(player, body, BodyPart.THORAX, config.reactionDamage, false);
                    body.addBloodLoss(config.reactionBloodLoss);
                }
                // Rabies itself does nothing at all, which is the point of it. There is
                // no case here on purpose: a silent illness that quietly took a little
                // health would not be silent, it would be findable.
                case FRENZY -> {
                    // And this one is not a treatment problem any more. It only gets
                    // worse, and it does it from the head outwards.
                    float through = Math.min(1.0F,
                            body.diseaseTicks(Disease.FRENZY) / (float) Math.max(1, disease.duration()));
                    Injuries.apply(player, body, BodyPart.HEAD,
                            config.frenzyDamage * (0.4F + through), false);
                    if (player.getRandom().nextFloat() < 0.25F) {
                        Body.notify(player, Component.translatable("triage.msg.frenzy"),
                                ChatFormatting.DARK_RED, null);
                    }
                }
                default -> {
                }
            }

            if (disease.permanent() || body.diseaseTicks(disease) < disease.duration()) {
                continue;
            }
            body.cure(disease);
            Disease next = disease.then();
            if (next != null && body.catchDisease(next)) {
                Reactions.say(player, "ill");
                Body.notify(player, Component.translatable("triage.msg.caught", next.title()),
                        next.colour(), SoundEvents.WITHER_SPAWN);
            } else {
                Body.notify(player, Component.translatable("triage.msg.recovered", disease.title()),
                        ChatFormatting.GREEN, null);
            }
        }
    }

    /**
     * A cold is mostly harmless, and mostly a noise problem: it gives away where you
     * are. Rare enough not to be irritating, often enough to matter while hiding.
     */
    /** Used by the food hook: something eaten that deserved it. */
    public static void poison(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        // Some of what is eaten badly is not poison at all - it is something alive, and
        // it is the other thing a course cannot touch.
        if (config.diseases && player.getRandom().nextFloat() < config.stomachChance
                && !body.has(Disease.STOMACH) && body.catchDisease(Disease.STOMACH)) {
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.STOMACH.title()),
                    Disease.STOMACH.colour(), SoundEvents.PLAYER_BURP);
            return;
        }
        // And some of it was not poison and not alive in the way the stomach one is.
        // It was alive in the way that stays.
        if (config.diseases && player.getRandom().nextFloat() < config.parasiteChance
                && !body.has(Disease.PARASITES) && body.catchDisease(Disease.PARASITES)) {
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.PARASITES.title()),
                    Disease.PARASITES.colour(), SoundEvents.PLAYER_BURP);
            return;
        }
        if (body.catchDisease(Disease.POISONING)) {
            Body.notify(player, Component.translatable("triage.msg.caught", Disease.POISONING.title()),
                    Disease.POISONING.colour(), SoundEvents.PLAYER_BURP);
        }
    }

    /**
     * The heart answering for what came out of the limb.
     *
     * <p>Fast, and it does not get worse gradually: what this counts down to is an arrest,
     * and the only thing that stops it is fluid. Which is the same bag that fixes the cause
     * - so the answer to "they went grey the moment I took the strap off" is the thing that
     * should already have been hanging before it came off.
     */
    private static void crushing(ServerPlayer player, BodyHealth body, Config.Values config,
                                 int interval) {
        if (!body.has(Disease.CRUSH)) {
            return;
        }
        // Filled up afterwards still counts. Late is worse than early and better than not.
        if (body.bloodLoss() <= 0.05F) {
            body.cure(Disease.CRUSH);
            Body.notify(player, Component.translatable("triage.msg.crush_settled"),
                    ChatFormatting.GREEN, null);
            return;
        }
        // Scaled by how short they actually are, and stopped well above empty. What kills
        // here is the arrest at the end, not the drain on the way to it - and the drain
        // getting lighter as the bag goes in is the mod agreeing with the medic while the
        // treatment is still half done.
        PartState chest = body.part(BodyPart.THORAX);
        if (chest.ratio() > 0.25F) {
            Injuries.apply(player, body, BodyPart.THORAX, 0.45F * body.bloodLoss(), false);
        }
        if (body.diseaseTicks(Disease.CRUSH) >= config.crushSeconds * 20 && !body.arrested()
                && config.cpr && config.arrestSeconds > 0) {
            body.setArrestTicks(config.arrestSeconds * 20);
            Body.notify(player, Component.translatable("triage.msg.crush_arrest"),
                    ChatFormatting.DARK_RED, null);
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }
}
