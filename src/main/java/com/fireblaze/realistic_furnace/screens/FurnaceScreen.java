package com.fireblaze.realistic_furnace.screens;

        import com.fireblaze.realistic_furnace.RealisticFurnace;
        import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
        import com.fireblaze.realistic_furnace.containers.FurnaceContainer;
        import net.minecraft.client.gui.GuiGraphics;
        import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
        import net.minecraft.client.renderer.GameRenderer;
        import net.minecraft.network.chat.Component;
        import net.minecraft.network.chat.Style;
        import net.minecraft.network.chat.TextColor;
        import net.minecraft.resources.ResourceLocation;
        import net.minecraft.world.entity.player.Inventory;
        import com.mojang.blaze3d.systems.RenderSystem;
        import net.minecraft.world.inventory.Slot;

public class FurnaceScreen extends AbstractContainerScreen<FurnaceContainer> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "textures/gui/realistic_furnace_gui.png");

    public FurnaceScreen(FurnaceContainer container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // === Hintergrund ===
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // === Variablen für Heat-Bar ===
        final int HEAT_BAR_WIDTH = 8;
        final int HEAT_BAR_HEIGHT = 64;
        final int HEAT_BAR_X = 72;
        final int HEAT_BAR_Y = 40;

        int centerX = leftPos + imageWidth / 2;
        int centerY = topPos + imageHeight / 2;

        int x = centerX + HEAT_BAR_X - HEAT_BAR_WIDTH / 2;
        int y = centerY - HEAT_BAR_Y - HEAT_BAR_HEIGHT / 2;

        float currentHeat = menu.getHeatData().get(0);
        int maxHeat = menu.getBlockEntity().getMaxHeat();
        int heatHeight = (int) (currentHeat / maxHeat * HEAT_BAR_HEIGHT);

        // --- Schritt 1: festen Gradient voll zeichnen ---
        int colorTop = 0xFFDD5555;    // kräftiges Rot
        int colorBottom = 0xFF5555DD; // kräftiges Blau

        gui.fillGradient(
                x,
                y,
                x + HEAT_BAR_WIDTH,
                y + HEAT_BAR_HEIGHT,
                colorTop,
                colorBottom
        );

        // --- Schritt 2: graue Overlay-Bar darüber ---
        // sichtbarer Anteil = cachedHeatHeight von unten nach oben
        int hiddenHeight = HEAT_BAR_HEIGHT - heatHeight;
        if (hiddenHeight > 0) {
            int overlayColor = 0xFF444444; // dunkles Grau
            gui.fill(
                    x,
                    y,
                    x + HEAT_BAR_WIDTH,
                    y + hiddenHeight,
                    overlayColor
            );
        }

// --- Schritt 3: Rand drüber zeichnen ---
        drawBorder(gui, x, y, HEAT_BAR_WIDTH, HEAT_BAR_HEIGHT, 1, 0xFF000000);



        // === Hitze-Skala ===
        final int[] heatMarks = {500, 1000, 1500};
        final String[] heatLabels = {"500 ", "1000", "1500"};
        final int scaleOffset = 2; // Linie ragt ein paar Pixel in die Bar
        final int scaleLength = 4; // Länge der Linie innerhalb der Bar
        final int textColor = 0xFF000000; // Schwarz
        float textScale = 0.7f; // 70% der normalen Größe

        for (int i = 0; i < heatMarks.length; i++) {
            int mark = heatMarks[i];
            float fraction = Math.min(1f, mark / (float) menu.getHeatData().get(1)); // Prozentual zur Max-Heat
            int markY = y + HEAT_BAR_HEIGHT - (int) (fraction * HEAT_BAR_HEIGHT);

            // Linie für die Skala
            gui.fill(x - scaleOffset, markY, x - scaleOffset + scaleLength, markY + 1, 0xFF000000);

            // Text links neben der Linie, rechtsbündig
            gui.pose().pushPose();
            gui.pose().scale(textScale, textScale, 1f);
            int textWidth = font.width(heatLabels[i]);
            gui.drawString(
                    font,
                    heatLabels[i],
                    (int) ((x - scaleOffset - textWidth + 6) / textScale), // links vom Strich, rechtsbündig
                    (int) ((markY - font.lineHeight / 2 + 2) / textScale),
                    textColor,
                    false
            );
            gui.pose().popPose();
        }

        // === Individuelle horizontale Fortschrittsbalken pro Slot ===
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);

            // Container-Slots (0-8) anzeigen, Inventory überspringen
            if (i > 8) break;

            // Fortschritt aus ContainerData holen
            int current = menu.getProgressData().get(i * 2) / 10;       // aktueller Fortschritt
            int stalledInt = menu.getProgressData().get(i * 2 + 1); // 0 = normal, 1 = stalled
            boolean isStalled = stalledInt != 0;

            int max = menu.getBlockEntity().getMaxProgress(); // max Fortschritt (maxProgress im BlockEntity)
            if (max <= 0) continue;

            int barWidth = 18;   // Breite des Balkens
            int barHeight = 1;   // Höhe des Balkens
            int scaledSlot = (int) Math.ceil(current * (double) barWidth / max); // horizontaler Fortschritt

            // Position: über dem Slot, zentriert
            int xSlot = leftPos + slot.x + (16 - barWidth) / 2;
            int ySlot = topPos + slot.y - barHeight - 2;

            // Farbverlauf von hellblau nach dunkelblau, rot wenn stalled
            int colorLeft = isStalled ? 0xFFFF5555 : 0xFF88CCFF;  // rot, falls stagnierend
            int colorRight = isStalled ? 0xFFAA4444 : 0xFF0044AA;

            // Balken zeichnen
            gui.fillGradient(xSlot, ySlot, xSlot + scaledSlot, ySlot + barHeight, colorLeft, colorRight);
        }


        // === Fuel-Slot Flamme ===
        int fuelSlotX = leftPos + 15; // X des Fuel-Slots in GUI
        int fuelSlotY = topPos + 34 - 17;  // Y des Fuel-Slots in GUI

        int FLAME_WIDTH = 14;
        int FLAME_HEIGHT = 14;

        int burnTime = menu.getBurnTimeData().get(0);     // aktueller Wert
        int burnTimeTotal = menu.getBurnTimeData().get(1); // Max. Wert

        if (burnTime > 0 && burnTimeTotal > 0) {
            // Skaliere die Höhe basierend auf Prozent
            int flameHeight = (int) Math.ceil((float) burnTime * FLAME_HEIGHT / burnTimeTotal);

            gui.blit(TEXTURE,
                    fuelSlotX,
                    fuelSlotY + (FLAME_HEIGHT - flameHeight), // schrumpft nach oben
                    176,
                    FLAME_HEIGHT - flameHeight,
                    FLAME_WIDTH,
                    flameHeight);
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

        // Eck-Quadrate
        gui.fill(x - thickness, y - thickness, x + thickness, y + thickness, color);
        gui.fill(x + width - thickness, y - thickness, x + width + thickness, y + thickness, color);
        gui.fill(x - thickness, y + height - thickness, x + thickness, y + height + thickness, color);
        gui.fill(x + width - thickness, y + height - thickness, x + width + thickness, y + height + thickness, color);
    }



    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);

        // Tooltip für die Hitze-Bar
        final int HEAT_BAR_WIDTH = 8; // 8
        final int HEAT_BAR_HEIGHT = 64; // 50
        final int HEAT_BAR_X = 72; // 72
        final int HEAT_BAR_Y = 40; // 44

        int centerX = leftPos + imageWidth / 2;
        int centerY = topPos + imageHeight / 2;
        int x = centerX + HEAT_BAR_X - HEAT_BAR_WIDTH / 2;
        int y = centerY - HEAT_BAR_Y - HEAT_BAR_HEIGHT / 2;

        if (mouseX >= x && mouseX < x + HEAT_BAR_WIDTH &&
                mouseY >= y && mouseY < y + HEAT_BAR_HEIGHT) {

            float currentHeat = menu.getHeatData().get(0);
            int threateningHeat = FurnaceControllerBlockEntity.getThreateningHeat();

            TextColor color;
            if (currentHeat >= threateningHeat) {
                color = TextColor.fromRgb(0xFF5555); // Rot bei Gefahr
            } else if (currentHeat >= 1600) {
                color = TextColor.fromRgb(0xFFFF55); // Gelb bei 1600+
            } else {
                color = TextColor.fromRgb(0xFFFFFF); // Weiß sonst
            }

            gui.renderTooltip(font,
                    Component.literal((int) currentHeat + "°C").withStyle(Style.EMPTY.withColor(color)),
                    mouseX, mouseY);
        }

        renderTooltip(gui, mouseX, mouseY);
    }

}

