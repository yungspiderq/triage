package com.claudemods.triage.event;

import com.claudemods.triage.body.Mending;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.Adrenaline;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.Ailments;
import com.claudemods.triage.body.Bedside;
import com.claudemods.triage.body.Breaths;
import com.claudemods.triage.body.Stuck;
import com.claudemods.triage.body.BloodEffects;
import com.claudemods.triage.body.Burns;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Carry;
import com.claudemods.triage.body.Chill;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Downed;
import com.claudemods.triage.body.Clots;
import com.claudemods.triage.body.Faint;
import com.claudemods.triage.body.Scars;
import com.claudemods.triage.body.Fatigue;
import com.claudemods.triage.body.Injuries;
import com.claudemods.triage.body.ModDamageTypes;
import com.claudemods.triage.body.Organ;
import com.claudemods.triage.body.Necrosis;
import com.claudemods.triage.body.PartEffects;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Sick;
import com.claudemods.triage.body.Tags;
import com.claudemods.triage.body.Vigil;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.med.Treatments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;

/**
 * Everything a body does to itself while nothing is hitting it: bleeding out, dragging
 * the thorax down with every limb that is gone, and hurting to walk on a broken leg.
 * Runs once a second, which is also the unit every rate in the config is written in.
 */
public final class BodyTicker {
    private static final int INTERVAL = 20;
    /** Share of blood gone at which the player starts being told they are going. */
    private static final float FAINT_WARNING = 0.9F;
    /** And the share at which they start hearing it. */
    private static final float HEARTBEAT_FROM = 0.6F;

    private BodyTicker() {
    }

    public static void onTick(ServerPlayer player) {
        if (player.tickCount % INTERVAL != 0) {
            return;
        }
        if (!Body.tracks(player)) {
            // Nobody should limp around in creative because of what happened in survival.
            PartEffects.clear(player);
            return;
        }
        if (player.isDeadOrDying()) {
            return;
        }
        BodyHealth body = Body.of(player);
        Config.Values config = Config.values();

        // Before anything that reads it: what the arms have been doing is a fact about
        // this second, and the treatments that ask are answered from it.
        Fatigue.tick(player, body, INTERVAL);
        Adrenaline.tick(player, body, INTERVAL);
        Sick.tick(player, body, INTERVAL);
        // Before the chill, because a burn is one of the things that makes somebody cold,
        // and after the illnesses, because breathing fire is how one of them is caught.
        Burns.tick(player, body, INTERVAL);
        Chill.tick(player, body, INTERVAL);
        Vigil.tick(player, body, INTERVAL);

        boolean bleeding = bleed(player, body, config);
        bleeding |= inside(player, body, config);
        blackedLimbs(player, body, config);
        fracturePain(player, body, config);
        blood(player, body, config, bleeding);
        Ailments.tick(player, body, config, INTERVAL);
        // Every step taken before somebody deals with it.
        Stuck.jostle(player, body, config);
        // And what the room can hear from where it is standing.
        Breaths.tick(player, body, config);
        // And what the room writes down while nobody is watching.
        Bedside.tick(player, body, config);
        Necrosis.tick(player, body, config, INTERVAL);
        Aid.tick(player, body, config, INTERVAL);
        // After everything that could have emptied them and before anything that reads
        // whether they are upright: a faint is the body's answer to the second that has
        // just happened, not to the one before it.
        Faint.tick(player, body, config, INTERVAL);
        // Before the faint reads whether they are upright and after everything that could
        // have emptied them: a clot is about the last minute of not moving, and the last
        // minute is what has just finished.
        Clots.tick(player, body, config, INTERVAL);
        Clots.embolism(player, body, config);
        // And the oldest thing on this list, which does nothing at all: what a healed
        // break has to say about the sky.
        Scars.ache(player, body, config, INTERVAL);

        if (body.tickUnder(INTERVAL)) {
            // Coming round is getting up, unless something else is holding them down.
            if (!body.downed() && body.canStand()) {
                Downed.stand(player);
            }
            Body.notify(player, Component.translatable("triage.msg.came_round"),
                    ChatFormatting.AQUA, SoundEvents.PLAYER_BREATH);
            body.setDirty(true);
        }
        if (body.tickPainkillers(INTERVAL)) {
            Body.notify(player, Component.translatable("triage.msg.painkiller_over"), ChatFormatting.GRAY, null);
            body.setDirty(true);
        }
        body.tickTolerance(INTERVAL, config.painkillerToleranceSeconds * 20);
        body.tickTag(INTERVAL, config.tagStaleSeconds * 20);
        heartbeat(player, body, config);

        // Nothing else deals a landing to a body on a physics engine, so this does.
        double fell = Ragdolls.landing(player);
        if (fell > 0.0) {
            Injuries.landed(player, body, fell);
        }

        PartEffects.apply(player, body);
        // Nothing to stand on, and no clock attached to it. Asked every second rather
        // than remembered, exactly like the downed pose: anything that takes somebody
        // off the floor - a physics mod, a carry ending - is undone within a second and
        // there is no second copy of this state to go stale.
        if (!body.downed() && (!body.canStand() || body.under())) {
            Downed.floor(player);
        }
        Carry.tick(player);
        // What was operated on, and whether they are leaving it alone.
        Mending.tick(player, body, INTERVAL);
        // Only actually sends anything when what the room can see has changed.
        Tags.refresh(player);
        Treatments.resendPatient(player);
        // And whether whoever is under those hands is still holding still.
        Treatments.panic(player);

        // Bleeding out, rotting from the inside and dying of your own wrecked limbs are
        // three different deaths, and the message should say which one it was.
        ResourceKey<DamageType> cause = Body.deathCause(body);
        if (body.downed()) {
            DownedEvents.shakeOffPursuers(player);
            ResourceKey<DamageType> fatal = Downed.tick(player, body, INTERVAL, cause);
            if (fatal != null && Body.killIfEmpty(player, fatal)) {
                return;
            }
            Body.mirror(player);
            Body.flush(player);
            return;
        }
        if (Body.killIfEmpty(player, cause)) {
            return;
        }
        if (bledOut(player, body, config)) {
            return;
        }
        Body.mirror(player);
        Body.flush(player);
    }

