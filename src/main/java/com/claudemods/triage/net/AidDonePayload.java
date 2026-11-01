package com.claudemods.triage.net;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.AidKind;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client to server: the hands did their part, and this is how well.
 *
 * <p>The number is a client talking about its own screen, which is the same bargain
 * every treatment in this mod already makes. What stops it being a free heal is the note
 * the server kept when it opened that screen: which kind, on whom, and not long ago. All
 * this packet can do is arrive early or claim to have been tidier than it was.
 */
public record AidDonePayload(AidKind kind, float quality) {


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer helper = context.getSender();
        if (helper != null) {
            Aid.finish(helper, kind, quality);
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind());
        buf.writeFloat(quality());
    }

    public static AidDonePayload decode(FriendlyByteBuf buf) {
        return new AidDonePayload(buf.readEnum(AidKind.class), buf.readFloat());
    }
}
