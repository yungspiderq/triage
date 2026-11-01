package com.claudemods.triage.body;

import net.minecraft.network.chat.Component;

/**
 * The things inside the three parts that have anything inside them.
 *
 * <p>A chest is not a thickness of muscle with a rib in it; it is a heart and two lungs in
 * a box. A belly is not padding. Until this existed the mod knew that a torso could be
 * "bleeding inside" and nothing more, which is the medical equivalent of a car that says
 * "engine".
 *
 * <p>Eight of them, and each is a different emergency. A spleen is a bag of blood that
 * empties fast and does nothing else; a gut spills what is in it and the danger is the
 * fever three minutes later; a lung crushes itself; a heart does not give anybody three
 * minutes. Reading which is which is the entire reason a chart of them is worth opening.
 *
 * <p>Each part keeps its own in two bits apiece, four slots to a part - which is why the
 * slot numbers matter and why there are never more than four in one place.
 */
public enum Organ {
    /** What a skull is for, and the one thing here a surgeon cannot put back. */
    BRAIN("brain", BodyPart.HEAD, 0, 0.30F),

    /** Fastest of the lot, and the reason a chest wound is not a belly wound. */
    HEART("heart", BodyPart.THORAX, 0, 2.00F),
    LEFT_LUNG("left_lung", BodyPart.THORAX, 1, 0.80F),
    RIGHT_LUNG("right_lung", BodyPart.THORAX, 2, 0.80F),

    /** Big, soft, full of blood, and sitting directly under the ribs. */
    LIVER("liver", BodyPart.STOMACH, 0, 1.40F),
    /** The classic: a bag of blood a kick can burst, with nothing outside to show it. */
    SPLEEN("spleen", BodyPart.STOMACH, 1, 1.60F),
    /** Bleeds least and frightens most, because what comes out of it is not blood. */
    GUT("gut", BodyPart.STOMACH, 2, 0.60F),
    KIDNEY("kidney", BodyPart.STOMACH, 3, 0.90F);

    public static final Organ[] VALUES = values();

    private final String id;
    private final BodyPart part;
    private final int slot;
    private final float bleed;

    Organ(String id, BodyPart part, int slot, float bleed) {
        this.id = id;
        this.part = part;
        this.slot = slot;
        this.bleed = bleed;
    }

    public String id() {
        return id;
    }

    public BodyPart part() {
        return part;
    }

    /** Which pair of bits in {@link PartState#organs} belongs to this one. */
    public int slot() {
        return slot;
    }

    /**
     * How fast a torn one empties somebody, as a multiple of the config rate.
     *
     * <p>These numbers are the whole point of having eight of these rather than one. A
     * spleen and a gut are both "bleeding inside", and one of them is twice the hurry.
     */
    public float bleed() {
        return bleed;
    }

    public Component title() {
        return Component.translatable("triage.organ." + id);
    }

    /** A torn lung does the other thing this mod already models: it crushes itself. */
    public boolean lung() {
        return this == LEFT_LUNG || this == RIGHT_LUNG;
    }

    /** A torn gut is not a bleeding problem. It is a fever three minutes from now. */
    public boolean spills() {
        return this == GUT;
    }

    /** Everything inside one part, in slot order. */
    public static Organ[] in(BodyPart part) {
        int count = 0;
        for (Organ organ : VALUES) {
            if (organ.part == part) {
                count++;
            }
        }
        Organ[] out = new Organ[count];
        for (Organ organ : VALUES) {
            if (organ.part == part) {
                out[organ.slot] = organ;
            }
        }
        return out;
    }

    /** Whether this part has anything in it worth tearing. */
    public static boolean inside(BodyPart part) {
        return part == BodyPart.HEAD || part == BodyPart.THORAX || part == BodyPart.STOMACH;
    }
}
