package com.claudemods.triage.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import com.claudemods.triage.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A drop of blood, and a mark where one landed.
 *
 * <p>What was here before was redstone dust, which is a small glowing cube. It reads as
 * magic because it is the particle magic is made of, and a mod about bodies cannot borrow
 * the sparkle off a redstone torch to say somebody is dying.
 *
 * <p>So it has its own, and it behaves like the thing rather than like a puff:
 *
 * <ul>
 *   <li><b>It falls and it accelerates</b>, and it does not drift. Blood is heavy and it
 *       is not smoke.</li>
 *   <li><b>It stretches as it goes.</b> A drop in the air is not round - it is pulled
 *       long by falling - and the stretch is what makes it read as falling rather than as
 *       hanging there.</li>
 *   <li><b>It lands and stays.</b> Hitting the ground turns it into a flat mark that sits
 *       still and darkens: what is on the floor a minute later is not the drop, it is the
 *       stain, and the stain is what somebody tracking you actually follows.</li>
 * </ul>
 */
public class BloodParticle extends TextureSheetParticle {
    /** Set the moment it lands, so nothing carries on happening to it after. */
    private boolean landed;
    private final float bornSize;
    /** Kept, because the three drawings of it are the three stages of a fall. */
    private final SpriteSet sprites;

    protected BloodParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.gravity = 1.1F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.quadSize = 0.055F + this.random.nextFloat() * 0.035F;
        this.bornSize = this.quadSize;
        this.lifetime = 90 + this.random.nextInt(70);
        // Arterial and venous are not the same colour and nobody is going to be told
        // which is which - but a wound that is all one flat red looks printed.
        float shade = 0.72F + this.random.nextFloat() * 0.28F;
        this.setColor(0.48F * shade, 0.035F * shade, 0.03F * shade);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
        if (this.landed) {
            return;
        }
        this.yd -= 0.04 * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.zd *= this.friction;
        this.yd *= 0.995;

        // onGround is the only landing signal a particle is given; a drop that has
        // stopped falling without it has hit a wall, and running down a wall is close
        // enough to landing to end the same way.
        if (this.onGround || (Math.abs(this.yd) < 0.0025 && this.age > 4)) {
            land();
            return;
        }
        // Longer the faster it is going, which is what a falling drop does - and the
        // drawing changes with it, because the three of them are the three stages of one.
        this.quadSize = this.bornSize * (float) (1.0 + Math.min(1.6, Math.abs(this.yd) * 5.0));
        this.setSpriteFromAge(this.sprites);
    }

    /**
     * It hits, and it stops being a drop.
     *
     * <p>A landed drop is a different thing from a falling one - it lies on the floor
     * and keeps one orientation while a falling one faces you - so it hands over to the
     * mark rather than pretending to be one.
     */
    private void land() {
        this.landed = true;
        this.remove();
        // A minute on the floor. Nothing is ever going to send this one again - it is
        // where a drop landed in front of somebody - so its own lifetime is the only
        // thing keeping it there.
        this.level.addParticle(ModParticles.STAIN.get(), this.x, this.y + 0.02, this.z,
                0.0, this.bornSize * 3.0, 1200.0);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new BloodParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }

}
