package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.PartState;
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
 * Salve on a burn: covering all of it, and not touching the blisters.
 *
 * <p>A burn is the one injury in this mod where the surface <em>is</em> the injury, and
 * the two rules about handling one are both about what you must not do to that surface.
 * Do not rub it - there is nothing under there to take it. And do not break the blisters,
 * because the roof of a blister is the best dressing that wound is ever going to get, and
 * a burst one is an open wound on a burn.
 *
 * <p>So the work is coverage with an exclusion in it. The salve goes on wherever the hand
 * passes, thin and slow; the blisters are lit and stay bare; and touching one bursts it,
 * which does not fail anything - it leaves a raw patch that also has to be covered, and
 * the extra work is the whole of the punishment. It is the only game here where the fastest
 * route across the screen is the wrong one for a reason you can see rather than be told.
 *
 * <p>The depth is passed in and the blisters follow it, though in practice the jar is only
 * ever offered for a blistered burn: red skin closes on its own and the mod refuses the jar
 * for one, so this game opens on the middle rung and no other. The parameter is the coupling
 * rather than a second rule - if what a salve answers ever widens, what is drawn on the burn
 * widens with it instead of being left behind.
 */
public final class SalveMinigame implements Minigame {
    /** The burn, as a grid of squares to be covered. */
    private static final int COLUMNS = 13;
    private static final int ROWS = 9;
    private static final int CELL = 9;
    /** How much of it has to be under salve before it counts as dressed. */
    private static final float ENOUGH = 0.94F;

    /** How wide the finger is, in cells. */
    private static final float SPREAD = 1.4F;
    /** Move faster than this in a tick and it is rubbing rather than spreading. */
    private static final float RUBBING = 7.0F;

    private static final int BLISTERS = 4;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_BURN = 0xFFB4542A;
    private static final int COLOR_BURN_DEEP = 0xFF8A3A18;
    private static final int COLOR_RAW = 0xFFC1483C;
    private static final int COLOR_SALVE = 0xE0E8D9A8;
    private static final int COLOR_BLISTER = 0xFFE8C88A;
    private static final int COLOR_BLISTER_EDGE = 0xFFB08A4A;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_BAD = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int gridLeft;
    private final int gridTop;

    /** 0 bare, 1 under salve. Cells outside the burn are never asked about. */
    private final boolean[] covered = new boolean[COLUMNS * ROWS];
    private final boolean[] burnt = new boolean[COLUMNS * ROWS];
    private final boolean[] raw = new boolean[COLUMNS * ROWS];
    private int burntCells;

    private final float[] blisterX = new float[BLISTERS];
    private final float[] blisterY = new float[BLISTERS];
    private final boolean[] burst = new boolean[BLISTERS];
    /** How many this burn actually has. None at all on one that never blistered. */
    private final int blisters;

    private boolean held;
    private float handX;
    private float handY;
    private float lastX;
    private float lastY;
    private boolean hasLast;
    private int rubFlash;
    private int burstFlash;
    private int spoiled;

