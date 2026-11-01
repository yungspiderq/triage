package com.claudemods.triage.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A blow that has already been through armour and is about to come off the health bar.
 *
 * <p>The one place in the mod where the wrapper is doing more than carrying arguments:
 * NeoForge fires this inside its own damage path, and on Fabric there is no damage path
 * to fire inside, so TriageDamage - the mixin - reproduces the two lines of vanilla that
 * come before this moment and then hands over completely. See {@link Event}.
 */
public final class LivingDamageEvent {
    private LivingDamageEvent() {
    }

    public static final class Pre {
        private final LivingEntity entity;
        private final DamageSource source;
        private float damage;

        public Pre(LivingEntity entity, DamageSource source, float damage) {
            this.entity = entity;
            this.source = source;
            this.damage = damage;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getNewDamage() {
            return damage;
        }

        public void setNewDamage(float value) {
            damage = value;
        }
    }
}
