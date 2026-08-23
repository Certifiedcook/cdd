package dev.cd.diagnostics.notification;

import dev.cd.diagnostics.DiagnosticsSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class CDDNotifications {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(2500L);

    private CDDNotifications() {
    }

    public static void show(String message) {
        show("CD Diagnostics", message);
    }

    public static void show(String title, String message) {
        if (!DiagnosticsSettings.notifications) return;
        showForced(title, message);
    }

    public static void showForced(String title, String message) {
        Minecraft client = Minecraft.getInstance();
        SystemToast.addOrUpdate(
                client.gui.toastManager(),
                TOAST_ID,
                Component.literal(title),
                Component.literal(message)
        );
    }
}
