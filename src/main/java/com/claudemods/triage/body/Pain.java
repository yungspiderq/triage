package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Out of ten.
 *
 * <p>It is the first question asked of anybody in pain anywhere in the world, it is asked
 * again every time somebody walks past them, and this mod did not have it. What the mod had
 * was <em>where</em> - a patient points at a part - and where is the question you ask
 * second.
 *
 * <p>Which makes this a third kind of reading, and the mod now has one of each. A pulse is a
 * <b>fact about a body</b>, taken off it with two fingers. A temperature is a <b>figure from
 * an instrument</b>, and it costs the instrument. A pain score is neither: it is an
 * <b>opinion</b>, it comes out of somebody's mouth, and it is the only number in here that
 * cannot be checked against anything.
 *
 * <p>So it is wrong in more ways than anything else here, and every one of those ways is
 * already in the mod:
 *
 * <ul>
 *   <li>Pills and morphine take it down without touching what is wrong.</li>
 *   <li>Adrenaline takes it down and hands the bill over twenty seconds later.</li>
 *   <li>A head that has been hit does not rank things, and neither does a mind that is
 *       somewhere else - both answer with a number that wanders.</li>
 *   <li>Shock makes people quiet. The sickest patient in the clearing is often the one
 *       complaining least, and that is not stoicism, it is a body shutting down.</li>
 *   <li>A burn through the skin is silent, because the nerves that would have reported it
 *       are part of what burnt.</li>
 * </ul>
 *
 * <p><b>And that is the whole reason to ask.</b> A two out of ten on a heart running at a
 * hundred and thirty is not a comfortable patient - it is a patient somebody has already
 * given something to, or one who is about to find out what adrenaline costs. The number on
 * its own says little. The number beside the pulse is the cheapest way in this mod to catch
 * what another medic did before you arrived and never mentioned.
 */
public final class Pain {
    /** The scale everybody uses, and the only one anybody would recognise. */
    public static final int WORST = 10;

    /** What a dose takes off what they say, not off what is wrong. */
    private static final float PILLS = 3.0F;
    private static final float MORPHINE = 5.5F;
    /** And what not feeling it yet is worth, which is most of it. */
    private static final float SURGE = 6.0F;
    /** A body shutting down stops complaining. This is how much it stops. */
    private static final float SHOCK = 2.5F;

    private Pain() {
    }

    /**
     * What is actually wrong, on the same ten.
     *
     * <p>Built from the parts rather than from the health bar, because a body at half
     * health with one broken femur and a body at half health with seven bruises are not
     * the same afternoon. The worst part carries it and the rest add less and less, which
     * is how pain works: a second broken bone does not double anything.
     */
    public static float burden(BodyHealth body) {
        float worst = 0.0F;
        float rest = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            float score = of(body.part(part));
            if (score > worst) {
                rest += worst;
                worst = score;
            } else {
                rest += score;
            }
        }
        // Diminishing, and deliberately steep: a man with one thing badly wrong and a man
        // with four things badly wrong both say ten, and both mean it.
        return Mth.clamp(worst + rest * 0.25F, 0.0F, WORST);
    }

    /**
     * One part, on the ten.
     *
     * <p>Kept to the same shape {@link Complaint} uses for the same reason it does - the
     * two readings should never disagree about which limb is the loud one - but scaled to
     * the ten everybody says out loud rather than to the numbers that rank limbs.
     */
    private static float of(PartState state) {
        if (state.gone()) {
            return state.prosthetic ? 2.0F : 3.5F;
        }
        float score = (1.0F - Mth.clamp(state.ratio(), 0.0F, 1.0F)) * 5.0F;
        if (state.necrotic()) {
            score += 6.0F;
        }
        if (state.organTorn()) {
            score += 5.5F;
        } else if (state.organHurt()) {
            score += 2.0F;
        }
        if (state.fractured()) {
            score += 5.0F;
        } else if (state.cracked) {
            score += 2.0F;
        }
        if (state.stuck()) {
            score += 4.0F;
        }
        if (state.arterial()) {
            score += 3.5F;
        } else if (state.bleed >= PartState.BLEED_HEAVY) {
            score += 2.0F;
        }
        // What is left over from something that healed. Not much, and never nothing:
        // a tight scar over a joint is a thing people mention every single day.
        if (state.stiff > 0.0F) {
            score += state.stiff * 1.5F;
        }
        if (state.infection >= PartState.INFECTION_SEVERE) {
            score += 4.0F;
        } else if (state.infection > PartState.INFECTION_NONE) {
            score += 2.0F;
        }
        // Blistered is the loudest thing on a burnt body and it is the survivable one.
        // Through the skin says nothing at all, which is the fact the whole burn system
        // is built on and has to be true here as well.
        if (state.burn == PartState.BURN_PARTIAL) {
            score += 3.0F;
        } else if (state.burn == PartState.BURN_SUPERFICIAL) {
            score += 1.5F;
        }
        return score;
    }

    /**
     * And what they actually say, which is a different number.
     *
     * @return -1 when they are in no state to answer at all
     */
    public static int said(BodyHealth body, RandomSource random) {
        if (body.downed() || body.under() || body.arrested()) {
            return -1;
        }
        float score = burden(body);
        if (body.painkilled()) {
            // The pills and the needle are both painkillers here; the needle is worth more
            // and the body knows which one it had, because the clock it set is longer.
            score -= body.painkillerTicks() > Config.values().painkillerSeconds * 20 ? MORPHINE : PILLS;
        }
        // Not feeling it yet, which is most of it - and the reason a patient who has just
        // done something appalling to themselves says four.
        if (Adrenaline.up(body)) {
            score -= SURGE * Adrenaline.relief(body);
        }
        if (body.has(Disease.SHOCK)) {
            score -= SHOCK;
        }
        int said = Math.round(Mth.clamp(score, 0.0F, WORST));
        // A head that has been hit does not rank things, and a mind that is elsewhere
        // ranks something else. Neither of them lies; both of them wander.
        if (body.has(Disease.CONCUSSION) || body.has(Disease.DELIRIUM)
                || body.has(Disease.SCHIZOPHRENIA) || body.has(Disease.FRENZY)) {
            said = Mth.clamp(said + random.nextInt(7) - 3, 0, WORST);
        }
        return said;
    }

    /** What a number that size is called, for the one line that has to summarise it. */
    public static Component title(int said) {
        String band = said <= 0 ? "none" : said <= 3 ? "mild" : said <= 6 ? "bad" : said <= 8 ? "severe" : "worst";
        return Component.translatable("triage.pain." + band);
    }

    public static ChatFormatting colour(int said) {
        if (said >= 9) {
            return ChatFormatting.DARK_RED;
        }
        if (said >= 7) {
            return ChatFormatting.RED;
        }
        return said >= 4 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
    }

    /**
     * Whether what they say and what their heart is doing are telling different stories.
     *
     * <p>The one thing this reading is actually for. A quiet patient with a racing heart
     * has had something, or is running on the last of something, and either way the next
     * person to open a bag over them needs to know it before they do.
     */
    public static boolean disagrees(BodyHealth body, int said, Pulse pulse) {
        return said >= 0 && said <= 3 && pulse.rate() >= 110;
    }
}
