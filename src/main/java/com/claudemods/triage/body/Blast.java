package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * What an explosion does to a person, which is four different things at once.
 *
 * <p>Until now this mod treated a blast as a big fire: damage smeared over the whole body,
 * nothing broken, nothing bleeding, nothing to treat. That is the one injury pattern
 * trauma medicine has a whole vocabulary for, and the mod had none of it.
 *
 * <p>Blast injury comes in four kinds and they arrive in this order:
 *
 * <ul>
 *   <li><b>The wave.</b> Pressure, moving faster than sound, and it does nothing at all to
 *       muscle. What it destroys is anything with gas in it - lungs first, then bowel. It
 *       leaves <b>no mark whatsoever</b>. Somebody walks away from a blast talking, and
 *       drowns in their own chest twenty minutes later.</li>
 *   <li><b>The fragments.</b> Everything the blast picked up, arriving as a shotgun.
 *       Several wounds at once, in whatever was facing it.</li>
 *   <li><b>The throw.</b> The body itself becomes the thing that hits something. The mod
 *       already breaks legs on landings, so this one needed nothing new.</li>
 *   <li><b>The heat.</b></li>
 * </ul>
 *
 * <p>And the rule that makes it worth building: <b>armour stops fragments and does nothing
 * for the wave.</b> A plated player at the centre of a blast walks out of it with no wound
 * on them anywhere and two torn lungs - which is precisely the casualty who is hardest to
 * find, precisely the reason the breathing rate exists, and precisely what happens.
 */
public final class Blast {
    /** Past this share of the radius, the wave is not worth modelling. */
    private static final float WAVE_REACH = 0.55F;
    /** And past this, nothing comes off anybody. Much tighter than the wave. */
    private static final float TEAR_REACH = 0.82F;
    /** How many fragments an unshielded body at the centre catches, at most. */
    private static final int MAX_FRAGMENTS = 5;

    private Blast() {
    }

    /**
     * Catches somebody in a blast.
     *
     * @param exposure 1 at the centre, 0 at the edge of the radius.
     */
    public static void caught(ServerPlayer player, BodyHealth body, float exposure) {
        Config.Values config = Config.values();
        if (!config.blast || exposure <= 0.0F) {
            return;
        }
        RandomSource random = player.getRandom();
        float share = Mth.clamp(exposure, 0.0F, 1.0F);

        // Before anything else, because if a limb has gone the rest of this is happening
        // to a body with one fewer piece and the fragments should not be landing in it.
        boolean anything = tearOff(player, body, config, random, share);
        anything |= fragments(player, body, config, random, share);
        anything |= wave(player, body, config, random, share);
        anything |= heat(player, body, config, random, share);

        if (anything) {
            body.setDirty(true);
        }
    }

