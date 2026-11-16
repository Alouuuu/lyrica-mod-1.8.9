package dev.relaylyrics.platform.mc;

/**
 * 1.8.9 facade for Minecraft 1.21's {@code ClickableWidget}: bounds, hover/focus state,
 * click dispatch and a message. Hosted by {@link Screen}.
 */
public abstract class ClickableWidget {
    protected int width;
    protected int height;
    private int x;
    private int y;
    private Text message;
    public boolean active = true;
    public boolean visible = true;
    protected boolean hovered;
    private boolean focused;
    private Tooltip tooltip;

    protected ClickableWidget(int x, int y, int width, int height, Text message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message == null ? Text.empty() : message;
    }

    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        hovered = isMouseOver(mouseX, mouseY);
        renderWidget(context, mouseX, mouseY, delta);
    }

    protected abstract void renderWidget(DrawContext context, int mouseX, int mouseY, float delta);

    public void onClick(double mouseX, double mouseY) { }
    public void onRelease(double mouseX, double mouseY) { }
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) { }

    protected boolean isValidClickButton(int button) { return button == 0; }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isValidClickButton(button) || !isMouseOver(mouseX, mouseY)) return false;
        playDownSound(MinecraftClient.getInstance().getSoundManager());
        onClick(mouseX, mouseY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isValidClickButton(button)) return false;
        onRelease(mouseX, mouseY);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isValidClickButton(button)) return false;
        onDrag(mouseX, mouseY, deltaX, deltaY);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) { return false; }
    public boolean keyPressed(int key, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char character, int modifiers) { return false; }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return active && visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public void playDownSound(MinecraftClient.SoundManager sounds) { sounds.playClick(); }

    public boolean isHovered() { return hovered; }
    public boolean isSelected() { return hovered || focused; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean value) { focused = value; }
    public boolean isNarratable() { return visible && active; }

    public Text getMessage() { return message; }
    public void setMessage(Text value) { message = value == null ? Text.empty() : value; }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int value) { x = value; }
    public void setY(int value) { y = value; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setWidth(int value) { width = value; }
    public void setHeight(int value) { height = value; }
    public int getRight() { return x + width; }
    public int getBottom() { return y + height; }

    public void setTooltip(Tooltip value) { tooltip = value; }
    public Tooltip getTooltip() { return tooltip; }

    /** Narration does not exist on 1.8.9; kept so ported widgets compile. */
    protected void appendClickableNarrations(NarrationMessageBuilder builder) { }
    protected void appendDefaultNarrations(NarrationMessageBuilder builder) { }
}
