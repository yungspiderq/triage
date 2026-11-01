package com.claudemods.triage.item;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Allergy;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.body.BodyPart;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What a stack can carry beyond being itself.
 *
 * <p>Two things. Whose blood is in the bag - a component rather than eight separate
 * items, so a filled bag is the same item as an empty one and two bags of the same type
 * still stack, which they should and which eight items would have made impossible.
 *
 * <p>And how dirty an instrument is, which is the same argument twice: a used saw is the
 * same saw, and nobody should end up with two stacks of them because one has been through
 * somebody.
 */
public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TriageMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BloodType>> BLOOD =
            TYPES.register("blood", () -> DataComponentType.<BloodType>builder()
                    .persistent(BloodType.CODEC)
                    .networkSynchronized(BloodType.STREAM_CODEC)
                    .build());

    /**
     * How many patients this instrument has been through since it was last cleaned.
     *
     * <p>The three tools here are carried rather than spent, which until now meant they
     * cost nothing at all after the day they were made. That leaves out the one thing
     * about old surgery everybody knows: the instrument goes from one person into the
     * next, and for most of the history of medicine that killed more people than anything
     * a surgeon did on purpose.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GRIME =
            TYPES.register("grime", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * Which limb a severed one is, and how long it has left.
     *
     * <p>Two components on one item rather than four items, for the third time and the
     * same reason: a left arm and a right leg are the same object with a different label,
     * and the label is what decides where it can go back on.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BodyPart>> LIMB =
            TYPES.register("limb", () -> DataComponentType.<BodyPart>builder()
                    .persistent(BodyPart.CODEC)
                    .networkSynchronized(BodyPart.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FRESH =
            TYPES.register("fresh", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** What is stamped on a bracelet, which is the one thing an unconscious body can say. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Allergy>> ALLERGY =
            TYPES.register("allergy", () -> DataComponentType.<Allergy>builder()
                    .persistent(Allergy.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(Allergy.CODEC))
                    .build());

    private ModComponents() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
