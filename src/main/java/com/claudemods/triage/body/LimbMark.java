package com.claudemods.triage.body;

import com.claudemods.triage.Config;

/**
 * The one part of a body that everybody can see without being told.
 *
 * <p>A chart belongs to its owner. Somebody else's bleeding, breaks and blood volume all
 * cost a walk over and a look, and that is the rule the whole mod is built on. This is
 * the second exception to it after the mark pinned to a chest, and it is not really an
 * exception at all: an arm that is not there is not information anybody has to go and
 * fetch. It is a thing about a person that is true from across a field.
 *
 * <p>So the four limbs travel with the mark, in eight bits - two per limb, because there
 * are three things a limb can be and one of them needs saying twice: gone, and gone with
 * something in its place.
 */
public final class LimbMark {
    /** The first limb in {@link BodyPart#VALUES}; everything here counts from it. */
    private static final int FIRST = BodyPart.LEFT_ARM.ordinal();
    public static final int LIMBS = 4;

    public static final int THERE = 0;
    public static final int GONE = 1;
    public static final int FITTED = 2;
    /** And the one with a joint in it, which is a different thing to look at. */
    public static final int FITTED_FINE = 3;

    /** And what is tied over each of the seven, which is the other visible thing. */
    public static final int BARE = 0;
    public static final int CLEAN = 1;
    public static final int SOILED = 2;

    private LimbMark() {
    }

    /** Everything the room can see about this body's limbs, as one byte. */
    public static int of(BodyHealth body) {
        int bits = 0;
        for (int i = 0; i < LIMBS; i++) {
            PartState state = body.part(BodyPart.VALUES[FIRST + i]);
            int value = !state.gone() ? THERE
                    : !state.prosthetic ? GONE
                    : state.fineProsthetic ? FITTED_FINE : FITTED;
            bits |= value << (i * 2);
        }
        return bits;
    }

    /** {@link #THERE}, {@link #GONE} or {@link #FITTED}, for a part that may not be a limb. */
    public static int state(int bits, BodyPart part) {
        if (!part.limb()) {
            return THERE;
        }
        return (bits >> ((part.ordinal() - FIRST) * 2)) & 0b11;
    }

