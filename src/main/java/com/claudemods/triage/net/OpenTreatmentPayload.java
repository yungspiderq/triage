package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.med.MedicineKind;
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
public record OpenTreatmentPayload(MedicineKind kind, int patientId, BodyHealth body) {


    /** True when this is somebody else's body on the screen. */
    public boolean onSomebodyElse() {
        return patientId >= 0;
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind());
        buf.writeVarInt(patientId());
        Buffers.writeBody(buf, body());
    }

    public static OpenTreatmentPayload decode(FriendlyByteBuf buf) {
        return new OpenTreatmentPayload(buf.readEnum(MedicineKind.class), buf.readVarInt(), Buffers.readBody(buf));
    }
}
