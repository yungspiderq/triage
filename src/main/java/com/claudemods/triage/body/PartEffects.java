package com.claudemods.triage.body;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.Config;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What a damaged body does to the player wearing it.
 *
 * <p>Almost all of it is attribute modifiers rather than potion effects: they stack
 * cleanly with everything else, they scale smoothly with how bad the damage is, and
 * they do not fill the corner of the screen with icons. Painkillers switch the whole
 * thing off for a while without healing anything - the damage is still there, it just
 * stops being felt.
 */
public final class PartEffects {
    /** Above this share of health a part is only scratched and costs nothing. */
    private static final float DEADZONE = 0.7F;
    /** A broken bone hurts at least this much, however much health the part has left. */
    private static final float FRACTURE_FLOOR = 0.5F;

    /**
     * Ceilings on the penalties that stack. Everything that slows a player down shares
     * one modifier, so no combination of wounds and illness can add up to standing
     * still - being crippled has to stay survivable to be worth playing.
     */
    private static final double MAX_SLOW = 0.6;
    private static final double MAX_WEAKNESS = 0.5;
    /** Ceiling on everything that slows a player at once, injuries and burdens together. */
    private static final double MAX_TOTAL_SLOW = 0.85;

    /** Shares of blood gone at which the world starts to swim, and then to go dark. */
    private static final float HEAVY_BLOOD_LOSS = 0.5F;
    private static final float CRITICAL_BLOOD_LOSS = 0.75F;

    private static final ResourceLocation SLOW = TriageMod.id("slow");
    private static final ResourceLocation WEAKNESS = TriageMod.id("weakness");
    private static final ResourceLocation ARM_BREAK_SPEED = TriageMod.id("arm_break_speed");
    private static final ResourceLocation ARM_ATTACK_SPEED = TriageMod.id("arm_attack_speed");
    private static final ResourceLocation LEG_JUMP = TriageMod.id("leg_jump");
    private static final ResourceLocation CARRYING = TriageMod.id("carrying");

    /** How much a part that is wrong in some other way still counts as hurt. */
    private static final float CRACK_FLOOR = 0.25F;
    /**
     * What a burn costs to use, by depth, and the reason the deepest costs nothing.
     *
     * <p>A full thickness burn does not hurt. The nerve endings that would have done the
     * hurting are part of what burnt, and that is not a piece of trivia - it means the
     * worst burn on a body is invisible to a player who is going by what hurts. Writing
     * it in the text and then charging pain for it anyway would have been the mod saying
     * one thing and doing another.
     */
    private static final float BURN_FLOOR = 0.4F;
    private static final float LIGHT_BURN_FLOOR = 0.2F;
    private static final float INFECTION_FLOOR = 0.3F;
    private static final float SEVERE_INFECTION_FLOOR = 0.55F;

    private PartEffects() {
    }

