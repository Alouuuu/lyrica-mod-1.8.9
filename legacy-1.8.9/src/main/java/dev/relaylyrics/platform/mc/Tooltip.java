package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code Tooltip}: holds the text (drawing is done by the owning screen). */
public final class Tooltip {
    private final Text text;

    private Tooltip(Text text) { this.text = text; }

    public static Tooltip of(Text text) { return new Tooltip(text); }

    public Text text() { return text; }
}
