package com.claudemods.triage.client;

import com.claudemods.triage.client.screen.BodyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The little cross beside the inventory that opens the body screen.
 *
 * <p>Drawn rather than textured, like everything else this mod puts on screen: a red
 * cross on a dark plate, which is legible at any GUI scale and needs no atlas entry.
 */
public final class BodyButton extends AbstractButton {
    private static final int SIZE = 20;

    private static final int COLOR_PLATE = 0xFF2A2438;
    private static final int COLOR_PLATE_HOVER = 0xFF3D3552;
    private static final int COLOR_EDGE = 0xFF07050C;
    private static final int COLOR_LIGHT = 0x30FFFFFF;
    private static final int COLOR_CROSS = 0xFFC42A2A;

    public BodyButton(int x, int y) {
        super(x, y, SIZE, SIZE, Component.translatable("triage.screen.body"));
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().setScreen(new BodyScreen());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + SIZE, y + SIZE, COLOR_EDGE);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, isHovered() ? COLOR_PLATE_HOVER : COLOR_PLATE);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + 2, COLOR_LIGHT);

        graphics.fill(x + 8, y + 4, x + 12, y + 16, COLOR_CROSS);
        graphics.fill(x + 4, y + 8, x + 16, y + 12, COLOR_CROSS);

        if (isHovered()) {
            graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
