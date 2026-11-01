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
 * Getting an arrow out, which begins with the question of which way it is coming.
 *
 * <p>The game this replaces had one answer: draw it back along the line it came in on,
 * slowly and straight. That is a fine test of a hand and it is the wrong instruction. A
 * broadhead has barbs. Barbs are shaped to hold going forwards and to catch going
 * backwards, and dragging one back up the channel it made turns a wound the width of a
 * blade into a wound the width of everything the blade tears on the way.
 *
 * <p>There are two right answers, and the arrow tells you which. If the point has carried
 * far enough to be pressing on the far side - and you can see that, because the skin over
 * it is <b>tented</b> - it goes on through and the head comes off out there, where it is
 * doing no more harm. If it has not, the point is deep, and the only way back is to open
 * the entry first so the barbs have somewhere to come through rather than something to
 * tear.
 *
 * <p>Which makes this the one game in the mod where the first thing to do is look. The
 * tenting is not a hint printed on the screen; it is the bump the point is making, in the
 * place it would make it.
 *
 * <p>It is also still the only treatment here that can leave somebody worse off than
 * before you helped, and the only one that can be given up on. Waiting for steadier hands
 * is a real plan.
 */
public final class ArrowMinigame implements Minigame {
    private static final int MAX_TEARS = 3;
    /** How thick the limb is along the line the arrow took. */
    private static final float THICKNESS = 74.0F;
    /** Past this share of the way through, the point is tenting the far skin. */
    private static final float TENTING = 0.66F;
    /** How far off the line the shaft may be levered before it starts cutting. */
    private static final float OFF_LINE = 9.0F;
    /** How much has to be cut across the entry before the barbs have room. */
    private static final float CUT_NEEDED = 26.0F;
    /** How far past the far surface the head has to go to be clear of it. */
    private static final float THROUGH = 16.0F;
    /** How far back the shaft has to come to be out. */
    private static final float OUT = -34.0F;

    private static final double ANGLE = Math.toRadians(24.0);

    private static final int COLOR_FLESH = 0xFF6E3A34;
    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_WOUND = 0xFF3A1614;
    private static final int COLOR_LINE = 0x30FFFFFF;
    private static final int COLOR_SHAFT = 0xFFC8A165;
    private static final int COLOR_FLETCH = 0xFFE8E4F0;
    private static final int COLOR_HEAD = 0xFFB8BCC4;
    private static final int COLOR_TENT = 0xFFD8A08A;
    private static final int COLOR_CUT = 0xFFFFE070;
    private static final int COLOR_TEAR = 0xFFCC3333;
    private static final int COLOR_TEAR_EMPTY = 0x40FFFFFF;
    private static final int COLOR_GOOD = 0xFF7FD46B;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int entryX;
    private final int entryY;
    private final float axisX;
    private final float axisY;

    /** How far in the point is, in pixels along the line. 0 is the entry. */
    private float head;
    /** Which way this one has to come, decided by where it stopped. */
    private final boolean tented;

    private boolean cut;
    private float cutDone;
    private boolean through;
    private boolean held;
    private float lastAlong;
    private float lastOff;
    private int tears;
    private int flash;

    public ArrowMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.axisX = (float) Math.cos(ANGLE);
        this.axisY = (float) Math.sin(ANGLE);
        this.entryX = areaLeft + Math.round(areaWidth * 0.19F);
        this.entryY = areaTop + Math.round(areaHeight * 0.24F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        float depth = 0.30F + random.nextFloat() * 0.62F;
        this.head = THICKNESS * depth;
        this.tented = depth >= TENTING;
        this.lastAlong = head;
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete() || failed()) {
            return false;
        }
        held = true;
        lastAlong = along(mouseX, mouseY);
        lastOff = off(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete() || failed()) {
            return false;
        }
        float nowAlong = along(mouseX, mouseY);
        float nowOff = off(mouseX, mouseY);
        float step = nowAlong - lastAlong;
        float across = nowOff - lastOff;
        lastAlong = nowAlong;
        lastOff = nowOff;

