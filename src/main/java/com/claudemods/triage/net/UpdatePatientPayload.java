package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the patient on your treatment screen has changed.
 *
 * <p>Their body used to travel once, when the screen opened, and then go stale while you
 * worked. Over a long operation that is exactly when it matters: they are still bleeding
 * while you dig, and the figure you are looking at should say so.
 */
public record UpdatePatientPayload(BodyHealth body) implements CustomPacketPayload {
    public static final Type<UpdatePatientPayload> TYPE = new Type<>(TriageMod.id("update_patient"));

    public static final StreamCodec<ByteBuf, UpdatePatientPayload> STREAM_CODEC =
            BodyHealth.STREAM_CODEC.map(UpdatePatientPayload::new, UpdatePatientPayload::body);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
