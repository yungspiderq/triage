package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Tags;
import com.claudemods.triage.body.TriageTag;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: write this on that patient, or take my own off.
 *
 * <p>An empty tag means the sender is tearing their own off, which is the only thing
 * anybody may do to their own mark. Everything else the server checks for itself -
 * whether the patient is really there, really in reach, and whether the sender is
 * actually carrying a tag to spend - because a packet is a wish, not a fact.
 */
public record WriteTagPayload(int patientId, Optional<TriageTag> tag) implements CustomPacketPayload {
    public static final Type<WriteTagPayload> TYPE = new Type<>(TriageMod.id("write_tag"));

    public static final StreamCodec<ByteBuf, WriteTagPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WriteTagPayload::patientId,
            ByteBufCodecs.optional(TriageTag.STREAM_CODEC), WriteTagPayload::tag,
            WriteTagPayload::new);

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer author)) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
