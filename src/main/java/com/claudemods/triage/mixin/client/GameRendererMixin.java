package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientEvents;
import com.claudemods.triage.client.ClientHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What a body in a bad way does to the view out of it.
 *
 * <p>Two things, and both are the eye rather than the world. The horizon stops being level,
 * which is a head that has been hit not keeping still; and the view narrows, which is the
 * world closing in rather than going dark.
 *
 * <p>Attached to the tilt vanilla already applies for damage, because that is the one place
 * in the frame where the world's own pose stack is in hand and it is reached every frame.
 * Roll only: pitch and yaw are where the player is looking, and taking those away is taking
 * the game away.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("RETURN"))
    private void triage$roll(PoseStack poses, float partialTick, CallbackInfo callback) {
        ClientHooks.ViewportEvent.ComputeCameraAngles event =
                new ClientHooks.ViewportEvent.ComputeCameraAngles(partialTick, 0.0F);
        ClientEvents.onCameraAngles(event);
        float roll = event.getRoll();
        if (roll != 0.0F) {
            poses.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll));
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void triage$fov(Camera camera, float partialTick, boolean useFovSetting,
            CallbackInfoReturnable<Double> callback) {
        ClientHooks.ViewportEvent.ComputeFov event =
                new ClientHooks.ViewportEvent.ComputeFov(callback.getReturnValueD());
        ClientEvents.onFov(event);
        if (event.getFOV() != callback.getReturnValueD()) {
            callback.setReturnValue(Math.max(1.0, event.getFOV()));
        }
    }
}
