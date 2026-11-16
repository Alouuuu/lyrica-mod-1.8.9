package dev.relaylyrics.platform.mc;

import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 1.8.9 facade for {@code net.minecraft.text.Text}: a styled string with appended siblings.
 * Translatable text is resolved eagerly through the client {@link I18n}.
 */
public class Text {
    private final String content;
    private Style style;
    private final List<Text> siblings = new ArrayList<>();

    protected Text(String content, Style style) {
        this.content = content == null ? "" : content;
        this.style = style == null ? Style.EMPTY : style;
    }

    public static Text literal(String value) { return new Text(value, Style.EMPTY); }
    public static Text empty() { return new Text("", Style.EMPTY); }

    public static Text translatable(String key, Object... args) {
        Object[] resolved = new Object[args.length];
        for (int i = 0; i < args.length; i++) resolved[i] = args[i] instanceof Text text ? text.getString() : args[i];
        return new Text(I18n.format(key, resolved), Style.EMPTY);
    }

    public static Text of(String value) { return literal(value); }

    public Style getStyle() { return style; }

    public Text setStyle(Style value) { this.style = value == null ? Style.EMPTY : value; return this; }

    public Text styled(UnaryOperator<Style> operator) { return setStyle(operator.apply(style)); }

    public Text formatted(Formatting... formattings) {
        Style next = style;
        for (Formatting formatting : formattings) next = next.withFormatting(formatting);
        return setStyle(next);
    }

    public Text append(Text sibling) { siblings.add(sibling); return this; }
    public Text append(String sibling) { return append(literal(sibling)); }

    public Text copy() {
        Text copy = new Text(content, style);
        for (Text sibling : siblings) copy.siblings.add(sibling.copy());
        return copy;
    }

    public List<Text> getSiblings() { return Collections.unmodifiableList(siblings); }

    public String getContentString() { return content; }

    /** Plain text, styles removed. */
    public String getString() {
        if (siblings.isEmpty()) return content;
        StringBuilder out = new StringBuilder(content);
        for (Text sibling : siblings) out.append(sibling.getString());
        return out.toString();
    }

    public OrderedText asOrderedText() { return new OrderedText(this); }

    /** FontRenderer string with legacy section codes (bold/italic/underline; color comes from the draw call). */
    public String toLegacyString() {
        StringBuilder out = new StringBuilder();
        appendLegacy(out, Style.EMPTY);
        return out.toString();
    }

    private void appendLegacy(StringBuilder out, Style inherited) {
        Style effective = merge(inherited, style);
        if (!content.isEmpty()) out.append("§r").append(effective.legacyPrefix()).append(content);
        for (Text sibling : siblings) sibling.appendLegacy(out, effective);
    }

    private static Style merge(Style parent, Style child) {
        Style merged = parent;
        if (child.getFont() != null) merged = merged.withFont(child.getFont());
        if (child.isBold()) merged = merged.withBold(true);
        if (child.isItalic()) merged = merged.withItalic(true);
        if (child.isUnderlined()) merged = merged.withUnderline(true);
        if (child.getColorRgb() != null) merged = merged.withColor(child.getColorRgb());
        return merged;
    }

    /** Effective font of the first styled segment (used by the TTF renderer). */
    public Identifier font() {
        if (style.getFont() != null) return style.getFont();
        for (Text sibling : siblings) { Identifier font = sibling.font(); if (font != null) return font; }
        return null;
    }

    /** Value equality like 1.21's MutableText (content, style, siblings); used as cache keys. */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Text)) return false;
        Text other = (Text) o;
        return content.equals(other.content) && style.equals(other.style) && siblings.equals(other.siblings);
    }

    @Override public int hashCode() { return (content.hashCode() * 31 + style.hashCode()) * 31 + siblings.hashCode(); }

    @Override public String toString() { return getString(); }
}
