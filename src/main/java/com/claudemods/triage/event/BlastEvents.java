package com.claudemods.triage.event;

import com.claudemods.triage.Reach;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Blast;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import com.claudemods.triage.event.LivingEvents.ExplosionEvent;

/**
 * Catching people in blasts.
 *
 * <p>Hooked at the explosion rather than at the damage, and that is the whole design. A
 * pressure wave does not care whether the damage roll got through: somebody in full plate
 * takes almost nothing off their health bar from a creeper at their feet, and their lungs
 * are still in the same room as the blast. Hanging this off the damage event would have
 * given exactly the wrong answer to exactly the interesting case.
 */
public final class BlastEvents {
    /**
     * How big the blast was.
     *
     * <p>1.20.1's {@code Explosion} keeps its radius to itself and Forge adds no getter,
     * so it is read off the object. By type rather than by name, because the name differs
     * between compiling and running and the type does not.
     */
    private static final java.lang.reflect.Field RADIUS = Reach.soleFloat(Explosion.class);

    private BlastEvents() {
    }

    public static void onDetonate(ExplosionEvent.Detonate event) {
        Explosion explosion = event.getExplosion();
        // Four, when the field cannot be read at all: that is TNT, which is what most
        // explosions in this game are. A guessed radius still produces the right shape of
        // injury; no radius at all would produce no injuries and no explanation.
        double radius = Math.max(1.0, Reach.getFloat(RADIUS, explosion, 4.0F));
        for (Entity entity : event.getAffectedEntities()) {
            if (!(entity instanceof ServerPlayer player) || !Body.tracks(player)) {
                continue;
            }
            double distance = entity.position().distanceTo(
                    com.claudemods.triage.compat.Blasts.centre(explosion) == null ? entity.position() : com.claudemods.triage.compat.Blasts.centre(explosion));
            // The radius vanilla hands out is the blast's own; entities out to twice it
            // are affected, so the exposure is measured against that same reach.
            float exposure = (float) (1.0 - distance / (radius * 2.0));
            if (exposure <= 0.0F) {
                continue;
            }
            BodyHealth body = Body.of(player);
            Blast.caught(player, body, exposure);
            Body.sync(player);
        }
    }
}
