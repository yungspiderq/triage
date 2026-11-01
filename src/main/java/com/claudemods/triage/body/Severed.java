package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.item.SeveredLimb;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * What happens to the arm.
 *
 * <p>Losing one has always been final in the flattest possible way here: the limb stopped
 * existing the moment it came off, and what was left was a stump and a shopping list. That
 * is true of an amputation done on purpose - the surgeon meant it, and the thing they took
 * off was already dead - and it is not true of the other way limbs come off. Something
 * takes one off you, and the thing it took is lying four feet away.
 *
 * <p>So it lands there, as an item, with a clock on it. It can be picked up, carried,
 * handed to somebody else, dropped down a hole and lost, and for a few minutes a surgeon
 * can put it back. What decides whether that works happened before anybody opened a
 * screen: how long it has been off and whether anyone thought to keep it cold.
 *
 * <p>None of this makes losing a limb survivable that was not survivable before. The stump
 * bleeds the same, the shock arrives the same, and somebody still has to close it. What it
 * adds is a second clock running next to the first one, and a decision about which of them
 * to spend the next minute on.
 */
public final class Severed {
    private Severed() {
    }

    /**
     * It lands next to them.
     *
     * @param alive whether it was still a living limb when it came off, which decides
     *              whether anybody can do anything with it
     */
    public static void drop(ServerPlayer player, BodyPart part, boolean alive) {
        Config.Values config = Config.values();
        if (!config.grafts || !part.limb() || player.level().isClientSide) {
            return;
        }
        ItemStack stack = SeveredLimb.of(part);
        if (!alive) {
            // A limb that died on the body comes off as meat. The item still drops and
            // still says what it is - a puff of smoke would teach nothing, and somebody
            // carrying a dead arm around knows exactly what they are carrying.
            stack.getOrCreateTag().putInt("triage_fresh", 0);
        }
        ItemEntity dropped = new ItemEntity(player.level(),
                player.getX(), player.getY() + 0.6, player.getZ(), stack);
        // Thrown clear rather than dropped on their feet: it came off, it did not fall off.
        dropped.setDeltaMovement(
                (player.getRandom().nextDouble() - 0.5) * 0.25,
                0.2,
                (player.getRandom().nextDouble() - 0.5) * 0.25);
        dropped.setPickUpDelay(20);
        player.level().addFreshEntity(dropped);
        if (alive) {
            Body.notify(player, Component.translatable("triage.msg.limb_dropped"),
                    ChatFormatting.DARK_RED, null);
        }
    }
}
