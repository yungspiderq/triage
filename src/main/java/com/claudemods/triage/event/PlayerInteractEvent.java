package com.claudemods.triage.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A right click: on a player, on a block, or on nothing. See {@link Event}. */
public abstract class PlayerInteractEvent extends Event {
    private final Player entity;
    private final InteractionHand hand;
    private final ItemStack stack;
    private InteractionResult cancellationResult = InteractionResult.PASS;

    private PlayerInteractEvent(Player entity, InteractionHand hand, ItemStack stack) {
        this.entity = entity;
        this.hand = hand;
        this.stack = stack;
    }

    public Player getEntity() {
        return entity;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return stack;
    }

    public void setCancellationResult(InteractionResult result) {
        cancellationResult = result;
    }

    /**
     * What Fabric wants handed back: PASS unless a handler took the click.
     *
     * <p>Where NeoForge has a cancelled event carrying a result, Fabric has the result
     * alone, and anything other than PASS is what stops the click going any further.
     */
    public InteractionResult result() {
        return isCanceled() ? cancellationResult : InteractionResult.PASS;
    }

    public static final class EntityInteract extends PlayerInteractEvent {
        private final Entity target;

        public EntityInteract(Player entity, InteractionHand hand, ItemStack stack, Entity target) {
            super(entity, hand, stack);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    public static final class RightClickBlock extends PlayerInteractEvent {
        public RightClickBlock(Player entity, InteractionHand hand, ItemStack stack) {
            super(entity, hand, stack);
        }
    }

    public static final class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(Player entity, InteractionHand hand, ItemStack stack) {
            super(entity, hand, stack);
        }
    }
}
