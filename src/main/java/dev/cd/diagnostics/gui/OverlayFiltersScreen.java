package dev.cd.diagnostics.gui;

import dev.cd.diagnostics.DiagnosticsSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OverlayFiltersScreen extends Screen {
    private final Screen parent;

    public OverlayFiltersScreen(Screen parent) {
        super(Component.literal("CD Diagnostics - Overlay Filters"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 154;
        int right = this.width / 2 + 4;
        int top = this.height / 2 - 76;
        int width = 150;

        addRenderableWidget(toggleButton("Chests", () -> DiagnosticsSettings.showChests, v -> DiagnosticsSettings.showChests = v, left, top, width));
        addRenderableWidget(toggleButton("Barrels", () -> DiagnosticsSettings.showBarrels, v -> DiagnosticsSettings.showBarrels = v, left, top + 24, width));
        addRenderableWidget(toggleButton("Shulkers", () -> DiagnosticsSettings.showShulkers, v -> DiagnosticsSettings.showShulkers = v, left, top + 48, width));
        addRenderableWidget(toggleButton("Ender Chests", () -> DiagnosticsSettings.showEnderChests, v -> DiagnosticsSettings.showEnderChests = v, left, top + 72, width));

        addRenderableWidget(Button.builder(Component.literal("Toggle All Storage"), button -> toggleAllStorage())
                .bounds(left, top + 104, width, 20).build());

        addRenderableWidget(toggleButton("Diamond", () -> DiagnosticsSettings.showDiamond, v -> DiagnosticsSettings.showDiamond = v, right, top, width));
        addRenderableWidget(toggleButton("Ancient Debris", () -> DiagnosticsSettings.showAncientDebris, v -> DiagnosticsSettings.showAncientDebris = v, right, top + 24, width));
        addRenderableWidget(toggleButton("Iron", () -> DiagnosticsSettings.showIron, v -> DiagnosticsSettings.showIron = v, right, top + 48, width));
        addRenderableWidget(toggleButton("Gold", () -> DiagnosticsSettings.showGold, v -> DiagnosticsSettings.showGold = v, right, top + 72, width));
        addRenderableWidget(toggleButton("Lapis", () -> DiagnosticsSettings.showLapis, v -> DiagnosticsSettings.showLapis = v, right, top + 96, width));
        addRenderableWidget(toggleButton("Redstone", () -> DiagnosticsSettings.showRedstone, v -> DiagnosticsSettings.showRedstone = v, right, top + 120, width));

        addRenderableWidget(Button.builder(Component.literal("Toggle All Ores"), button -> toggleAllOres())
                .bounds(right, top + 152, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(this.width / 2 - 75, top + 184, 150, 20).build());
    }

    private Button toggleButton(String name, BoolGet getter, BoolSet setter, int x, int y, int width) {
        return Button.builder(Component.literal(label(name, getter.get())), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(Component.literal(label(name, next)));
        }).bounds(x, y, width, 20).build();
    }

    private static String label(String name, boolean value) {
        return name + ": " + (value ? "ON" : "OFF");
    }

    private void toggleAllStorage() {
        boolean allOn = DiagnosticsSettings.showChests
                && DiagnosticsSettings.showBarrels
                && DiagnosticsSettings.showShulkers
                && DiagnosticsSettings.showEnderChests;
        boolean next = !allOn;
        DiagnosticsSettings.showChests = next;
        DiagnosticsSettings.showBarrels = next;
        DiagnosticsSettings.showShulkers = next;
        DiagnosticsSettings.showEnderChests = next;
        rebuildWidgets();
    }

    private void toggleAllOres() {
        boolean allOn = DiagnosticsSettings.showDiamond
                && DiagnosticsSettings.showAncientDebris
                && DiagnosticsSettings.showIron
                && DiagnosticsSettings.showGold
                && DiagnosticsSettings.showLapis
                && DiagnosticsSettings.showRedstone;
        boolean next = !allOn;
        DiagnosticsSettings.showDiamond = next;
        DiagnosticsSettings.showAncientDebris = next;
        DiagnosticsSettings.showIron = next;
        DiagnosticsSettings.showGold = next;
        DiagnosticsSettings.showLapis = next;
        DiagnosticsSettings.showRedstone = next;
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, Component.literal("Storage"), this.width / 2 - 154, this.height / 2 - 98, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.literal("Ores"), this.width / 2 + 4, this.height / 2 - 98, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @FunctionalInterface
    private interface BoolGet {
        boolean get();
    }

    @FunctionalInterface
    private interface BoolSet {
        void set(boolean value);
    }
}
