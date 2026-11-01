package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;

/**
 * Server -> client: here is what that person looks like.
 *
 * <p>A look, not a treatment. Nothing is spent, nothing is started and nothing is
 * promised - which is the point, because until this the only way to find out how badly
 * somebody was hurt was to start working on them.
 *
 * @param patientId entity id of whoever was looked at, for their name
 */
public record ShowBodyPayload(int patientId, BodyHealth body) {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(patientId());
        Buffers.writeBody(buf, body());
    }

    public static ShowBodyPayload decode(FriendlyByteBuf buf) {
        return new ShowBodyPayload(buf.readVarInt(), Buffers.readBody(buf));
    }
}
