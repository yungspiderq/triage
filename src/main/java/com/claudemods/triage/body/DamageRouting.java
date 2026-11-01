package com.claudemods.triage.body;

import com.claudemods.triage.compat.Tacz;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Decides which part of a body a hit lands on. */
public final class DamageRouting {
    /** Above this share of the victim's height, a blow is coming down at them. */
    private static final double HIGH_BLOW = 0.85;
    /** Below this, it is coming up from around their feet. */
    private static final double LOW_BLOW = 0.15;

    /** Where the parts sit on a body, as shares of its height from the feet up. */
    private static final double HEAD_LINE = 0.88;
    private static final double THORAX_LINE = 0.62;
    private static final double STOMACH_LINE = 0.47;
    /** Further out sideways than this and the blow met an arm before it met a chest. */
    private static final double ARM_REACH = 0.19;
    /** How far a swing is traced looking for the body it landed on. */
    private static final double MELEE_TRACE = 6.0;

    private DamageRouting() {
    }

    public enum Kind {
        /** {@code /kill}, the void: nothing may stand between this and death. */
        BYPASS,
        /** Always the legs. */
        FALL,
        /** No point of impact - drowning, poison, fire. Spread over the whole body. */
        SYSTEMIC,
        /** An ordinary blow, which lands somewhere in particular. */
        PHYSICAL
    }

