package com.claudemods.triage.client.particle;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Blood on the floor, lying on the floor.
 *
 * <p>The first attempt at this used an ordinary particle, and an ordinary particle turns
 * to face the camera. Which means a mark on the ground stood up as you walked round it -
 * a red disc hovering at ankle height, turning to watch you. Nothing about that reads as
 * something spilt.
 *
 * <p>A stain is not a thing in the air. It is <em>on</em> a surface, it has one
 * orientation, and it keeps it however you look at it. So this one is laid flat: the quad
 * is turned a quarter turn onto its back and then spun to a random angle, because forty
 * marks all lined up the same way is a stencil rather than a trail.
 *
 * <p>And it dries. What is on the floor a minute after somebody passed is not what it was
 * when it landed - it goes from wet red to old brown and thins out - which is the only
 * thing a trail can say about which way somebody went. Follow the fresh end.
 */
public class StainParticle extends TextureSheetParticle {
    /** Which way round this one landed. No two the same. */
    private final float spin;
    /** How dry it already was when it appeared. */
    private float born;

    protected StainParticle(ClientLevel level, double x, double y, double z,
                            double age, double size, double ticks, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.spin = this.random.nextFloat() * Mth.TWO_PI;
        this.quadSize = 0.09F + (float) Mth.clamp(size, 0.0, 1.0) * 0.30F
                + this.random.nextFloat() * 0.03F;
        // Two sorts of mark end up here and they want opposite lifetimes.
        //
        // One is re-sent by the world every second so that everybody near it sees the
        // same trail; that one only has to outlive the gap between sends. The other is
        // where a drop actually landed in front of you, and nothing will ever send it
        // again - so if it lasts a second, blood vanishes a second after it lands, which
        // is not what anybody means by blood on the floor.
        this.lifetime = ticks > 0.0 ? (int) ticks : 26;
        this.born = (float) Mth.clamp(age, 0.0, 1.0);
        dry(this.born);
        this.pickSprite(sprites);
    }

    /**
     * How old this one is, as a colour.
     *
     * <p>Fresh is red and wet. Old is the colour blood actually goes, which is nearer
     * brown than red and is why a trail an hour old is so much harder to see than one
     * from five minutes ago.
     */
    private void dry(float age) {
        this.rCol = 0.52F - age * 0.28F;
        this.gCol = 0.055F - age * 0.020F;
        this.bCol = 0.045F - age * 0.020F;
        this.alpha = 0.95F - age * 0.35F;
    }

    /**
     * Its own render type, and the reason is one line of state: no face culling.
     *
     * <p>Every vanilla particle faces the camera, so no vanilla render type ever has to
     * think about which side of a quad you are looking at - and they all leave culling
     * on. This one does not face the camera. It lies on the floor, and the vertex order
     * a flat quad ends up with after being turned onto its back is the order that gets it
     * thrown away as a back face.
     *
     * <p>Which is why the marks were there and invisible. Turning culling off is both the
     * fix and the right answer on its own terms: a stain is a thing on a surface, and a
     * thing on a surface has no back.
     */
    private static final ParticleRenderType FLAT = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "triage:flat_stain";
        }
    };

    @Override
    public ParticleRenderType getRenderType() {
        return FLAT;
    }

    /**
     * Flat, and staying flat.
     *
     * <p>A quarter turn onto its back puts the quad on the ground instead of facing the
     * camera; the spin after it is the only thing stopping every mark in a trail from
     * being the same mark at the same angle.
     */
    /**
     * Flat, and staying flat - with the quad written out.
     *
     * <p>1.21 has a helper that takes a rotation and does this. 1.20.1 has none, so the
     * four corners are worked out here: a square on the ground, spun to this mark's own
     * angle, moved to where it landed and offset against the camera.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - eye.x());
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - eye.y());
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - eye.z());
        float size = this.getQuadSize(partialTick);
        float cos = Mth.cos(this.spin) * size;
        float sin = Mth.sin(this.spin) * size;

        float[][] corners = {
                {-cos - sin, -sin + cos},
                {-cos + sin, -sin - cos},
                {cos + sin, sin - cos},
                {cos - sin, sin + cos},
        };
        float[][] uvs = {{this.getU1(), this.getV1()}, {this.getU1(), this.getV0()},
                {this.getU0(), this.getV0()}, {this.getU0(), this.getV1()}};
        int light = this.getLightColor(partialTick);
        for (int i = 0; i < 4; i++) {
            buffer.vertex(x + corners[i][0], y, z + corners[i][1])
                    .uv(uvs[i][0], uvs[i][1])
                    .color(this.rCol, this.gCol, this.bCol, this.alpha)
                    .uv2(light)
                    .endVertex();
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // A mark that is going to be here a while dries while it is here, rather than
        // being one shade for its whole life and then blinking out.
        if (this.lifetime > 60) {
            dry(Math.min(1.0F, this.born + (1.0F - this.born) * (this.age / (float) this.lifetime)));
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /**
         * @param dx how dry it is, 0 fresh to 1 old.
         * @param dy how big the pool is, 0 a drop to 1 somebody who stood there a while.
         * @param dz how many ticks it should last, or 0 for one the world keeps re-sending.
         */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new StainParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
