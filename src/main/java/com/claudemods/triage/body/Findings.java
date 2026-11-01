package com.claudemods.triage.body;

/**
 * What has been found out about a body, as opposed to what is true about it.
 *
 * <p>In arcade the two are the same thing and this class is never consulted. In realistic
 * they come apart, and everything that follows from that - a chart with blanks in it, an
 * examination worth doing, a wrong guess that costs a splint - follows from this one
 * distinction: <b>a part nobody has looked at is not a healthy part.</b>
 *
 * <p>Findings live on the patient rather than on whoever found them, which is not laziness
 * about who knows what. It is how a chart works in life: it travels with the person, and
 * the medic who arrives second reads what the first one wrote. The mod already had the
 * shape of this - a pulse is stored with the time it was taken and the name of whoever
 * took it - and this is that idea for everything else.
 *
 * <p>The facts are bits and a part's worth of them is a byte, so seven parts fit in one
 * long. Beside them, one time per part: when it was last examined. Not one time per fact,
 * which would be eight times the storage to answer a question nobody asks - what is worth
 * knowing is how old the look was, and a look takes in everything the hands were on.
 */
public final class Findings {
    /** How many bits each part gets. Seven parts, eight facts, one long. */
    private static final int WIDTH = 8;

    /**
     * Whether the bone in there is broken, and how many times.
     *
     * <p>The fact this whole mode was asked for. Found by feeling along the bone, or read
     * off a film - and those are the only two ways, because a bone that has not moved out
     * of line puts nothing at all on the outside.
     */
    public static final int BONE = 1;

    /** How much of the part is left: the number the chart used to print for free. */
    public static final int FLESH = 1 << 1;

    /** Whether what is in the wound has started to grow. */
    public static final int ROT = 1 << 2;

    /**
     * How deep the burn went.
     *
     * <p>That there <em>is</em> a burn is never a finding - it is on the skin. How far it
     * goes is, and the mod has had the way to find out since burns arrived: press on it,
     * and listen for the part that does not answer.
     */
    public static final int BURN = 1 << 3;

    /** Something in there that should not be: a fragment, an arrowhead. */
    public static final int METAL = 1 << 4;

    /** What the organs under it are doing. Never visible from outside, ever. */
    public static final int ORGAN = 1 << 5;

    /** Blood in a limb that has stopped moving. */
    public static final int CLOT = 1 << 6;

    /** What the chest sounds like. The thorax alone, and only through a stethoscope. */
    public static final int BREATH = 1 << 7;

    public static final int ALL = BONE | FLESH | ROT | BURN | METAL | ORGAN | CLOT | BREATH;

    /**
     * Facts about the whole person rather than about one part of them.
     *
     * <p>Seven parts at a byte each leave a byte over in the same long, which is where
     * these live. There is one so far.
     */
    public static final int WHOLE = 7 * WIDTH;

    /**
     * Which blood they have.
     *
     * <p>Not a thing anybody reads off a body - it is a thing a person knows and says, or
     * has stamped on a band because one day they will not be able to say it. Which is why
     * this is the only finding in the mod that can be established by an object rather than
     * by an examination.
     */
    public static final int GROUP = 1;

    /** The bit for a fact about the whole person. */
    public static long wholeBit(int fact) {
        return ((long) (fact & 0xFF)) << WHOLE;
    }

    private Findings() {
    }

    /** The bit that says "this fact, about this part", inside the one long. */
    public static long bit(BodyPart part, int fact) {
        return ((long) (fact & 0xFF)) << (part.ordinal() * WIDTH);
    }

    /**
     * What anybody standing there can see without touching, in either difficulty.
     *
     * <p>The rule behind the list is one question asked of each fact: would a person who
     * walked into the room know this without doing anything? An arm that is not there,
     * yes. A leg lying at an angle it does not bend at, yes. Two cracked ribs under an
     * unbroken skin, no - and that "no" is the whole mode.
     *
     * <p>Note what this does <em>not</em> do: it never hides something that is plain. A
     * limb gone black, a bone through the skin and blood coming out of somebody are facts
     * about the world, not entries on a form, and a difficulty setting that pretended
     * otherwise would be a difficulty setting about lying to people.
     */
    public static boolean plain(PartState state, int fact) {
        return switch (fact) {
            // A limb that is missing, black, dead or hanging useless is not a reading.
            case FLESH -> state.missing || state.prosthetic || state.necrotic() || state.blacked();
            // Only when the bone has moved or come through. A clean break shows nothing.
            case BONE -> state.displaced || state.openBreak;
            // That it is burnt is on the skin; how deep it went is not.
            case BURN -> false;
            // An infection announces itself once it is bad enough to be seen.
            case ROT -> state.infection >= PartState.INFECTION_SEVERE;
            // Everything else is inside somebody.
            default -> false;
        };
    }

    /**
     * Whether this fact is worth asking about for this part at all.
     *
     * <p>Chest sounds are a thorax question. Everything else is asked of anything.
     */
    public static boolean applies(BodyPart part, int fact) {
        return fact != BREATH || part == BodyPart.THORAX;
    }
}
