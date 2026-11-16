package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/** GL calls must run on the client thread (LWJGL2 context is current only there). */
final class GlThread {
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private GlThread() { }

    static boolean onClient() { return Minecraft.getMinecraft().isCallingFromMinecraftThread(); }

    /** Runs now on the client thread, otherwise queues it (during shutdown the queue never drains: harmless). */
    static void run(Runnable glWork) {
        if (onClient()) glWork.run();
        else Minecraft.getMinecraft().addScheduledTask(glWork);
    }

    /** Logs once when GL-backed work is attempted off-thread, so the first in-game run is diagnosable. */
    static boolean check(String what) {
        if (onClient()) return true;
        if (WARNED.compareAndSet(false, true))
            dev.relaylyrics.LyricaMod.LOGGER.warn("Lyrica: {} called off the client thread ({}); deferring GL work",
                    what, Thread.currentThread().getName(), new Throwable("stack"));
        return false;
    }
}
