package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code SliderWidget}: a 0..1 value set by click/drag and arrow keys. */
public abstract class SliderWidget extends ClickableWidget {
    protected double value;

    protected SliderWidget(int x, int y, int width, int height, Text message, double value) {
        super(x, y, width, height, message);
        this.value = clamp(value);
    }

    protected abstract void updateMessage();
    protected abstract void applyValue();

    @Override protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + width, getY() + height, 0x80000000);
        int knob = getX() + (int) Math.round(value * (width - 8));
        context.fill(knob, getY(), knob + 8, getY() + height, isSelected() ? 0xFFFFFFFF : 0xFFA0A0A0);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
    }

    @Override public void onClick(double mouseX, double mouseY) { setValueFromMouse(mouseX); }

    @Override protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) { setValueFromMouse(mouseX); }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            setValue(value + (key == GLFW.GLFW_KEY_LEFT ? -1 : 1) / (double) Math.max(1, width - 8));
            return true;
        }
        return false;
    }

    private void setValueFromMouse(double mouseX) {
        setValue((mouseX - (getX() + 4)) / (double) Math.max(1, width - 8));
    }

    private void setValue(double next) {
        double old = value;
        value = clamp(next);
        if (old != value) applyValue();
        updateMessage();
    }

    private static double clamp(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
}
