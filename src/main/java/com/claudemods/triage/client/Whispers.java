package com.claudemods.triage.client;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Disease;
import java.util.List;
import net.minecraft.ChatFormatting;
import com.claudemods.triage.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Everything that is heard, read or half-seen and is not there.
 *
 * <p>All of it on the client. The server used to send the voices as chat, which worked
 * and was wrong in a way worth fixing: none of this exists, so nothing about it should
 * travel. A hallucination that leaves the machine it happens on is a broadcast.
 *
 * <p>Four kinds, and the last one is the one people describe most and games never do.
 * A noise out of place. A line of text with nobody attached to it. A word across the
 * middle of the screen. And a message in chat with somebody's name on it that they did
 * not write - which is not a trick played on the player so much as the plainest way to
 * render the thing itself, because that is exactly what being unable to tell your own
 * thoughts from somebody else's speech is like.
 *
 * <p>None of the lines is an instruction. Real voices comment, criticise and repeat
 * themselves; the ones that hand out orders are a thing films made up, and a mod that
 * claims to be accurate does not get to borrow them.
 */
public final class Whispers {
    /** How many of each there are in the language file. */
    private static final int LINES = 40;
    private static final int SAID = 24;
    private static final int SCREEN_TICKS = 70;

    private static @Nullable Component onScreen;
    private static int screenLeft;
    private static float screenX;
    private static float screenY;

    private Whispers() {
    }

    /** What is written across the screen just now, or null. */
    public static @Nullable Component screenText() {
        return onScreen;
    }

    /** 0 to 1, for how solid it should be drawn. */
    public static float screenAlpha() {
        if (onScreen == null) {
            return 0.0F;
        }
        // Up over the first half second, down over the last.
        float left = screenLeft / (float) SCREEN_TICKS;
        return Mth.clamp(Math.min(left * 3.0F, (1.0F - left) * 6.0F), 0.0F, 1.0F);
    }

    public static float screenX() {
        return screenX;
    }

    public static float screenY() {
        return screenY;
    }

    public static void tick(Minecraft minecraft) {
        if (screenLeft > 0 && --screenLeft <= 0) {
            onScreen = null;
        }
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        BodyHealth body = ClientBody.get();
        if (!body.has(Disease.SCHIZOPHRENIA) || body.medicated()) {
            return;
        }
        int seconds = Config.values().whisperSeconds;
        if (seconds <= 0) {
            return;
        }
        float chance = 1.0F / (seconds * 20.0F) * (body.rebounding() ? 2.0F : 1.0F);
        RandomSource random = minecraft.player.getRandom();
        if (random.nextFloat() >= chance) {
            return;
        }
        switch (random.nextInt(4)) {
            case 0 -> noise(minecraft, random);
            case 1 -> line(minecraft, random);
            case 2 -> onScreen(minecraft, random);
            default -> saidBySomebody(minecraft, random);
        }
    }

    /**
     * An ordinary sound of the world, from where nothing is - or somebody talking.
     *
     * <p>The borrowed ones are honest: a hallucinated door should sound exactly like a
     * door, because the whole trouble with one is that there is no way to tell. The voice
     * is the one thing that could not be borrowed from anything, and a symptom that is
     * entirely text is a symptom you read rather than one you have.
     *
     * <p>Weighted towards it, because it is the one people mean when they talk about
     * this. The door is the noise around it.
     */
    private static void noise(Minecraft minecraft, RandomSource random) {
        if (random.nextInt(10) < 4) {
            // Close and quiet, because that is where it always is.
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    ModSounds.WHISPER.get(),
                    0.92F + random.nextFloat() * 0.16F, 0.55F + random.nextFloat() * 0.25F));
            return;
        }
        SoundEvent sound = switch (random.nextInt(5)) {
            case 0 -> SoundEvents.WOODEN_DOOR_OPEN;
            case 1 -> SoundEvents.GRAVEL_STEP;
            case 2 -> SoundEvents.AMBIENT_CAVE.value();
            case 3 -> SoundEvents.VILLAGER_AMBIENT;
            default -> SoundEvents.PLAYER_BREATH;
        };
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                sound, 0.7F + random.nextFloat() * 0.6F, 0.25F + random.nextFloat() * 0.3F));
    }

    /** A line in the chat with nobody attached to it. */
    private static void line(Minecraft minecraft, RandomSource random) {
        minecraft.gui.getChat().addMessage(
                Component.translatable("triage.voice." + (1 + random.nextInt(LINES)))
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** A word across the middle of everything, gone before it can be read twice. */
    private static void onScreen(Minecraft minecraft, RandomSource random) {
        onScreen = Component.translatable("triage.voice." + (1 + random.nextInt(LINES)));
        screenLeft = SCREEN_TICKS;
        // Somewhere near the middle but never exactly on it: a thing that appears in the
        // same place every time is a readout, and this is not one.
        screenX = 0.5F + (random.nextFloat() - 0.5F) * 0.4F;
        screenY = 0.42F + (random.nextFloat() - 0.5F) * 0.25F;
    }

    /**
     * A message in chat, with somebody else's name on it, that they did not write.
     *
     * <p>The one that has to be indistinguishable from the real thing to be worth
     * anything, so it is built out of the same component the server would have sent and
     * goes into the same chat line. Nobody else sees it; the person whose name is on it
     * least of all.
     *
     * <p>It only happens with somebody else on the server, which is the honest limit of
     * it: this is a symptom about other people, and on your own there is nobody for it
     * to borrow.
     */
    private static void saidBySomebody(Minecraft minecraft, RandomSource random) {
        if (minecraft.getConnection() == null || minecraft.player == null) {
            return;
        }
        List<PlayerInfo> online = minecraft.getConnection().getOnlinePlayers().stream()
                .filter(info -> !info.getProfile().getId().equals(minecraft.player.getUUID()))
                .toList();
        if (online.isEmpty()) {
            line(minecraft, random);
            return;
        }
        PlayerInfo who = online.get(random.nextInt(online.size()));
        Component name = who.getTabListDisplayName() != null
                ? who.getTabListDisplayName()
                : Component.literal(who.getProfile().getName());
        saidBy(minecraft, name);
    }

    /**
     * One line, with that name on it.
     *
     * <p>Public because the figures use it too: one that turns round and watches you
     * sometimes says something first, and it says it as whoever it is wearing. That is
     * the two symptoms meeting, and it is the closest this gets to being frightening.
     */
    public static void saidBy(Minecraft minecraft, Component name) {
        if (minecraft.player == null) {
            return;
        }
        int line = 1 + minecraft.player.getRandom().nextInt(SAID);
        minecraft.gui.getChat().addMessage(Component.translatable("chat.type.text", name,
                Component.translatable("triage.said." + line)));
    }

    /** Left the world: nothing half-written on the screen should survive it. */
    public static void forget() {
        onScreen = null;
        screenLeft = 0;
    }
}