    /**
     * What is tied over each of the seven parts, in two bits apiece.
     *
     * <p>Rides beside the limbs and for the same reason: a bandage is not something
     * anybody has to walk over and read. It is white, it is on an arm, and whether it has
     * gone through is the single thing about somebody else's treatment that is obvious
     * from across a field - which makes it the third and last thing the mod lets a body
     * say out loud about itself.
     */
    public static int dressings(BodyHealth body, int cleanTicks) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            int value = !state.dressed ? BARE : state.soiled(cleanTicks) ? SOILED : CLEAN;
            bits |= value << (part.ordinal() * 2);
        }
        return bits;
    }

    /** {@link #BARE}, {@link #CLEAN} or {@link #SOILED}. */
    public static int dressing(int bits, BodyPart part) {
        return (bits >> (part.ordinal() * 2)) & 0b11;
    }

    /** And the third thing tied round a limb: a strap, and how long it has been on. */
    public static final int LOOSE = 0;
    public static final int FRESH = 1;
    public static final int LATE = 2;

    /**
     * Which limbs are strapped, and which of those are running out of time.
     *
     * <p>The last of the three things a body says about itself out loud, and the one that
     * matters most to somebody walking past. A tourniquet is a decision with a clock on
     * it, and until now the clock could only be read by walking over and opening a chart -
     * which is exactly the wrong place for it, because the person it is on may be in no
     * state to be asked. A strap that has been on too long should be visible from the
     * doorway, and now it is: it darkens as the time goes.
     */
    public static int straps(BodyHealth body, int deadTicks) {
        int bits = 0;
        for (int i = 0; i < LIMBS; i++) {
            BodyPart part = BodyPart.VALUES[FIRST + i];
            PartState state = body.part(part);
            int value = !state.tourniquet ? LOOSE
                    : state.deadTicks >= deadTicks * 2 / 3 ? LATE : FRESH;
            bits |= value << (i * 2);
        }
        return bits;
    }

    public static int strap(int bits, BodyPart part) {
        if (!part.limb()) {
            return LOOSE;
        }
        return (bits >> ((part.ordinal() - FIRST) * 2)) & 0b11;
    }

    // ---- and where it is coming out ---------------------------------------

    public static final int DRY = 0;
    public static final int SEEPING = 1;
    public static final int RUNNING = 2;
    /**
     * And the one that is thrown rather than lost.
     *
     * <p>Free: two bits hold four values and only three were ever used, so the thing the
     * mod most needs to be able to say across a clearing cost nothing at all to say.
     */
    public static final int SPURTING = 3;

    /**
     * Which parts are bleeding, and how badly, in two bits apiece.
     *
     * <p>The fourth thing a body says out loud, and on reflection the most obvious of the
     * four. Everything else here needed an argument for why it should be visible without
     * a chart. This one needed an argument for why it ever was not: blood on somebody is
     * not information you walk over and read off a screen, it is the thing you see first
     * and from furthest away, and it is why anybody walks over at all.
     *
     * <p>A part with a dressing on it says nothing. What is under a bandage is under a
     * bandage - and the bandage has its own two bits, and a soaked one is its own answer.
     */
    public static int bleeds(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            // A dressing hides a cut and does not hide an artery: what soaks through
            // one of those in ten seconds is the thing everybody in the clearing needs
            // to have seen, and hiding it under a bandage would be the mod lying about
            // the one injury it cannot afford to be quiet about.
            int value = !state.bleeding() ? DRY
                    : state.arterial() ? SPURTING
                    : state.dressed ? DRY
                    : state.bleed == PartState.BLEED_HEAVY ? RUNNING : SEEPING;
            bits |= value << (part.ordinal() * 2);
        }
        return bits;
    }

    /**
     * {@link #DRY}, {@link #SEEPING} or {@link #RUNNING}.
     *
     * <p>Takes the unpacked mask, like {@link #dressing} and {@link #strap} beside it.
     * One of these reading a packed number and the other two reading a plain mask would
     * be a trap laid for whoever adds the fifth.
     */
    public static int bleed(int bits, BodyPart part) {
        return (bits >> (part.ordinal() * 2)) & 0b11;
    }

    // ---- and the one tied on over the top ---------------------------------

    /**
     * Which parts have a splint on them, one bit each.
     *
     * <p>The sixth thing a body says out loud, and the least arguable of the six: a stick
     * tied to a leg is not a diagnosis, it is a stick tied to a leg. Everything else here
     * had to justify being visible without a chart. This one would have had to justify
     * being invisible.
     *
     * <p>It is also the first of them that is <b>good news</b>. Every other mark on a body
     * says something is wrong with it; this one says somebody has already been here, which
     * is exactly what a medic walking up on a stranger wants to know first.
     */
    public static int splints(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).splinted()) {
                bits |= 1 << part.ordinal();
            }
        }
        return bits;
    }

    public static boolean splinted(int bits, BodyPart part) {
        return (bits & (1 << part.ordinal())) != 0;
    }

    /**
     * And which parts have somebody's weapon in them.
     *
     * <p>One bit each and nothing else: what it is does not travel, because what it is
     * does not matter from across a field. That there is something standing out of
     * somebody's leg is the whole message, and it is a message nobody has to be taught.
     */
    public static int blades(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).bladeIn()) {
                bits |= 1 << part.ordinal();
            }
        }
        return bits;
    }

    public static boolean bladeIn(int bits, BodyPart part) {
        return (bits & (1 << part.ordinal())) != 0;
    }

    /**
     * And which parts carry something that outlived what caused it.
     *
     * <p>One bit rather than a count, because across a field a scar is a scar: what is
     * worth knowing at that distance is that this body has been through something here,
     * and what exactly is a question for the chart.
     */
    public static int scars(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).marked()) {
                bits |= 1 << part.ordinal();
            }
        }
        return bits;
    }

    public static boolean scarred(int bits, BodyPart part) {
        return (bits & (1 << part.ordinal())) != 0;
    }

    // ---- and the shape of the bone underneath -----------------------------

    public static final int BONE_FINE = 0;
    /** Healed, and healed out of line. The limb keeps the angle for good. */
    public static final int BONE_CROOKED = 1;
    /** Broken and out of line now, which is a thing anybody can see. */
    public static final int BONE_DISPLACED = 2;
    /** And the end of it outside the skin. */
    public static final int BONE_OPEN = 3;

    /**
     * What shape each limb is, two bits apiece.
     *
     * <p>The best argument for being visible of anything in this class, and it needed no
     * argument at all: a limb with a bone out of line in it is <b>the wrong shape</b>.
     * Nobody reads that off a chart. They see it from the other side of a clearing and
     * start running, and until now the mod made them walk over and open a screen to find
     * out what everyone in the room could already see.
     */
    public static int bones(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            int value = state.openFracture() ? BONE_OPEN
                    : state.outOfLine() ? BONE_DISPLACED
                    : state.bent() ? BONE_CROOKED
                    : BONE_FINE;
            bits |= value << (part.ordinal() * 2);
        }
        return bits;
    }

    public static int bone(int bits, BodyPart part) {
        return (bits >> (part.ordinal() * 2)) & 0b11;
    }

    // ---- all four at once -------------------------------------------------

    /**
     * The three masks in one number.
     *
     * <p>Not a saving, a limit: the codec that builds the packet takes six fields and
     * there were seven of them. Eight bits of limbs, fourteen of dressings and eight of
     * straps is thirty, which fits in an int with room to spare - and it keeps the three
     * things a body says about itself out loud in one place, which is where they belong.
     */
    private static final int DRESSING_SHIFT = 8;
    private static final int STRAP_SHIFT = 22;
    private static final int BLEED_SHIFT = 30;
    private static final int DEAD_SHIFT = 44;
    private static final int BURN_SHIFT = 48;

    public static long pack(int limbs, int dressings, int straps, int bleeds,
                            int dying, int burns) {
        return (limbs & 0xFFL) | ((dressings & 0x3FFFL) << DRESSING_SHIFT)
                | ((straps & 0xFFL) << STRAP_SHIFT)
                | ((bleeds & 0x3FFFL) << BLEED_SHIFT)
                | ((dying & 0xFL) << DEAD_SHIFT)
                | ((burns & 0x3FFFL) << BURN_SHIFT);
    }

    public static int dyingOf(long packed) {
        return (int) ((packed >> DEAD_SHIFT) & 0xFL);
    }

    public static int burnsOf(long packed) {
        return (int) ((packed >> BURN_SHIFT) & 0x3FFFL);
    }

    public static int bleedsOf(long bits) {
        return (int) ((bits >> BLEED_SHIFT) & 0x3FFF);
    }

    public static int limbsOf(long packed) {
        return (int) (packed & 0xFFL);
    }

    public static int dressingsOf(long packed) {
        return (int) ((packed >> DRESSING_SHIFT) & 0x3FFFL);
    }

    public static int strapsOf(long packed) {
        return (int) ((packed >> STRAP_SHIFT) & 0xFFL);
    }

    // ---- the second number ------------------------------------------------

    /**
     * The two newest masks, in a long of their own.
     *
     * <p>The first one is full: eight bits of limbs, fourteen of dressings, eight of
     * straps, fourteen of bleeds, four of dying and fourteen of burns is sixty-two, and
     * a long has sixty-four. Rather than squeeze two more masks into two bits, the
     * packet grew its sixth and last field - which is also the last one the stream codec
     * will take, so whoever adds the seventh mask will be reading this comment and
     * writing a second packet.
     */
    private static final int SCAR_SHIFT = 7;
    private static final int BONE_SHIFT = 14;
    /**
     * And the colour of the person, which is four bits and did not need a packet of its
     * own: two for how white they have gone and two for how blue, in the room the second
     * long had left over. Whoever wrote the note above about the seventh mask was right
     * about the first long and wrong about this one.
     */
    private static final int FACE_SHIFT = 28;
    /** And one bit a part for what is standing out of it. */
    private static final int BLADE_SHIFT = 32;

    public static long packWorn(int splints, int scars, int bones, int face, int blades) {
        return (splints & 0x7FL) | ((scars & 0x7FL) << SCAR_SHIFT)
                | ((bones & 0x3FFFL) << BONE_SHIFT)
                | ((face & 0xFL) << FACE_SHIFT)
                | ((blades & 0x7FL) << BLADE_SHIFT);
    }

    public static int splintsOf(long packed) {
        return (int) (packed & 0x7FL);
    }

    public static int scarsOf(long packed) {
        return (int) ((packed >> SCAR_SHIFT) & 0x7FL);
    }

    public static int bonesOf(long packed) {
        return (int) ((packed >> BONE_SHIFT) & 0x3FFFL);
    }

    // ---- the colour of a person -------------------------------------------

    public static int bladesOf(long packed) {
        return (int) ((packed >> BLADE_SHIFT) & 0x7FL);
    }

    /** How white. Volume, and only volume. */
    public static int paleOf(long packed) {
        return (int) ((packed >> FACE_SHIFT) & 0x3L);
    }

    /** How blue. Air, and only air. */
    public static int blueOf(long packed) {
        return (int) ((packed >> (FACE_SHIFT + 2)) & 0x3L);
    }

    /**
     * What somebody looks like from across a room.
     *
     * <p>The oldest two findings in medicine and the first two in this mod that nobody
     * has to fetch. They are worth having precisely because they cannot be confused with
     * each other: white is blood and blue is air, they are never the same emergency, and
     * they want opposite things done first. A player who learns nothing else from the
     * realistic difficulty will learn those two colours, and that is already most of what
     * standing in a doorway is for.
     *
     * <p>Neither is redacted and neither needs to be. Redaction in this mod is about facts
     * that would have cost somebody a look; these two <em>are</em> the look.
     */
    public static int face(BodyHealth body) {
        float loss = body.bloodLoss();
        int pale = loss >= 0.62F ? 3 : loss >= 0.42F ? 2 : loss >= 0.26F ? 1 : 0;

        int blue;
        Breathing breathing = Breathing.of(body, Config.values().airwaySeconds);
        if (body.arrested() || breathing.character() == Breathing.ABSENT) {
            blue = 3;
        } else if (breathing.character() == Breathing.SHALLOW
                || breathing.character() == Breathing.SLOW) {
            blue = 2;
        } else if (body.has(Disease.SMOKE) || body.has(Disease.EMBOLISM)
                || body.has(Disease.PNEUMONIA)) {
            blue = 1;
        } else {
            blue = 0;
        }
        return (pale & 0x3) | ((blue & 0x3) << 2);
    }

    // ---- and the two that were invisible ----------------------------------

    /**
     * Which limbs have died on the body, one bit each.
     *
     * <p>The mark has always said whether a limb is there or gone, and said nothing at all
     * about the state in between - the one where it is still attached and no longer alive.
     * That is the worst gap of the three, because it is the state with a decision in it:
     * a limb that is gone needs nothing from anybody and a limb that is dying needs a saw
     * today. It went unseen while the two either side of it were obvious.
     *
     * <p>A strap long enough to kill a limb is already visible, and this is what that strap
     * did. The two of them together tell the whole story from across a field.
     */
    public static int dying(BodyHealth body) {
        int bits = 0;
        for (int i = 0; i < LIMBS; i++) {
            if (body.part(BodyPart.VALUES[FIRST + i]).necrotic()) {
                bits |= 1 << i;
            }
        }
        return bits;
    }

    public static boolean dying(int bits, BodyPart part) {
        return part.limb() && (bits & (1 << (part.ordinal() - FIRST))) != 0;
    }

    /**
     * How burnt each part is, two bits apiece, which is the depth itself.
     *
     * <p>The one injury in the mod that is entirely on the outside of somebody, and until
     * now the only way to know about one was to open a chart. A burn is the first thing
     * anybody would see - it is on the skin, it has a colour, and the difference between
     * the depths is a difference in how it looks rather than how it feels. Which is the
     * whole argument for it being on the model, and the same argument the blood won.
     */
    public static int burns(BodyHealth body) {
        int bits = 0;
        for (BodyPart part : BodyPart.VALUES) {
            bits |= (body.part(part).burn & 0b11) << (part.ordinal() * 2);
        }
        return bits;
    }

    public static int burn(int bits, BodyPart part) {
        return (bits >> (part.ordinal() * 2)) & 0b11;
    }
}
