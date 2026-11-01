package com.claudemods.triage.item;

import com.claudemods.triage.body.BloodType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Reading and writing what is in a bag. */
public final class BloodBag {
    private BloodBag() {
    }

    /** Whose blood this stack holds, or null when the bag is empty. */
    public static @Nullable BloodType typeOf(ItemStack stack) {
        return stack.getItem() instanceof BloodBagItem ? stack.get(ModComponents.BLOOD.get()) : null;
    }

    public static boolean filled(ItemStack stack) {
        return typeOf(stack) != null;
    }

    public static ItemStack of(BloodType type) {
        ItemStack stack = new ItemStack(ModItems.BLOOD_BAG.get());
        stack.set(ModComponents.BLOOD.get(), type);
        return stack;
    }
}
