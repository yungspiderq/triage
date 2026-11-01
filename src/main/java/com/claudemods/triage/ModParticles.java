package com.claudemods.triage;

/*
 * Translated from the Forge 1.20.1 tree by tools/registries.py. What each thing is, and
 * why, is the same file there; only the lines that hand it to a registry are different.
 */

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mod's own particles.
 *
 * <p>Two, and they are the same drop at two moments: one falling and one already on the
 * ground. Splitting them is not decoration - the trail is re-sent every second so somebody
 * arriving late can still follow it, and re-dropping a drop from head height every second
 * would be rain rather than a trail.
 */
public final class ModParticles {

    /** Coming off a wound. */
    public static final Supplier<SimpleParticleType> BLOOD =
            particle("blood", FabricParticleTypes.simple(false));
    /** Already down, and drying. */
    public static final Supplier<SimpleParticleType> STAIN =
            particle("stain", FabricParticleTypes.simple(false));

    private ModParticles() {
    }

    /** Touching the class is what registers it; called once from the initialiser. */
    public static void register() {
    }

    private static Supplier<SimpleParticleType> particle(String name, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, TriageMod.id(name), type);
        return () -> type;
    }
}
