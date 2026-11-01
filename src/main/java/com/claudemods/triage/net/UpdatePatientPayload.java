package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;

/**
 * Server -> client: the patient on your treatment screen has changed.
 *
 * <p>Their body used to travel once, when the screen opened, and then go stale while you
 * worked. Over a long operation that is exactly when it matters: they are still bleeding
 * while you dig, and the figure you are looking at should say so.
 */
public record UpdatePatientPayload(BodyHealth body) {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        Buffers.writeBody(buf, body());
    }

    public static UpdatePatientPayload decode(FriendlyByteBuf buf) {
        return new UpdatePatientPayload(Buffers.readBody(buf));
    }
}
