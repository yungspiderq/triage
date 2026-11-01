package com.claudemods.triage.net;

import com.claudemods.triage.Config;
import com.claudemods.triage.TriageMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client, once, on joining: the server's own tuning.
 *
 * <p>This packet has no counterpart in the NeoForge tree because it does not need one -
 * a server config there is sent to the client by the loader. Fabric has no such idea, and
 * without this every client would be reading its own config/triage-server.toml: two people
 * in the same world would disagree about how much a broken leg costs, and the one drawing
 * the screens would be the one who is wrong.
 *
 * <p>Sent whole rather than as a diff. Three hundred numbers is a few kilobytes once per
 * join, which is nothing against getting it wrong.
 */
public record ConfigPayload(Config.Values values) implements CustomPacketPayload {
    public static final Type<ConfigPayload> TYPE = new Type<>(TriageMod.id("config"));

    public static final StreamCodec<FriendlyByteBuf, ConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> Config.encode(buf, payload.values()),
            buf -> new ConfigPayload(Config.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
