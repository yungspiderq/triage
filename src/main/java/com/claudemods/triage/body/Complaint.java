package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * What the patient says when you ask them where it hurts.
 *
 * <p>Every other reading in this mod is taken <em>off</em> a body. This one comes out of
 * one, and it is the only channel here that does. That makes it the only thing a medic
 * can learn without touching anybody, the only thing that stops working the moment the
 * patient stops being able to talk - and the only one that can be wrong.
 *
 * <p>It is also the third leg of the stool this mod has been building. A pulse says the
 * blood went somewhere. A chest says the air is not getting in. Neither says <em>where</em>.
 * A conscious person with a torn spleen will tell you their belly hurts, and that is how
 * that injury is actually found in the world - by asking, and believing the answer.
 *
 * <p>And the failure is the good part. Painkillers work, so a dosed patient reports
 * less than is true: the small complaints go entirely and the big ones come down a grade,
 * so the belly that was unbearable is now an ache. The medic keeps the location and loses
 * the urgency - which is the interesting half to lose, because urgency is what decides
 * who gets treated first. Nothing else in the mod makes a treatment cost information.
 */
public final class Complaint {
    /** They named the worst of it, and they were right. */
    public static final int CLEAR = 0;
    /** They named something real, but not the worst thing. A rattled head does this. */
    public static final int MUDDLED = 1;
    /** They say nothing much hurts, and they believe it. */
    public static final int NUMB = 2;
    /** Nobody home. */
    public static final int SILENT = 3;
    /** Awake and willing, and did not hear you ask. */
    public static final int UNHEARD = 4;
    /**
     * They named a place that is not where the injury is, and they are not confused.
     *
     * <p>The famous one is a torn spleen, which hurts at the tip of the left shoulder.
     * It is called Kehr's sign, it is real, and it is how that injury is found in an
     * emergency room by somebody with no machine to hand. The liver does the same thing
     * on the right. A heart does it down the left arm.
     *
     * <p>The mod does not mark this answer as suspect, because the patient is not lying
     * and does not know anything is wrong with their shoulder. What gives it away is the
     * chart: a shoulder that hurts more than anything else in a body, on a shoulder with
     * nothing whatever wrong with it, is not a shoulder problem. That is the whole
     * puzzle, and it is the same one a real medic solves - which is why this only ever
     * fires when the named place is genuinely unhurt. A coincidence would not be a clue.
     */
    public static final int REFERRED = 5;

    /** What they said, and how much that is worth. */
    public record Answer(int kind, @Nullable BodyPart part, int severity) {
    }

    /** Barely worth mentioning, worth mentioning, and the thing they cannot stop saying. */
    public static final int ACHE = 0;
    public static final int SORE = 1;
    public static final int AGONY = 2;

    /** Under this, nothing is worth complaining about. */
    private static final float WORTH_SAYING = 3.0F;
    /**
     * What a dose takes off everything, honestly, including off the truth.
     *
     * <p>Enough to bury the small complaints entirely and to talk the big ones down a
     * grade - so a torn spleen stops being the thing they cannot talk past and becomes
     * an ache in the belly. That is the real trade and the interesting one: painkillers
     * cost the medic the <em>severity</em>, not the location. Set high enough to erase a
     * torn organ outright they would have made dosing a blindfold rather than a choice.
     */
    private static final float PAINKILLER_RELIEF = 4.0F;

    private Complaint() {
    }

    /**
     * Asks them.
     *
     * @param random needed because a rattled head does not give the same answer twice,
     *               which is the entire reason a concussion is dangerous to a medic and
     *               not only to the person who has one.
     */
    public static Answer ask(BodyHealth body, RandomSource random) {
        // Nobody unconscious tells anybody anything. This is not a refusal, it is the
        // answer: a patient who cannot say where it hurts is a patient you now have to
        // find it on, and that is worth knowing.
        if (body.downed() || body.arrested() || body.under()) {
            return new Answer(SILENT, null, 0);
        }
        // Awake, and did not hear a word of it. Which is the casualty who was nearest the
        // explosion - so the channel that says WHERE stops working exactly where the
        // injury that says nothing else has just happened.
        if (body.has(Disease.DEAFNESS)) {
            return new Answer(UNHEARD, null, 0);
        }

        float relief = body.painkilled() ? PAINKILLER_RELIEF : 0.0F;
        List<BodyPart> hurting = new ArrayList<>();
        BodyPart worst = null;
        float worstScore = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            float score = pain(body.part(part)) - relief;
            if (score < WORTH_SAYING) {
                continue;
            }
            hurting.add(part);
            if (score > worstScore) {
                worstScore = score;
                worst = part;
            }
        }
        if (worst == null) {
            // Which is a true statement about how they feel and a false one about how
            // they are. The mod does not correct them.
            return new Answer(NUMB, null, 0);
        }

