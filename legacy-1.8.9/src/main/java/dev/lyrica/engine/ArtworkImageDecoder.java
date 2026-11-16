package dev.lyrica.engine;

import java.io.IOException;

/**
 * 1.8.9 port: upstream decodes WebP/AVIF artwork through JavaCV/FFmpeg, which is not bundled here.
 * PNG/JPEG pass through unchanged; WebP/AVIF are rejected so callers fall back to another artwork URL.
 */
public final class ArtworkImageDecoder {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private ArtworkImageDecoder() { }

    public static byte[] normalize(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("Invalid artwork size");
        boolean webp = bytes.length > 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
        boolean avif = false;
        if (bytes.length > 16 && ascii(bytes, 4, "ftyp"))
            for (int i = 8; i + 4 <= Math.min(64, bytes.length); i += 4)
                avif |= ascii(bytes, i, "avif") || ascii(bytes, i, "avis");
        if (webp || avif) throw new IOException("WebP/AVIF artwork is not supported on 1.8.9");
        return bytes;
    }

    private static boolean ascii(byte[] bytes, int at, String value) {
        if (at + value.length() > bytes.length) return false;
        for (int i = 0; i < value.length(); i++) if (bytes[at + i] != value.charAt(i)) return false;
        return true;
    }
}
