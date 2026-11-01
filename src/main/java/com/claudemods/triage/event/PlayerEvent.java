package com.claudemods.triage.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Arriving, leaving, coming back, noticing somebody, and digging. See {@link Event}. */
public abstract class PlayerEvent extends Event {
    private final Player entity;

    private PlayerEvent(Player entity) {
        this.entity = entity;
    }

    public Player getEntity() {
        return entity;
    }

    public static final class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player entity) {
            super(entity);
        }
    }

    public static final class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player entity) {
            super(entity);
        }
    }

    public static final class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;

        public PlayerRespawnEvent(Player entity, boolean endConquered) {
            super(entity);
            this.endConquered = endConquered;
        }

        /**
         * Whether this was the walk out of the end rather than a death.
         *
         * <p>The distinction matters here more than it does in most mods: dying is what
         * puts a body back together, and stepping through the portal is not dying.
         */
        public boolean isEndConquered() {
            return endConquered;
        }
    }

    public static final class PlayerChangedDimensionEvent extends PlayerEvent {
        public PlayerChangedDimensionEvent(Player entity) {
            super(entity);
        }
    }

    /** getEntity() is the one doing the looking; getTarget() is what came into view. */
    public static final class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(Player entity, Entity target) {
            super(entity);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /**
     * How fast this player is getting through this block.
     *
     * <p>The one wrapper with no counterpart in the 1.21 line, because the rule it carries
     * has none either: that build takes mining speed off an attribute, and this one is
     * asked the question every time somebody starts digging. Being asked is the better end
     * to arrive from - it can look at the body as it is at that moment rather than at a
     * number somebody remembered to keep up to date - and on Fabric the asking is a mixin.
     */
    public static final class BreakSpeed extends PlayerEvent {
        private final BlockState state;
        private final float original;
        private float speed;

        public BreakSpeed(Player entity, BlockState state, float speed) {
            super(entity);
            this.state = state;
            this.original = speed;
            this.speed = speed;
        }

        public BlockState getState() {
            return state;
        }

        public float getOriginalSpeed() {
            return original;
        }

        public void setNewSpeed(float value) {
            speed = value;
        }

        public float getNewSpeed() {
            return speed;
        }
    }
}
