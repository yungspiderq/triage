package com.claudemods.triage.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Arriving, leaving, coming back, and noticing somebody. See {@link Event}. */
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
}
