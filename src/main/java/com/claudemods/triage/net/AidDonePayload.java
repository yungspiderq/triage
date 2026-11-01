package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.AidKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: the hands did their part, and this is how well.
 *
 * <p>The number is a client talking about its own screen, which is the same bargain
 * every treatment in this mod already makes. What stops it being a free heal is the note
 * the server kept when it opened that screen: which kind, on whom, and not long ago. All
 * this packet can do is arrive early or claim to have been tidier than it was.
 */
public record AidDonePayload(AidKind kind, float quality) implements CustomPacketPayload {
    public static final Type<AidDonePayload> TYPE = new Type<>(TriageMod.id("aid_done"));

    public static final StreamCodec<ByteBuf, AidDonePayload> STREAM_CODEC = StreamCodec.composite(
            AidKind.STREAM_CODEC, AidDonePayload::kind,
            ByteBufCodecs.FLOAT, AidDonePayload::quality,
            AidDonePayload::new);

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer helper) {
            Aid.finish(helper, kind, quality);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
