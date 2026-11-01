package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;

/** Server -> client: whatever you were treating, you have been knocked off it. */
public record CloseTreatmentPayload() {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        // Nothing to say. The packet is the message.
    }

    public static CloseTreatmentPayload decode(FriendlyByteBuf buf) {
        return new CloseTreatmentPayload();
    }
}