    /**
     * A limb taken off by the blast itself.
     *
     * <p>The worst thing in this mod and now the only one that can happen to somebody
     * without anybody deciding it. Everything else that ends with a stump is a decision:
     * a strap somebody left on, a saw somebody picked up. This is a distance.
     *
     * <p>Which is why it is kept to the middle of a blast and kept unlikely there. A rule
     * that takes an arm off at the edge of a radius would make explosions unplayable; one
     * that takes an arm off at the centre makes standing next to a charge a thing people
     * do once.
     *
     * <p>And armour does not stop it, for the same reason it does not stop the wave: at
     * that distance the question is not whether something got through the plate.
     *
     * <p>What is left is worse than what a saw leaves. A cut is straight and a surgeon
     * chose where; this is a tear, there is no strap above it because nobody knew it was
     * coming, and it is the one wound in the mod that arrives already emptying somebody.
     */
    private static boolean tearOff(ServerPlayer player, BodyHealth body, Config.Values config,
                                   RandomSource random, float share) {
        if (!config.blastTearOff || !config.amputation || share < TEAR_REACH) {
            return false;
        }
        float force = (share - TEAR_REACH) / (1.0F - TEAR_REACH);
        if (random.nextFloat() >= force * config.blastTearChance) {
            return false;
        }
        // Only ever one, and only ever something a body can be short of. Taking two off
        // a person in one go is not a rarer version of this, it is a different game.
        BodyPart[] limbs = {BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM,
                BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
        BodyPart taken = null;
        for (int tries = 0; tries < 8 && taken == null; tries++) {
            BodyPart candidate = limbs[random.nextInt(limbs.length)];
            if (!body.part(candidate).gone()) {
                taken = candidate;
            }
        }
        if (taken == null) {
            return false;
        }
        torn(player, body, taken, config);
        return true;
    }

    /**
     * What a torn-off limb leaves behind, wherever it was torn off by.
     *
     * <p>Shared with the blow that takes a dead limb off, because the stump is the same
     * stump: ragged, open, and nobody had a strap ready.
     */
    public static void torn(ServerPlayer player, BodyHealth body, BodyPart part, Config.Values config) {
        PartState state = body.part(part);
        boolean wasDead = state.necrotic();
        state.amputate();
        state.startBleeding(PartState.BLEED_HEAVY, 0);
        body.addBloodLoss(config.amputationBloodLoss + config.amputationOpenBloodLoss);
        if (config.shock) {
            body.catchDisease(Disease.SHOCK);
        }
        body.setDirty(true);
        // And it lands next to them. This is the way limbs come off that leaves something
        // to pick up: nobody chose it, nobody had a strap ready, and the arm is right
        // there - which is the only reason a clock on it means anything.
        Severed.drop(player, part, !wasDead);
        Reactions.say(player, "lost");
        Body.notify(player, Component.translatable("triage.msg.torn_off", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    /**
     * The fragments, and the one thing plate is actually for.
     *
     * <p>Armour is counted here and nowhere else in this class. That is the whole point:
     * a breastplate is a very good answer to a piece of metal travelling at somebody and
     * no answer at all to the air around it.
     */
    private static boolean fragments(ServerPlayer player, BodyHealth body, Config.Values config,
                                     RandomSource random, float share) {
        if (!config.blastFragments || !config.arrows) {
            return false;
        }
        // Twenty points of armour is full diamond, and stops nearly all of it.
        float armour = Mth.clamp(player.getArmorValue() / 20.0F, 0.0F, 1.0F);
        float caught = share * (1.0F - armour * config.blastArmourStop);
        int count = 0;
        for (int i = 0; i < MAX_FRAGMENTS; i++) {
            if (random.nextFloat() < caught) {
                count++;
            }
        }
        if (count <= 0) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            BodyPart part = DamageRouting.roll(random);
            PartState state = body.part(part);
            if (state.gone()) {
                continue;
            }
            state.arrows++;
            // So the chart can call it what it is. An arrowhead and a piece of casing are
            // the same problem and the same answer, and they are not the same story.
            state.shrapnel = true;
            // Small holes, several of them. The danger of a fragment is rarely the hole.
            if (random.nextFloat() < 0.7F) {
                state.startBleeding(PartState.BLEED_LIGHT,
                        config.lightBleedSeconds <= 0 ? PartState.BLEED_FOREVER
                                : config.lightBleedSeconds * 20);
            }
        }
        Body.notify(player, Component.translatable("triage.msg.blast_fragments", count),
                ChatFormatting.RED, SoundEvents.ARROW_HIT);
        return true;
    }

    /**
     * The pressure wave, which armour does not touch.
     *
     * <p>Nothing appears on the figure and nothing bleeds anywhere anybody can see. The
     * only sign is a chest that is working too hard, which is exactly the reading this
     * mod added a breathing rate for.
     */
    private static boolean wave(ServerPlayer player, BodyHealth body, Config.Values config,
                                RandomSource random, float share) {
        if (!config.blastWave || share < WAVE_REACH) {
            return false;
        }
        // Rescaled so the wave goes from nothing at its reach to certain at the centre.
        float force = (share - WAVE_REACH) / (1.0F - WAVE_REACH);
        boolean any = false;

        if (config.organs) {
            PartState chest = body.part(BodyPart.THORAX);
            // Both lungs are in the same box as the blast. Taking them one at a time
            // rather than together, because one torn lung and two are different
            // emergencies and the difference should be luck rather than a rule.
            for (Organ lung : new Organ[]{Organ.LEFT_LUNG, Organ.RIGHT_LUNG}) {
                if (random.nextFloat() < force * config.blastLungChance) {
                    chest.tearOrgan(lung, PartState.ORGAN_TORN);
                    if (config.pneumothorax) {
                        chest.pneumo = true;
                    }
                    any = true;
                }
            }
            // And the other gas-filled thing, which is the one that kills slowly.
            if (random.nextFloat() < force * config.blastGutChance) {
                body.part(BodyPart.STOMACH).tearOrgan(Organ.GUT, PartState.ORGAN_TORN);
                any = true;
            }
        }
        // A blast rattles a head without touching it, which is the same thing the mod
        // already models for a blow - so it uses the same chance rather than a new one.
        if (random.nextFloat() < force * config.concussionChance
                && body.catchDisease(Disease.CONCUSSION)) {
            any = true;
        }
        // And the ears, which is the injury everybody near a blast has. Nearly certain
        // rather than a roll: this one is not luck, it is what being close to one is.
        if (config.deafnessSeconds > 0 && body.catchDisease(Disease.DEAFNESS)) {
            Body.notify(player, Component.translatable("triage.msg.deafened"),
                    ChatFormatting.GRAY, null);
            any = true;
        }
        if (any) {
            // Said to them and to nobody else, because there is nothing to see. This is
            // the only line they will get: what it turns into is a chest that will not
            // settle, and finding that is somebody's job.
            Body.notify(player, Component.translatable("triage.msg.blast_wave"),
                    ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        }
        return any;
    }

    /** And the heat, which is the least interesting quarter of it. */
    private static boolean heat(ServerPlayer player, BodyHealth body, Config.Values config,
                                RandomSource random, float share) {
        if (!config.burns || random.nextFloat() >= share * config.blastBurnChance) {
            return false;
        }
        // Same as any other burn from here: shallow, and then it is up to them. A
        // blast is a worse way to get one only because of everything else it does at the
        // same time, which is the honest difference.
        return Burns.caught(player, body, DamageRouting.roll(random));
    }

    /** Whether this body is wearing anything at all, for the message about what saved them. */
    public static boolean plated(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR
                    && !player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
