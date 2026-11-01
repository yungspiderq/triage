package com.claudemods.triage.client.screen;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Counting a dose out of a bottle.
 *
 * <p>The painkiller used to share the needle game with the antibiotics, the course and
 * the decompression needle - a mark to hit, four times over, with the difficulty turned
 * down for this one. It was the lightest thing in the mod and it deserved to stay light;
 * it did not deserve to be a fourth copy of somebody else's game.
 *
 * <p>What tablets are is a number. There is one thing you can get wrong with them and it
 * is how many, which is also the one thing this mod already has an opinion about: the
 * body remembers every dose inside the window and each one buys a shorter silence than
 * the last. So the game is the count. Tip the bottle and they come; tip it further and
 * they come faster; and the whole of the skill is easing off in time, because the fourth
 * tablet is not a better dose, it is a tablet you now have to put back.
 *
 * <p>Nothing here can be failed. It can only take longer, which is exactly the weight
 * this treatment should carry.
 */
public final class PillMinigame implements Minigame {
    /** How many come out for an ordinary dose, when nobody says otherwise. */
    private static final int ORDINARY = 2;
    /** How far the hand has to pull down for the bottle to be right over. */
    private static final float FULL_TIP = 52.0F;
    /** Ticks between tablets at full tip. */
    private static final float FASTEST = 7.0F;
    /** Under this the bottle counts as upright again. */
    private static final float UPRIGHT = 0.12F;

    private static final int COLOR_GLASS = 0x66C8D8E0;
    private static final int COLOR_GLASS_EDGE = 0xFF8FA6B2;
    private static final int COLOR_CAP = 0xFFB44B3A;
    private static final int COLOR_LABEL = 0xFFF2ECE0;
    private static final int COLOR_PILL = 0xFFF4F1E6;
    private static final int COLOR_PILL_EDGE = 0xFFB9B2A0;
    private static final int COLOR_PALM = 0xFFB07A5A;
    private static final int COLOR_PALM_EDGE = 0xFF6E4632;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int bottleY;
    private final int palmY;
    /**
     * How many this one is.
     *
     * <p>A course for a cold is not a course for blood poisoning, and the bottle is the
     * one place in the mod where that can be said without a word of explanation: it is
     * three marks under the palm instead of two.
     */
    private final int dose;

    /** 0 upright, 1 straight over. */
    private float tip;
    private boolean held;
    private float sinceLast;
    /** How many are in the palm, and how many have been tipped back. */
    private int out;
    private int spilled;
    private int age;
    private int flash;

    public PillMinigame(int left, int top, int width, int height) {
        this(left, top, width, height, ORDINARY);
    }

    public PillMinigame(int left, int top, int width, int height, int dose) {
        this.dose = Math.max(1, dose);
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;
        this.bottleY = top + 26;
        this.palmY = top + height - 26;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (complete() || !held || tip <= UPRIGHT) {
            sinceLast = 0.0F;
            return;
        }
        // Steeper is faster, and squared rather than straight, so easing off actually
        // does something rather than trimming a tenth off the rate.
        sinceLast += tip * tip;
        if (sinceLast >= FASTEST) {
            sinceLast = 0.0F;
            drop();
        }
    }

    private void drop() {
        if (over()) {
            // Tipping back: they go in one at a time, the same as they came out.
            out--;
            spilled++;
            play(SoundEvents.ITEM_PICKUP, 0.9F);
            return;
        }
        out++;
        play(SoundEvents.STONE_BUTTON_CLICK_ON, 1.1F + 0.08F * out);
        if (out > dose) {
            flash = 16;
        }
    }

