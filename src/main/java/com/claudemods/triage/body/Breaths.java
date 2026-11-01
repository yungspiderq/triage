package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * A chest you can hear from a body length away.
 *
 * <p>Everything this mod knows about somebody else's chest has had to be fetched: kneel,
 * count, or put a stethoscope on it. That is wrong about the one organ that announces
 * itself - a chest in trouble is <em>loud</em>, and it is loud to anybody in the room
 * before any instrument is out of any bag.
 *
 * <p>So a body that is breathing badly makes the noise it makes. Quietly, at the range of
 * about a body length, which is the range at which it is true.
 *
 * <p>This does not replace the instrument and it is careful not to: what you hear is that
 * something is wrong with a chest, never the rate and never the cause. The stethoscope
 * stops being the way to find out that a chest is bad and becomes the way to find out
 * <b>which</b> bad it is, which is what a stethoscope is actually for.
 *
 * <p>The worst case makes no sound at all, and that is not an oversight: a chest that has
 * stopped is silent, and somebody who has learned to listen for the noise will one day
 * notice it missing. There is no sound in this mod for that, and there should not be.
 */
public final class Breaths {
    /** Quiet enough to want leaning in for, which is the whole point of it. */
    private static final float VOLUME = 0.22F;

    private Breaths() {
    }

    /**
     * Once a second at most, and never on a chest that is fine.
     *
     * <p>Rate decides how often rather than how loud, so a chest running at forty is
     * audibly hurrying and one running at six is audibly not - which is the one piece of
     * the reading that arrives honestly through a wall of sound rather than as a number.
     */
    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.breathSounds || player.isSpectator()) {
            return;
        }
        Breathing breathing = Breathing.of(body, config.airwaySeconds);
        SoundEvent sound = voice(body, breathing);
        if (sound == null) {
            return;
        }
        // Fast chests are heard more often than slow ones. Twenty breaths a minute is one
        // every three seconds; forty is one every second and a half, and it sounds like
        // somebody who cannot get enough.
        float chance = Math.min(1.0F, breathing.rate() / 34.0F);
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        // Pitched by how slow it is, because that is what a slow breath sounds like and
        // because it keeps two patients in the same room from sounding identical.
        float pitch = 0.62F + Math.min(0.55F, breathing.rate() / 60.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, VOLUME, pitch);
    }

    /**
     * Which noise, and none at all for a chest that is working.
     *
     * <p>Three sounds and one silence. All three are the game's own - a mod that shipped
     * its own recording of a person struggling to breathe would be a different kind of
     * thing entirely, and the game already owns a gasp, a wet one and a hurried one.
     */
    private static @Nullable SoundEvent voice(BodyHealth body, Breathing breathing) {
        if (breathing.character() == Breathing.ABSENT || body.arrested()) {
            // Nothing. Somebody who has learned the noise will notice it stop.
            return null;
        }
        // Fluid first, because it is the loudest thing a chest does and the one that
        // cannot be mistaken for effort: this is a chest with something in it.
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).pneumo) {
                return SoundEvents.PLAYER_HURT_DROWN;
            }
        }
        if (body.has(Disease.PNEUMONIA) || body.has(Disease.SMOKE)) {
            return SoundEvents.PLAYER_HURT_DROWN;
        }
        return switch (breathing.character()) {
            // A gasp: the top of a breath that never finishes.
            case Breathing.SHALLOW, Breathing.SLOW -> SoundEvents.AMBIENT_UNDERWATER_EXIT;
            // And somebody working at it.
            case Breathing.LABOURED -> SoundEvents.GENERIC_SWIM;
            default -> null;
        };
    }
}
