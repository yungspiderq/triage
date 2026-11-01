package com.claudemods.triage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.jetbrains.annotations.Nullable;

/**
 * The few fields this mod has to reach that nothing lets it reach.
 *
 * <p>The 1.21 line opens them with an access transformer and never thinks about it again.
 * ForgeGradle on this side did not apply one, so they are opened at runtime instead.
 *
 * <p><b>Never by name.</b> A field name is one thing while the mod is being written and
 * another when it is running: this loader ships SRG names, where {@code templates} is
 * {@code f_210560_}, and the Fabric build of the same version ships intermediary ones. A
 * lookup by name therefore works on exactly one machine in the world - the one the mod was
 * written on - and fails on every machine anybody plays on. It did, for three versions, in
 * a static initialiser, which turned a cosmetic village building into a mod that would not
 * load. Thank you to Sleeper_Creeper for the report that found it.
 *
 * <p>So everything here is found by <b>type</b>, which is not remapped and cannot be. And
 * everything here is allowed to fail: a missing field returns null, says so in the log
 * once, and the feature that wanted it does without. No field on this list is worth a
 * crash - one of them adds a building to a village and one of them draws a bandage over a
 * sleeve.
 */
public final class Reach {
    private Reach() {
    }

    /**
     * The one field of exactly this type, whatever the runtime calls it.
     *
     * <p>Exactly this type, not a subtype: that is what tells two lists apart when a class
     * keeps two, which is the case that matters here - a template pool holds one
     * {@code List} and one {@code ObjectArrayList}, and picking by assignability would find
     * either one.
     *
     * @return the field, or null when there is not exactly one - in which case the caller
     *         does without and the game carries on
     */
    public static @Nullable Field sole(Class<?> owner, Class<?> type) {
        Field found = null;
        for (Field field : owner.getDeclaredFields()) {
            if (field.getType() != type || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (found != null) {
                TriageMod.LOGGER.warn("triage: {} has more than one {} field; leaving it alone",
                        owner.getName(), type.getSimpleName());
                return null;
            }
            found = field;
        }
        if (found == null) {
            TriageMod.LOGGER.warn("triage: no {} field on {}; the feature that wanted it is off",
                    type.getSimpleName(), owner.getName());
            return null;
        }
        try {
            found.setAccessible(true);
        } catch (RuntimeException e) {
            TriageMod.LOGGER.warn("triage: cannot open {} on {}: {}",
                    found.getName(), owner.getName(), e.toString());
            return null;
        }
        return found;
    }

    /**
     * The one float a class keeps.
     *
     * <p>For {@link net.minecraft.world.level.Explosion}, which knows how big it was and
     * will not say. This one has always been by type, and it is the reason the rest of
     * this class now is.
     */
    public static @Nullable Field soleFloat(Class<?> owner) {
        return sole(owner, float.class);
    }

    /** @return the value, or null when the field could not be opened at all */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T get(@Nullable Field field, Object from) {
        if (field == null) {
            return null;
        }
        try {
            return (T) field.get(from);
        } catch (IllegalAccessException | RuntimeException e) {
            TriageMod.LOGGER.warn("triage: cannot read {}: {}", field.getName(), e.toString());
            return null;
        }
    }

    /** @param fallback what to use when the field is not there, rather than throwing */
    public static float getFloat(@Nullable Field field, Object from, float fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return field.getFloat(from);
        } catch (IllegalAccessException | RuntimeException e) {
            TriageMod.LOGGER.warn("triage: cannot read {}: {}", field.getName(), e.toString());
            return fallback;
        }
    }
}
