package dev.relaylyrics.compat;

import com.tx24.spicyplayer.lyrics.spicy.romanization.Romanizer;
import com.tx24.spicyplayer.lyrics.spicy.romanization.Romanizers;
import com.tx24.spicyplayer.lyrics.spicy.romanization.Script;
import com.tx24.spicyplayer.lyrics.spicy.romanization.ScriptDetector;

/**
 * 1.8.9 port: replaces ICU4J {@code Transliterator.getInstance("Any-Latin")}
 */
public final class AnyLatin {
    private AnyLatin() { }

    public static String transform(String source) {
        String result = source;
        for (Script script : ScriptDetector.INSTANCE.detect(source)) {
            if (script == Script.LATIN) continue;
            Romanizer romanizer = Romanizers.INSTANCE.forScript(script);
            if (romanizer != null && romanizer.isAvailable()) result = romanizer.romanize(result);
        }
        return result;
    }
}
