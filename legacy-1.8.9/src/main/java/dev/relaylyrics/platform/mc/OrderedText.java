package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code OrderedText}: a laid-out line of {@link Text}. */
public final class OrderedText {
    private final Text text;

    OrderedText(Text text) { this.text = text; }

    public static OrderedText of(Text text) { return new OrderedText(text); }

    public Text text() { return text; }

    /** Upstream {@code CharacterVisitor}: (index, style, codePoint) -> continue. */
    public interface CharacterVisitor { boolean accept(int index, Style style, int codePoint); }

    public boolean accept(CharacterVisitor visitor) {
        String value = text.getString();
        for (int i = 0, index = 0; i < value.length(); index++) {
            int cp = value.codePointAt(i);
            if (!visitor.accept(index, text.getStyle(), cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    @Override public String toString() { return text.getString(); }
}
