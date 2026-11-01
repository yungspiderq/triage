package com.claudemods.triage.net;

import com.claudemods.triage.body.BodyHealth;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server -> client: the picture, and whose arm it is.
 *
 * <p>The same body the chart travels in, sent to one person: whoever pressed the machine.
 * A film is not published to the room - it is a thing somebody took and is now holding,
 * and the only mark it leaves on anybody else is the line on the notes saying a film
 * exists.
 *
 * <p>Hand-written like every packet on this line, and for the same reason: there is no
 * stream codec here, so a message says how it goes onto the wire itself.
 *
 * @param patientId entity id of whoever was standing at the plate, for the name on it
 */
public record ShowFilmPayload(int patientId, BodyHealth body) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(patientId());
        Buffers.writeBody(buf, body());
    }

    public static ShowFilmPayload decode(FriendlyByteBuf buf) {
        return new ShowFilmPayload(buf.readVarInt(), Buffers.readBody(buf));
    }
}
