package dev.cd.diagnostics.gui;

import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.config.CDDConfigManager;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.FreecamSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DiagnosticsScreen extends Screen {
    private final Screen parent;
    private Section section = Section.OVERLAYS;
    private String status = "Settings autosave";

    public DiagnosticsScreen(Screen parent) {
        super(Component.literal("CD Diagnostics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 154;
        int right = this.width / 2 + 4;
        int width = 150;
        int tabTop = this.height / 2 - 116;
        int top = this.height / 2 - 78;

        addRenderableWidget(Button.builder(
                Component.literal(section == Section.OVERLAYS ? "> Overlays <" : "Overlays"),
                button -> switchSection(Section.OVERLAYS))
                .bounds(left, tabTop, width, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal(section == Section.MISC ? "> Misc <" : "Misc"),
                button -> switchSection(Section.MISC))
                .bounds(right, tabTop, width, 20).build());

        if (section == Section.OVERLAYS) {
            buildOverlays(left, right, top, width);
        } else {
            buildMisc(left, right, top, width);
        }
    }

    private void buildOverlays(int left, int right, int top, int width) {
        addRenderableWidget(toggleButton("Player Overlay", () -> DiagnosticsSettings.playerEsp,
                v -> DiagnosticsSettings.playerEsp = v, left, top, width));
        addRenderableWidget(toggleButton("Player Tracers", () -> DiagnosticsSettings.playerTracers,
                v -> DiagnosticsSettings.playerTracers = v, left, top + 24, width));
        addRenderableWidget(toggleButton("Storage Overlay", () -> DiagnosticsSettings.storageEsp,
                v -> DiagnosticsSettings.storageEsp = v, left, top + 48, width));
        addRenderableWidget(toggleButton("Storage Through Walls", () -> DiagnosticsSettings.storageDepthOverride,
                v -> DiagnosticsSettings.storageDepthOverride = v, left, top + 72, width));
        addRenderableWidget(toggleButton("Chests", () -> DiagnosticsSettings.showChests,
                v -> DiagnosticsSettings.showChests = v, left, top + 96, width));
        addRenderableWidget(toggleButton("Barrels", () -> DiagnosticsSettings.showBarrels,
                v -> DiagnosticsSettings.showBarrels = v, left, top + 120, width));
        addRenderableWidget(toggleButton("Shulker Boxes", () -> DiagnosticsSettings.showShulkers,
                v -> DiagnosticsSettings.showShulkers = v, left, top + 144, width));
        addRenderableWidget(toggleButton("Ender Chests", () -> DiagnosticsSettings.showEnderChests,
                v -> DiagnosticsSettings.showEnderChests = v, left, top + 168, width));

        addRenderableWidget(toggleButton("Ore Overlay", () -> DiagnosticsSettings.oreEsp,
                v -> DiagnosticsSettings.oreEsp = v, right, top, width));
        addRenderableWidget(toggleButton("Ore Through Walls", () -> DiagnosticsSettings.oreDepthOverride,
                v -> DiagnosticsSettings.oreDepthOverride = v, right, top + 24, width));
        addRenderableWidget(toggleButton("Diamond", () -> DiagnosticsSettings.showDiamond,
                v -> DiagnosticsSettings.showDiamond = v, right, top + 48, width));
        addRenderableWidget(toggleButton("Ancient Debris", () -> DiagnosticsSettings.showAncientDebris,
                v -> DiagnosticsSettings.showAncientDebris = v, right, top + 72, width));
        addRenderableWidget(toggleButton("Iron", () -> DiagnosticsSettings.showIron,
                v -> DiagnosticsSettings.showIron = v, right, top + 96, width));
        addRenderableWidget(toggleButton("Gold", () -> DiagnosticsSettings.showGold,
                v -> DiagnosticsSettings.showGold = v, right, top + 120, width));
        addRenderableWidget(toggleButton("Lapis", () -> DiagnosticsSettings.showLapis,
                v -> DiagnosticsSettings.showLapis = v, right, top + 144, width));
        addRenderableWidget(toggleButton("Redstone", () -> DiagnosticsSettings.showRedstone,
                v -> DiagnosticsSettings.showRedstone = v, right, top + 168, width));
    }

    private void buildMisc(int left, int right, int top, int width) {
        addRenderableWidget(Button.builder(Component.literal(fakeLabel()), button -> {
            FakePlayerModule.toggle(this.minecraft);
            button.setMessage(Component.literal(fakeLabel()));
        }).bounds(left, top, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal(freecamLabel()), button -> {
            FreecamModule.toggle(this.minecraft);
            button.setMessage(Component.literal(freecamLabel()));
        }).bounds(left, top + 24, width, 20).build());

        addPair(left, top + 48, width,
                "Speed - (" + oneDecimal(FreecamSettings.speed()) + ")",
                () -> changeFreecamSpeed(-2.0),
                "Speed + (" + oneDecimal(FreecamSettings.speed()) + ")",
                () -> changeFreecamSpeed(2.0));

        addPair(left, top + 72, width,
                "Boost - (" + oneDecimal(FreecamSettings.boostMultiplier()) + "x)",
                () -> changeBoost(-0.5),
                "Boost + (" + oneDecimal(FreecamSettings.boostMultiplier()) + "x)",
                () -> changeBoost(0.5));

        addRenderableWidget(Button.builder(Component.literal(smoothingLabel()), button -> {
            FreecamSettings.toggleSmoothing();
            CDDConfigManager.save();
            rebuildWidgets();
        }).bounds(left, top + 96, width, 20).build());

        addPair(left, top + 120, width,
                "Smooth - (" + oneDecimal(FreecamSettings.smoothingStrength()) + ")",
                () -> changeSmoothingStrength(-1.0),
                "Smooth + (" + oneDecimal(FreecamSettings.smoothingStrength()) + ")",
                () -> changeSmoothingStrength(1.0));

        addPair(left, top + 144, width,
                "Sens - (" + oneDecimal(FreecamSettings.mouseSensitivity()) + ")",
                () -> changeSensitivity(-0.1),
                "Sens + (" + oneDecimal(FreecamSettings.mouseSensitivity()) + ")",
                () -> changeSensitivity(0.1));

        addRenderableWidget(Button.builder(Component.literal("Reload Config"), button -> {
            CDDConfigManager.load();
            status = "Reloaded config from disk";
            rebuildWidgets();
        }).bounds(left, top + 168, width, 20).build());

        addPresetButton(CDDConfigManager.Preset.DEFAULT, right, top, width);
        addPresetButton(CDDConfigManager.Preset.MINING, right, top + 24, width);
        addPresetButton(CDDConfigManager.Preset.DIAGNOSTICS, right, top + 48, width);
        addPresetButton(CDDConfigManager.Preset.CINEMATIC, right, top + 72, width);
        addPresetButton(CDDConfigManager.Preset.MINIMAL, right, top + 96, width);

        addRenderableWidget(Button.builder(Component.literal("Save Custom Preset"), button -> {
            CDDConfigManager.saveCustomPreset();
            status = "Saved Custom preset";
            rebuildWidgets();
        }).bounds(right, top + 120, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Load Custom Preset"), button -> {
            boolean loaded = CDDConfigManager.loadCustomPreset();
            status = loaded ? "Loaded Custom preset" : "No Custom preset saved";
            rebuildWidgets();
        }).bounds(right, top + 144, width, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> {
            CDDConfigManager.resetDefaults();
            status = "Reset settings to defaults";
            rebuildWidgets();
        }).bounds(right, top + 168, width, 20).build());
    }

    private void addPresetButton(CDDConfigManager.Preset preset, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal("Preset: " + preset.label()), button -> {
            CDDConfigManager.applyPreset(preset);
            status = "Applied " + preset.label() + " preset";
            rebuildWidgets();
        }).bounds(x, y, width, 20).build());
    }

    private void addPair(int x, int y, int width, String leftLabel, Runnable leftAction,
                         String rightLabel, Runnable rightAction) {
        int half = (width - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal(leftLabel), button -> {
            leftAction.run();
            rebuildWidgets();
        }).bounds(x, y, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal(rightLabel), button -> {
            rightAction.run();
            rebuildWidgets();
        }).bounds(x + half + 4, y, half, 20).build());
    }

    private Button toggleButton(String name, BoolGet getter, BoolSet setter, int x, int y, int width) {
        return Button.builder(Component.literal(label(name, getter.get())), button -> {
            boolean next = !getter.get();
            setter.set(next);
            CDDConfigManager.save();
            button.setMessage(Component.literal(label(name, next)));
        }).bounds(x, y, width, 20).build();
    }

    private void switchSection(Section next) {
        if (section == next) return;
        section = next;
        rebuildWidgets();
    }

    private void changeFreecamSpeed(double delta) {
        FreecamSettings.setSpeed(FreecamSettings.speed() + delta);
        CDDConfigManager.save();
    }

    private void changeBoost(double delta) {
        FreecamSettings.setBoostMultiplier(FreecamSettings.boostMultiplier() + delta);
        CDDConfigManager.save();
    }

    private void changeSmoothingStrength(double delta) {
        FreecamSettings.setSmoothingStrength(FreecamSettings.smoothingStrength() + delta);
        CDDConfigManager.save();
    }

    private void changeSensitivity(double delta) {
        FreecamSettings.setMouseSensitivity(FreecamSettings.mouseSensitivity() + delta);
        CDDConfigManager.save();
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
        return "Freecam Smoothing: " + (FreecamSettings.smoothing() ? "ON" : "OFF");
    }

    private static String oneDecimal(double value) {
        return String.format("%.1f", value);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(
                this.font,
                Component.literal("CD Diagnostics"),
                this.width / 2 - 38,
                this.height / 2 - 138,
                0xFFFFFFFF,
                true
        );

        if (section == Section.OVERLAYS) {
            graphics.text(this.font, Component.literal("Players & Storage"),
                    this.width / 2 - 154, this.height / 2 - 94, 0xFFFFFFFF, true);
            graphics.text(this.font, Component.literal("Ores"),
                    this.width / 2 + 4, this.height / 2 - 94, 0xFFFFFFFF, true);
        } else {
            graphics.text(this.font, Component.literal("Tools & Camera"),
                    this.width / 2 - 154, this.height / 2 - 94, 0xFFFFFFFF, true);
            graphics.text(this.font, Component.literal("Presets"),
                    this.width / 2 + 4, this.height / 2 - 94, 0xFFFFFFFF, true);
        }

        graphics.text(
                this.font,
                Component.literal(status),
                this.width / 2 - this.font.width(status) / 2,
                this.height / 2 + 116,
                0xFFAAAAAA,
                false
        );
    }

    @Override
    public void onClose() {
        CDDConfigManager.save();
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    private enum Section {
        OVERLAYS,
        MISC
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
