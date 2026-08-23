package dev.cd.diagnostics.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cd.diagnostics.CDDiagnosticsClient;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class CDDKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(CDDiagnosticsClient.MOD_ID, "diagnostics")
    );

    public static final KeyMapping GUI = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cd_diagnostics.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_O, CATEGORY
    ));

    public static final KeyMapping FREECAM = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cd_diagnostics.freecam", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, CATEGORY
    ));

    public static final KeyMapping PANIC = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cd_diagnostics.panic", InputConstants.Type.KEYSYM, InputConstants.KEY_F7, CATEGORY
    ));

    public static final KeyMapping TEMP_OVERLAY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cd_diagnostics.temp_overlay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY
    ));

    private CDDKeyMappings() {
    }

    public static void bootstrap() {
    }

    public static boolean bindGuiToLetter(Minecraft client, String value) {
        if (value == null || !value.matches("(?i)[a-z]")) return false;
        String letter = value.toLowerCase(Locale.ROOT);
        GUI.setKey(InputConstants.getKey("key.keyboard." + letter));
        KeyMapping.resetMapping();
        client.options.save();
        return true;
    }
}
