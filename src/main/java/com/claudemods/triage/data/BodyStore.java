package com.claudemods.triage.data;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where a body lives on 1.20.1.
 *
 * <p>The other line of this mod hangs one off a player with an attachment: four lines,
 * a codec, and a flag saying whether it survives dying. There is no such thing here, so
 * this is the same idea assembled out of what 1.20.1 has - a capability, a provider, an
 * event that bolts the provider on, and a second event that copies it across a respawn.
 *
 * <p>All four of those exist to do what {@code AttachmentType.builder(...).serialize(...)}
 * did on the other side, and the reason it is worth writing out is that <b>everything
 * above it stays the same</b>. The codec is the same codec. {@code Body.of(player)} is
 * still one line. Nothing in the injuries, the illnesses or the organs knows or cares
 * which of the two is underneath it, and keeping it that way is the whole point of doing
 * the floor first.
 */
@Mod.EventBusSubscriber(modid = TriageMod.MODID)
public final class BodyStore {
    public static final Capability<Holder> BODY = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation ID = TriageMod.id("body");

    private BodyStore() {
    }

    /** The one line everything above this asks. */
    public static BodyHealth of(Player player) {
        return player.getCapability(BODY)
                .map(Holder::body)
                .orElseGet(BodyHealth::new);
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(Holder.class);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new Provider());
        }
    }

    /**
     * Dying is what heals you here, and a fresh body is the reward for it - so nothing
     * is carried across a death.
     *
     * <p>A respawn through the End portal is not a death and has to keep everything,
     * which is the one case this event exists to tell apart. Getting that backwards would
     * quietly hand every player a full heal for walking through a portal.
     */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(BODY).ifPresent(old ->
                    event.getEntity().getCapability(BODY).ifPresent(fresh ->
                            fresh.set(old.body())));
            event.getOriginal().invalidateCaps();
        }
    }

    /** The thing itself, and the only mutable state in here. */
    public static final class Holder {
        private BodyHealth body = new BodyHealth();

        public BodyHealth body() {
            return body;
        }

        public void set(BodyHealth other) {
            this.body = other;
        }
    }

    /**
     * The plumbing: hands the capability out, and writes it down as the codec says.
     *
     * <p>Straight NBT through the same {@code BodyHealth.CODEC} the other line uses, so
     * a body written by one build reads on the other. That is not a goal anybody asked
     * for and it costs nothing, which is the best kind.
     */
    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final Holder holder = new Holder();
        private final LazyOptional<Holder> optional = LazyOptional.of(() -> holder);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                          @Nullable Direction side) {
            return BODY.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            Tag written = BodyHealth.CODEC.encodeStart(NbtOps.INSTANCE, holder.body())
                    .resultOrPartial(TriageMod.LOGGER::error)
                    .orElse(new CompoundTag());
            CompoundTag tag = new CompoundTag();
            tag.put("body", written);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            if (!tag.contains("body")) {
                return;
            }
            BodyHealth.CODEC.parse(NbtOps.INSTANCE, tag.get("body"))
                    .resultOrPartial(TriageMod.LOGGER::error)
                    .ifPresent(holder::set);
        }
    }
}
