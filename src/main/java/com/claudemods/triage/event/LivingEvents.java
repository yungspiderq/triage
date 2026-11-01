package com.claudemods.triage.event;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;

/**
 * The rest of the wrappers, kept in one file because none of them is more than a name and
 * a getter. See {@link Event} for why they exist at all.
 */
public final class LivingEvents {
    private LivingEvents() {
    }

    /** Mending, before it is spread over the parts that can still use it. */
    public static final class LivingHealEvent extends Event {
        private final LivingEntity entity;
        private final float amount;

        public LivingHealEvent(LivingEntity entity, float amount) {
            this.entity = entity;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public float getAmount() {
            return amount;
        }
    }

    public static final class LivingDeathEvent extends Event {
        private final LivingEntity entity;

        public LivingDeathEvent(LivingEntity entity) {
            this.entity = entity;
        }

        public LivingEntity getEntity() {
            return entity;
        }
    }

    /** Something deciding to come after somebody, before it has decided it. */
    public static final class LivingChangeTargetEvent extends Event {
        private final LivingEntity target;

        public LivingChangeTargetEvent(LivingEntity target) {
            this.target = target;
        }

        public LivingEntity getNewAboutToBeSetTarget() {
            return target;
        }
    }

    public static final class AttackEntityEvent extends Event {
        private final net.minecraft.world.entity.player.Player entity;

        public AttackEntityEvent(net.minecraft.world.entity.player.Player entity) {
            this.entity = entity;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }
    }

    /** The last swallow, with the thing that was swallowed still in hand. */
    public static final class LivingEntityUseItemEvent {
        private LivingEntityUseItemEvent() {
        }

        public static final class Finish {
            private final LivingEntity entity;
            private final ItemStack item;

            public Finish(LivingEntity entity, ItemStack item) {
                this.entity = entity;
                this.item = item;
            }

            public LivingEntity getEntity() {
                return entity;
            }

            public ItemStack getItem() {
                return item;
            }
        }
    }

    /**
     * A blast, and everything standing in it.
     *
     * <p>The list is gathered by the mixin rather than taken from vanilla, which does not
     * keep one past the method that builds it. Same reach: vanilla affects entities out to
     * twice the blast's own radius, and that is the box this is filled from.
     */
    public static final class ExplosionEvent {
        private ExplosionEvent() {
        }

        public static final class Detonate {
            private final Explosion explosion;
            private final List<Entity> affected;

            public Detonate(Explosion explosion, List<Entity> affected) {
                this.explosion = explosion;
                this.affected = affected;
            }

            public Explosion getExplosion() {
                return explosion;
            }

            public List<Entity> getAffectedEntities() {
                return affected;
            }
        }
    }
}
