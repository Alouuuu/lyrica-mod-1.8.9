package dev.lyrica.engine;

import com.google.gson.*;
import com.tx24.spicyplayer.update.Releases;

/** Only published releases carrying a JAR for this Minecraft version are offered. */
public final class LyricaReleaseLookup {
    private LyricaReleaseLookup() { }
    public static String newest(String json, String installed, String minecraft, boolean prereleases) {
        String newest = "";
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject release = element.getAsJsonObject();
            if (release.get("draft").getAsBoolean() || !prereleases && release.get("prerelease").getAsBoolean()) continue;
            String version = release.get("tag_name").getAsString().replaceFirst("^v", "");
            if (!version.matches("[0-9]+(?:\\.[0-9]+){1,3}(?:-[A-Za-z0-9.-]+)?")) continue;
            boolean compatible = false;
            for (JsonElement asset : release.getAsJsonArray("assets")) {
                String name = asset.getAsJsonObject().get("name").getAsString();
                // The port's build names its JAR lyrica-<version>.jar (e.g. lyrica-0.9.4-1.8.9.jar);
                // upstream's lyrica-<minecraft>-<version>.jar form is still accepted.
                if (name.equals("lyrica-" + version + ".jar")
                        || name.equals("lyrica-" + minecraft + "-" + version + ".jar")) compatible = true;
            }
            if (compatible && Releases.INSTANCE.compareVersions(version, installed) > 0
                    && (newest.isEmpty() || Releases.INSTANCE.compareVersions(version, newest) > 0)) newest = version;
        }
        return newest;
    }
}
