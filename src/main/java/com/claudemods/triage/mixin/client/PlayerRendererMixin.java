package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientHooks;
import com.claudemods.triage.client.Limbs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A body with a piece of it not drawn.
 *
 * <p>Both halves of that: the limb switched off before the model is drawn and switched
 * back on after it, because one model is shared by every player on the screen and the
 * first one-armed player in the room would otherwise take everybody else's arm with them.
 *
 * <p>And the first person, where the arm you are looking at is the one that is gone.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void triage$beforeRender(AbstractClientPlayer player, float yaw, float partialTick,
            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo callback) {
        Limbs.before(new ClientHooks.RenderPlayerEvent.Pre(player, (PlayerRenderer) (Object) this));
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void triage$afterRender(AbstractClientPlayer player, float yaw, float partialTick,
            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo callback) {
        Limbs.after(new ClientHooks.RenderPlayerEvent.Post(player, (PlayerRenderer) (Object) this));
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void triage$rightHand(PoseStack poses, MultiBufferSource buffers, int light,
            AbstractClientPlayer player, CallbackInfo callback) {
        if (hidden(player, HumanoidArm.RIGHT)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void triage$leftHand(PoseStack poses, MultiBufferSource buffers, int light,
            AbstractClientPlayer player, CallbackInfo callback) {
        if (hidden(player, HumanoidArm.LEFT)) {
            callback.cancel();
        }
    }

    private static boolean hidden(AbstractClientPlayer player, HumanoidArm arm) {
        ClientHooks.RenderArmEvent event = new ClientHooks.RenderArmEvent(player, arm);
        Limbs.arm(event);
        return event.isCanceled();
    }
}
