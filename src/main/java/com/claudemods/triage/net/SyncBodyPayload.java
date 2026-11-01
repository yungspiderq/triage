package com.claudemods.triage.net;

import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;

/** Server -> client: the player's whole body. Sent on login and after anything changes. */
public record SyncBodyPayload(BodyHealth body) {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        Buffers.writeBody(buf, body());
    }

    public static SyncBodyPayload decode(FriendlyByteBuf buf) {
        return new SyncBodyPayload(Buffers.readBody(buf));
    }
}
