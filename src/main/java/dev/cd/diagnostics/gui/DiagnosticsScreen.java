package dev.cd.diagnostics.gui;

import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.FreecamSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DiagnosticsScreen extends Screen {
    private final Screen parent;

    public DiagnosticsScreen(Screen parent) {
        super(Component.literal("CD Diagnostics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 154;
        int right = this.width / 2 + 4;
        int top = this.height / 2 - 78;
        int width = 150;

        addRenderableWidget(toggleButton("Player Overlay", () -> DiagnosticsSettings.playerEsp, v -> DiagnosticsSettings.playerEsp = v, left, top, width));
        addRenderableWidget(toggleButton("Player Tracers", () -> DiagnosticsSettings.playerTracers, v -> DiagnosticsSettings.playerTracers = v, left, top + 24, width));
        addRenderableWidget(toggleButton("Storage Overlay", () -> DiagnosticsSettings.storageEsp, v -> DiagnosticsSettings.storageEsp = v, left, top + 48, width));
        addRenderableWidget(toggleButton("Storage Through Walls", () -> DiagnosticsSettings.storageDepthOverride, v -> DiagnosticsSettings.storageDepthOverride = v, left, top + 72, width));
        addRenderableWidget(toggleButton("Ore Overlay", () -> DiagnosticsSettings.oreEsp, v -> DiagnosticsSettings.oreEsp = v, left, top + 96, width));
        addRenderableWidget(toggleButton("Ore Through Walls", () -> DiagnosticsSettings.oreDepthOverride, v -> DiagnosticsSettings.oreDepthOverride = v, left, top + 120, width));

        addRenderableWidget(Button.builder(Component.literal("Overlay Filters..."), button ->
                this.minecraft.gui.setScreen(new OverlayFiltersScreen(this)))
                .bounds(right, top, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal(fakeLabel()), button -> {
            FakePlayerModule.toggle(this.minecraft);
            button.setMessage(Component.literal(fakeLabel()));
        }).bounds(right, top + 24, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal(freecamLabel()), button -> {
            FreecamModule.toggle(this.minecraft);
            button.setMessage(Component.literal(freecamLabel()));
        }).bounds(right, top + 48, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Freecam Speed -"), button ->
                FreecamSettings.setSpeed(FreecamSettings.speed() - 2.0))
                .bounds(right, top + 72, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Freecam Speed +"), button ->
                FreecamSettings.setSpeed(FreecamSettings.speed() + 2.0))
                .bounds(right, top + 96, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal(smoothingLabel()), button -> {
            FreecamSettings.toggleSmoothing();
            button.setMessage(Component.literal(smoothingLabel()));
        }).bounds(right, top + 120, width, 20).build());
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

    private static String fakeLabel() {
        return "Fake Player: " + (FakePlayerModule.isSpawned() ? "REMOVE" : "SPAWN");
    }

    private static String freecamLabel() {
        return "Freecam: " + (FreecamModule.isActive() ? "ON" : "OFF");
    }

    private static String smoothingLabel() {
        return "Smoothing: " + (FreecamSettings.smoothing() ? "ON" : "OFF");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(
                this.font,
                Component.literal("CD Diagnostics"),
                this.width / 2 - 38,
                this.height / 2 - 102,
                0xFFFFFFFF,
                true
        );
        graphics.text(
                this.font,
                Component.literal("Freecam speed: " + String.format("%.1f", FreecamSettings.speed()) + " b/s"),
                this.width / 2 + 4,
                this.height / 2 + 69,
                0xFFFFFFFF,
                true
        );
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
