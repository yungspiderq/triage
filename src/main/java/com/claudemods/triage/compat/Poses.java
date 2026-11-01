package com.claudemods.triage.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

/**
 * Holding a player in a pose they would not choose.
 *
 * <p>NeoForge adds setForcedPose to every entity and the mod uses it in four places -
 * somebody carried, somebody down, somebody out cold. Fabric has no such thing, so the
 * same idea is added by a mixin on Player and reached through here.
 *
 * <p>Worth saying what a forced pose is and is not: it changes what the player looks like
 * and what shape they take up, and nothing else. Being unable to stand is decided
 * elsewhere, by the body.
 */
public final class Poses {
    /** Implemented by Player through PlayerMixin. */
    public interface Forced {
        void triage$setForcedPose(@Nullable Pose pose);

        @Nullable
        Pose triage$forcedPose();
    }

    private Poses() {
    }

    /** Null puts them back on their own feet. */
    public static void force(Entity entity, @Nullable Pose pose) {
        if (entity instanceof Forced forced) {
            forced.triage$setForcedPose(pose);
            if (pose != null) {
                entity.setPose(pose);
            }
            entity.refreshDimensions();
        }
    }

    @Nullable
    public static Pose forced(Entity entity) {
        return entity instanceof Forced forced ? forced.triage$forcedPose() : null;
    }
}
