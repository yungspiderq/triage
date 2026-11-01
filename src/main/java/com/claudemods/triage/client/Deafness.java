package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.BodyHealth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import com.claudemods.triage.client.ClientHooks.PlaySoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Actually going deaf.
 *
 * <p>The illness existed before this and it took away the mod's own sounds - the heartbeat,
 * the beats under the fingers - which was the honest half of the job and about a tenth of
 * the effect. Somebody standing in a world that is still perfectly audible is not deafened,
 * they are reading that they are, and this mod is not supposed to have any of those.
 *
 * <p>So every sound the world makes is turned down to almost nothing while it lasts. Not
 * silenced: muffled. Being blown up does not switch the world off, it puts it behind a
 * wall, and the difference is the whole feeling of it - you can tell something happened
 * and not what.
 *
 * <p>What is left at full volume is the interface: a screen a player is looking at is not
 * a thing they are hearing across a room, and taking its clicks away would be taking away
 * the game rather than the hearing. The one exception is deliberate and lives elsewhere -
 * the pulse a helper is trying to count by ear goes silent too, because that one is the
 * point.
 *
 * <p>And it happens the way it happens. Not a flat setting that switches off after a
 * minute and a half:
 *
 * <ul>
 *   <li><b>The world goes almost completely out at the moment of the blast</b>, and comes
 *       back gradually. That curve is the whole experience - anybody who has been near
 *       one describes the silence first and the coming-back second.</li>
 *   <li><b>The ringing is one continuous tone</b>, because tinnitus is a tone. A beep
 *       every few seconds is a smoke alarm. It is loudest at the start, when there is
 *       nothing else to hear, and fades out with the rest of it.</li>
 * </ul>
 */
public final class Deafness {
    /** What the world is worth at the moment of the blast. Almost nothing. */
    private static final float WORST = 0.04F;
    /** And how loud the ring is then. */
    private static final float RING_LOUDEST = 0.30F;

    private static @Nullable Ring ring;

    private Deafness() {
    }

    /** Whether the player at this client cannot hear. */
    public static boolean deaf() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        return ClientBody.get().has(Disease.DEAFNESS);
    }

    /**
     * How far through it they are: 0 at the blast, 1 as it lifts.
     *
     * <p>Everything here is drawn off this one number, so the silence and the ring go
     * together the way they do in a head - the quieter it is, the louder the ring, and
     * both let go at the same time.
     */
    private static float through() {
        int full = Disease.DEAFNESS.duration();
        if (full <= 0) {
            return 1.0F;
        }
        return Mth.clamp(ClientBody.get().diseaseTicks(Disease.DEAFNESS) / (float) full,
                0.0F, 1.0F);
    }

    /** What a world sound is worth right now. */
    private static float hearing() {
        if (!deaf()) {
            return 1.0F;
        }
        // Squared, so most of the recovery happens late. Hearing does not fade back in
        // evenly; it stays gone and then arrives.
        float t = through();
        return Mth.lerp(t * t, WORST, 1.0F);
    }

    /**
     * The room receding, which is not deafness and arrives the same way.
     *
     * <p>Ask anybody who has fainted what happened first and they do not say the edges
     * closed in. They say the voices went far away. It is the oldest warning sign there
     * is, it costs a player nothing to understand, and this mod drew nine versions of
     * going out without ever once turning the volume down.
     *
     * <p>It is also the only symptom here in a channel of its own. Everything on the
     * screen is competing with everything else on the screen by the time somebody is this
     * far gone - the edges are in, the specks are up, the grey is over all of it - and a
     * player who has stopped reading the picture will still notice the world going quiet.
     */
    private static float fading() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return 1.0F;
        }
        BodyHealth body = ClientBody.get();
        // Out is out: a body that is not there is not hearing the room either.
        if (body.fainted() || body.under()) {
            return 0.12F;
        }
        // Nothing at all until the last third of somebody's volume, and then quickly.
        float gone = Mth.clamp((body.bloodLoss() - 0.55F) / 0.45F, 0.0F, 1.0F);
        return Mth.lerp(gone * gone, 1.0F, 0.30F);
    }

    public static void onSound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        // Two different reasons for the same quiet, and a head can have both: ears that
        // were blown out, and a body that is nearly gone. Whichever is worse wins.
        float share = Math.min(hearing(), fading());
        if (share >= 0.995F) {
            return;
        }
        // A screen is looked at, not heard across a room. Its clicks stay. The ring is
        // inside the head and is not something the head can be deaf to.
        if (sound.getSource() == SoundSource.MASTER || sound.getSource() == SoundSource.MUSIC
                || sound instanceof Ring) {
            return;
        }
        event.setSound(new Muffled(sound, share));
    }

    /** Starts the tone when the ears go, and lets the tone look after itself after that. */
    public static void onTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null) {
            return;
        }
        if (!deaf()) {
            ring = null;
            return;
        }
        if (ring == null || ring.isStopped()) {
            ring = new Ring();
            client.getSoundManager().play(ring);
        }
    }

    /**
     * The tone.
     *
     * <p>One continuous note rather than a beep every few seconds, because that is what
     * tinnitus is and a beep every few seconds is a smoke alarm. It rides the same curve
     * as the silence: loudest when there is nothing else to hear, and gone when the rest
     * comes back.
     */
    private static final class Ring extends AbstractTickableSoundInstance {
        private Ring() {
            super(SoundEvents.BEACON_AMBIENT, SoundSource.MASTER, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.pitch = 2.0F;
            this.volume = RING_LOUDEST;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (!deaf()) {
                stop();
                this.volume = 0.0F;
                return;
            }
            float left = 1.0F - through();
            this.volume = RING_LOUDEST * left * left;
        }
    }

    /**
     * The same sound, a long way off.
     *
     * <p>A delegate rather than a rebuilt {@link SimpleSoundInstance}, so that whatever it
     * was - a looping one, one attached to an entity, one another mod made - stays exactly
     * what it was in every respect except how loud.
     */
    private record Muffled(SoundInstance from, float share) implements SoundInstance {
        @Override
        public ResourceLocation getLocation() {
            return from.getLocation();
        }

        @Override
        public @Nullable WeighedSoundEvents resolve(SoundManager manager) {
            return from.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return from.getSound();
        }

        @Override
        public SoundSource getSource() {
            return from.getSource();
        }

        @Override
        public boolean isLooping() {
            return from.isLooping();
        }

        @Override
        public boolean isRelative() {
            return from.isRelative();
        }

        @Override
        public int getDelay() {
            return from.getDelay();
        }

        @Override
        public float getVolume() {
            return from.getVolume() * share;
        }

        @Override
        public float getPitch() {
            return from.getPitch();
        }

        @Override
        public double getX() {
            return from.getX();
        }

        @Override
        public double getY() {
            return from.getY();
        }

        @Override
        public double getZ() {
            return from.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return from.getAttenuation();
        }

        @Override
        public boolean canPlaySound() {
            return from.canPlaySound();
        }

        @Override
        public boolean canStartSilent() {
            return from.canStartSilent();
        }
    }
}