    /** Stains belong to the world rather than to anybody, so they dry on its clock. */
    public static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL == 0) {
            BloodEffects.tickStains(level);
        }
    }

    public static void onLevelUnload(ServerLevel level) {
        BloodEffects.forget(level);
    }

    /** @return true if anything was bleeding this second */
    private static boolean bleed(ServerPlayer player, BodyHealth body, Config.Values config) {
        boolean any = false;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);

            // A strapped limb is not losing blood; it is losing itself instead - and
            // itself is all it loses.
            //
            // Straight onto the part rather than through Injuries.apply, which routes
            // what a part cannot absorb into the chest. That is right for a blow and
            // wrong for a strap: a tourniquet does nothing whatever to a chest, and the
            // chest getting the bill for one was how three minutes under a strap became
            // unsurvivable. Nothing on a stump or a dead limb either - there is nothing
            // left in either of those to take.
            if (state.tourniquet) {
                if (!state.gone() && !state.necrotic() && config.tourniquetDamage > 0.0F) {
                    state.damage(config.tourniquetDamage);
                    body.setDirty(true);
                }
                continue;
            }
            if (!state.bleeding()) {
                continue;
            }
            any = true;
            boolean heavy = state.bleed >= PartState.BLEED_HEAVY;
            boolean artery = state.arterial();
            if (state.pressed()) {
                // Somebody has their hands on it. Nothing comes out while they are
                // there, and they are not there for long - but the clotting underneath
                // goes on counting, so a light bleed held long enough does close.
                state.pressedTicks = Math.max(0, state.pressedTicks - INTERVAL);
                body.setDirty(true);
            } else {
                Injuries.apply(player, body, part,
                        artery ? config.arterialRate
                                : heavy ? config.heavyBleedRate : config.lightBleedRate, false);
                body.addBloodLoss(artery ? config.bloodPerArterial
                        : heavy ? config.bloodPerHeavy : config.bloodPerLight);
            }

            // Cold blood does not set. While a body is hypothermic, a light cut that
            // would have closed on its own simply keeps running - which is how somebody
            // who was merely cold turns into somebody who is bleeding out.
            // Nor does a wound with something still in it. An arrow holds the channel
            // open, which is the whole reason it has to come out before anything else
            // can be done about the wound it made.
            if (!body.has(Disease.HYPOTHERMIA) && !state.stuck()
                    && state.bleed == PartState.BLEED_LIGHT && state.bleedTicks > 0) {
                state.bleedTicks -= INTERVAL;
                if (state.bleedTicks <= 0) {
                    state.stopBleeding();
                    body.setDirty(true);
                    Body.notify(player, Component.translatable("triage.msg.clotted", part.title()),
                            ChatFormatting.GRAY, null);
                }
            }
        }
        return any;
    }

    /**
     * What is happening inside, which is nothing anybody can see.
     *
     * <p>Counted as bleeding for everything that asks - so a drip is refused, and volume
     * does not come back - because it is bleeding. It is simply bleeding somewhere the
     * bandages cannot reach.
     *
     * @return true if anything inside anybody was open this second
     */
    private static boolean inside(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.organs) {
            return false;
        }
        boolean any = false;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.organHurt()) {
                continue;
            }
            // Every torn one at its own rate, which is the whole reason there are eight
            // of them: a spleen and a gut are both "bleeding inside" and one of them is
            // twice the hurry.
            float rate = 0.0F;
            for (Organ organ : Organ.in(part)) {
                if (state.organ(organ) < PartState.ORGAN_TORN) {
                    continue;
                }
                rate += organ.bleed();
                // What comes out of a gut is not blood, and the danger is three minutes
                // from now rather than now.
                // It used to roll straight for sepsis here, which took a body from a
                // wound to the worst thing in the mod in one step with nothing in
                // between. Peritonitis is that step: it says so, it runs hot, and it
                // leaves time to close the hole.
                if (organ.spills() && config.diseases && !body.has(Disease.SEPSIS)
                        && !body.has(Disease.PERITONITIS)
                        && player.getRandom().nextFloat() < config.organSepsis
                        && body.catchDisease(Disease.PERITONITIS)) {
                    Body.notify(player, Component.translatable("triage.msg.caught",
                            Disease.PERITONITIS.title()), Disease.PERITONITIS.colour(),
                            SoundEvents.WITHER_SPAWN);
                }
            }
            if (rate > 0.0F) {
                // Never clots and never eases. The only thing that ends this is a surgeon
                // finding it, which is the whole reason it is worth being afraid of.
                any = true;
                body.addBloodLoss(config.organBleed * rate);
                if (config.organDamage > 0.0F) {
                    state.damage(config.organDamage * rate);
                }
                body.setDirty(true);
            }
            // Bruised settles on its own, which is the difference between the two and
            // the reason both exist: one of them is a scare and one is a deadline.
            //
            // Its clock runs whether or not something else in the same part is torn. A
            // belly can hold a burst spleen and a bruised kidney at once, and the kidney
            // has no idea about the spleen: settling is what a bruise does, and stopping
            // it because of the neighbour would have quietly made "it settles by itself"
            // untrue exactly where somebody is most likely to be reading the chart.
            state.organTicks += INTERVAL;
            body.setDirty(true);
            if (state.organTicks >= config.organHealSeconds * 20 && state.organBruised()) {
                state.settleBruises();
                Body.notify(player, Component.translatable("triage.msg.organ_settled", part.title()),
                        ChatFormatting.GRAY, null);
            }
        }
        return any;
    }

    /**
     * Blood comes back on its own, slowly, and only while there is food to make it out
     * of. The only thing that hurries it is a drip, and a drip is refused while anything
     * is still open - so a bleed still has to be closed before it can be undone.
     */
    private static void blood(ServerPlayer player, BodyHealth body, Config.Values config, boolean bleeding) {
        if (bleeding) {
            BloodEffects.bleed(player, body);
            return;
        }
        if (body.bloodLoss() <= 0.0F) {
            return;
        }
        // A burn big enough to be losing fluid is an open wound the size of a limb, and
        // lying down does not answer one. This is the same rule a bleed gets, for the same
        // reason, and it is what makes a bag of saline the answer to a burn rather than a
        // nicety: there is nothing here to close, so waiting is not a plan.
        if (Burns.draining(body)) {
            return;
        }
        if (player.getFoodData().getFoodLevel() < config.bloodRecoveryHunger) {
            return;
        }
        // Blood is made by resting, not by walking it off. Sleeping is worth most,
        // standing still is worth something, and running is worth nothing extra.
        float rate = config.bloodRecovery;
        if (player.isSleeping()) {
            rate *= 4.0F;
        } else if (player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
            rate *= 2.0F;
        }
        // A body in shock is not moving blood well enough to be making more of it, and a
        // cold one is busy. Halved rather than stopped, each: fluids are the fast way out
        // of shock, but somebody with no drip and no bags still has a way, and it is to
        // lie down and wait.
        if (body.has(Disease.SHOCK)) {
            rate *= 0.5F;
        }
        if (body.has(Disease.HYPOTHERMIA)) {
            rate *= 0.5F;
        }
        // The half of that illness nobody puts in a game, because there is no scene in
        // it: not wanting to do anything, and a body that is looked after worse for it.
        // It is also the half that does the most damage in life, so it is the half that
        // costs the player something here.
        if (config.schizophrenia && body.has(Disease.SCHIZOPHRENIA) && !body.medicated()) {
            rate *= 0.5F;
        }
        body.addBloodLoss(-rate);
    }

    /**
     * Running out of blood entirely kills. Without this the word "bleeding" meant a
     * player who was permanently slow and dizzy and otherwise fine, which is not what
     * bleeding is - and it left the one injury with no floor under it.
     *
     * @return true when the killing blow was dealt
     */
    private static boolean bledOut(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.bloodLossKills) {
            return false;
        }
        if (body.bloodLoss() >= FAINT_WARNING && body.bloodLoss() < 1.0F) {
            Body.notify(player, Component.translatable("triage.msg.bleeding_out"), ChatFormatting.DARK_RED, null);
        }
        if (body.bloodLoss() < 1.0F || player.isDeadOrDying()) {
            return false;
        }
        player.hurt(ModDamageTypes.source(player.level(), ModDamageTypes.BLEEDING), Float.MAX_VALUE / 8.0F);
        return true;
    }

    /**
     * The one thing a body in trouble does that you hear rather than read.
     *
     * <p>Everything else this mod says arrives as text on a bar or as a colour in the
     * corner, both of which are easy to be too busy to look at. A heartbeat is not: it
     * arrives whether or not you were looking, it gets faster as it gets worse, and it
     * is the difference between knowing you are in trouble and reading that you are.
     *
     * <p>Only ever sent to the player it belongs to. Nobody else can hear yours.
     */
    private static void heartbeat(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.heartbeat) {
            return;
        }
        // Nothing to hear. A stopped heart is the one thing in the mod that announces
        // itself by going quiet, and filling that silence with a courtesy beat would
        // take away the only moment a player is told something without being told it.
        if (body.arrested()) {
            return;
        }
        // Ears blown out. The one warning in this mod that arrives whether or not you
        // were looking is exactly the one a blast takes away, and it takes it away from
        // the person who was closest to it. Nothing replaces it: the point of losing a
        // sense is that it is gone.
        if (body.has(Disease.DEAFNESS)) {
            return;
        }
        float loss = body.bloodLoss();
        if (!body.downed() && loss < HEARTBEAT_FROM) {
            return;
        }
        // Downed is as bad as it gets; short of that the pitch climbs with what is gone.
        float severity = body.downed()
                ? 1.0F
                : Mth.clamp((loss - HEARTBEAT_FROM) / (1.0F - HEARTBEAT_FROM), 0.0F, 1.0F);
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                0.5F + 0.3F * severity, 0.8F + 0.5F * severity);
    }

    /** Every limb that is gone keeps costing the thorax, until something is done about it. */
    private static void blackedLimbs(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (config.blackedLimbDrain <= 0.0F) {
            return;
        }
        int blacked = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            // Neither of the two ends of that road counts here. A limb that has died is
            // charged for by Necrosis at its own much heavier rate, and one that has
            // come off is charged for by nobody - which is the entire point of the saw
            // and would be undone by quietly billing the chest for it anyway.
            if (!part.vital() && state.blacked() && !state.necrotic() && !state.gone()) {
                blacked++;
            }
        }
        if (blacked > 0) {
            Injuries.apply(player, body, BodyPart.THORAX, blacked * config.blackedLimbDrain, false);
        }
    }

    /** Running on a broken leg is possible. It is just a bad idea. */
    private static void fracturePain(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.fractures || config.fracturePain <= 0.0F || !player.isSprinting()) {
            return;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (!part.isLeg()) {
                continue;
            }
            PartState state = body.part(part);
            if (state.fractured() && !state.blacked()) {
                Injuries.apply(player, body, part, config.fracturePain, false);
            }
        }
    }
}
