package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.TriageTag;
import java.util.Optional;

/**
 * Server -> client: what that player is wearing, and whether they are on the floor.
 *
 * <p>The one thing about somebody else's body that every client is told. The chart
 * itself never leaves its owner without somebody walking over and looking, and that is
 * the point of it - but a mark is pinned to a chest and a body lying in the mud is a
 * body lying in the mud, and both of those are visible from across a field.
 *
 * <p>Sent when the mark changes, and to each client as it starts seeing the player.
 * Staleness is decided by the server rather than counted down here, so two players
 * looking at the same nameplate never disagree about whether it is old.
 *
 * <p>The limbs ride along on the same packet for the same reason the mark does: an arm
 * that is not there is not private. It is the one fact about somebody else's body that
 * needs no walking over, no screen and no permission, because it is the shape of them.
 */
public record SyncTagPayload(int entityId, Optional<TriageTag> tag, boolean stale, boolean downed,
                             long marks, long worn) {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId());
        buf.writeOptional(tag(), FriendlyByteBuf::writeEnum);
        buf.writeBoolean(stale());
        buf.writeBoolean(downed());
        buf.writeVarLong(marks());
        // The second number: what is tied on over the top, and what is left of what used
        // to be wrong. Written last, so a client on the old build reading a new packet
        // would stop at exactly the point where it used to end.
        buf.writeVarLong(worn());
    }

    public static SyncTagPayload decode(FriendlyByteBuf buf) {
        return new SyncTagPayload(buf.readVarInt(), buf.readOptional(b -> b.readEnum(TriageTag.class)), buf.readBoolean(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong());
    }
}
