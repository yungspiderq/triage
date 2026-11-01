package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

/**
 * An open body loses heat, and a cold body does not clot.
 *
 * <p>This mod already had two thirds of the oldest idea in trauma surgery and did not
 * know it. Hypothermia here stops clotting outright - a bleed that would have closed by
 * itself simply never does - and losing blood cools somebody, which is written into the
 * temperature reading. What was missing was the third side, and it is the one that turns
 * the other two into a circle: <b>the operation itself is what cools the patient.</b>
 *
 * <p>Until now the only way to get cold in here was to stand in powder snow or in the
 * rain in the wrong biome. A man lying opened up on a table in a blizzard caught nothing
 * at all. In life he is the likeliest person in the world to, which is why an operating
 * theatre is kept uncomfortably warm for everybody standing up in it, and why a trauma
 * surgeon's first virtue is speed.
 *
 * <p>So opening somebody takes heat, and where you do it decides how much. A lit room out
 * of the weather costs almost nothing. A cut made in the open, in the snow, at night,
 * costs a great deal - and enough of them, close enough together, is hypothermia, and
 * hypothermia is the thing that stops the bleeding stopping.
 *
 * <p>Nothing new punishes anybody for that. Every consequence was already written and was
 * already correct. All this does is supply the cause that was missing, and the answer it
 * asks for is the one every surgeon would give: somewhere warm, and be quick.
 */
public final class Chill {
    /** Enough light to be standing by something burning. The same test warmth uses. */
    private static final int FIRE_LIGHT = 10;
    /** Biome temperature below which the air itself is working against you. */
    private static final float COLD_BIOME = 0.15F;
    /** And above which it is helping. */
    private static final float WARM_BIOME = 0.9F;
    /** Where they are told, and where a body stops being able to close a wound. */
    private static final float COLD = 0.5F;

    private Chill() {
    }

    /**
     * Somebody has just been opened.
     *
     * <p>Charged per treatment rather than per second, because that is the shape of it:
     * the heat goes while the body is open, and how long that is depends on the job and
     * not on the clock. What varies is where - and that is the whole point, because it is
     * the one thing the person holding the knife gets to choose.
     */
    public static void opened(ServerPlayer healer, ServerPlayer patient) {
        Config.Values config = Config.values();
        if (!config.chill) {
            return;
        }
        BodyHealth body = Body.of(patient);
        float before = body.chill;
        body.chill = Mth.clamp(body.chill + config.chillPerOperation * exposure(patient), 0.0F, 1.0F);
        announce(healer, patient, before, body.chill);
        settles(patient, body, config);
    }

    /**
     * How hard this place is working against a body with a hole in it.
     *
     * <p>One when the weather is doing its worst, near zero in a warm room by a fire. The
     * table, the light and the roof are already worth something to the surgeon's hands;
     * this is the same room asked a different question, and the answers do not have to
     * agree. A cave is a fine place to hold a lamp and a poor place to be cold in.
     */
    public static float exposure(ServerPlayer patient) {
        float exposure = 1.0F;
        if (!patient.level().canSeeSky(patient.blockPosition())) {
            exposure -= 0.35F;
        }
        if (patient.level().getBrightness(LightLayer.BLOCK, patient.blockPosition()) >= FIRE_LIGHT) {
            exposure -= 0.35F;
        }
        float biome = patient.level().getBiome(patient.blockPosition()).value().getBaseTemperature();
        if (biome < COLD_BIOME) {
            exposure += 0.4F;
        } else if (biome > WARM_BIOME) {
            exposure -= 0.2F;
        }
        if (patient.isInWaterOrRain()) {
            exposure += 0.3F;
        }
        return Mth.clamp(exposure, 0.05F, 1.6F);
    }

    /**
     * One second of getting it back, and it only goes one way on its own.
     *
     * <p>Warmth is the only thing that does this, which is the advice: a fire, a roof, and
     * the same doubling by firelight that coming out of hypothermia already uses. There is
     * no item for it either - everything in this mod whose answer is warmth has the same
     * answer, and it is a sensible one to have to learn.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.chill) {
            body.chill = 0.0F;
            return;
        }
        if (body.chill <= 0.0F) {
            return;
        }
        float seconds = interval / 20.0F;
        float back = seconds / Math.max(1, config.chillWarmSeconds);
        if (player.level().getBrightness(LightLayer.BLOCK, player.blockPosition()) >= FIRE_LIGHT) {
            back *= 2.0F;
        }
        if (player.level().getBiome(player.blockPosition()).value().getBaseTemperature() < COLD_BIOME) {
            back *= 0.5F;
        }
        body.chill = Math.max(0.0F, body.chill - back);
    }

    /**
     * Past a point it stops being a reading and becomes the illness.
     *
     * <p>Which is the whole circle closing: the mod already knows that somebody with
     * hypothermia does not clot, so from here a wound that would have shut by itself
     * never does. Nothing about that needed writing. It only needed a way to happen to
     * somebody who was being helped.
     */
    private static void settles(ServerPlayer patient, BodyHealth body, Config.Values config) {
        if (body.chill < config.chillHypothermia || body.has(Disease.HYPOTHERMIA)) {
            return;
        }
        if (body.catchDisease(Disease.HYPOTHERMIA)) {
            Body.notify(patient, Component.translatable("triage.msg.caught",
                    Disease.HYPOTHERMIA.title()), Disease.HYPOTHERMIA.colour(), null);
        }
    }

    /** What it takes off the reading on the forehead. */
    public static float degrees(BodyHealth body) {
        Config.Values config = Config.values();
        return config.chill ? Mth.clamp(body.chill, 0.0F, 1.0F) * config.chillDegrees : 0.0F;
    }

    /**
     * Said once, on the way down, and said to the person who can do something about it.
     *
     * <p>The patient is usually on the floor and often cannot move at all. The one who
     * can carry them somewhere warm, or light a fire, or decide the next cut can wait, is
     * the one holding the knife - so they are the one who gets told. The patient is told
     * too, because it is happening to them.
     */
    private static void announce(ServerPlayer healer, ServerPlayer patient, float before, float now) {
        if (before >= COLD || now < COLD) {
            return;
        }
        Body.notify(patient, Component.translatable("triage.msg.going_cold"),
                ChatFormatting.DARK_AQUA, null);
        if (healer != patient) {
            Body.notify(healer, Component.translatable("triage.msg.going_cold_them", patient.getName()),
                    ChatFormatting.DARK_AQUA, null);
        }
    }
}
