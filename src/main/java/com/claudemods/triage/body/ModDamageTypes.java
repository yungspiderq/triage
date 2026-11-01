package com.claudemods.triage.body;

import com.claudemods.triage.TriageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * The two ways this mod can kill someone by itself. They exist so that the death
 * message says what actually happened - "bled out" rather than a generic death - and
 * so the damage handler can recognise its own killing blow and let it through instead
 * of routing it into a body part all over again.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BLEEDING =
            ResourceKey.create(Registries.DAMAGE_TYPE, TriageMod.id("bleeding"));
    public static final ResourceKey<DamageType> PAIN =
            ResourceKey.create(Registries.DAMAGE_TYPE, TriageMod.id("pain"));
    public static final ResourceKey<DamageType> SEPSIS =
            ResourceKey.create(Registries.DAMAGE_TYPE, TriageMod.id("sepsis"));

    private ModDamageTypes() {
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    /** True for damage this mod deals to finish off a body it has already emptied. */
    public static boolean isInternal(DamageSource source) {
        return source.typeHolder().is(BLEEDING) || source.typeHolder().is(PAIN) || source.typeHolder().is(SEPSIS);
    }
}
