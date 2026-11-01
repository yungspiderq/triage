package com.claudemods.triage.net;

import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server -> client: the screen where a free thing gets earned.
 *
 * <p>Carries a copy of the body it is being done to, the same way a treatment does, so
 * the screen can say whose chest it is without having to ask for it again - and the part
 * it was opened for, because a body can carry several burns at different depths, and a
 * screen that draws the worst one while the server reports the one that was clicked is a
 * screen that lies.
 */
public record OpenAidPayload(AidKind kind, int patientId, BodyHealth body,
                             Optional<BodyPart> part) {

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind());
        buf.writeVarInt(patientId());
        Buffers.writeBody(buf, body());
        buf.writeOptional(part(), FriendlyByteBuf::writeEnum);
    }

    public static OpenAidPayload decode(FriendlyByteBuf buf) {
        return new OpenAidPayload(buf.readEnum(AidKind.class), buf.readVarInt(),
                Buffers.readBody(buf), buf.readOptional(b -> b.readEnum(BodyPart.class)));
    }
}
