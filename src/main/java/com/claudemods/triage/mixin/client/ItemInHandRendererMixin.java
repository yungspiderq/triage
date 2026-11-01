package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientHooks;
import com.claudemods.triage.client.Limbs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The hand in front of you, and whatever it is holding.
 *
 * <p>Separate from the arm itself because they are drawn separately: a sword floating in
 * front of a shoulder is worse than no sword, and it is also honest - that hand is not
 * holding anything, whatever the hotbar says.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void triage$renderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch,
            InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poses,
            MultiBufferSource buffers, int light, CallbackInfo callback) {
        ClientHooks.RenderHandEvent event = new ClientHooks.RenderHandEvent(hand);
        Limbs.hand(event);
        if (event.isCanceled()) {
            callback.cancel();
        }
    }
}
