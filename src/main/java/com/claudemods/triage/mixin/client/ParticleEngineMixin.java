package com.claudemods.triage.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the particle render types the game does not know about.
 *
 * <p>Vanilla keeps particles in a map keyed by render type, and then draws them by walking a
 * fixed list of its own five - so a type that is not one of those five collects particles
 * that are ticked, counted and never drawn. Nothing is logged, because as far as the engine
 * is concerned nothing is wrong.
 *
 * <p>NeoForge patches that loop to walk the map instead, which is why the blood on the floor
 * works there and was invisible here. This does the same thing from the outside: after
 * vanilla has drawn its five and while the light layer is still bound, anything left in the
 * map is drawn the same way.
 *
 * <p>The mod has exactly one such type - a stain lies flat on the ground rather than facing
 * the camera, which needs face culling off, and no vanilla type has it off.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Final
    private static List<ParticleRenderType> RENDER_ORDER;

    @Shadow
    @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LightTexture;turnOffLightLayer()V"))
    private void triage$renderOtherTypes(LightTexture lightTexture, Camera camera, float partialTick,
            CallbackInfo callback) {
        for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particles.entrySet()) {
            ParticleRenderType type = entry.getKey();
            // The five vanilla already drew, and the one it deliberately never draws.
            if (RENDER_ORDER.contains(type) || type == ParticleRenderType.NO_RENDER) {
                continue;
            }
            Queue<Particle> queue = entry.getValue();
            if (queue.isEmpty()) {
                continue;
            }
            BufferBuilder builder = type.begin(Tesselator.getInstance(), textureManager);
            if (builder == null) {
                continue;
            }
            for (Particle particle : queue) {
                particle.render(builder, camera, partialTick);
            }
            MeshData mesh = builder.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }
    }
}
