package com.claudemods.triage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Drawing a piece of a model in a colour, on 1.20.1.
 *
 * <p>The other line hands {@link ModelPart#render} a packed ARGB int and is done. Here the
 * same method takes four floats, so every place in the mod that tints something would
 * have to unpack the colour itself - four times, four chances to shift by the wrong
 * number, and four places to fix when somebody changes a shade.
 *
 * <p>So it is unpacked once, here. The colours upstairs stay written as {@code 0xFFB01008}
 * in both builds, which is the whole point: the port should be invisible from above.
 */
public final class Tint {
    private Tint() {
    }

    public static void render(ModelPart piece, PoseStack poseStack, MultiBufferSource buffer,
                              ResourceLocation texture, boolean translucent, int light, int argb) {
        render(piece, poseStack, buffer.getBuffer(translucent
                ? RenderType.entityTranslucent(texture) : RenderType.entitySolid(texture)), light, argb);
    }

    /** The same, when the caller already knows which buffer it wants. */
    public static void render(ModelPart piece, PoseStack poseStack,
                              com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int argb) {
        piece.render(poseStack, buffer,
                light, OverlayTexture.NO_OVERLAY,
                ((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F);
    }
}
