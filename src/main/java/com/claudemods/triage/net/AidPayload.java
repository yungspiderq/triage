package com.claudemods.triage.net;

import com.claudemods.triage.net.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyPart;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Client to server: my hands are on that patient.
 *
 * <p>Sent from the chart, which is where the reason to do either of these is written
 * down. Nothing about the state that allows it is trusted from here - whether they are
 * really down, really in arrest and really within arm's reach is all decided on the
 * server, because a packet is a wish and not a fact.
 */
public record AidPayload(int patientId, AidKind kind, Optional<BodyPart> part) {


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer helper = context.getSender();
            if (helper == null) {
                return;
            }
        // Nothing on their own chart is addressed to anybody else, and -1 is how that
        // chart says so: it never knew an entity id for its own body.
        if (patientId < 0) {
            Aid.give(helper, helper, kind, part.orElse(null));
            return;
        }
        Entity target = helper.level().getEntity(patientId);
        if (target instanceof ServerPlayer patient) {
            Aid.give(helper, patient, kind, part.orElse(null));
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(patientId());
        buf.writeEnum(kind());
        buf.writeOptional(part(), FriendlyByteBuf::writeEnum);
    }

    public static AidPayload decode(FriendlyByteBuf buf) {
        return new AidPayload(buf.readVarInt(), buf.readEnum(AidKind.class), buf.readOptional(b -> b.readEnum(BodyPart.class)));
    }
}
