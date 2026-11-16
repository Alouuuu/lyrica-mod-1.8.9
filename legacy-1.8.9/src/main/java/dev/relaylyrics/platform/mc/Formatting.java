package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code net.minecraft.util.Formatting}. */
public enum Formatting {
    BLACK('0', 0x000000), DARK_BLUE('1', 0x0000AA), DARK_GREEN('2', 0x00AA00), DARK_AQUA('3', 0x00AAAA),
    DARK_RED('4', 0xAA0000), DARK_PURPLE('5', 0xAA00AA), GOLD('6', 0xFFAA00), GRAY('7', 0xAAAAAA),
    DARK_GRAY('8', 0x555555), BLUE('9', 0x5555FF), GREEN('a', 0x55FF55), AQUA('b', 0x55FFFF),
    RED('c', 0xFF5555), LIGHT_PURPLE('d', 0xFF55FF), YELLOW('e', 0xFFFF55), WHITE('f', 0xFFFFFF),
    OBFUSCATED('k', -1), BOLD('l', -1), STRIKETHROUGH('m', -1), UNDERLINE('n', -1), ITALIC('o', -1), RESET('r', -1);

    private final char code;
    private final int rgb;

    Formatting(char code, int rgb) {
        this.code = code;
        this.rgb = rgb;
    }

    public boolean isColor() { return rgb >= 0; }
    public int rgb() { return rgb; }
    public Integer getColorValue() { return rgb >= 0 ? rgb : null; }
    public char getCode() { return code; }

    @Override public String toString() { return "§" + code; }
}
