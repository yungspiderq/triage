package com.claudemods.triage.net;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Carry;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client -> server: set down whoever I am carrying.
 *
 * <p>Shares its key with giving up, which costs nothing: a player who is carrying
 * somebody is by definition on their feet, and a player on the floor is carrying nobody.
 * One key, one meaning - let go.
 */
public record PutDownPayload() {


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            Carry.putDown(player);
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        // Nothing to say. The packet is the message.
    }

    public static PutDownPayload decode(FriendlyByteBuf buf) {
        return new PutDownPayload();
    }
}
