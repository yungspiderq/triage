package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.med.MedicineKind;
import com.claudemods.triage.med.Treatments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Client -> server: the treatment went well. The part is absent for the medicine that
 * is aimed at the whole body rather than at a limb. Nothing here is trusted -
 * {@link Treatments#finish} checks it against what the server let the player start.
 *
 * <p>The number on the end is how well, and almost nothing reads it: an item treatment
 * is pass or fail, because the bandage is spent either way and the wound is closed
 * either way. The saw is the exception. It cannot be failed - there is no putting it
 * down halfway - so the only thing left for doing it badly to cost is the patient, and
 * this is how that arrives.
 */
public record TreatmentDonePayload(MedicineKind kind, @Nullable BodyPart part,
                                   float quality) implements CustomPacketPayload {
    /** Sent in place of a part ordinal when the treatment has no part. */
    private static final int NO_PART = -1;

    public static final Type<TreatmentDonePayload> TYPE = new Type<>(TriageMod.id("treatment_done"));

    public static final StreamCodec<ByteBuf, TreatmentDonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.kind().ordinal(),
            ByteBufCodecs.VAR_INT, payload -> payload.part() == null ? NO_PART : payload.part().ordinal(),
            ByteBufCodecs.FLOAT, TreatmentDonePayload::quality,
            (kind, part, quality) -> new TreatmentDonePayload(
                    MedicineKind.VALUES[Mth.clamp(kind, 0, MedicineKind.VALUES.length - 1)],
                    part < 0 || part >= BodyPart.VALUES.length ? null : BodyPart.VALUES[part],
                    Mth.clamp(quality, 0.0F, 1.0F)));

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Treatments.finish(player, kind, part, quality);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
