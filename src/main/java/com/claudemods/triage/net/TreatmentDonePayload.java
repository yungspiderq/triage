package com.claudemods.triage.net;

import com.claudemods.triage.net.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.med.MedicineKind;
import com.claudemods.triage.med.Treatments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
                                   float quality) {
    /** Sent in place of a part ordinal when the treatment has no part. */
    private static final int NO_PART = -1;



    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> handleNow(context));
        context.setPacketHandled(true);
    }

    private void handleNow(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            Treatments.finish(player, kind, part, quality);
        }
    }

    /** Onto the wire, field by field, because 1.20.1 has nothing that does it for you. */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind());
        buf.writeVarInt(part() == null ? -1 : part().ordinal());
        buf.writeFloat(quality());
    }

    public static TreatmentDonePayload decode(FriendlyByteBuf buf) {
        return new TreatmentDonePayload(buf.readEnum(MedicineKind.class), readPart(buf), buf.readFloat());
    }

    /**
     * A part that might not be one.
     *
     * <p>Not every treatment has a limb - a transfusion is for the whole person - so the
     * ordinal is written with a sentinel rather than wrapped in an optional. It was that
     * on the other line too, and keeping it means the two builds put the same bytes on
     * the wire for the same packet.
     */
    private static BodyPart readPart(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        return ordinal < 0 ? null : BodyPart.VALUES[ordinal];
    }
}
