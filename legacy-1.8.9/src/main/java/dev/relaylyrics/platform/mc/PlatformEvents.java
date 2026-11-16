package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

/** GUI-scaled mouse coordinates for the current LWJGL2 mouse event (top-left origin, sub-pixel). */
public final class PlatformEvents {
    private PlatformEvents() { }

    public static double mouseX() {
        Minecraft mc = Minecraft.getMinecraft();
        return Mouse.getEventX() * (double) new ScaledResolution(mc).getScaledWidth() / mc.displayWidth;
    }

    public static double mouseY() {
        Minecraft mc = Minecraft.getMinecraft();
        int height = new ScaledResolution(mc).getScaledHeight();
        return height - Mouse.getEventY() * (double) height / mc.displayHeight - 1;
    }
}
