package com.claudemods.triage.event;

import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;

/**
 * The rest of the wrappers, kept in one file because none of them is more than a name and
 * a getter. See {@link Event} for what they are and why.
 */
public final class LivingEvents {
    private LivingEvents() {
    }

    /**
     * A blow that has already been through armour and is about to come off the health bar.
     *
     * <p>Worth one note, because this is the one place where the two Fabric trees differ
     * from the loaders they were carried from. Forge fires its hurt event before armour has
     * had a say, which means a mod that answers it and sets the damage to nothing has taken
     * armour out of the game without meaning to. The mixin here attaches where NeoForge
     * fires instead - after armour and enchantments, before absorption - so a plated chest
     * is worth something on 1.20.1 as well, and the comment at the top of DamageEvents is
     * true on both.
     */
    public static final class LivingHurtEvent extends Event {
        private final LivingEntity entity;
        private final DamageSource source;
        private float amount;

        public LivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return amount;
        }

        public void setAmount(float value) {
            amount = value;
        }
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

        public LivingEntity getNewTarget() {
            return target;
        }
    }

    public static final class AttackEntityEvent extends Event {
        private final Player entity;

        public AttackEntityEvent(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
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
