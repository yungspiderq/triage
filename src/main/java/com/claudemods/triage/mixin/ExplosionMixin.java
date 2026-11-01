package com.claudemods.triage.mixin;

import com.claudemods.triage.event.BlastEvents;
import com.claudemods.triage.event.LivingEvents;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A blast, caught at the blast rather than at the damage it did.
 *
 * <p>That is the whole design and it is worth the mixin. A pressure wave does not care
 * whether the damage roll got through: somebody in full plate takes almost nothing off
 * their health bar from a creeper at their feet, and their lungs are still in the same
 * room as it. Hanging this off damage would give exactly the wrong answer to exactly the
 * interesting case.
 *
 * <p>The list of who was caught is gathered here rather than taken from vanilla, which
 * builds one and drops it inside the method. Same box vanilla uses: twice the blast's own
 * radius.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "explode", at = @At("RETURN"))
    private void triage$onDetonate(CallbackInfo callback) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        Explosion explosion = (Explosion) (Object) this;
        double reach = Math.max(1.0, explosion.radius()) * 2.0;
        Vec3 center = explosion.center();
        List<Entity> caught = level.getEntities((Entity) null,
                new AABB(center.x - reach, center.y - reach, center.z - reach,
                        center.x + reach, center.y + reach, center.z + reach));
        if (!caught.isEmpty()) {
            BlastEvents.onDetonate(new LivingEvents.ExplosionEvent.Detonate(explosion, caught));
        }
    }
}