    /** Called once a second per player, and whenever the body changes shape. */
    public static void apply(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        boolean suppressed = !config.effects || body.painkilled();
        boolean illSuppressed = suppressed || !config.diseases;

        // What is not being felt this second, or - during the crash - what is being felt
        // twice over. Nothing here if the painkillers already took the lot.
        float relief = suppressed ? 0.0F : Adrenaline.relief(body);
        float arms = suppressed ? 0.0F : felt(body, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, relief);
        float legs = suppressed ? 0.0F : felt(body, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, relief);
        float head = suppressed ? 0.0F : felt(body, BodyPart.HEAD, BodyPart.HEAD, relief);
        float stomach = suppressed ? 0.0F : felt(body, BodyPart.STOMACH, BodyPart.STOMACH, relief);

        // Slowness comes from three places at once, and unclamped they add up past
        // -100%, which pins the player in place with no way out of whatever is hitting
        // them. Capped, a body in its worst state is still a body that can walk away.
        // Blood loss is felt whether or not the wound that caused it still hurts, so
        // painkillers do not hide it: there is nothing to numb, the blood is simply gone.
        double bled = config.effects ? 0.35 * bloodSeverity(body) : 0.0;

        // Shock and cold are not pain either, so painkillers do not touch them: there is
        // nothing to numb. Circulation has stopped reaching the far end of things and the
        // body has stopped being warm, and neither is an opinion the nerves are holding.
        double failing = config.effects ? failingBody(body) : 0.0;

        // A stiff leg and a splinted one are the same cost for opposite reasons: one
        // will not bend and the other is not allowed to. Neither is pain, so neither is
        // hidden by painkillers - there is nothing in either of them to numb.
        double held = config.effects ? Scars.legsLost(body) : 0.0;
        double slow = Math.min(MAX_SLOW,
                0.55 * legs + 0.15 * head + illnessSlow(body, illSuppressed) + bled + failing + held);
        double weakness = Math.min(MAX_WEAKNESS,
                0.4 * arms + illnessWeakness(body, illSuppressed) + bled * 0.6 + failing * 0.8);

        // Nothing about being on the floor is negotiable: no walking, no swinging. Nor
        // is anything about not being in the body at all - which is not the same state
        // and has no clock on it, but has exactly the same answer to "can they move".
        if (body.downed() || body.under() || body.fainted()) {
            slow = 1.0;
            weakness = 1.0;
        } else if (!body.canStand()) {
            // Not the same thing at all. Somebody with nothing to stand on can still
            // drag themselves, and their hands work perfectly well - a legless surgeon
            // is still a surgeon. What they cannot do is walk, and they cannot jump at
            // all, which is the difference between slow and on the ground.
            slow = MAX_TOTAL_SLOW;
        }
        toggle(player, Attributes.MOVEMENT_SPEED, SLOW, -slow);
        toggle(player, Attributes.ATTACK_DAMAGE, WEAKNESS, -weakness);
        toggle(player, Attributes.BLOCK_BREAK_SPEED, ARM_BREAK_SPEED, -0.6 * arms);
        toggle(player, Attributes.ATTACK_SPEED, ARM_ATTACK_SPEED, -0.3 * arms);
        // Nothing to push off with, so nothing to push off with. The one thing on this
        // list that goes to zero rather than down: a body with no legs does not jump
        // badly, it does not jump.
        toggle(player, Attributes.JUMP_STRENGTH, LEG_JUMP,
                !body.downed() && !body.canStand() ? -1.0 : -0.5 * legs);

        // Carrying is its own modifier rather than more of the one above, because it is
        // not an injury and must not be capped away by them: a healthy medic and a
        // half-wrecked one should both find a body heavy.
        //
        // The two of them together still need a ceiling. Both are multipliers off the
        // same base, and a wounded medic at the injury cap who then picks somebody up
        // adds past -100%, which does not mean slow - it means pinned in place with
        // whatever is hitting them. Capped, the worst case is a crawl.
        // And a body gets heavier the longer it is carried, which is the only part of
        // this the carrier feels while they are still walking. The ceiling is the same
        // one: tired or not, the worst case has to remain a crawl rather than a stop.
        double load = config.carrySlow * (1.0F + config.fatigueCarrySlow * Fatigue.of(body));
        double carrying = Carry.carrying(player) != null
                ? Mth.clamp(MAX_TOTAL_SLOW - slow, 0.0, load)
                : 0.0;
        toggle(player, Attributes.MOVEMENT_SPEED, CARRYING, -carrying);

        // Under: the lights are off. Not a penalty - it is the thing that was paid for,
        // and it is the only state in the mod a patient should be glad to be in.
        if (body.under()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, false));
        }

        // A head this far gone stops being able to tell where it is - which the
        // client now draws for itself, in a way that is about a head rather than about
        // a bottle of nausea. Nothing is added here on purpose. See Symptoms.

        // A wrecked stomach burns through food, which is the long way round to the same
        // place: no food, no natural healing.
        if (stomach > 0.0F) {
            player.causeFoodExhaustion(0.6F * stomach);
        }

        // Losing enough of it greys the world out before it does anything else, and
        // so does shock, which is the same thing arriving by a different road. Both are
        // drawn rather than dosed now: the edges close in, the colour goes, and the
        // whole picture moves on the beat, which is a thing a potion cannot do.
        //
        // The weakness stays. That one is not a symptom, it is an arm that has nothing
        // to run on, and it belongs to the body rather than to the view.
        if (config.effects && body.bloodLoss() >= CRITICAL_BLOOD_LOSS) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
        }
        // Cold hands. Not a punishment for being cold - it is the thing being cold does.
        if (config.effects && body.has(Disease.HYPOTHERMIA)) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, true, false, false));
        }

        // A concussion is the one thing painkillers do not touch. There is no pain in it
        // to numb - the head is rattled, not hurt - and numbing it would hide the only
        // thing it does. What it does is drawn as well: the horizon stops being level
        // and will not quite come back.

        illness(player, body, illSuppressed);
    }

    /** What a body that has stopped keeping itself running costs, on top of the wounds. */
    private static double failingBody(BodyHealth body) {
        double amount = 0.0;
        if (body.has(Disease.SHOCK)) {
            amount += 0.30;
        }
        if (body.has(Disease.HYPOTHERMIA)) {
            amount += 0.15;
        }
        return amount;
    }

    /**
     * How much the missing blood is felt. Nothing at all until a quarter of it is gone,
     * then straight up - the first cut should not slow anybody down.
     */
    private static float bloodSeverity(BodyHealth body) {
        return Mth.clamp((body.bloodLoss() - 0.25F) / 0.75F, 0.0F, 1.0F);
    }

    private static double illnessSlow(BodyHealth body, boolean suppressed) {
        if (suppressed) {
            return 0.0;
        }
        double slow = 0.0;
        if (body.has(Disease.COLD)) {
            slow += 0.08;
        }
        if (body.has(Disease.POISONING)) {
            slow += 0.10;
        }
        if (body.has(Disease.FEVER)) {
            slow += 0.20;
        }
        if (body.has(Disease.SEPSIS)) {
            slow += 0.30;
        }
        if (body.has(Disease.REACTION)) {
            slow += 0.25;
        }
        return slow;
    }

    private static double illnessWeakness(BodyHealth body, boolean suppressed) {
        if (suppressed) {
            return 0.0;
        }
        double weakness = 0.0;
        if (body.has(Disease.FEVER)) {
            weakness += 0.20;
        }
        if (body.has(Disease.SEPSIS)) {
            weakness += 0.30;
        }
        if (body.has(Disease.REACTION)) {
            weakness += 0.25;
        }
        return weakness;
    }

    /**
     * What being ill costs beyond the attributes above: the effects that have no number
     * to add to, and the hunger it burns.
     */
    private static void illness(ServerPlayer player, BodyHealth body, boolean suppressed) {
        if (suppressed) {
            return;
        }
        if (body.has(Disease.FEVER)) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false, false));
            player.causeFoodExhaustion(0.5F);
        }
        if (body.has(Disease.POISONING)) {
            player.causeFoodExhaustion(0.8F);
        }
        if (body.has(Disease.SEPSIS)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, false));
        }
        if (body.has(Disease.REACTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
            player.causeFoodExhaustion(0.6F);
        }
    }

    /** Removes everything this class applied. Used when a body is wiped or reset. */
    public static void clear(ServerPlayer player) {
        toggle(player, Attributes.MOVEMENT_SPEED, SLOW, 0.0);
        toggle(player, Attributes.ATTACK_DAMAGE, WEAKNESS, 0.0);
        toggle(player, Attributes.BLOCK_BREAK_SPEED, ARM_BREAK_SPEED, 0.0);
        toggle(player, Attributes.ATTACK_SPEED, ARM_ATTACK_SPEED, 0.0);
        toggle(player, Attributes.JUMP_STRENGTH, LEG_JUMP, 0.0);
        toggle(player, Attributes.MOVEMENT_SPEED, CARRYING, 0.0);
    }

    /**
     * How badly a pair of parts is holding the player back: 0 while either one is above
     * the deadzone, 1 when the worse of them is gone. Pass the same part twice for the
     * ones there is only one of.
     */
    /**
     * How far the arms are from working, 0 to 1.
     *
     * <p>Wanted by anything that asks them to lift something, which since this existed
     * means carrying as well as swinging.
     */
    public static float arms(BodyHealth body) {
        return severity(body, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
    }

    /**
     * What this pair of parts actually costs right now, pain included or excluded.
     *
     * <p>Relief above zero is adrenaline and takes a share of it away; below zero is the
     * crash afterwards and adds one on. Either way it stops at the structural floor,
     * because the whole of the rule is that the chemistry hides pain and does not mend
     * bone: a leg that is not there is not there, however little it hurts.
     */
    private static float felt(BodyHealth body, BodyPart first, BodyPart second, float relief) {
        float full = severity(body, first, second);
        if (relief == 0.0F) {
            return full;
        }
        if (relief < 0.0F) {
            return Mth.clamp(full * (1.0F - relief), 0.0F, 1.0F);
        }
        return Math.max(structural(body, first, second), full * (1.0F - relief));
    }

    /**
     * The part of it no amount of not-caring gets you past.
     *
     * <p>Gone, dead on the body, or beaten past working at all. A fitted prosthetic still
     * counts here and is still worth what it is worth - a wooden leg is a mechanical
     * limit, not a painful one.
     */
    private static float structural(BodyHealth body, BodyPart first, BodyPart second) {
        float floor = 0.0F;
        for (BodyPart part : new BodyPart[]{first, second}) {
            PartState state = body.part(part);
            if (state.gone() || state.blacked() || state.necrotic()) {
                floor = Math.max(floor,
                        Mth.clamp((DEADZONE - working(state)) / DEADZONE, 0.0F, 1.0F));
            }
        }
        return floor;
    }

    private static float severity(BodyHealth body, BodyPart first, BodyPart second) {
        float worst = Math.min(working(body.part(first)), working(body.part(second)));
        float value = Mth.clamp((DEADZONE - worst) / DEADZONE, 0.0F, 1.0F);
        return Math.max(floor(body.part(first)), Math.max(floor(body.part(second)), value));
    }

    /**
     * How much of a limb is actually working.
     *
     * <p>The same as its health everywhere except one place. A part that is not there has
     * no health and never will, so left alone the arithmetic would say a stump and a
     * wooden leg are the same thing - and the wooden leg is the only reason anybody would
     * ever choose to have a stump. So a fitted one counts as a fixed fraction of a limb:
     * not a leg, and not nothing.
     */
    private static float working(PartState state) {
        if (!state.prosthetic) {
            return state.ratio();
        }
        Config.Values config = Config.values();
        float fresh = state.fineProsthetic ? config.prostheticRatioFine : config.prostheticRatio;
        // And what it is worth today rather than on the day it was fitted. A worn one is
        // not a broken one - it never falls off - it is simply less of a leg.
        float worn = state.wornShare(config.prostheticWearSeconds * 20);
        return fresh * Mth.lerp(worn, 1.0F, config.prostheticWornRatio);
    }

    /**
     * The least a part can cost you while something is wrong with it, however much
     * health it still has. A broken bone on a full-health limb still hurts to use.
     */
    private static float floor(PartState state) {
        float floor = 0.0F;
        if (state.fractured()) {
            floor = Math.max(floor, FRACTURE_FLOOR);
        }
        if (state.cracked) {
            floor = Math.max(floor, CRACK_FLOOR);
        }
        if (state.burn == PartState.BURN_SUPERFICIAL) {
            floor = Math.max(floor, LIGHT_BURN_FLOOR);
        } else if (state.burn == PartState.BURN_PARTIAL) {
            // Blistered is the most painful of the three, and it is the middle one.
            floor = Math.max(floor, BURN_FLOOR);
        }
        if (state.infected()) {
            floor = Math.max(floor, state.infection >= PartState.INFECTION_SEVERE
                    ? SEVERE_INFECTION_FLOOR : INFECTION_FLOOR);
        }
        return floor;
    }

    /** Adds the modifier at {@code amount}, or removes it when the amount is zero. */
    private static void toggle(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
