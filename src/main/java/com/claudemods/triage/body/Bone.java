package com.claudemods.triage.body;

/**
 * What is actually under each part, and why breaking it is not the same everywhere.
 *
 * <p>Until this existed a fracture was one boolean and one splint, so a rib and a femur
 * and a skull were the same injury with a different label on it. They are not. A rib is
 * one of a set and you can break several; a femur is the biggest bone in the body and
 * there is only the one; and nobody has ever set a skull or a back by strapping a stick
 * to it.
 *
 * <p>Two properties carry all of that. How many of the bone there are decides whether one
 * splint finishes the job or is the first of four, and whether it can be splinted at all
 * decides which item is the answer - the ones that cannot are surgery, which is the
 * expensive thing, and that is the right price for a broken back.
 */
public enum Bone {
    /**
     * The skull. One plate, and a splint is no use to it: what a cracked skull needs is
     * somebody opening it, which is also why a hard knock to the head has always been
     * the injury this mod is least able to walk off.
     */
    SKULL("skull", 1, false),
    /**
     * Ribs. The only bone here there is more than one of, and the reason this whole
     * class exists: a chest that has been kicked in is three or four separate breaks,
     * each of them minor on its own and each of them needing its own pass.
     */
    RIBS("ribs", 4, true),
    /** The spine. One, and strapping a stick to somebody's back does nothing at all. */
    SPINE("spine", 1, false),
    /** The forearm: the ordinary case, and the one a splint was invented for. */
    FOREARM("forearm", 1, true),
    /** The femur: the same job, on the largest bone there is. */
    FEMUR("femur", 1, true);

    private final String id;
    private final int count;
    private final boolean settable;

    Bone(String id, int count, boolean settable) {
        this.id = id;
        this.count = count;
        this.settable = settable;
    }

    public String id() {
        return id;
    }

    /**
     * How many separate breaks this bone can be carrying at once.
     *
     * <p>Not a config value, because it is not a balance lever - it is how many ribs a
     * person has on one side, roughly, and a pack that wanted five would be arguing with
     * anatomy rather than with the mod.
     */
    public int count() {
        return count;
    }

    /** Whether a splint is the answer. When it is not, the answer is a surgical kit. */
    public boolean settable() {
        return settable;
    }
}
