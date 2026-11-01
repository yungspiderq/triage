package com.claudemods.triage.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A blank tag, waiting for somebody to write on it.
 *
 * <p>The item does nothing by itself, which is the honest shape of it: a tag is not a
 * treatment and holding one heals nobody. It is here to be spent when somebody decides
 * what a patient is, and to run out at the moment that decision matters most.
 *
 * <p>Holding a handful counts as an empty hand for the purpose of looking somebody over,
 * so the gesture that reads a body and the gesture that marks it are the same one.
 */
public class TriageTagItem extends Item {
    public TriageTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("triage.tooltip.triage_tag").withStyle(ChatFormatting.GRAY));
    }
}
