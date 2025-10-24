package com.fireblaze.realistic_furnace.compat;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class CustomProgressDrawable implements IDrawable {
    private final int width;
    private final int height;

    public CustomProgressDrawable(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(GuiGraphics gui, int xOffset, int yOffset) {
        int simulatedProgress = (int)((System.currentTimeMillis() / 100) % (width + 1)); // loop

        int barHeight = 1;
        int colorLeft = 0xFF88CCFF;
        int colorRight = 0xFF0044AA;

        gui.fillGradient(
                xOffset, yOffset,
                xOffset + simulatedProgress, yOffset + barHeight,
                colorLeft, colorRight
        );
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }
}