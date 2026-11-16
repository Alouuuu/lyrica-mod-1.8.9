package dev.relaylyrics;

import org.apache.logging.log4j.Logger;

/** Shared logger holder used by the sources ported from upstream (1.8.9: log4j instead of slf4j). */
public final class FanDroidMod {
    public static final String MOD_ID = "relaylyrics";
    public static final Logger LOGGER = LyricaMod.LOGGER;

    private FanDroidMod() {
    }
}
