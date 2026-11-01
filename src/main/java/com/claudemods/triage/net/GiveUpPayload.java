package com.claudemods.triage.net;

import com.claudemods.triage.net.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Downed;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client -> server: stop waiting for a rescue that is not coming.
 *
 * <p>Held rather than pressed on the client side, so this only arrives when somebody
 * meant it. The server still checks they are actually on the floor.
 */
public record GiveUpPayload() {


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            Downed.giveUp(player);
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        // Nothing to say. The packet is the message.
    }

    public static GiveUpPayload decode(FriendlyByteBuf buf) {
        return new GiveUpPayload();
    }
}
