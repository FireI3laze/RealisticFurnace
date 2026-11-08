package com.fireblaze.realistic_furnace.compat;

import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class HeatBarDrawable implements IDrawable {

    private final int width;
    private final int height;
    private final int heatValue; // 0-maxHeat (z.B. 0-2000)
    private final int maxHeat;   // maxHeat für die Ressource
    private final int overheatedHeat;
    private final ItemStack input;
    private final ItemStack overheatedOutput;

    public HeatBarDrawable(int width, int height, int heatValue, int overheatedHeat, int maxHeat, ItemStack input, ItemStack overheatedOutput) {
        this.width = width;
        this.height = height;
        this.heatValue = heatValue;
        this.overheatedHeat = overheatedHeat;
        this.maxHeat = maxHeat;
        this.input = input;
        this.overheatedOutput = overheatedOutput;
    }

    @Override
    public void draw(GuiGraphics gui, int x, int y) {
        int colorTop = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_TOP_COLOR_TOO_COLD_JEI.get() );
        int colorBottom = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_BOTTOM_COLOR_TOO_COLD_JEI.get() );

        gui.fillGradient(x, y, x + width, y + height, colorTop, colorBottom);

        float displayHeat = toDisplayTemperature(overheatedHeat);
        float displayMax = toDisplayTemperature(maxHeat);
        float fraction = Math.min(1f, displayHeat / displayMax);

        int filledHeight = (int)(height * fraction);
        int hiddenHeight = height - filledHeight;

        if (hiddenHeight > 0) {
            /*
            int overlayColor = 0xFF444444;
            gui.fill(x, y, x + width, y + hiddenHeight, overlayColor);
            */

            int colorTop2 = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_TOP_COLOR_TOO_HOT_JEI.get() );
            int colorBottom2 = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_BOTTOM_COLOR_TOO_HOT_JEI.get() );
            gui.fillGradient(x, y, x + width, y + hiddenHeight, colorTop2, colorBottom2);
        }

        float requiredFraction = (float) heatValue / maxHeat;
        float overheatFraction = Math.min(1f, (float) overheatedHeat / maxHeat);

        int yRequired = y + height - (int)(requiredFraction * height);
        int yOverheat = y + height - (int)(overheatFraction * height);

        int overheatColor = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_SMELT_RANGE_COLOR_JEI.get() );; //0x3333FF88;

        if (yOverheat < yRequired) {
            gui.fill(x, yOverheat, x + width, yRequired, overheatColor);
        }

        int borderColor = 0xFF000000;
        drawBorder(gui, x, y, width, height, 1, borderColor);
        int mark = RealisticFurnaceConfig.HEAT_INCREMENTS.get();

        while (mark < maxHeat) {
            float markDisplay = toDisplayTemperature(mark);
            float markFraction = Math.min(1f, markDisplay / displayMax);
            int markY = y + height - (int)(markFraction * height);
            gui.fill(x - 2, markY, x + 2, markY + 1, 0xFF000000);

            mark += RealisticFurnaceConfig.HEAT_INCREMENTS.get();

            // Label rechts vom Strich
            /*
            String label = getTemperatureLabel(mark);
            gui.pose().pushPose();
            gui.pose().scale(textScale, textScale, 1f);
            gui.drawString(
                    Minecraft.getInstance().font,
                    label,
                    (int)((x + 4) / textScale), // etwas rechts vom Strich
                    (int)((markY - Minecraft.getInstance().font.lineHeight / 2) / textScale),
                    textColor,
                    false
            );
            gui.pose().popPose();
            */
        }

        int lineColor = RealisticFurnaceConfig.parseColor( RealisticFurnaceConfig.HEATBAR_OVERHEAT_THRESHOLD_LINE_COLOR_JEI.get() );; // kräftiges Rot
        gui.fill(x, Math.min(yOverheat, height), x + width, yOverheat + 1, lineColor);

        float scale = 0.6f;
        int iconSize = 16;
        int scaledSize = (int)(iconSize * scale);

        int iconX = x + width + 2;
        int iconY = yOverheat - (scaledSize / 2) - 1;

        // Input
        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1f);
        gui.renderItem(input, (int)(iconX / scale), (int)(iconY / scale));
        gui.pose().popPose();

        // Pfeil (zentriert zur Linie)
        Minecraft mc = Minecraft.getInstance();
        int arrowY = yOverheat - (mc.font.lineHeight / 2);

        gui.drawString(
                mc.font,
                "→",
                iconX + scaledSize + 1,
                arrowY,
                0xFFFFFFFF,
                false
        );

        // Output oder Flame
        if (overheatedOutput != null && !overheatedOutput.isEmpty()) {
            // Input
            gui.pose().pushPose();
            gui.pose().scale(scale, scale, 1f);
            gui.renderItem(overheatedOutput, (int)(iconX / scale) + 31, (int)(iconY / scale));
            gui.pose().popPose();
        } else {
            gui.drawString(
                    mc.font,
                    "🔥",
                    iconX + 20,
                    iconY + 1,
                    0xFFFFAA00,
                    false
            );
        }
    }

    private static float toDisplayTemperature(float tempCelsius) {
        if (RealisticFurnaceConfig.TEMPERATURE_UNIT.get() == RealisticFurnaceConfig.TemperatureUnit.FAHRENHEIT) {
            return tempCelsius * 9 / 5 + 32;
        }
        return tempCelsius;
    }

    public static String getTemperatureLabel(float tempCelsius) {
        if (RealisticFurnaceConfig.TEMPERATURE_UNIT.get() == RealisticFurnaceConfig.TemperatureUnit.FAHRENHEIT) {
            return ((int)(tempCelsius * 9 / 5 + 32)) + "°F";
        } else {
            return ((int) tempCelsius) + "°C";
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
