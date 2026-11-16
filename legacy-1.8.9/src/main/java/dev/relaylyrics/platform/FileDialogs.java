package dev.relaylyrics.platform;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/** 1.8.9 replacement for LWJGL3 TinyFileDialogs: the native AWT file dialog (blocking; call off-thread). */
public final class FileDialogs {
    private FileDialogs() { }

    /** Returns the chosen absolute path, or null when cancelled/unavailable. */
    public static String openFile(String title, String pattern) {
        if (java.awt.GraphicsEnvironment.isHeadless()) return null;
        AtomicReference<String> chosen = new AtomicReference<>();
        Frame owner = new Frame();
        try {
            FileDialog dialog = new FileDialog(owner, title, FileDialog.LOAD);
            dialog.setFile(pattern);
            String suffix = pattern.startsWith("*") ? pattern.substring(1).toLowerCase(java.util.Locale.ROOT) : "";
            if (!suffix.isEmpty()) dialog.setFilenameFilter((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(suffix));
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);   // modal: blocks until closed
            if (dialog.getFile() != null) chosen.set(new File(dialog.getDirectory(), dialog.getFile()).getAbsolutePath());
            dialog.dispose();
        } finally {
            owner.dispose();
        }
        return chosen.get();
    }
}
