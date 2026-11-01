package com.claudemods.triage.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A needle between the ribs, and the one rule that decides whether it works or maims.
 *
 * <p>This shared a game with the antibiotics, the course and the painkillers: a mark, and
 * a hand that had to land on it. Which is nearly the right shape - it <em>is</em> a needle
 * finding a place - and entirely the wrong place. The mark was drawn in mid-air. The
 * whole difficulty of this procedure is that the place is defined by two ribs, and by
 * which of the two you go along.
 *
 * <p><b>Over the top of the lower rib, never under the upper one.</b> The vessels and the
 * nerve for each rib run in a groove along its <em>underside</em>, so a needle that hugs
 * the rib above goes straight into them, and one that hugs the rib below goes into the
 * space and nothing else. It is the first thing taught about the procedure and it is
 * invisible in every game that has ever included it.
 *
 * <p>So the space is drawn between two ribs, the groove under the upper one is drawn as
 * the thing it is, and going in is a push - not a click - because a needle goes in until
 * something changes and then stops. What changes is the hiss.
 */
public final class NeedleMinigame implements Minigame {
    /** How wide the gap between the ribs is, and how much of it is the safe half. */
    private static final float GAP = 22.0F;
    private static final float GROOVE = 8.0F;

    /** How deep the needle has to go, and how far past that is too far. */
    private static final float DEPTH = 42.0F;
    private static final float TOO_DEEP = 58.0F;
    /** How fast it may be pushed before it is a stab rather than an advance. */
    private static final float MAX_STEP = 2.2F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_FLESH = 0xFF7A4438;
    private static final int COLOR_RIB = 0xFFE4DCC4;
    private static final int COLOR_RIB_EDGE = 0xFFB6AC92;
    private static final int COLOR_GROOVE = 0xFF8E1616;
    private static final int COLOR_SPACE = 0x33FFE070;
    private static final int COLOR_SPACE_EDGE = 0x99FFE070;
    private static final int COLOR_NEEDLE = 0xFFB8BCC4;
    private static final int COLOR_HUB = 0xFF6E757C;
    private static final int COLOR_AIR = 0x90BFE4E8;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int skinX;
    private final int upperRibY;
    private final int lowerRibY;

    /** Where the needle is aimed, and how far in it is. */
    private float aimY;
    private float depth;
    private boolean held;
    private float lastX;
    private boolean hasLast;
    private int hits;
    private int flash;
    private int age;
    private boolean hissing;

    public NeedleMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.skinX = areaLeft + Math.round(areaWidth * 0.30F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // Not the same two ribs every time: the space is a thing to find.
        int middle = areaTop + areaHeight / 2 + random.nextInt(17) - 8;
        this.upperRibY = Math.round(middle - GAP / 2.0F);
        this.lowerRibY = Math.round(middle + GAP / 2.0F);
        this.aimY = middle;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        lastX = (float) mouseX;
        hasLast = true;
        if (depth <= 0.0F) {
            aimY = (float) mouseY;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        if (depth <= 0.0F) {
            // Still outside: the hand is choosing the space.
            aimY = (float) mouseY;
        }
        float step = (float) mouseX - lastX;
        lastX = (float) mouseX;
        if (!hasLast) {
            hasLast = true;
            return true;
        }
        if (step <= 0.0F) {
            depth = Math.max(0.0F, depth + step);
            return true;
        }
        if (step > MAX_STEP) {
            // Driven rather than advanced. A needle goes in until something changes.
            step = MAX_STEP;
            strike(SoundEvents.PLAYER_HURT, "hurried");
        }
        push(step);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            hasLast = false;
        }
    }

    /**
     * The needle goes in, and what it meets on the way is decided by where it was aimed.
     *
     * <p>Three answers: bone if it is on a rib, the bundle if it is hugging the underside
     * of the upper one, and nothing at all if it is in the space - which is the answer
     * everybody wants and the one that feels like nothing is happening, because that is
     * what it is like.
     */
    private void push(float step) {
        if (aimY <= upperRibY + 3.0F || aimY >= lowerRibY - 3.0F) {
            strike(SoundEvents.BONE_BLOCK_HIT, "bone");
            return;
        }
        if (aimY < upperRibY + GROOVE) {
            strike(SoundEvents.SLIME_SQUISH, "bundle");
            return;
        }
        depth = Math.min(TOO_DEEP, depth + step);
        if (!hissing && depth >= DEPTH) {
            hissing = true;
            play(SoundEvents.BOTTLE_EMPTY, 1.4F);
        }
    }

