package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Putting things on the wire on 1.20.1.
 *
 * <p>The other line of this mod describes a packet with a {@code StreamCodec} and never
 * writes a byte by hand. There is no such thing here, so the shapes are written out - and
 * the one thing worth not writing out is a whole body, which already has a codec for
 * saving to disk.
 *
 * <p>So a body goes over the wire as the tag it saves as. Slower than a purpose-built
 * encoder and shorter than one by about two hundred lines, and it cannot drift from the
 * save format, because it <em>is</em> the save format.
 */
public final class Buffers {
    private Buffers() {
    }

    public static void writeBody(FriendlyByteBuf buf, BodyHealth body) {
        buf.writeNbt(write(BodyHealth.CODEC, body));
    }

    public static BodyHealth readBody(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            return new BodyHealth();
        }
        return BodyHealth.CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(TriageMod.LOGGER::error)
                .orElseGet(BodyHealth::new);
    }

    private static <T> CompoundTag write(Codec<T> codec, T value) {
        Tag written = codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(TriageMod.LOGGER::error)
                .orElseGet(CompoundTag::new);
        return written instanceof CompoundTag compound ? compound : new CompoundTag();
    }
}
