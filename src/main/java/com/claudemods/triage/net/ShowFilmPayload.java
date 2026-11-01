package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the picture, and whose arm it is.
 *
 * <p>The same body the chart travels in, sent to one person: whoever pressed the machine.
 * A film is not published to the room - it is a thing somebody took and is now holding,
 * and the only mark it leaves on anybody else is the line on the notes saying a film
 * exists.
 *
 * @param patientId entity id of whoever was standing at the plate, for the name on it
 */
public record ShowFilmPayload(int patientId, BodyHealth body) implements CustomPacketPayload {
    public static final Type<ShowFilmPayload> TYPE = new Type<>(TriageMod.id("show_film"));

    public static final StreamCodec<ByteBuf, ShowFilmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShowFilmPayload::patientId,
            BodyHealth.STREAM_CODEC, ShowFilmPayload::body,
            ShowFilmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
