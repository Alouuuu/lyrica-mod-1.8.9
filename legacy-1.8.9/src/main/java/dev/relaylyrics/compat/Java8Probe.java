package dev.relaylyrics.compat;

import dev.lyrica.engine.DesktopLyricsEngine;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Headless smoke test for the Java 8 runtime (no Minecraft needed):
 * {@code java -cp lyrica-<ver>.jar dev.relaylyrics.compat.Java8Probe [all|apis|engine]}.
 * Exercises the Java 9-21 APIs upstream relies on (through JvmDowngrader stubs), LegacyTls and
 * a real lyrics lookup.
 */
public final class Java8Probe {
    record Point(int x, int y) { }

    public static void main(String[] args) throws Exception {
        System.out.println("LegacyTls.installJvmDefault=" + LegacyTls.installJvmDefault());
        run(args.length > 0 ? args[0] : "all");
        System.exit(0);
    }

    /** Also invoked in-game with {@code -Dlyrica.smoke=true} (see LyricaMod). */
    public static void run(String mode) throws Exception {
        System.out.println("java.version=" + System.getProperty("java.version"));
        if (!mode.equals("engine")) apis();
        if (!mode.equals("apis")) engine();
        System.out.println("[Lyrica probe] done");
    }

    private static void apis() throws Exception {
        check("record", () -> new Point(1, 2).toString() + " eq=" + new Point(1, 2).equals(new Point(1, 2)));
        check("List.of/getFirst", () -> List.of("a", "b").getFirst());
        check("String.strip/isBlank/repeat", () -> "  x ".strip() + "|" + " ".isBlank() + "|" + "ab".repeat(2));
        check("Stream.toList", () -> List.of(1, 2, 3).stream().map(i -> i * 2).toList());
        check("Math.clamp", () -> Math.clamp(15, 0, 10));
        check("HexFormat", () -> HexFormat.of().formatHex(new byte[]{1, (byte) 0xAB}));
        check("Path.of/Files.readString", () -> {
            Path temp = Files.createTempFile("lyrica", ".txt");
            Files.writeString(temp, "ok");
            String value = Files.readString(Path.of(temp.toString()));
            Files.delete(temp);
            return value;
        });
        check("LinkedHashMap.pollFirstEntry", () -> {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            map.put("a", 1); map.put("b", 2);
            return map.pollFirstEntry().getKey() + " left=" + map.size();
        });
        check("Thread.ofVirtual", () -> {
            Thread thread = Thread.ofVirtual().name("probe").start(() -> { });
            thread.join(2000);
            return "joined";
        });
        check("pattern switch", () -> {
            Object value = 3;
            return switch (value) { case Integer i when i > 2 -> "big " + i; default -> "other"; };
        });
        check("java.net.http", () -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("https://lrclib.net/api/get?track_name=Numb&artist_name=Linkin+Park"))
                    .header("User-Agent", "Lyrica-1.8.9-probe").GET().build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + " len=" + response.body().length();
        });
    }

    private static void engine() throws Exception {
        check("LegacyTls bundle", () -> "factory=" + (LegacyTls.newBuilder().build().sslSocketFactory() != null));
        File cache = Files.createTempDirectory("lyrica-probe").toFile();
        try (DesktopLyricsEngine engine = new DesktopLyricsEngine(cache, "")) {
            check("engine.sources", () -> engine.sources().size() + " sources");
            check("engine.find(Numb)", () -> {
                DesktopLyricsEngine.Result result = engine.find("Numb", "Linkin Park", "Meteora", 187_000L,
                        List.of(), engine.sources().stream().filter(DesktopLyricsEngine.Source::getDefaultEnabled)
                                .map(DesktopLyricsEngine.Source::getId).toList(), ignored -> { }).get(45, TimeUnit.SECONDS);
                return "found=" + result.found() + " provider=" + result.getProvider()
                        + " ttml=" + (result.getTtml() != null) + " lrc=" + (result.getLrc() != null)
                        + " diag=" + abbreviate(result.getDiagnostics());
            });
        }
    }

    private static String abbreviate(String value) {
        if (value == null) return "null";
        String flat = value.replace('\n', ' ');
        return flat.length() > 300 ? flat.substring(0, 300) + "..." : flat;
    }

    private interface Body { Object run() throws Exception; }

    private static void check(String name, Body body) {
        try {
            System.out.println("[OK]   " + name + " -> " + body.run());
        } catch (Throwable failure) {
            System.out.println("[FAIL] " + name + " -> " + failure);
            Throwable cause = failure.getCause();
            if (cause != null) System.out.println("       cause: " + cause);
        }
    }
}
