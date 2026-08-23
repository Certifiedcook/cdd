package dev.cd.diagnostics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.module.FreecamSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CDDConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("cd-diagnostics.json");

    private static SettingsSnapshot customPreset;

    private CDDConfigManager() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                save();
                return;
            }

            if (data.settings != null) {
                data.settings.apply();
            }
            customPreset = data.customPreset;
        } catch (Exception exception) {
            System.err.println("[CD Diagnostics] Failed to load config: " + exception.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ConfigData data = new ConfigData();
            data.settings = SettingsSnapshot.capture();
            data.customPreset = customPreset;

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception exception) {
            System.err.println("[CD Diagnostics] Failed to save config: " + exception.getMessage());
        }
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    public static void saveCustomPreset() {
        customPreset = SettingsSnapshot.capture();
        save();
    }

    public static boolean loadCustomPreset() {
        if (customPreset == null) {
            return false;
        }

        customPreset.apply();
        save();
        return true;
    }

    public static void applyPreset(Preset preset) {
        SettingsSnapshot snapshot = SettingsSnapshot.capture();

        switch (preset) {
            case DEFAULT -> {
                snapshot.playerEsp = true;
                snapshot.playerTracers = false;
                snapshot.storageEsp = true;
                snapshot.storageDepthOverride = true;
                snapshot.showChests = true;
                snapshot.showBarrels = true;
                snapshot.showShulkers = true;
                snapshot.showEnderChests = true;
                snapshot.oreEsp = true;
                snapshot.oreDepthOverride = true;
                snapshot.setAllOres(true);
            }
            case MINING -> {
                snapshot.playerEsp = false;
                snapshot.playerTracers = false;
                snapshot.storageEsp = false;
                snapshot.oreEsp = true;
                snapshot.oreDepthOverride = true;
                snapshot.setAllOres(true);
            }
            case DIAGNOSTICS -> {
                snapshot.playerEsp = true;
                snapshot.playerTracers = true;
                snapshot.storageEsp = true;
                snapshot.storageDepthOverride = true;
                snapshot.showChests = true;
                snapshot.showBarrels = true;
                snapshot.showShulkers = true;
                snapshot.showEnderChests = true;
                snapshot.oreEsp = false;
            }
            case CINEMATIC -> {
                snapshot.playerEsp = false;
                snapshot.playerTracers = false;
                snapshot.storageEsp = false;
                snapshot.oreEsp = false;
            }
            case MINIMAL -> {
                snapshot.playerEsp = true;
                snapshot.playerTracers = false;
                snapshot.storageEsp = false;
                snapshot.oreEsp = false;
            }
        }

        snapshot.apply();
        save();
    }

    public static void resetDefaults() {
        new SettingsSnapshot().apply();
        save();
    }

    public enum Preset {
        DEFAULT("Default"),
        MINING("Mining"),
        DIAGNOSTICS("Diagnostics"),
        CINEMATIC("Cinematic"),
        MINIMAL("Minimal");

        private final String label;

        Preset(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final class ConfigData {
        int version = 2;
        SettingsSnapshot settings = new SettingsSnapshot();
        SettingsSnapshot customPreset;
    }

    public static final class SettingsSnapshot {
        public boolean playerEsp = true;
        public boolean playerTracers = false;

        public boolean storageEsp = true;
        public boolean storageDepthOverride = true;
        public boolean showChests = true;
        public boolean showBarrels = true;
        public boolean showShulkers = true;
        public boolean showEnderChests = true;

        public boolean oreEsp = true;
        public boolean oreDepthOverride = true;
        public boolean showDiamond = true;
        public boolean showAncientDebris = true;
        public boolean showIron = true;
        public boolean showGold = true;
        public boolean showLapis = true;
        public boolean showRedstone = true;

        public boolean notifications = true;
        public boolean temporaryOverlayMode = false;

        public double freecamSpeed = 12.0;
        public double freecamBoostMultiplier = 3.0;
        public boolean freecamSmoothing = true;
        public double freecamSmoothingStrength = 10.0;
        public double freecamMouseSensitivity = 1.0;

        public SettingsSnapshot() {
        }

        public static SettingsSnapshot capture() {
            SettingsSnapshot snapshot = new SettingsSnapshot();

            snapshot.playerEsp = DiagnosticsSettings.playerEsp;
            snapshot.playerTracers = DiagnosticsSettings.playerTracers;

            snapshot.storageEsp = DiagnosticsSettings.storageEsp;
            snapshot.storageDepthOverride = DiagnosticsSettings.storageDepthOverride;
            snapshot.showChests = DiagnosticsSettings.showChests;
            snapshot.showBarrels = DiagnosticsSettings.showBarrels;
            snapshot.showShulkers = DiagnosticsSettings.showShulkers;
            snapshot.showEnderChests = DiagnosticsSettings.showEnderChests;

            snapshot.oreEsp = DiagnosticsSettings.oreEsp;
            snapshot.oreDepthOverride = DiagnosticsSettings.oreDepthOverride;
            snapshot.showDiamond = DiagnosticsSettings.showDiamond;
            snapshot.showAncientDebris = DiagnosticsSettings.showAncientDebris;
            snapshot.showIron = DiagnosticsSettings.showIron;
            snapshot.showGold = DiagnosticsSettings.showGold;
            snapshot.showLapis = DiagnosticsSettings.showLapis;
            snapshot.showRedstone = DiagnosticsSettings.showRedstone;

            snapshot.notifications = DiagnosticsSettings.notifications;
            snapshot.temporaryOverlayMode = DiagnosticsSettings.temporaryOverlayMode;

            snapshot.freecamSpeed = FreecamSettings.speed();
            snapshot.freecamBoostMultiplier = FreecamSettings.boostMultiplier();
            snapshot.freecamSmoothing = FreecamSettings.smoothing();
            snapshot.freecamSmoothingStrength = FreecamSettings.smoothingStrength();
            snapshot.freecamMouseSensitivity = FreecamSettings.mouseSensitivity();

            return snapshot;
        }

        public void apply() {
            DiagnosticsSettings.playerEsp = playerEsp;
            DiagnosticsSettings.playerTracers = playerTracers;

            DiagnosticsSettings.storageEsp = storageEsp;
            DiagnosticsSettings.storageDepthOverride = storageDepthOverride;
            DiagnosticsSettings.showChests = showChests;
            DiagnosticsSettings.showBarrels = showBarrels;
            DiagnosticsSettings.showShulkers = showShulkers;
            DiagnosticsSettings.showEnderChests = showEnderChests;

            DiagnosticsSettings.oreEsp = oreEsp;
            DiagnosticsSettings.oreDepthOverride = oreDepthOverride;
            DiagnosticsSettings.showDiamond = showDiamond;
            DiagnosticsSettings.showAncientDebris = showAncientDebris;
            DiagnosticsSettings.showIron = showIron;
            DiagnosticsSettings.showGold = showGold;
            DiagnosticsSettings.showLapis = showLapis;
            DiagnosticsSettings.showRedstone = showRedstone;

            DiagnosticsSettings.notifications = notifications;
            DiagnosticsSettings.temporaryOverlayMode = temporaryOverlayMode;

            FreecamSettings.setSpeed(freecamSpeed);
            FreecamSettings.setBoostMultiplier(freecamBoostMultiplier);
            FreecamSettings.setSmoothing(freecamSmoothing);
            FreecamSettings.setSmoothingStrength(freecamSmoothingStrength);
            FreecamSettings.setMouseSensitivity(freecamMouseSensitivity);
        }

        private void setAllOres(boolean value) {
            showDiamond = value;
            showAncientDebris = value;
            showIron = value;
            showGold = value;
            showLapis = value;
            showRedstone = value;
        }
    }
}
