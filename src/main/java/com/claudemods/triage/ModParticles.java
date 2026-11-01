package com.claudemods.triage;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own particles.
 *
 * <p>Two, and they are the same drop at two moments: one falling and one already on the
 * ground. Splitting them is not decoration - the trail is re-sent every second so somebody
 * arriving late can still follow it, and re-dropping a drop from head height every second
 * would be rain rather than a trail.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TriageMod.MODID);

    /** Coming off a wound. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD =
            PARTICLES.register("blood", () -> new SimpleParticleType(false));
    /** Already down, and drying. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAIN =
            PARTICLES.register("stain", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
