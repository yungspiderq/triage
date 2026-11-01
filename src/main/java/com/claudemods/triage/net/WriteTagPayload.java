package com.claudemods.triage.net;

import com.claudemods.triage.net.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Tags;
import com.claudemods.triage.body.TriageTag;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Client -> server: write this on that patient, or take my own off.
 *
 * <p>An empty tag means the sender is tearing their own off, which is the only thing
 * anybody may do to their own mark. Everything else the server checks for itself -
 * whether the patient is really there, really in reach, and whether the sender is
 * actually carrying a tag to spend - because a packet is a wish, not a fact.
 */
public record WriteTagPayload(int patientId, Optional<TriageTag> tag) {


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer author = context.getSender();
            if (author == null) {
                return;
            }
        if (tag.isEmpty()) {
            Tags.tearOff(author);
            return;
        }
        Entity target = author.level().getEntity(patientId);
        if (target instanceof ServerPlayer patient) {
            Tags.write(author, patient, tag.get());
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(patientId());
        buf.writeOptional(tag(), FriendlyByteBuf::writeEnum);
    }

    public static WriteTagPayload decode(FriendlyByteBuf buf) {
        return new WriteTagPayload(buf.readVarInt(), buf.readOptional(b -> b.readEnum(TriageTag.class)));
    }
}
