package dev.relaylyrics.platform.mc;

/**
 * 1.8.9 facade for {@code net.minecraft.text.Style}: immutable font/bold/italic/color.
 * The font is kept for the TTF renderer; the vanilla FontRenderer ignores it.
 */
public final class Style {
    public static final Style EMPTY = new Style(null, false, false, false, null);

    private final Identifier font;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final Integer color;

    private Style(Identifier font, boolean bold, boolean italic, boolean underline, Integer color) {
        this.font = font;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.color = color;
    }

    public Identifier getFont() { return font; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public boolean isUnderlined() { return underline; }
    public Integer getColorRgb() { return color; }

    public Style withFont(Identifier value) { return new Style(value, bold, italic, underline, color); }
    public Style withBold(Boolean value) { return new Style(font, value != null && value, italic, underline, color); }
    public Style withItalic(Boolean value) { return new Style(font, bold, value != null && value, underline, color); }
    public Style withUnderline(Boolean value) { return new Style(font, bold, italic, value != null && value, color); }
    public Style withColor(int rgb) { return new Style(font, bold, italic, underline, rgb & 0xFFFFFF); }

    public Style withFormatting(Formatting formatting) {
        switch (formatting) {
            case BOLD: return withBold(true);
            case ITALIC: return withItalic(true);
            case UNDERLINE: return withUnderline(true);
            case RESET: return EMPTY;
            default: return formatting.isColor() ? withColor(formatting.rgb()) : this;
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Style)) return false;
        Style other = (Style) o;
        return bold == other.bold && italic == other.italic && underline == other.underline
                && java.util.Objects.equals(font, other.font) && java.util.Objects.equals(color, other.color);
    }

    @Override public int hashCode() { return java.util.Objects.hash(font, bold, italic, underline, color); }

    /** Legacy section-sign codes for FontRenderer. */
    String legacyPrefix() {
        StringBuilder prefix = new StringBuilder();
        if (bold) prefix.append("§l");
        if (italic) prefix.append("§o");
        if (underline) prefix.append("§n");
        return prefix.toString();
    }
}
