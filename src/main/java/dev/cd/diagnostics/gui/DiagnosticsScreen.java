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
        int left = this.width / 2 - 100;
        int top = this.height / 2 - 108;

        addRenderableWidget(toggleButton("Player Overlay", () -> DiagnosticsSettings.playerEsp, v -> DiagnosticsSettings.playerEsp = v, left, top));
        addRenderableWidget(toggleButton("Storage Overlay", () -> DiagnosticsSettings.storageEsp, v -> DiagnosticsSettings.storageEsp = v, left, top + 24));
        addRenderableWidget(toggleButton("Tracers", () -> DiagnosticsSettings.tracers, v -> DiagnosticsSettings.tracers = v, left, top + 48));
        addRenderableWidget(toggleButton("Storage Depth Override", () -> DiagnosticsSettings.depthOverride, v -> DiagnosticsSettings.depthOverride = v, left, top + 72));

        addRenderableWidget(Button.builder(Component.literal(fakeLabel()), button -> {
            FakePlayerModule.toggle(this.minecraft);
            button.setMessage(Component.literal(fakeLabel()));
        }).bounds(left, top + 104, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal(freecamLabel()), button -> {
            FreecamModule.toggle(this.minecraft);
            button.setMessage(Component.literal(freecamLabel()));
        }).bounds(left, top + 128, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Freecam Speed -"), button -> FreecamSettings.setSpeed(FreecamSettings.speed() - 2.0))
                .bounds(left, top + 152, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Freecam Speed +"), button -> FreecamSettings.setSpeed(FreecamSettings.speed() + 2.0))
                .bounds(left + 102, top + 152, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Smoothing: " + (FreecamSettings.smoothing() ? "ON" : "OFF")), button -> {
            FreecamSettings.toggleSmoothing();
            button.setMessage(Component.literal("Smoothing: " + (FreecamSettings.smoothing() ? "ON" : "OFF")));
        }).bounds(left, top + 176, 200, 20).build());
    }

    private Button toggleButton(String name, BoolGet getter, BoolSet setter, int x, int y) {
        return Button.builder(Component.literal(label(name, getter.get())), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(Component.literal(label(name, next)));
        }).bounds(x, y, 200, 20).build();
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, Component.literal("Speed: " + String.format("%.1f", FreecamSettings.speed()) + " b/s"), this.width / 2 - 100, this.height / 2 + 94, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @FunctionalInterface
    private interface BoolGet { boolean get(); }

    @FunctionalInterface
    private interface BoolSet { void set(boolean value); }
}
