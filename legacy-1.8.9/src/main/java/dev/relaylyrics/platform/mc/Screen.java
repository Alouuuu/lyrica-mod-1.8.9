package dev.relaylyrics.platform.mc;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.8.9 facade for Minecraft 1.21's {@code Screen}. Bridges the 1.8.9 {@link GuiScreen}
 * lifecycle/input (int coordinates, LWJGL2 keys, polled mouse) to the 1.21 callbacks
 * (double GUI coordinates, GLFW key codes, boolean "handled" results) and hosts
 * {@link ClickableWidget} children with focus.
 */
public abstract class Screen extends GuiScreen {
    protected final Text title;
    protected MinecraftClient client;
    protected TextRenderer textRenderer;
    private final List<ClickableWidget> children = new ArrayList<>();
    private ClickableWidget focused;
    private int draggingButton = -1;
    private double lastMouseX, lastMouseY;

    protected Screen(Text title) {
        this.title = title == null ? Text.empty() : title;
    }

    public Text getTitle() { return title; }

    // ------------------------------------------------------------------ 1.21 API

    protected void init() { }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (ClickableWidget child : children) child.render(context, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client.world == null) drawBackground(0);
        else context.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
    }

    /** 1.21 menu blur, via {@link MenuBlur} (no-op without framebuffers, e.g. OptiFine Fast Render). */
    protected void applyBlur(float delta) { MenuBlur.apply(delta); }

    public void tick() { }

    public void close() { client.setScreen(null); }

    public void removed() { }

    public boolean shouldPause() { return true; }

    public boolean shouldCloseOnEsc() { return true; }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            ClickableWidget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button)) {
                setFocused(child);
                if (button == 0) draggingButton = 0;
                return true;
            }
        }
        setFocused(null);
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingButton = -1;
        return focused != null && focused.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return focused != null && focused.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (ClickableWidget child : children)
            if (child.isMouseOver(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        return false;
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) { close(); return true; }
        return focused != null && focused.keyPressed(key, scanCode, modifiers);
    }

    public boolean keyReleased(int key, int scanCode, int modifiers) { return false; }

    public boolean charTyped(char character, int modifiers) {
        return focused != null && focused.charTyped(character, modifiers);
    }

    public void mouseMoved(double mouseX, double mouseY) { }

    protected <T extends ClickableWidget> T addDrawableChild(T widget) { children.add(widget); return widget; }
    protected <T extends ClickableWidget> T addSelectableChild(T widget) { return addDrawableChild(widget); }
    protected void remove(ClickableWidget widget) { children.remove(widget); if (focused == widget) setFocused(null); }
    protected void clearChildren() { children.clear(); focused = null; }
    public List<ClickableWidget> children() { return Collections.unmodifiableList(children); }

    public ClickableWidget getFocused() { return focused; }

    public void setFocused(ClickableWidget widget) {
        if (focused == widget) return;
        if (focused != null) focused.setFocused(false);
        focused = widget;
        if (widget != null) widget.setFocused(true);
    }

    // ------------------------------------------------------------------ 1.8.9 bridge

    private void refresh() {
        client = MinecraftClient.getInstance();
        textRenderer = client.textRenderer;
    }

    @Override
    public final void initGui() {
        refresh();
        Keyboard.enableRepeatEvents(true);
        clearChildren();
        init();
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        refresh();
        DrawContext context = DrawContext.begin(partialTicks);
        try { render(context, mouseX, mouseY, partialTicks); }
        finally { context.end(); }
    }

    @Override
    public final void updateScreen() { tick(); }

    @Override
    public final void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        MenuBlur.release();
        removed();
    }

    @Override
    public final boolean doesGuiPauseGame() { return shouldPause(); }

    /** GUI-scaled mouse X with sub-pixel precision (1.21 passes doubles). */
    // Like 1.21's Mouse: GUI coordinates come from the window's scaled size, never from this.width/height
    // (screens such as LyricsConfigScreen replace width/height with a virtual, ui-scaled size).
    protected final double eventMouseX() {
        return Mouse.getEventX() * (double) new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth() / mc.displayWidth;
    }
    protected final double eventMouseY() {
        int scaled = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight();
        return scaled - Mouse.getEventY() * (double) scaled / mc.displayHeight - 1;
    }

    @Override
    public final void handleMouseInput() {
        refresh();
        double x = eventMouseX(), y = eventMouseY();
        int button = Mouse.getEventButton();
        int wheel = Mouse.getEventDWheel();
        if (button >= 0) {
            if (Mouse.getEventButtonState()) {
                draggingButton = button;
                mouseClicked(x, y, button);
            } else {
                mouseReleased(x, y, button);
                draggingButton = -1;
            }
        } else if (wheel != 0) {
            mouseScrolled(x, y, 0, Integer.signum(wheel));
        } else if (x != lastMouseX || y != lastMouseY) {
            if (draggingButton >= 0 && Mouse.isButtonDown(draggingButton))
                mouseDragged(x, y, draggingButton, x - lastMouseX, y - lastMouseY);
            else mouseMoved(x, y);
        }
        lastMouseX = x;
        lastMouseY = y;
    }

    @Override
    public final void handleKeyboardInput() {
        refresh();
        int lwjgl = Keyboard.getEventKey();
        char character = Keyboard.getEventCharacter();
        int key = GLFW.toGlfw(lwjgl);
        int mods = GLFW.modifiers();
        if (Keyboard.getEventKeyState()) {
            boolean handled = key != GLFW.GLFW_KEY_UNKNOWN && keyPressed(key, lwjgl, mods);
            if (!handled && character >= ' ' && character != 127) charTyped(character, mods);
        } else if (key != GLFW.GLFW_KEY_UNKNOWN) {
            keyReleased(key, lwjgl, mods);
        }
        mc.dispatchKeypresses();
    }

    // The 1.8.9 int-based handlers are routed through handleMouseInput/handleKeyboardInput above.
    @Override protected final void mouseClicked(int mouseX, int mouseY, int mouseButton) { }
    @Override protected final void mouseReleased(int mouseX, int mouseY, int state) { }
    @Override protected final void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) { }
    @Override protected final void keyTyped(char typedChar, int keyCode) { }
}
