package com.claudemods.triage.item;

import net.minecraft.nbt.CompoundTag;
import com.claudemods.triage.body.BloodType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Reading and writing what is in a bag. */
public final class BloodBag {
    private BloodBag() {
    }

    /**
     * The tag the type lives under.
     *
     * <p>1.20.1 has no data components, so what is one line and a registered type on the
     * other side is a string in the stack's NBT here. Same fact, older filing cabinet -
     * and the name is written out once so a typo cannot put blood in a bag that nothing
     * can read it back out of.
     */
    private static final String TAG = "triage_blood";

    /** Whose blood this stack holds, or null when the bag is empty. */
    public static @Nullable BloodType typeOf(ItemStack stack) {
        if (!(stack.getItem() instanceof BloodBagItem)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG)) {
            return null;
        }
        return BloodType.byId(tag.getString(TAG));
    }

    public static boolean filled(ItemStack stack) {
        return typeOf(stack) != null;
    }

    public static ItemStack of(BloodType type) {
        ItemStack stack = new ItemStack(ModItems.BLOOD_BAG.get());
        stack.getOrCreateTag().putString(TAG, type.id());
        return stack;
    }
}
