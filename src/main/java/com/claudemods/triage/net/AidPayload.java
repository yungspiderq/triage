package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyPart;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.claudemods.triage.net.IPayloadContext;

/**
 * Client to server: my hands are on that patient.
 *
 * <p>Sent from the chart, which is where the reason to do either of these is written
 * down. Nothing about the state that allows it is trusted from here - whether they are
 * really down, really in arrest and really within arm's reach is all decided on the
 * server, because a packet is a wish and not a fact.
 */
public record AidPayload(int patientId, AidKind kind, Optional<BodyPart> part) implements CustomPacketPayload {
    public static final Type<AidPayload> TYPE = new Type<>(TriageMod.id("aid"));

    public static final StreamCodec<ByteBuf, AidPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AidPayload::patientId,
            AidKind.STREAM_CODEC, AidPayload::kind,
            ByteBufCodecs.optional(BodyPart.STREAM_CODEC), AidPayload::part,
            AidPayload::new);

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer helper)) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
