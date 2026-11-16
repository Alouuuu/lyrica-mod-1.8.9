package dev.relaylyrics.protocol.music;

public record MusicCommand(MusicAction action, Double value) {
    public static MusicCommand simple(MusicAction action) {
        return new MusicCommand(action, null);
    }
}

