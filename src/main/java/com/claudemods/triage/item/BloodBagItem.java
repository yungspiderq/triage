package com.claudemods.triage.item;

import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.med.MedicineKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * One item that is two treatments: empty it draws blood, full it gives it.
 *
 * <p>Which one it is depends on what is in it, which is why {@link #kind(ItemStack)}
 * exists at all. It reads better in the hand than two items would - you carry bags, and
 * whether a bag is worth anything depends on what is in it and who is bleeding.
 */
public class BloodBagItem extends MedicineItem {
    public BloodBagItem(Properties properties) {
        super(MedicineKind.TRANSFUSION, properties);
    }

    @Override
    public MedicineKind kind(ItemStack stack) {
        return BloodBag.filled(stack) ? MedicineKind.TRANSFUSION : MedicineKind.DRAW;
    }

    /** A full bag says whose blood it is on the item itself, not only in the tooltip. */
    @Override
    public Component getName(ItemStack stack) {
        BloodType type = BloodBag.typeOf(stack);
        return type == null
                ? super.getName(stack)
                : Component.translatable("item.triage.blood_bag.filled", type.label());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        @Nullable BloodType type = BloodBag.typeOf(stack);
        if (type == null) {
            return;
        }
        tooltip.add(Component.translatable("triage.tooltip.blood_bag_type", type.title())
                .withStyle(ChatFormatting.GRAY));
        if (type.universalDonor()) {
            tooltip.add(Component.translatable("triage.tooltip.universal_donor")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
