package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Downed;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: stop waiting for a rescue that is not coming.
 *
 * <p>Held rather than pressed on the client side, so this only arrives when somebody
 * meant it. The server still checks they are actually on the floor.
 */
public record GiveUpPayload() implements CustomPacketPayload {
    public static final Type<GiveUpPayload> TYPE = new Type<>(TriageMod.id("give_up"));

    public static final StreamCodec<ByteBuf, GiveUpPayload> STREAM_CODEC = StreamCodec.unit(new GiveUpPayload());

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Downed.giveUp(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
