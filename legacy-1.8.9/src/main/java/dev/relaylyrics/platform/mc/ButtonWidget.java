package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for {@code ButtonWidget} (with the 1.21 builder). */
public class ButtonWidget extends ClickableWidget {
    public interface PressAction { void onPress(ButtonWidget button); }

    private final PressAction onPress;

    protected ButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public static Builder builder(Text message, PressAction onPress) { return new Builder(message, onPress); }

    @Override public void onClick(double mouseX, double mouseY) { onPress.onPress(this); }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            onPress.onPress(this);
            return true;
        }
        return false;
    }

    @Override protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int fill = !active ? 0xA0303030 : isSelected() ? 0xE0505878 : 0xC0303448;
        context.fill(getX(), getY(), getX() + width, getY() + height, fill);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0x60FFFFFF);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2,
                active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    public static final class Builder {
        private final Text message;
        private final PressAction onPress;
        private int x, y, width = 150, height = 20;
        private Tooltip tooltip;

        private Builder(Text message, PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder position(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder size(int width, int height) { this.width = width; this.height = height; return this; }
        public Builder dimensions(int x, int y, int width, int height) { return position(x, y).size(width, height); }
        public Builder tooltip(Tooltip tooltip) { this.tooltip = tooltip; return this; }

        public ButtonWidget build() {
            ButtonWidget button = new ButtonWidget(x, y, width, height, message, onPress);
            button.setTooltip(tooltip);
            return button;
        }
    }
}