    public SalveMinigame(int left, int top, int width, int height, int depth) {
        this.blisters = depth >= PartState.BURN_PARTIAL ? BLISTERS : 0;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.gridLeft = left + (width - COLUMNS * CELL) / 2;
        this.gridTop = top + (height - ROWS * CELL) / 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // A burn is a splash, not a rectangle: a blob with a ragged edge, so covering it
        // is a shape somebody has to look at rather than a box they can wipe.
        float cx = (COLUMNS - 1) / 2.0F;
        float cy = (ROWS - 1) / 2.0F;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                float dx = (x - cx) / (COLUMNS * 0.44F);
                float dy = (y - cy) / (ROWS * 0.46F);
                float edge = dx * dx + dy * dy + (random.nextFloat() - 0.5F) * 0.35F;
                if (edge <= 1.0F) {
                    burnt[y * COLUMNS + x] = true;
                    burntCells++;
                }
            }
        }
        for (int i = 0; i < blisters; i++) {
            int cell;
            do {
                cell = random.nextInt(COLUMNS * ROWS);
            } while (!burnt[cell]);
            blisterX[i] = gridLeft + (cell % COLUMNS) * CELL + CELL / 2.0F;
            blisterY[i] = gridTop + (cell / COLUMNS) * CELL + CELL / 2.0F;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        if (rubFlash > 0) {
            rubFlash--;
        }
        if (burstFlash > 0) {
            burstFlash--;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        hasLast = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        spread();
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
     * One pass of the hand.
     *
     * <p>Fast is not more: past a speed it stops laying anything on and counts as rubbing,
     * which is the thing burns are ruined by. So the only way through is the slow one, and
     * the game says so by simply not covering anything while the hand is hurrying.
     */
    private void spread() {
        if (!hasLast) {
            lastX = handX;
            lastY = handY;
            hasLast = true;
            return;
        }
        float dx = handX - lastX;
        float dy = handY - lastY;
        lastX = handX;
        lastY = handY;
        float speed = Mth.sqrt(dx * dx + dy * dy);

        // A blister is burst by being touched at all, fast or slow.
        for (int i = 0; i < blisters; i++) {
            if (burst[i]) {
                continue;
            }
            if (Math.abs(handX - blisterX[i]) <= 5.0F && Math.abs(handY - blisterY[i]) <= 5.0F) {
                burst[i] = true;
                burstFlash = 18;
                spoiled++;
                openUp(blisterX[i], blisterY[i]);
                play(SoundEvents.SLIME_SQUISH, 0.8F);
            }
        }

        if (speed > RUBBING) {
            rubFlash = 12;
            return;
        }
        int column = Math.round((handX - gridLeft - CELL / 2.0F) / CELL);
        int row = Math.round((handY - gridTop - CELL / 2.0F) / CELL);
        boolean any = false;
        for (int y = row - 2; y <= row + 2; y++) {
            for (int x = column - 2; x <= column + 2; x++) {
                if (x < 0 || y < 0 || x >= COLUMNS || y >= ROWS) {
                    continue;
                }
                int cell = y * COLUMNS + x;
                if (!burnt[cell] || covered[cell]) {
                    continue;
                }
                float px = gridLeft + x * CELL + CELL / 2.0F;
                float py = gridTop + y * CELL + CELL / 2.0F;
                if (Math.abs(px - handX) > SPREAD * CELL || Math.abs(py - handY) > SPREAD * CELL) {
                    continue;
                }
                covered[cell] = true;
                any = true;
            }
        }
        if (any && rubFlash == 0) {
            play(SoundEvents.HONEY_BLOCK_SLIDE, 1.4F);
        }
    }

    /** A burst blister leaves raw skin, and raw skin still has to be covered. */
    private void openUp(float x, float y) {
        int column = Math.round((x - gridLeft - CELL / 2.0F) / CELL);
        int row = Math.round((y - gridTop - CELL / 2.0F) / CELL);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = column + dx;
                int cy = row + dy;
                if (cx < 0 || cy < 0 || cx >= COLUMNS || cy >= ROWS) {
                    continue;
                }
                int cell = cy * COLUMNS + cx;
                if (!burnt[cell]) {
                    burnt[cell] = true;
                    burntCells++;
                }
                raw[cell] = true;
                covered[cell] = false;
            }
        }
    }

    // ---- where it stands --------------------------------------------------

    private float done() {
        int on = 0;
        for (int i = 0; i < covered.length; i++) {
            if (burnt[i] && covered[i]) {
                on++;
            }
        }
        return burntCells == 0 ? 1.0F : on / (float) burntCells;
    }

    @Override
    public float progress() {
        return Mth.clamp(done() / ENOUGH, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return done() >= ENOUGH;
    }

    /** A burn is not a thing anybody walks away from halfway. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - spoiled * 0.2F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (burstFlash > 0) {
            return Component.translatable("triage.minigame.salve_burst");
        }
        if (rubFlash > 0) {
            return Component.translatable("triage.minigame.salve_rub");
        }
        return Component.translatable("triage.minigame.salve_spread");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        graphics.fill(gridLeft - 13, gridTop - 11, gridLeft + COLUMNS * CELL + 13,
                gridTop + ROWS * CELL + 11, COLOR_SKIN_EDGE);
        Draw.body(graphics, gridLeft - 12, gridTop - 10, COLUMNS * CELL + 24, ROWS * CELL + 20, COLOR_SKIN);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                int cell = y * COLUMNS + x;
                if (!burnt[cell]) {
                    continue;
                }
                int px = gridLeft + x * CELL;
                int py = gridTop + y * CELL;
                int base = raw[cell] ? COLOR_RAW : ((x + y) % 2 == 0 ? COLOR_BURN : COLOR_BURN_DEEP);
                graphics.fill(px, py, px + CELL, py + CELL, base);
                if (covered[cell]) {
                    graphics.fill(px, py, px + CELL, py + CELL, COLOR_SALVE);
                    graphics.fill(px, py, px + CELL, py + 1, Draw.alpha(0xFFFFFFFF, 0.18F));
                }
            }
        }

        for (int i = 0; i < blisters; i++) {
            int x = Math.round(blisterX[i]);
            int y = Math.round(blisterY[i]);
            if (burst[i]) {
                continue;
            }
            Draw.disc(graphics, x, y, 5, COLOR_BLISTER_EDGE);
            Draw.disc(graphics, x, y, 4, COLOR_BLISTER);
            Draw.disc(graphics, x - 1, y - 1, 1, 0xFFFFF4D8);
            graphics.renderOutline(x - 6, y - 6, 12, 12,
                    burstFlash > 0 ? COLOR_BAD : Draw.alpha(COLOR_MARK, 0.6F));
        }

        if (held && !complete()) {
            int x = Math.round(Mth.clamp(handX, left + 4, left + width - 4));
            int y = Math.round(Mth.clamp(handY, top + 4, top + height - 4));
            int colour = rubFlash > 0 ? COLOR_BAD : COLOR_SALVE;
            int r = Math.round(SPREAD * CELL);
            graphics.renderOutline(x - r, y - r, r * 2, r * 2, colour);
            Draw.disc(graphics, x, y, 3, colour);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
