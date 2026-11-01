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
 * Cutting a burn off somebody, down to the part that is still alive.
 *
 * <p>The operation for a burn that went through the skin, and the reason it needs a knife
 * at all: what is on the arm is not injured tissue, it is dead tissue, and dead tissue does
 * not heal, does not take a salve, and is where everything that goes wrong afterwards
 * starts. It has to come off. All of it, and nothing else.
 *
 * <p>Which is a question with no gauge and no number attached: <b>how do you know when you
 * have reached living skin?</b> And the answer, the real one, is the best thing in this
 * whole operation - <b>it bleeds.</b> Dead tissue is dry and takes the blade without
 * complaint. The moment a patch answers with blood, that patch is done, and going on from
 * there is cutting a hole in somebody who did not have one.
 *
 * <p>So the field is a patch of leather with no depth marked on it, the blade takes a
 * layer at a time, and the only instrument in the room is the colour of what is under it.
 */
public final class ExciseMinigame implements Minigame {
    private static final int COLUMNS = 7;
    private static final int ROWS = 5;
    /** Ticks of the blade on one square before a layer comes away. */
    private static final int PASS = 7;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_EDGE = 0xFF8E5F41;
    /** Three depths of dead, from scorched to the leathery thing underneath. */
    private static final int[] DEAD = {0xFF6E4A3C, 0xFF53382E, 0xFF3A2620};
    private static final int LIVING = 0xFFB4483E;
    private static final int BLEEDING = 0xFF8E1616;
    private static final int CUT_TOO_FAR = 0xFF5E0E0E;
    private static final int BLADE = 0xFFD8DEE4;
    private static final int BLADE_DARK = 0xFF8A9199;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;
    private static final int EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int cell;
    private final int fieldX;
    private final int fieldY;
    /** How many layers of dead are left on each square, and how far past it was cut. */
    private final int[] depth = new int[COLUMNS * ROWS];
    private final boolean[] over = new boolean[COLUMNS * ROWS];

    private boolean cutting;
    private int onCell = -1;
    private int working;
    private int cleared;
    private int overcuts;
    private int age;
    private int flash;

    public ExciseMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.cell = Math.max(8, Math.min((this.width - 24) / COLUMNS, (this.height - 30) / ROWS));
        this.fieldX = left + (this.width - cell * COLUMNS) / 2;
        this.fieldY = top + (this.height - cell * ROWS) / 2 - 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // Deepest in the middle and shallow at the edges, which is the shape a burn has:
        // whatever was hottest was in the middle of it.
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                float dx = (column - (COLUMNS - 1) / 2.0F) / ((COLUMNS - 1) / 2.0F);
                float dy = (row - (ROWS - 1) / 2.0F) / ((ROWS - 1) / 2.0F);
                float away = Mth.sqrt(dx * dx + dy * dy);
                int layers = Math.round(Mth.clamp(3.2F - away * 2.4F, 0.0F, 3.0F));
                if (random.nextFloat() < 0.18F) {
                    layers = Mth.clamp(layers + (random.nextBoolean() ? 1 : -1), 0, 3);
                }
                depth[row * COLUMNS + column] = layers;
            }
        }
    }

    // ---- the blade ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        cutting = true;
        onCell = cellAt(mouseX, mouseY);
        working = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!cutting) {
            return false;
        }
        int now = cellAt(mouseX, mouseY);
        if (now != onCell) {
            onCell = now;
            working = 0;
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        cutting = false;
        working = 0;
    }

    private int cellAt(double x, double y) {
        int column = (int) Math.floor((x - fieldX) / cell);
        int row = (int) Math.floor((y - fieldY) / cell);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (complete() || !cutting || onCell < 0) {
            return;
        }
        working++;
        if (working < PASS) {
            return;
        }
        working = 0;
        if (depth[onCell] > 0) {
            depth[onCell]--;
            play(SoundEvents.SHEEP_SHEAR, 0.9F);
            if (depth[onCell] == 0) {
                cleared++;
                play(SoundEvents.SLIME_SQUISH, 1.4F);
            }
            return;
        }
        // Already bleeding, and the blade went on anyway. There is nothing under this
        // but the patient.
        if (!over[onCell]) {
            over[onCell] = true;
            overcuts++;
        }
        flash = 6;
        play(SoundEvents.PLAYER_HURT, 1.2F);
    }

    // ---- where it stands ----------------------------------------------------

    @Override
    public float progress() {
        return cleared / (float) (COLUMNS * ROWS);
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - overcuts * 0.12F, 0.15F, 1.0F);
    }

    @Override
    public boolean complete() {
        return cleared >= COLUMNS * ROWS;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (complete()) {
            return Component.translatable("triage.minigame.excise_done");
        }
        if (onCell >= 0 && cutting && depth[onCell] == 0) {
            return Component.translatable("triage.minigame.excise_stop");
        }
        return Component.translatable(cleared == 0
                ? "triage.minigame.excise_hint" : "triage.minigame.excise_on");
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + width, top + height, SKIN);
        graphics.fill(left, top + height - 3, left + width, top + height, SKIN_EDGE);
        field(graphics);
        blade(graphics, mouseX, mouseY);
        bar(graphics);
    }

    private void field(GuiGraphics graphics) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = row * COLUMNS + column;
                int x = fieldX + column * cell;
                int y = fieldY + row * cell;
                int colour = depth[index] > 0 ? DEAD[Math.min(DEAD.length - 1, depth[index] - 1)]
                        : over[index] ? CUT_TOO_FAR : BLEEDING;
                graphics.fill(x, y, x + cell - 1, y + cell - 1, colour);
                if (depth[index] == 0) {
                    // Living tissue answers. Drawn as a wet edge rather than a flat red,
                    // so that "it has started bleeding" is a thing you see rather than
                    // a colour you are told to look for.
                    graphics.fill(x + 1, y + 1, x + cell - 2, y + 3, LIVING);
                    if ((age / 6 + index) % 5 == 0) {
                        Draw.disc(graphics, x + cell / 2, y + cell / 2, 2, LIVING);
                    }
                }
                if (index == onCell && !complete()) {
                    Paper.outline(graphics, x, y, cell - 1, cell - 1, 0x99FFFFFF);
                }
            }
        }
    }

    private void blade(GuiGraphics graphics, int mouseX, int mouseY) {
        if (complete()) {
            return;
        }
        int colour = flash > 0 ? BAD : BLADE;
        graphics.fill(mouseX - 1, mouseY - 12, mouseX + 2, mouseY + 2, colour);
        graphics.fill(mouseX - 1, mouseY - 12, mouseX, mouseY + 2, BLADE_DARK);
        graphics.fill(mouseX - 3, mouseY + 2, mouseX + 4, mouseY + 5, BLADE_DARK);
        if (cutting && onCell >= 0 && depth[onCell] > 0) {
            int done = Math.round(cell * (working / (float) PASS));
            graphics.fill(mouseX - cell / 2, mouseY + 7, mouseX - cell / 2 + done, mouseY + 9, GOOD);
        }
    }

    private void bar(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barWidth = width - 16;
        int barTop = top + height - 8;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * progress()), barTop + 4, GOOD);
        for (int i = 0; i < overcuts && i < 10; i++) {
            graphics.fill(barLeft + i * 6, top + 3, barLeft + i * 6 + 4, top + 7, BAD);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F), 0.35F));
    }
}
