package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the screen where a free thing gets earned.
 *
 * <p>Carries a copy of the body it is being done to, the same way a treatment does, so
 * the screen can say whose chest it is without having to ask for it again.
 */
public record OpenAidPayload(AidKind kind, int patientId, BodyHealth body,
                            Optional<BodyPart> part) implements CustomPacketPayload {
    public static final Type<OpenAidPayload> TYPE = new Type<>(TriageMod.id("open_aid"));

    public static final StreamCodec<ByteBuf, OpenAidPayload> STREAM_CODEC = StreamCodec.composite(
            AidKind.STREAM_CODEC, OpenAidPayload::kind,
            ByteBufCodecs.VAR_INT, OpenAidPayload::patientId,
            BodyHealth.STREAM_CODEC, OpenAidPayload::body,
            ByteBufCodecs.optional(BodyPart.STREAM_CODEC), OpenAidPayload::part,
            OpenAidPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
