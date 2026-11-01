package com.claudemods.triage.body;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * What a chest is doing, which is the one vital you get without touching anybody.
 *
 * <p>A pulse has to be felt for. Breathing does not: you watch the chest, and you have
 * the answer from across the room in about four seconds. That makes it the cheapest
 * reading in the mod and the first one anybody actually gets, which is exactly its place
 * in the order everybody is taught - airway, breathing, circulation, in that order,
 * because that is the order they kill you in.
 *
 * <p>And it is what the pulse is to internal bleeding, for the other injury this mod
 * hides: a crushed lung. A pneumothorax has never had an outward sign. It sits on the
 * chart under a name most players will not know, and the needle that answers it is the
 * least obvious item in the mod. Fast and shallow is what it looks like from outside,
 * and now that is visible.
 *
 * <p>Pure arithmetic over a {@link BodyHealth}, like {@link Pulse}, so nothing is stored
 * and nothing can go stale.
 */
public record Breathing(int rate, int character) {
    /** No chest movement. Either the airway is shut or the heart has stopped. */
    public static final int ABSENT = 0;
    /** Fast and not deep - the one a crushed lung makes. */
    public static final int SHALLOW = 1;
    /** Working at it. */
    public static final int LABOURED = 2;
    /**
     * Too few, and getting fewer.
     *
     * <p>The one somebody else caused. Ether and opium both press on the drive to
     * breathe, and the distance between a patient who cannot feel an operation and one
     * who has stopped breathing is a few drops. Printing "easy" over nine breaths a
     * minute would have hidden the one number the mask exists to be judged by.
     */
    public static final int SLOW = 3;
    public static final int EASY = 4;

    /** Breaths a minute for somebody with nothing wrong with them. */
    private static final int RESTING = 14;

    /**
     * Reads the chest.
     *
     * <p>Up with anything that means the body needs more air than it is getting - lost
     * volume, a lung that is not working, being on the floor - and down with the two
     * things that press on the drive to breathe at all. That second list is short and
     * it is the dangerous one: everything on it kills by stopping this.
     */
    public static Breathing of(BodyHealth body, int airwaySeconds) {
        if (body.arrested() || body.airwayBlocked(airwaySeconds * 20)) {
            return new Breathing(0, ABSENT);
        }
        float rate = RESTING;
        float loss = body.bloodLoss();
        rate += 14.0F * loss;

        boolean crushed = false;
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).pneumo) {
                crushed = true;
            }
        }
        if (crushed) {
            rate += 10.0F;
        }
        if (body.downed()) {
            rate += 5.0F;
        }
        // A chest that is itself broken does not fill. Ribs are the one bone whose job
        // is a movement rather than a shape, so breaking them shows up here.
        PartState chest = body.part(BodyPart.THORAX);
        boolean chestBad = chest.fractured() || chest.ratio() < 0.4F;
        if (chestBad) {
            rate += 5.0F;
        }
        // A lung that is filling up is the whole of what this line is for.
        if (body.has(Disease.PNEUMONIA)) {
            rate += 9.0F;
        }
        if (body.has(Disease.SHOCK)) {
            rate += 4.0F;
        }
        // An airway that is swelling shut. It is the fastest thing on this list and the
        // only one whose whole course is measured in minutes.
        boolean smoke = body.has(Disease.SMOKE);
        if (smoke) {
            rate += 12.0F;
        }
        // And the one that is not swelling, not fluid and not a wound: a piece of
        // something solid where the air was going. It is the steepest line here.
        if (body.has(Disease.EMBOLISM)) {
            rate += 16.0F;
        }

        // And the two that press it down. Ether is the whole reason the mask has a way
        // to go wrong: past a point this reaches nothing and stops.
        if (body.under()) {
            rate -= 5.0F;
        } else if (body.painkillerTicks() > 0) {
            rate -= 2.0F;
        }
        if (body.has(Disease.HYPOTHERMIA)) {
            rate -= 3.0F;
        }

        int breaths = Mth.clamp(Math.round(rate), 4, 46);
        // Slow outranks shallow: a chest that is barely moving is a chest that is about
        // to stop, and that is worse news than a chest working badly.
        int character = breaths <= 9 ? SLOW
                : crushed || chestBad || smoke ? SHALLOW
                : breaths >= 22 ? LABOURED
                : EASY;
        return new Breathing(breaths, character);
    }

    /** Whether this is worth saying out loud without being asked. */
    public boolean alarming() {
        return character == ABSENT || character == SHALLOW || character == SLOW || rate >= 24;
    }

    public Component characterTitle() {
        return Component.translatable(switch (character) {
            case ABSENT -> "triage.breathing.absent";
            case SHALLOW -> "triage.breathing.shallow";
            case LABOURED -> "triage.breathing.laboured";
            case SLOW -> "triage.breathing.slow";
            default -> "triage.breathing.easy";
        });
    }

    public ChatFormatting colour() {
        return switch (character) {
            case ABSENT -> ChatFormatting.DARK_RED;
            case SHALLOW -> ChatFormatting.RED;
            case LABOURED -> ChatFormatting.GOLD;
            case SLOW -> ChatFormatting.RED;
            default -> ChatFormatting.GREEN;
        };
    }
}
