package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Carry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: set down whoever I am carrying.
 *
 * <p>Shares its key with giving up, which costs nothing: a player who is carrying
 * somebody is by definition on their feet, and a player on the floor is carrying nobody.
 * One key, one meaning - let go.
 */
public record PutDownPayload() implements CustomPacketPayload {
    public static final Type<PutDownPayload> TYPE = new Type<>(TriageMod.id("put_down"));

    public static final StreamCodec<ByteBuf, PutDownPayload> STREAM_CODEC = StreamCodec.unit(new PutDownPayload());

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Carry.putDown(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
