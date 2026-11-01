package com.claudemods.triage.compat;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

/**
 * Where a blast was.
 *
 * <p>1.20.1's Explosion keeps its own position to itself: there is no getter, Forge adds
 * one, and the Forge tree calls it. Here the three fields are opened by the access widener
 * instead - see triage.accesswidener - and this is the one line that reads them.
 */
public final class Blasts {
    private Blasts() {
    }

    public static Vec3 centre(Explosion explosion) {
        return new Vec3(explosion.x, explosion.y, explosion.z);
    }
}
