package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: whatever you were treating, you have been knocked off it. */
public record CloseTreatmentPayload() implements CustomPacketPayload {
    public static final Type<CloseTreatmentPayload> TYPE = new Type<>(TriageMod.id("close_treatment"));

    public static final StreamCodec<ByteBuf, CloseTreatmentPayload> STREAM_CODEC =
            StreamCodec.unit(new CloseTreatmentPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
