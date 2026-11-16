package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code net.minecraft.client.resource.language.I18n}. */
public final class I18n {
    private I18n() { }

    public static String translate(String key, Object... args) {
        return net.minecraft.client.resources.I18n.format(key, args);
    }

    /** 1.8.9 returns the key itself when a translation is missing. */
    public static boolean hasTranslation(String key) {
        return !net.minecraft.client.resources.I18n.format(key).equals(key);
    }
}
