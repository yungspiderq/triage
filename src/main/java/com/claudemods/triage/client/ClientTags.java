package com.claudemods.triage.client;

import com.claudemods.triage.body.LimbMark;
import com.claudemods.triage.body.TriageTag;
import com.claudemods.triage.net.SyncTagPayload;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * What this client knows about other people's bodies, which is deliberately almost
 * nothing: the mark somebody pinned on them and whether they are on the floor.
 *
 * <p>Everything else still costs a walk over and a look. Kept by entity id rather than
 * by name because that is what the renderer has in its hand when it comes to draw a
 * nameplate, and dropped the moment the entity goes.
 */
public final class ClientTags {
    /** @param stale decided by the server, so no two clients disagree about the age */
    public record Mark(@Nullable TriageTag tag, boolean stale, boolean downed, long marks, long worn) {
    }

    private static final Map<Integer, Mark> MARKS = new HashMap<>();

    private ClientTags() {
    }

    public static void accept(SyncTagPayload payload) {
        Mark mark = new Mark(payload.tag().orElse(null), payload.stale(), payload.downed(),
                payload.marks(), payload.worn());
        if (mark.tag() == null && !mark.downed() && mark.marks() == 0 && mark.worn() == 0) {
            MARKS.remove(payload.entityId());
            return;
        }
        MARKS.put(payload.entityId(), mark);
    }

    /**
     * What that player is missing, as {@link LimbMark} bits. Zero for anybody this
     * client has never been told anything about, which is almost everybody.
     */
    public static int limbs(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.limbsOf(mark.marks());
    }

    /** How white that player has gone: 0 to 3, and 0 for almost everybody. */
    public static int pale(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.paleOf(mark.worn());
    }

    /** Which of that player's parts have something standing out of them. */
    public static int blades(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.bladesOf(mark.worn());
    }

    /** And how blue, which is a different emergency with a different answer. */
    public static int blue(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.blueOf(mark.worn());
    }

    /** What is tied over them, as {@link LimbMark} dressing bits. */
    public static int dressings(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.dressingsOf(mark.marks());
    }

    /** Which limbs have died on the body, as {@link LimbMark} bits. */
    public static int dying(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.dyingOf(mark.marks());
    }

    /** How burnt each part is, as {@link LimbMark} burn bits. */
    public static int burns(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.burnsOf(mark.marks());
    }

    /** Which limbs are strapped, as {@link LimbMark} strap bits. */
    public static int straps(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.strapsOf(mark.marks());
    }

    /** Where they are bleeding, as {@link LimbMark} bleed bits. */
    public static int bleeds(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.bleedsOf(mark.marks());
    }

    /** Which parts have a stick tied to them, as {@link LimbMark} splint bits. */
    public static int splints(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.splintsOf(mark.worn());
    }

    /** And which carry something that outlived the wound, as {@link LimbMark} scar bits. */
    public static int scars(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.scarsOf(mark.worn());
    }

    /** What shape the bones in each limb are, as {@link LimbMark} bone bits. */
    public static int bones(int entityId) {
        Mark mark = MARKS.get(entityId);
        return mark == null ? 0 : LimbMark.bonesOf(mark.worn());
    }

    public static @Nullable Mark of(int entityId) {
        return MARKS.get(entityId);
    }

    public static void forget(int entityId) {
        MARKS.remove(entityId);
    }

    public static void reset() {
        MARKS.clear();
    }
}