    /** Whether the bottle is upside down over the palm or the other way round. */
    private boolean over() {
        return out > dose;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        held = true;
        aim(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        held = true;
        aim(mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            tip = 0.0F;
        }
    }

    /**
     * How far the bottle is tipped: how far the hand has pulled down past the neck.
     *
     * <p>One axis, and the one the hand is holding still on. That was the lesson of the
     * bandage and it applies to everything in here now.
     */
    private void aim(double mouseY) {
        tip = Mth.clamp(((float) mouseY - bottleY) / FULL_TIP, 0.0F, 1.0F);
    }

    @Override
    public float progress() {
        return Mth.clamp(Math.min(out, dose) / (float) dose, 0.0F, 1.0F);
    }

    /**
     * Exactly the dose, and the bottle back upright.
     *
     * <p>The upright half is the whole game. Without it the third tablet would finish
     * this the instant it landed and there would be no way to pour too many - which is
     * the only mistake tablets have.
     */
    @Override
    public boolean complete() {
        return out == dose && tip <= UPRIGHT;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - spilled * 0.2F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (over()) {
            return Component.translatable("triage.minigame.pill_over", out - dose);
        }
        if (out == dose) {
            return Component.translatable("triage.minigame.pill_upright");
        }
        return Component.translatable("triage.minigame.pill_tip", dose - out);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        palm(graphics, font);
        bottle(graphics);
        if (held && tip > UPRIGHT && !complete()) {
            stream(graphics);
        }
    }

    /**
     * The bottle, turned as far as the hand has taken it.
     *
     * <p>Turned, not leaned. The first version of this moved the body by one amount and
     * the neck by another, on the theory that a few pixels of shear would read as a
     * bottle tipping; what it read as was a bottle coming apart in the middle, with the
     * cap floating off on its own. A pill bottle is a rigid object, so it is drawn as a
     * rigid object and the matrix does the turning.
     */
    private void bottle(GuiGraphics graphics) {
        float angle = tip * 168.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, pivotY(), 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.pose().translate(-centreX, -pivotY(), 0.0F);

        int bodyTop = bottleY - 6;
        int bodyBottom = bottleY + 22;
        graphics.fill(centreX - 13, bodyTop, centreX + 13, bodyBottom, COLOR_GLASS_EDGE);
        Draw.vGradient(graphics, centreX - 12, bodyTop + 1, 24, bodyBottom - bodyTop - 2,
                COLOR_GLASS, Draw.alpha(COLOR_GLASS, 0.65F));
        // The label, and two lines of something nobody reads on it.
        graphics.fill(centreX - 9, bodyTop + 7, centreX + 9, bodyBottom - 6, COLOR_LABEL);
        graphics.fill(centreX - 6, bodyTop + 11, centreX + 6, bodyTop + 12, COLOR_PILL_EDGE);
        graphics.fill(centreX - 6, bodyTop + 15, centreX + 2, bodyTop + 16, COLOR_PILL_EDGE);
        // What is still in it, stacked at the bottom of the glass wherever that now is.
        int stillIn = Math.max(0, 8 - out);
        for (int i = 0; i < stillIn; i++) {
            Draw.disc(graphics, centreX - 7 + (i % 4) * 5, bodyBottom - 5 - (i / 4) * 5, 2,
                    Draw.alpha(COLOR_PILL, 0.8F));
        }
        // The neck and the cap, on top of the body where a cap belongs.
        graphics.fill(centreX - 6, bottleY - 12, centreX + 6, bodyTop + 1, COLOR_GLASS_EDGE);
        graphics.fill(centreX - 5, bottleY - 11, centreX + 5, bodyTop, COLOR_GLASS);
        graphics.fill(centreX - 7, bottleY - 18, centreX + 7, bottleY - 11, COLOR_CAP);
        graphics.fill(centreX - 7, bottleY - 18, centreX + 7, bottleY - 17,
                Draw.shade(COLOR_CAP, 1.2F));
        graphics.pose().popPose();
    }

    /** The point the bottle turns about: the middle of the glass. */
    private float pivotY() {
        return bottleY + 8.0F;
    }

    /**
     * Where the mouth has ended up, worked out rather than guessed.
     *
     * <p>The stream has to start at the hole. With the bottle turning about its middle
     * that is a rotated point and nothing else, so it is one line of trigonometry instead
     * of a second set of numbers that can drift out of step with the first.
     */
    private float mouthX() {
        float angle = tip * 168.0F * Mth.DEG_TO_RAD;
        return centreX + Mth.sin(angle) * (pivotY() - (bottleY - 18.0F));
    }

    private float mouthY() {
        float angle = tip * 168.0F * Mth.DEG_TO_RAD;
        return pivotY() - Mth.cos(angle) * (pivotY() - (bottleY - 18.0F));
    }

    /** What is coming out of it, from the mouth down to the palm. */
    private void stream(GuiGraphics graphics) {
        int x = Math.round(mouthX());
        int from = Math.round(mouthY()) + 4;
        for (int y = from; y < palmY - 8; y += 9) {
            int wobble = ((y + age) / 3) % 3 - 1;
            Draw.disc(graphics, x + wobble, y, 2, Draw.alpha(COLOR_PILL, 0.55F));
        }
    }

    /** The palm, what is in it, and how many should be. */
    private void palm(GuiGraphics graphics, Font font) {
        int half = 46;
        graphics.fill(centreX - half - 1, palmY - 1, centreX + half + 1, palmY + 17, COLOR_PALM_EDGE);
        Draw.body(graphics, centreX - half, palmY, half * 2, 16, COLOR_PALM);

        for (int i = 0; i < out; i++) {
            int x = centreX - (Math.min(out, 6) - 1) * 11 / 2 + (i % 6) * 11;
            int y = palmY + 8 - (i / 6) * 7;
            int colour = i < dose ? COLOR_PILL : COLOR_BAD;
            Draw.disc(graphics, x, y, 4, Draw.shade(colour, 0.8F));
            Draw.disc(graphics, x, y - 1, 3, colour);
        }

        // The dose, as marks under the palm: filled for what is there, hollow for what
        // is still in the bottle.
        for (int i = 0; i < dose; i++) {
            int x = centreX - (dose - 1) * 9 / 2 + i * 9;
            int y = palmY + 22;
            int colour = out > dose ? COLOR_BAD : i < out ? COLOR_GOOD : COLOR_MARK;
            if (i < out || out > dose) {
                graphics.fill(x - 3, y - 3, x + 3, y + 3, colour);
            } else {
                graphics.renderOutline(x - 3, y - 3, 6, 6, colour);
            }
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
