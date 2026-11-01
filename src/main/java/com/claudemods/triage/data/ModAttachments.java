package com.claudemods.triage.data;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TriageMod.MODID);

    /**
     * Deliberately not {@code copyOnDeath}: dying is what heals you here, and a fresh
     * body is the reward for it. Carrying it over is one config flag away if a pack
     * ever wants a harsher version.
     */
    public static final Supplier<AttachmentType<BodyHealth>> BODY = ATTACHMENT_TYPES.register(
            "body", () -> AttachmentType.builder(BodyHealth::new)
                    .serialize(BodyHealth.CODEC)
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
