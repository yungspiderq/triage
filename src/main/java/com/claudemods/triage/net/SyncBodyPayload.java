package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player's whole body. Sent on login and after anything changes. */
public record SyncBodyPayload(BodyHealth body) implements CustomPacketPayload {
    public static final Type<SyncBodyPayload> TYPE = new Type<>(TriageMod.id("sync_body"));

    public static final StreamCodec<ByteBuf, SyncBodyPayload> STREAM_CODEC =
            BodyHealth.STREAM_CODEC.map(SyncBodyPayload::new, SyncBodyPayload::body);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
