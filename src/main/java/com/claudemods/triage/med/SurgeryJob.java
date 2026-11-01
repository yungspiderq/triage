package com.claudemods.triage.med;

import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.PartState;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What the surgical kit is actually being opened for.
 *
 * <p>For eight versions the kit was one item, one screen and one game for six different
 * operations. A limb that had been beaten to nothing, a spleen with a hole in it, a burn
 * that had to be cut off, a skull that had to be opened, a leg with the bone outside it
 * and a bone that healed in the wrong shape - all of them were "use the surgical kit" and
 * all of them were the same two minutes of clamping bleeders.
 *
 * <p>Which quietly said the wrong thing twice. It said these are the same problem, and it
 * said the answer to all of them is the same set of hands doing the same thing. Neither is
 * true, and the mod spends the rest of its text insisting that the difference between two
 * injuries <b>is</b> the difference between what you do about them.
 *
 * <p>So there are six now, and this enum is the one place that decides which. Both the
 * screen that picks the game and the server that decides the outcome read it, so the two
 * can never disagree about what operation is happening - which is the failure that would
 * otherwise be waiting: a player playing one game and a server performing another.
 *
 * <p>The order below is the order of the checks, and it is triage: what kills soonest goes
 * first, and the elective one goes last.
 */
public enum SurgeryJob {
    /**
     * A bone that has been outside. Wash it out, take out what came in with it, close it.
     *
     * <p>First because it is a race rather than a repair: the wound is open to the world
     * and every second of it is the fastest road to infection the mod has.
     */
    IRRIGATE("irrigate"),
    /**
     * A skull or a spine. Neither takes a splint, and what both want is the pressure off
     * whatever is underneath before it does the damage that does not come back.
     */
    TREPHINE("trephine"),
    /**
     * Something torn inside. Find the tear on a surface that will not hold still, and
     * sew it - which is the only operation here done against a clock the patient sets
     * with their own breathing.
     */
    ORGAN("organ"),
    /**
     * A burn through the skin. Cut down until it bleeds, because bleeding is the only
     * sign there is that what is under the knife is still alive.
     */
    EXCISE("excise"),
    /**
     * A limb beaten to nothing: open it, stop what is filling it, close it. The original
     * operation, and still the one the word means when nobody says which.
     */
    DEBRIDE("debride"),
    /**
     * And the one that is not an emergency at all: a bone that healed crooked, cut and
     * broken again on purpose so that somebody can set it straight this time.
     */
    OSTEOTOMY("osteotomy");

    private final String id;

    SurgeryJob(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component title() {
        return Component.translatable("triage.surgery." + id);
    }

    /** One line on the screen saying which of the six this is. */
    public Component hint() {
        return Component.translatable("triage.surgery." + id + ".hint");
    }

    /**
     * Which operation this part is asking for, worst first, or null for none.
     *
     * <p>One at a time and deliberately. A surgeon with a chest open does deal with what
     * they find in there - which is why the organ job repairs everything torn in the part
     * rather than one thing - but a leg with the bone outside it and a burn on the same
     * leg is two operations, because it is two operations.
     */
    public static @Nullable SurgeryJob of(BodyPart part, PartState state) {
        if (state.openFracture()) {
            return IRRIGATE;
        }
        if (state.fractured() && !part.bone().settable()) {
            return TREPHINE;
        }
        if (state.organHurt()) {
            return ORGAN;
        }
        if (state.burnFull()) {
            return EXCISE;
        }
        if (state.blacked()) {
            return DEBRIDE;
        }
        if (state.bent()) {
            return OSTEOTOMY;
        }
        return null;
    }
}
