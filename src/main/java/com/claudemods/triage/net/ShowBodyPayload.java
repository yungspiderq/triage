package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: here is what that person looks like.
 *
 * <p>A look, not a treatment. Nothing is spent, nothing is started and nothing is
 * promised - which is the point, because until this the only way to find out how badly
 * somebody was hurt was to start working on them.
 *
 * @param patientId entity id of whoever was looked at, for their name
 */
public record ShowBodyPayload(int patientId, BodyHealth body) implements CustomPacketPayload {
    public static final Type<ShowBodyPayload> TYPE = new Type<>(TriageMod.id("show_body"));

    public static final StreamCodec<ByteBuf, ShowBodyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShowBodyPayload::patientId,
            BodyHealth.STREAM_CODEC, ShowBodyPayload::body,
            ShowBodyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