    private void strike(SoundEvent sound, String why) {
        if (flash > 0) {
            return;
        }
        hits++;
        flash = 16;
        play(sound, 0.8F);
        // The needle does not go in when it is on bone, and it comes back out of the
        // bundle. Either way the hand has to start again from the skin.
        depth = 0.0F;
        hissing = false;
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return Mth.clamp(depth / DEPTH, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return hissing && depth >= DEPTH && depth < TOO_DEEP;
    }

    /** It can be got wrong repeatedly. It cannot be walked away from. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - hits * 0.18F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (flash > 0) {
            if (aimY <= upperRibY + 3.0F || aimY >= lowerRibY - 3.0F) {
                return Component.translatable("triage.minigame.needle_bone");
            }
            if (aimY < upperRibY + GROOVE) {
                return Component.translatable("triage.minigame.needle_bundle");
            }
            return Component.translatable("triage.minigame.needle_hurry");
        }
        if (depth <= 0.0F) {
            return Component.translatable("triage.minigame.needle_find");
        }
        return Component.translatable("triage.minigame.needle_in");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        chest(graphics);
        needle(graphics);
    }

    /** Two ribs, the space between them, and the groove that makes one edge of it wrong. */
    private void chest(GuiGraphics graphics) {
        int chestRight = left + width - 12;
        graphics.fill(skinX, top + 8, chestRight, top + height - 8, COLOR_FLESH);
        graphics.fill(skinX - 4, top + 8, skinX, top + height - 8, COLOR_SKIN);
        graphics.fill(skinX - 5, top + 8, skinX - 4, top + height - 8, COLOR_SKIN_EDGE);

        for (int y : new int[]{upperRibY, lowerRibY}) {
            graphics.fill(skinX, y - 7, chestRight, y + 7, COLOR_RIB_EDGE);
            graphics.fill(skinX, y - 6, chestRight, y + 6, COLOR_RIB);
        }
        // The bundle in its groove, under the lower edge of the upper rib.
        graphics.fill(skinX, upperRibY + 7, chestRight, upperRibY + 10, COLOR_GROOVE);

        // The half of the space that is the space.
        int safeTop = Math.round(upperRibY + GROOVE);
        int safeBottom = lowerRibY - 3;
        graphics.fill(skinX, safeTop, chestRight, safeBottom, COLOR_SPACE);
        graphics.fill(skinX, safeTop, chestRight, safeTop + 1, COLOR_SPACE_EDGE);
        graphics.fill(skinX, safeBottom - 1, chestRight, safeBottom, COLOR_SPACE_EDGE);

        // The air coming out, once it is out.
        if (hissing) {
            for (int i = 0; i < 4; i++) {
                int x = skinX - 10 - ((age * 2 + i * 9) % 34);
                Draw.disc(graphics, x, Math.round(aimY) - 2 - i, 2, COLOR_AIR);
            }
        }
    }

    /** The needle, at the height it is aimed and the depth it has reached. */
    private void needle(GuiGraphics graphics) {
        int y = Math.round(Mth.clamp(aimY, top + 12, top + height - 12));
        int tip = skinX + Math.round(depth);
        int hub = skinX - 46 + Math.round(depth);
        boolean good = aimY >= upperRibY + GROOVE && aimY <= lowerRibY - 3.0F;
        int colour = flash > 0 ? COLOR_BAD : good ? COLOR_NEEDLE : COLOR_BAD;

        graphics.fill(hub, y - 1, tip, y + 1, colour);
        graphics.fill(hub - 12, y - 4, hub, y + 4, COLOR_HUB);
        if (depth <= 0.0F) {
            // Where it would go, before it goes.
            Draw.line(graphics, skinX, y, left + width - 14, y, 1,
                    Draw.alpha(good ? COLOR_GOOD : COLOR_BAD, 0.45F));
        }

        // How far in, and where far enough is.
        int barLeft = left + 8;
        int barTop = top + height - 6;
        int barWidth = width - 16;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, 0x40FFFFFF);
        int mark = barLeft + Math.round(barWidth * DEPTH / TOO_DEEP);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * depth / TOO_DEEP), barTop + 3,
                hissing ? COLOR_GOOD : COLOR_NEEDLE);
        graphics.fill(mark, barTop - 2, mark + 1, barTop + 5, COLOR_SPACE_EDGE);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
