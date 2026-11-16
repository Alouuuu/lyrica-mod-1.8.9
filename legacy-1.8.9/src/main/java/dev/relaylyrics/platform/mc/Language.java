package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code net.minecraft.util.Language}. */
public final class Language {
    private static final Language INSTANCE = new Language();

    private Language() { }

    public static Language getInstance() { return INSTANCE; }

    public String get(String key) { return net.minecraft.client.resources.I18n.format(key); }

    public String get(String key, String fallback) { return I18n.hasTranslation(key) ? get(key) : fallback; }

    public boolean hasTranslation(String key) { return I18n.hasTranslation(key); }
}
