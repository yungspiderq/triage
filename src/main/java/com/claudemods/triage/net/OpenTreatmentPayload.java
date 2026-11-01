package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.med.MedicineKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;

/**
 * Server -> client: open the treatment screen.
 *
 * <p>The body travels with the packet rather than being read off the client, because
 * since 0.6.0 it might not be the client's own: nobody's client knows what state anyone
 * else's limbs are in until it is told.
 *
 * @param patientId entity id of the player being treated, or -1 when it is yourself
 */
public record OpenTreatmentPayload(MedicineKind kind, int patientId, BodyHealth body) implements CustomPacketPayload {
    public static final Type<OpenTreatmentPayload> TYPE = new Type<>(TriageMod.id("open_treatment"));

    public static final StreamCodec<ByteBuf, OpenTreatmentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.kind().ordinal(),
            ByteBufCodecs.VAR_INT, OpenTreatmentPayload::patientId,
            BodyHealth.STREAM_CODEC, OpenTreatmentPayload::body,
            (kind, patient, body) -> new OpenTreatmentPayload(
                    MedicineKind.VALUES[Mth.clamp(kind, 0, MedicineKind.VALUES.length - 1)], patient, body));

    /** True when this is somebody else's body on the screen. */
    public boolean onSomebodyElse() {
        return patientId >= 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
