package dev.relaylyrics.platform.mc;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.8.9 facade for the GLFW constants/queries upstream uses. Constants keep their real GLFW
 * values (upstream does range checks on them); LWJGL2 key codes are translated at the boundary
 * ({@link #toGlfw(int)} for key events, {@link #glfwGetKey} for polling).
 */
public final class GLFW {
    private GLFW() { }

    public static final int GLFW_RELEASE = 0, GLFW_PRESS = 1, GLFW_REPEAT = 2;
    public static final int GLFW_MOD_SHIFT = 0x1, GLFW_MOD_CONTROL = 0x2, GLFW_MOD_ALT = 0x4, GLFW_MOD_SUPER = 0x8;
    public static final int GLFW_MOUSE_BUTTON_LEFT = 0, GLFW_MOUSE_BUTTON_RIGHT = 1, GLFW_MOUSE_BUTTON_MIDDLE = 2;

    public static final int GLFW_KEY_UNKNOWN = -1;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_0 = 48, GLFW_KEY_9 = 57;
    public static final int GLFW_KEY_A = 65, GLFW_KEY_C = 67, GLFW_KEY_F = 70, GLFW_KEY_V = 86, GLFW_KEY_X = 88, GLFW_KEY_Z = 90;
    public static final int GLFW_KEY_ESCAPE = 256, GLFW_KEY_ENTER = 257, GLFW_KEY_TAB = 258, GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_INSERT = 260, GLFW_KEY_DELETE = 261;
    public static final int GLFW_KEY_RIGHT = 262, GLFW_KEY_LEFT = 263, GLFW_KEY_DOWN = 264, GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_PAGE_UP = 266, GLFW_KEY_PAGE_DOWN = 267, GLFW_KEY_HOME = 268, GLFW_KEY_END = 269;
    public static final int GLFW_KEY_KP_ENTER = 335;
    public static final int GLFW_KEY_LEFT_SHIFT = 340, GLFW_KEY_LEFT_CONTROL = 341, GLFW_KEY_LEFT_ALT = 342, GLFW_KEY_LEFT_SUPER = 343;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344, GLFW_KEY_RIGHT_CONTROL = 345, GLFW_KEY_RIGHT_ALT = 346, GLFW_KEY_RIGHT_SUPER = 347;

    private static final Map<Integer, Integer> TO_LWJGL = new HashMap<>();
    private static final Map<Integer, Integer> TO_GLFW = new HashMap<>();

    static {
        map(GLFW_KEY_SPACE, Keyboard.KEY_SPACE);
        int[] digits = {Keyboard.KEY_0, Keyboard.KEY_1, Keyboard.KEY_2, Keyboard.KEY_3, Keyboard.KEY_4,
                Keyboard.KEY_5, Keyboard.KEY_6, Keyboard.KEY_7, Keyboard.KEY_8, Keyboard.KEY_9};
        for (int i = 0; i < 10; i++) map(GLFW_KEY_0 + i, digits[i]);
        int[] letters = {Keyboard.KEY_A, Keyboard.KEY_B, Keyboard.KEY_C, Keyboard.KEY_D, Keyboard.KEY_E, Keyboard.KEY_F,
                Keyboard.KEY_G, Keyboard.KEY_H, Keyboard.KEY_I, Keyboard.KEY_J, Keyboard.KEY_K, Keyboard.KEY_L, Keyboard.KEY_M,
                Keyboard.KEY_N, Keyboard.KEY_O, Keyboard.KEY_P, Keyboard.KEY_Q, Keyboard.KEY_R, Keyboard.KEY_S, Keyboard.KEY_T,
                Keyboard.KEY_U, Keyboard.KEY_V, Keyboard.KEY_W, Keyboard.KEY_X, Keyboard.KEY_Y, Keyboard.KEY_Z};
        for (int i = 0; i < 26; i++) map(GLFW_KEY_A + i, letters[i]);
        map(GLFW_KEY_ESCAPE, Keyboard.KEY_ESCAPE);
        map(GLFW_KEY_ENTER, Keyboard.KEY_RETURN);
        map(GLFW_KEY_TAB, Keyboard.KEY_TAB);
        map(GLFW_KEY_BACKSPACE, Keyboard.KEY_BACK);
        map(GLFW_KEY_INSERT, Keyboard.KEY_INSERT);
        map(GLFW_KEY_DELETE, Keyboard.KEY_DELETE);
        map(GLFW_KEY_RIGHT, Keyboard.KEY_RIGHT);
        map(GLFW_KEY_LEFT, Keyboard.KEY_LEFT);
        map(GLFW_KEY_DOWN, Keyboard.KEY_DOWN);
        map(GLFW_KEY_UP, Keyboard.KEY_UP);
        map(GLFW_KEY_PAGE_UP, Keyboard.KEY_PRIOR);
        map(GLFW_KEY_PAGE_DOWN, Keyboard.KEY_NEXT);
        map(GLFW_KEY_HOME, Keyboard.KEY_HOME);
        map(GLFW_KEY_END, Keyboard.KEY_END);
        map(GLFW_KEY_KP_ENTER, Keyboard.KEY_NUMPADENTER);
        map(GLFW_KEY_LEFT_SHIFT, Keyboard.KEY_LSHIFT);
        map(GLFW_KEY_LEFT_CONTROL, Keyboard.KEY_LCONTROL);
        map(GLFW_KEY_LEFT_ALT, Keyboard.KEY_LMENU);
        map(GLFW_KEY_LEFT_SUPER, Keyboard.KEY_LMETA);
        map(GLFW_KEY_RIGHT_SHIFT, Keyboard.KEY_RSHIFT);
        map(GLFW_KEY_RIGHT_CONTROL, Keyboard.KEY_RCONTROL);
        map(GLFW_KEY_RIGHT_ALT, Keyboard.KEY_RMENU);
        map(GLFW_KEY_RIGHT_SUPER, Keyboard.KEY_RMETA);
    }

    private static void map(int glfw, int lwjgl) {
        TO_LWJGL.put(glfw, lwjgl);
        TO_GLFW.put(lwjgl, glfw);
    }

    /** LWJGL2 key code (from {@code keyTyped}/{@code Keyboard.getEventKey()}) to GLFW. */
    public static int toGlfw(int lwjglKey) {
        Integer glfw = TO_GLFW.get(lwjglKey);
        return glfw == null ? GLFW_KEY_UNKNOWN : glfw;
    }

    public static int toLwjgl(int glfwKey) {
        Integer lwjgl = TO_LWJGL.get(glfwKey);
        return lwjgl == null ? Keyboard.KEY_NONE : lwjgl;
    }

    /** Current GLFW-style modifier bits. */
    public static int modifiers() {
        int mods = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) mods |= GLFW_MOD_SHIFT;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) mods |= GLFW_MOD_CONTROL;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) mods |= GLFW_MOD_ALT;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) mods |= GLFW_MOD_SUPER;
        return mods;
    }

    public static int glfwGetKey(long window, int key) {
        int lwjgl = toLwjgl(key);
        return lwjgl != Keyboard.KEY_NONE && Keyboard.isKeyDown(lwjgl) ? GLFW_PRESS : GLFW_RELEASE;
    }

    public static int glfwGetMouseButton(long window, int button) {
        return Mouse.isButtonDown(button) ? GLFW_PRESS : GLFW_RELEASE;
    }
}