    public static Kind classify(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return Kind.BYPASS;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return Kind.FALL;
        }
        // A bullet has a point of impact even when it ignores armour. TaCZ marks its
        // armour-piercing rounds with the vanilla tag that would otherwise make this
        // systemic, and a rifle round spread over the whole body is not a body model.
        if (Tacz.bullet(source)) {
            return Kind.PHYSICAL;
        }
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return Kind.SYSTEMIC;
        }
        return Kind.PHYSICAL;
    }

    /** Picks a part for a physical hit, by the weights in the config. */
    public static BodyPart roll(RandomSource random) {
        return roll(random, null, null);
    }

    /**
     * The part actually struck, worked out from where the blow met the body - or null
     * when there is no honest way to tell and the weights should decide instead.
     *
     * <p>An arrow knows where it is. A melee swing does not, so the attacker's line of
     * sight is traced into the victim's box and the entry point is taken as the impact.
     * Aiming at a head and hitting a head is worth this much arithmetic: it is the
     * difference between combat you fight and combat that happens to you.
     */
    public static @Nullable BodyPart aimed(DamageSource source, Player victim) {
        Entity direct = source.getDirectEntity();
        Vec3 impact = null;

        if (direct != null && !(direct instanceof LivingEntity)) {
            // A projectile: it is standing at the point of impact by definition.
            impact = direct.position();
        } else if (direct instanceof Player attacker) {
            // Only a player is really aiming. A mob's look vector points at your eyes
            // because that is what its look controller does, not because it is going for
            // your head - and taking it literally turned every zombie into a headhunter
            // that killed in two swings. Their swings go back to the weights.
            Vec3 eye = attacker.getEyePosition();
            Vec3 reach = eye.add(attacker.getViewVector(1.0F).scale(MELEE_TRACE));
            impact = victim.getBoundingBox().inflate(0.1).clip(eye, reach).orElse(null);
        }
        if (impact == null) {
            return null;
        }

        double height = Math.max(0.1, victim.getBbHeight());
        double up = (impact.y - victim.getY()) / height;
        if (up > 1.2 || up < -0.2) {
            return null; // nowhere near the body; do not pretend to know
        }

        // Sideways, in the victim's own frame: which of their arms is on that side.
        Vec3 forward = victim.getViewVector(1.0F);
        Vec3 offset = impact.subtract(victim.position());
        double lateral = offset.x * -forward.z + offset.z * forward.x;
        boolean farOut = Math.abs(lateral) > ARM_REACH;
        boolean onTheirRight = lateral > 0.0;

        if (up >= HEAD_LINE) {
            return BodyPart.HEAD;
        }
        if (up >= STOMACH_LINE) {
            if (farOut) {
                return onTheirRight ? BodyPart.RIGHT_ARM : BodyPart.LEFT_ARM;
            }
            return up >= THORAX_LINE ? BodyPart.THORAX : BodyPart.STOMACH;
        }
        return onTheirRight ? BodyPart.RIGHT_LEG : BodyPart.LEFT_LEG;
    }

    /**
     * Picks a part for a physical hit.
     *
     * <p>The config weights say where blows land on average. This tilts them towards
     * where the blow actually came from: a skeleton shooting down from a ledge finds
     * heads, a silverfish underfoot finds legs. Anything with no position behind it -
     * or level with the victim - falls back to the plain weights.
     */
    public static BodyPart roll(RandomSource random, @Nullable DamageSource source, @Nullable Player victim) {
        float[] weights = new float[BodyPart.VALUES.length];
        for (BodyPart part : BodyPart.VALUES) {
            weights[part.ordinal()] = Math.max(0, part.hitWeight());
        }
        tiltByHeight(weights, source, victim);

        float total = 0.0F;
        for (float weight : weights) {
            total += weight;
        }
        if (total <= 0.0F) {
            return BodyPart.THORAX;
        }
        float roll = random.nextFloat() * total;
        for (BodyPart part : BodyPart.VALUES) {
            roll -= weights[part.ordinal()];
            if (roll < 0.0F) {
                return part;
            }
        }
        return BodyPart.THORAX;
    }

    /**
     * Where the blow came from, vertically, as a share of the victim's height: 0 at
     * their feet, 1 at the top of their head. The arrow itself is asked first, since a
     * skeleton stands still while its arrow arrives from wherever it was shot.
     */
    private static void tiltByHeight(float[] weights, @Nullable DamageSource source, @Nullable Player victim) {
        if (source == null || victim == null) {
            return;
        }
        Entity origin = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        if (origin == null) {
            return;
        }
        double height = Math.max(0.1, victim.getBbHeight());
        double relative = (origin.getY() - victim.getY()) / height;

        if (relative > HIGH_BLOW) {
            weights[BodyPart.HEAD.ordinal()] *= 2.5F;
            weights[BodyPart.THORAX.ordinal()] *= 1.3F;
            weights[BodyPart.LEFT_LEG.ordinal()] *= 0.4F;
            weights[BodyPart.RIGHT_LEG.ordinal()] *= 0.4F;
        } else if (relative < LOW_BLOW) {
            weights[BodyPart.HEAD.ordinal()] *= 0.2F;
            weights[BodyPart.LEFT_LEG.ordinal()] *= 2.5F;
            weights[BodyPart.RIGHT_LEG.ordinal()] *= 2.5F;
            weights[BodyPart.STOMACH.ordinal()] *= 1.2F;
        }
    }

    public static BodyPart rollLeg(RandomSource random) {
        return random.nextBoolean() ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
    }

    /**
     * Whether the player is wearing anything over this part.
     *
     * <p>Vanilla armour has already been subtracted by the time a hit gets routed, and
     * it defends the whole body evenly. This is the part that cannot express: a blow
     * that lands where there is no plate hurts more than the average suggests. Arms are
     * never covered, because there is no slot for them - which is deliberate, and why
     * arms are the limbs that go first.
     */
    public static boolean covered(Player player, BodyPart part) {
        EquipmentSlot slot = switch (part) {
            case HEAD -> EquipmentSlot.HEAD;
            case THORAX, STOMACH -> EquipmentSlot.CHEST;
            case LEFT_LEG, RIGHT_LEG -> EquipmentSlot.LEGS;
            case LEFT_ARM, RIGHT_ARM -> null;
        };
        if (slot == null) {
            return false;
        }
        if (!player.getItemBySlot(slot).isEmpty()) {
            return true;
        }
        // Boots are the other half of covering a leg, and plenty of players wear only
        // one of the two.
        return slot == EquipmentSlot.LEGS && !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }
}
