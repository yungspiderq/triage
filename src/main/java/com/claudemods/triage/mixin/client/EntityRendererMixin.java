package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientHooks;
import com.claudemods.triage.client.NamePlate;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * What is written over somebody's head.
 *
 * <p>A triage mark belongs there rather than on a chart: the whole point of one is that it
 * is readable from across a field by somebody who has no time to walk over.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    /**
     * The whole argument list after the value, because that is the only shape Mixin takes:
     * a handler may have the variable alone or the variable and every argument, and this
     * one needs the entity as well as the name.
     */
    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component triage$nameTag(Component content, Entity entity, Component displayName,
            PoseStack poses, MultiBufferSource buffers, int light, float partialTick) {
        ClientHooks.RenderNameTagEvent event = new ClientHooks.RenderNameTagEvent(entity, content);
        NamePlate.onRenderNameTag(event);
        return event.getContent();
    }
}
