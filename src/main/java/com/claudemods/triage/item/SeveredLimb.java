package com.claudemods.triage.item;

import org.jetbrains.annotations.Nullable;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyPart;
import net.minecraft.nbt.CompoundTag;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import com.claudemods.triage.med.MedicineKind;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The arm, on the floor, with a clock on it.
 *
 * <p>Losing a limb in this mod has always been final in the flattest possible way: the arm
 * stopped existing the moment the saw finished, and what was left was a stump and a
 * shopping list. That is true of an amputation done properly and it is not true of the
 * other way limbs come off, which is that something takes one off you - and the thing it
 * took is lying about four feet away.
 *
 * <p>So it drops. It is an item, it can be picked up, carried, handed to somebody, dropped
 * again and lost, and for a few minutes a surgeon can put it back. What decides whether
 * that works is the one thing that decides it in life: <b>how long it has been off, and how
 * cold it was kept.</b> Warm in a pocket it is finished in five minutes. In water or in
 * snow it keeps for three times that, which is the actual advice and the reason a severed
 * hand travels to hospital on ice.
 *
 * <p>Past its time it does not vanish and it is not confiscated. It stays in the bag,
 * useless, which is a better teacher than a puff of smoke: somebody carrying a dead arm
 * around knows exactly what they are carrying and exactly how long they took.
 */
public final class SeveredLimb extends MedicineItem {
    public SeveredLimb(Properties properties) {
        super(MedicineKind.GRAFT, properties);
    }

    /** Where the two facts live, because 1.20.1 has no components to put them in. */
    private static final String LIMB = "triage_limb";
    private static final String FRESH = "triage_fresh";

    /** Which one it is. A left arm does not go on a right shoulder. */
    public static BodyPart partOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LIMB)) {
            return BodyPart.LEFT_ARM;
        }
        BodyPart part = BodyPart.byId(tag.getString(LIMB));
        return part == null ? BodyPart.LEFT_ARM : part;
    }

    /** How long it has left, in ticks, before it is meat. */
    public static int freshness(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(FRESH));
    }

    public static boolean usable(ItemStack stack) {
        return freshness(stack) > 0;
    }

    /** One that has just come off somebody. */
    public static ItemStack of(BodyPart part) {
        ItemStack stack = new ItemStack(ModItems.SEVERED_LIMB.get());
        stack.getOrCreateTag().putString(LIMB, part.id());
        stack.getOrCreateTag().putInt(FRESH, Config.values().graftSeconds * 20);
        stack.setHoverName(Component.translatable("item.triage.severed_limb.named", part.title())
                .withStyle(ChatFormatting.RED));
        return stack;
    }

    /**
     * One second of being off somebody, in a pocket.
     *
     * <p>Every tick rather than every twenty, because the clock is the item and an item
     * whose clock only runs while somebody happens to be looking is not a clock.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        wither(stack, level.getGameTime(),
                holder.isInWater() || holder.isInPowderSnow ? Config.values().graftColdFactor : 1.0F);
    }

    /**
     * And on the ground, where the cold actually helps.
     *
     * <p>The same hook the instruments use to rinse themselves off, for the same reason:
     * it is the one thing that fires for an item nobody is holding.
     */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide) {
            return false;
        }
        boolean cold = entity.isInWater() || entity.isInPowderSnow;
        wither(stack, entity.level().getGameTime(), cold ? Config.values().graftColdFactor : 1.0F);
        return false;
    }

    /**
     * @param now    the world clock, which is what decides which ticks count
     * @param factor how fast it is going off - below one when it is being kept cold
     *
     * <p>Keyed off the world clock rather than off what is left on the item. The first
     * version tested the remaining count against the divisor, which works once and then
     * never again: the moment it stops being divisible it stops decrementing, so a limb
     * on ice would have sat at the same number forever. The clock keeps running whatever
     * the item does.
     */
    private static void wither(ItemStack stack, long now, float factor) {
        int left = freshness(stack);
        if (left <= 0) {
            return;
        }
        int every = factor >= 1.0F ? 1 : Math.max(1, Math.round(1.0F / factor));
        if (now % every != 0) {
            return;
        }
        stack.getOrCreateTag().putInt(FRESH, left - 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        int left = freshness(stack);
        if (left <= 0) {
            tooltip.add(Component.translatable("triage.tooltip.limb_dead").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        tooltip.add(Component.translatable("triage.tooltip.limb_fresh", left / 20)
                .withStyle(left < Config.values().graftSeconds * 20 / 3
                        ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("triage.tooltip.limb_cold").withStyle(ChatFormatting.DARK_GRAY));
    }
}
