package dev.relaylyrics.client;

import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

/** Determines exactly which Minecraft surfaces may contain the positioned popup. */
public enum LyricsRenderType {
    ESCAPE_ONLY,
    IN_GAME_ONLY,
    NON_INTRUSIVE,
    ALWAYS,
    INVENTORY_ONLY,
    ESC_AND_INVENTORY,
    TAB_OVERLAY;

    public LyricsRenderType next() { return values()[(ordinal() + 1) % values().length]; }
    public LyricsRenderType previous() { return values()[(ordinal() - 1 + values().length) % values().length]; }

    public boolean rendersWorld(boolean tabVisible) {
        return switch (this) {
            case IN_GAME_ONLY, NON_INTRUSIVE, ALWAYS -> true;
            case TAB_OVERLAY -> tabVisible;
            default -> false;
        };
    }

    public boolean rendersScreen(GuiScreen screen) {
        if (screen == null) return false;
        boolean escape = screen instanceof GuiIngameMenu;
        boolean inventory = screen instanceof GuiContainer;
        return switch (this) {
            case ESCAPE_ONLY -> escape;
            case NON_INTRUSIVE, ESC_AND_INVENTORY -> escape || inventory;
            case ALWAYS -> true;
            case INVENTORY_ONLY -> inventory;
            case IN_GAME_ONLY, TAB_OVERLAY -> false;
        };
    }
}
