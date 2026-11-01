package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.TriageTag;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
                             long marks, long worn) implements CustomPacketPayload {
    public static final Type<SyncTagPayload> TYPE = new Type<>(TriageMod.id("sync_tag"));

    public static final StreamCodec<ByteBuf, SyncTagPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncTagPayload::entityId,
            ByteBufCodecs.optional(TriageTag.STREAM_CODEC), SyncTagPayload::tag,
            ByteBufCodecs.BOOL, SyncTagPayload::stale,
            ByteBufCodecs.BOOL, SyncTagPayload::downed,
            // A long rather than an int: four masks at two bits a part came to forty-four
            // bits, and the codec takes six fields and already has five. Widening the one
            // that was there beat inventing a sixth.
            ByteBufCodecs.VAR_LONG, SyncTagPayload::marks,
            // And the sixth, which is the last one this codec will take: what is tied on
            // over the top, and what is left of what used to be wrong.
            ByteBufCodecs.VAR_LONG, SyncTagPayload::worn,
            SyncTagPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