        // Cutting down onto the head: a stroke across the entry rather than along it,
        // and only worth doing on the one that has to come back.
        if (!tented && !cut && nowAlong < 26.0F && Math.abs(across) > Math.abs(step)) {
            cutDone += Math.abs(across);
            if (cutDone >= CUT_NEEDED) {
                cut = true;
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F);
            }
            return true;
        }
        if (Math.abs(nowOff) > OFF_LINE) {
            // Levered sideways. A shaft is a lever and the head is on the end of it.
            tear(SoundEvents.SLIME_SQUISH);
            return true;
        }
        move(step);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
        }
    }

    /**
     * The shaft moves along its own line, forwards or back, and which of those is right
     * was decided before the player touched it.
     */
    private void move(float step) {
        if (Math.abs(step) < 0.01F) {
            return;
        }
        boolean forward = step > 0.0F;

        if (through) {
            // The head is off out there. What is left is a smooth shaft, and it comes
            // back the way it came - which is now the safe direction and the only one.
            if (!forward) {
                head = Math.max(OUT - 6.0F, head + step);
            }
            return;
        }
        if (tented) {
            if (!forward) {
                tear(SoundEvents.PLAYER_HURT);
                return;
            }
            head = Math.min(THICKNESS + THROUGH + 2.0F, head + step);
            if (head >= THICKNESS + THROUGH) {
                through = true;
                play(SoundEvents.ITEM_BREAK, 0.9F);
            }
            return;
        }
        // Deep. Forwards is more tissue for nothing; backwards is the barbs, unless the
        // entry has been opened for them.
        if (forward || !cut) {
            tear(SoundEvents.PLAYER_HURT);
            return;
        }
        head = Math.max(OUT - 6.0F, head + step);
    }

    private float along(double mouseX, double mouseY) {
        return (float) ((mouseX - entryX) * axisX + (mouseY - entryY) * axisY);
    }

    private float off(double mouseX, double mouseY) {
        return (float) ((mouseX - entryX) * -axisY + (mouseY - entryY) * axisX);
    }

    private void tear(SoundEvent sound) {
        // One tear per moment rather than one per frame: a hand that is wrong for half a
        // second has been wrong once, not thirty times.
        if (flash > 0) {
            return;
        }
        tears++;
        flash = 12;
        play(sound, 0.8F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        if (through) {
            return Mth.clamp(0.6F + 0.4F * (1.0F - (head - OUT) / (THICKNESS + THROUGH - OUT)),
                    0.6F, 1.0F);
        }
        if (tented) {
            return Mth.clamp(0.6F * (head / (THICKNESS + THROUGH)), 0.0F, 0.6F);
        }
        float opened = cut ? 0.3F : 0.3F * (cutDone / CUT_NEEDED);
        return Mth.clamp(opened + 0.7F * (1.0F - Mth.clamp(head / THICKNESS, 0.0F, 1.0F)), 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return head <= OUT;
    }

    /** Three tears and it has come out badly. The hands were free; the patient was not. */
    @Override
    public boolean failed() {
        return tears >= MAX_TEARS;
    }

    @Override
    public float quality() {
        return Math.max(0.0F, 1.0F - tears / (float) MAX_TEARS);
    }

    @Override
    public Component instruction() {
        if (flash > 0) {
            return Component.translatable("triage.minigame.arrow_tearing");
        }
        if (through) {
            return Component.translatable("triage.minigame.arrow_back");
        }
        if (tented) {
            return Component.translatable("triage.minigame.arrow_through");
        }
        if (!cut) {
            return Component.translatable("triage.minigame.arrow_cut");
        }
        return Component.translatable("triage.minigame.arrow_draw");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        shaft(graphics);
        tearPips(graphics);
    }

    private int px(float alongAxis, float offAxis) {
        return entryX + Math.round(axisX * alongAxis - axisY * offAxis);
    }

    private int py(float alongAxis, float offAxis) {
        return entryY + Math.round(axisY * alongAxis + axisX * offAxis);
    }

    /**
     * The limb in section, with the near surface the arrow went in at and the far one the
     * point is or is not pressing on.
     *
     * <p>Drawn along the line rather than as an upright box, so both surfaces are where
     * the arithmetic says they are instead of where they look about right. The tenting has
     * to be trustworthy: it is the thing the whole decision is read off.
     */
    private void limb(GuiGraphics graphics) {
        for (float o = -34.0F; o <= 34.0F; o += 1.0F) {
            Draw.line(graphics, px(0.0F, o), py(0.0F, o), px(THICKNESS, o), py(THICKNESS, o), 1, COLOR_FLESH);
        }
        Draw.line(graphics, px(0.0F, -34.0F), py(0.0F, -34.0F), px(0.0F, 34.0F), py(0.0F, 34.0F),
                3, COLOR_SKIN);
        Draw.line(graphics, px(THICKNESS, -34.0F), py(THICKNESS, -34.0F),
                px(THICKNESS, 34.0F), py(THICKNESS, 34.0F), 3, COLOR_SKIN);

        Draw.line(graphics, px(-52.0F, 0.0F), py(-52.0F, 0.0F),
                px(THICKNESS + 42.0F, 0.0F), py(THICKNESS + 42.0F, 0.0F), 1, COLOR_LINE);
        Draw.disc(graphics, entryX, entryY, 5, COLOR_WOUND);

        // The point pressing the far skin up, which is the whole decision.
        if (!through && head > THICKNESS * TENTING) {
            float over = Mth.clamp((head - THICKNESS * TENTING) / (THICKNESS * (1.0F - TENTING)),
                    0.0F, 1.0F);
            for (float o = -9.0F; o <= 9.0F; o += 1.0F) {
                float rise = (1.0F - Math.abs(o) / 9.0F) * 7.0F * over;
                Draw.disc(graphics, px(THICKNESS + rise, o), py(THICKNESS + rise, o), 2, COLOR_TENT);
            }
        }

        // Where to cut, and the cut once it is made.
        if (!tented && !through) {
            if (cut) {
                Draw.line(graphics, px(6.0F, -12.0F), py(6.0F, -12.0F), px(6.0F, 12.0F), py(6.0F, 12.0F),
                        3, COLOR_WOUND);
            } else {
                int colour = Draw.alpha(COLOR_CUT, 0.35F + 0.55F * (cutDone / CUT_NEEDED));
                Draw.line(graphics, px(6.0F, -12.0F), py(6.0F, -12.0F), px(6.0F, 12.0F), py(6.0F, 12.0F),
                        1, colour);
            }
        }
    }

    /** The arrow: what is still in, what is out, and the head on the end of it. */
    private void shaft(GuiGraphics graphics) {
        float nock = head - 62.0F;
        int nockX = px(nock, 0.0F);
        int nockY = py(nock, 0.0F);
        int headX = px(head, 0.0F);
        int headY = py(head, 0.0F);
        int colour = flash > 0 ? COLOR_TEAR : COLOR_SHAFT;

        Draw.line(graphics, nockX, nockY, headX, headY, 3, colour);
        Draw.line(graphics, px(nock + 8.0F, -5.0F), py(nock + 8.0F, -5.0F),
                px(nock + 8.0F, 5.0F), py(nock + 8.0F, 5.0F), 2, COLOR_FLETCH);
        Draw.disc(graphics, nockX, nockY, 4, held ? COLOR_FLETCH : COLOR_SHAFT);

        if (through) {
            // Snapped off on the far side, where it is doing no more harm.
            Draw.disc(graphics, px(THICKNESS + THROUGH + 6.0F, 0.0F),
                    py(THICKNESS + THROUGH + 6.0F, 0.0F), 3, COLOR_GOOD);
            return;
        }
        // The head, and the barbs that decide everything, as a wedge pointing inwards.
        Draw.line(graphics, px(head - 9.0F, -4.0F), py(head - 9.0F, -4.0F), headX, headY, 2, COLOR_HEAD);
        Draw.line(graphics, px(head - 9.0F, 4.0F), py(head - 9.0F, 4.0F), headX, headY, 2, COLOR_HEAD);
    }

    private void tearPips(GuiGraphics graphics) {
        for (int i = 0; i < MAX_TEARS; i++) {
            int x = left + width - 10 - i * 7;
            graphics.fill(x, top + 4, x + 4, top + 8, i < tears ? COLOR_TEAR : COLOR_TEAR_EMPTY);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
