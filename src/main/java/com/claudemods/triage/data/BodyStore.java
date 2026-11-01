package com.claudemods.triage.data;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * Where a body is kept, on the one version that has nowhere to put it.
 *
 * <p>The 1.21 line hangs it off the player as an attachment and is done; Forge 1.20.1 has
 * capabilities, which are four classes saying the same thing. Fabric on 1.20.1 has neither -
 * the Attachment API arrived in 1.20.5 - so the body lives in a field added to Player by a
 * mixin, and this is the door to it.
 *
 * <p>Written out through the same {@link BodyHealth#CODEC} the other two trees use, under
 * the same tag name, so a body written by any build of this mod reads on the others. Nobody
 * asked for that and it costs nothing, which is the best kind of compatibility.
 */
public final class BodyStore {
    /** The tag the body is written under, inside the player's own. */
    public static final String TAG = "triage:body";

    /** Implemented by Player through PlayerMixin. */
    public interface Holder {
        BodyHealth triage$body();

        void triage$setBody(BodyHealth body);
    }

    private BodyStore() {
    }

    /** The one line everything above this asks. */
    public static BodyHealth of(Player player) {
        return player instanceof Holder holder ? holder.triage$body() : new BodyHealth();
    }

    public static void register() {
    }

    // ---- what the mixin calls --------------------------------------------

    public static void save(Holder holder, CompoundTag tag) {
        BodyHealth.CODEC.encodeStart(NbtOps.INSTANCE, holder.triage$body())
                .resultOrPartial(TriageMod.LOGGER::error)
                .ifPresent(written -> tag.put(TAG, written));
    }

    public static void load(Holder holder, CompoundTag tag) {
        if (!tag.contains(TAG)) {
            return;
        }
        Tag written = tag.get(TAG);
        BodyHealth.CODEC.parse(NbtOps.INSTANCE, written)
                .resultOrPartial(TriageMod.LOGGER::error)
                .ifPresent(holder::triage$setBody);
    }

    /**
     * A respawn that was not a death has to keep the body.
     *
     * <p>Dying is what heals you here and a fresh body is the reward for it, so nothing is
     * carried across one. Walking out of the End is not dying, and that is the one case
     * this exists to tell apart: getting it backwards would quietly hand every player a
     * full heal for stepping through a portal.
     */
    public static void copy(Player from, Player to, boolean alive) {
        if (alive && from instanceof Holder old && to instanceof Holder fresh) {
            fresh.triage$setBody(old.triage$body());
        }
    }
}
