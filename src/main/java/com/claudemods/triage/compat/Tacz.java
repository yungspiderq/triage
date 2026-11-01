package com.claudemods.triage.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * The one thing Triage needs to know about a gun mod: a bullet is still a blow.
 *
 * <p>TaCZ's armour-piercing rounds carry vanilla's {@code bypasses_armor} tag, which is
 * correct for vanilla and wrong for a body model. To vanilla it means "do not subtract
 * armour points"; to Triage it meant "there is no point of impact", and every rifle round
 * was spread evenly over seven body parts instead of landing where it went in.
 *
 * <p>No class from TaCZ is named here. The mod is optional and may be a different build
 * than the one this was written against, so the check is on the two things that survive
 * a version bump: the damage type's message id and the entity's package. A bullet from a
 * future version of the mod is still a bullet.
 */
public final class Tacz {
    /**
     * The one method on a TaCZ bullet this mod needs. Cached per class because it is
     * looked up on a hit and a class is exactly as stable as the build it came from.
     */
    private static final ClassValue<java.lang.reflect.Method> AMMO_ID =
            new ClassValue<>() {
                @Override
                protected java.lang.reflect.Method computeValue(Class<?> type) {
                    try {
                        return type.getMethod("getAmmoId");
                    } catch (NoSuchMethodException | RuntimeException ignored) {
                        return null;
                    }
                }
            };

    private Tacz() {
    }

    /**
     * Whether this damage came from a TaCZ projectile.
     *
     * <p>Both the ordinary and the armour-piercing damage types use the same message id,
     * so this catches the rounds that {@link net.minecraft.tags.DamageTypeTags#BYPASSES_ARMOR}
     * would otherwise send down the systemic path.
     */
    public static boolean bullet(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct != null && direct.getClass().getName().startsWith("com.tacz.guns.")) {
            return true;
        }
        Entity owner = source.getEntity();
        if (owner != null && owner.getClass().getName().startsWith("com.tacz.guns.")) {
            return true;
        }
        String message = source.getMsgId();
        return message != null && message.startsWith("tacz.");
    }

    /**
     * What went in, as TaCZ names it: {@code tacz:9mm}, {@code tacz:762x39}.
     *
     * <p>The projectile entity has carried this since the first version this mod was
     * written against; the method name is the one stable thing about it. Reading it by
     * reflection keeps the compile classpath free of TaCZ, and a missing method - a very
     * old or very new build - returns null, which the caller stores as the generic round
     * rather than as no round at all.
     *
     * @return the ammo id as a string, or null when this was not a TaCZ projectile or it
     *         did not say
     */
    public static String ammo(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null || !direct.getClass().getName().startsWith("com.tacz.guns.")) {
            return null;
        }
        java.lang.reflect.Method method = AMMO_ID.get(direct.getClass());
        if (method == null) {
            return null;
        }
        try {
            Object id = method.invoke(direct);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
