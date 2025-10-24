package com.fireblaze.realistic_furnace.compat;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class HeatBarDrawable implements IDrawable {

    private final int width;
    private final int height;
    private final int heatValue; // 0-maxHeat (z.B. 0-2000)
    private final int maxHeat;   // maxHeat für die Ressource

    public HeatBarDrawable(int width, int height, int heatValue, int maxHeat) {
        this.width = width;
        this.height = height;
        this.heatValue = heatValue;
        this.maxHeat = maxHeat;
    }

    @Override
    public void draw(GuiGraphics gui, int x, int y) {
        // 1️⃣ Gradient von oben nach unten
        int colorTop = 0xFFDD5555;    // kräftiges Rot
        int colorBottom = 0xFF5555DD; // kräftiges Blau
        gui.fillGradient(x, y, x + width, y + height, colorTop, colorBottom);

        // 2️⃣ Overlay für nicht-geladene Hitze (grau)
        float fraction = Math.min(1f, heatValue / (float) maxHeat);
        int filledHeight = (int)(height * fraction);
        int hiddenHeight = height - filledHeight;

        if (hiddenHeight > 0) {
            int overlayColor = 0xFF444444; // dunkles Grau
            gui.fill(x, y, x + width, y + hiddenHeight, overlayColor);
        }

        // 3️⃣ Rand zeichnen
        int borderColor = 0xFF000000;
        drawBorder(gui, x, y, width, height, 1, borderColor);

        // 4️⃣ Optional: Skalenmarkierungen (z.B. 500, 1000, 1500)
        final int[] heatMarks = {500, 1000, 1500};
        for (int mark : heatMarks) {
            float markFraction = Math.min(1f, mark / (float) maxHeat);
            int markY = y + height - (int)(markFraction * height);
            gui.fill(x - 2, markY, x + 2, markY + 1, 0xFF000000);
        }
    }

    private void drawBorder(GuiGraphics gui, int x, int y, int width, int height, int thickness, int color) {
        // Oben
        gui.fill(x - thickness, y - thickness, x + width + thickness, y, color);
        // Unten
        gui.fill(x - thickness, y + height, x + width + thickness, y + height + thickness, color);
        // Links
        gui.fill(x - thickness, y, x, y + height, color);
        // Rechts
        gui.fill(x + width, y, x + width + thickness, y + height, color);

        // Ecken
        gui.fill(x - thickness, y - thickness, x + thickness, y + thickness, color);
        gui.fill(x + width - thickness, y - thickness, x + width + thickness, y + thickness, color);
        gui.fill(x - thickness, y + height - thickness, x + thickness, y + height + thickness, color);
        gui.fill(x + width, y + height - thickness, x + width + thickness, y + height + thickness, color);
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }
}