        // A head that has been hit does not rank things. It answers with something that
        // hurts, which may be the graze on the arm rather than the belly filling up.
        boolean muddled = body.has(Disease.CONCUSSION) || body.has(Disease.DELIRIUM)
                || body.has(Disease.SCHIZOPHRENIA) || body.has(Disease.SHOCK)
                || body.has(Disease.FRENZY);
        if (muddled && hurting.size() > 1 && random.nextFloat() < 0.55F) {
            BodyPart said = hurting.get(random.nextInt(hurting.size()));
            return new Answer(MUDDLED, said, severity(pain(body.part(said)) - relief));
        }
        // And the one answer that is wrong on purpose and useful because of it.
        BodyPart elsewhere = referred(body, worst, hurting);
        if (elsewhere != null) {
            return new Answer(REFERRED, elsewhere, severity(worstScore));
        }
        return new Answer(CLEAR, worst, severity(worstScore));
    }

    /**
     * Where a torn organ makes itself felt instead.
     *
     * <p>Only for an organ that is actually torn - a bruise does not do this - and only
     * when the place it points at has nothing else wrong with it. If the shoulder is
     * also broken the patient would have been complaining about the shoulder anyway, and
     * an answer that could mean either is not information, it is noise.
     */
    private static @Nullable BodyPart referred(BodyHealth body, BodyPart worst, List<BodyPart> hurting) {
        if (!Config.values().referredPain) {
            return null;
        }
        PartState state = body.part(worst);
        for (Organ organ : Organ.in(worst)) {
            if (state.organ(organ) < PartState.ORGAN_TORN) {
                continue;
            }
            BodyPart site = switch (organ) {
                // Kehr's sign. Blood under the diaphragm, felt at the top of the shoulder
                // on the same side, and the belly may not be saying anything yet at all.
                case SPLEEN, HEART -> BodyPart.LEFT_ARM;
                case LIVER -> BodyPart.RIGHT_ARM;
                default -> null;
            };
            if (site != null && !hurting.contains(site)) {
                return site;
            }
        }
        return null;
    }

    /**
     * How much one part is complaining.
     *
     * <p>Weighted so that the things you cannot see outrank the things you can: a torn
     * organ hurts more than the cut over it, because otherwise asking would only ever
     * confirm what the chart already showed and the whole channel would be decoration.
     */
    private static float pain(PartState state) {
        if (state.gone()) {
            // A stump that is not there still reports. It is the one complaint in here
            // that points at nothing a treatment can reach.
            return state.prosthetic ? 3.5F : 5.0F;
        }
        float score = (1.0F - Mth.clamp(state.ratio(), 0.0F, 1.0F)) * 10.0F;
        if (state.necrotic()) {
            score += 9.0F;
        }
        if (state.organTorn()) {
            score += 8.0F;
        } else if (state.organHurt()) {
            score += 3.0F;
        }
        if (state.fractured()) {
            score += 7.0F;
        } else if (state.cracked) {
            score += 3.0F;
        }
        if (state.stuck()) {
            score += 6.0F;
        }
        // Quiet, and the only thing on this list with nothing to see. A leg with a clot
        // in it is a leg that aches, and asking is the only way the mod ever hands it
        // over - which makes this the second injury that is found by talking to people.
        if (state.clotted()) {
            score += 4.5F;
        }
        if (state.infection >= PartState.INFECTION_SEVERE) {
            score += 6.0F;
        } else if (state.infection > PartState.INFECTION_NONE) {
            score += 3.0F;
        }
        // A burn through the skin is worth nothing here, and that is the whole point of
        // it. The nerve endings that would have put it on this list are part of what
        // burnt, so asking somebody where it hurts will never find their worst burn -
        // which is why the mod has a way to press on one. Blistered is the loudest thing
        // on a burnt body, and it is the survivable one.
        if (state.burn == PartState.BURN_PARTIAL) {
            score += 4.0F;
        } else if (state.burn == PartState.BURN_SUPERFICIAL) {
            score += 2.0F;
        }
        if (state.bleed == PartState.BLEED_HEAVY) {
            score += 3.0F;
        } else if (state.bleeding()) {
            score += 1.0F;
        }
        if (state.tourniquet) {
            score += 4.0F;
        }
        return score;
    }

    private static int severity(float score) {
        return score >= 16.0F ? AGONY : score >= 8.0F ? SORE : ACHE;
    }
}
