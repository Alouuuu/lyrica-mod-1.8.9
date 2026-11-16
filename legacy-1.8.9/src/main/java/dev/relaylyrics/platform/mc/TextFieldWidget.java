package dev.relaylyrics.platform.mc;

import net.minecraft.util.ChatAllowedCharacters;

import java.util.function.Consumer;

/**
 * 1.8.9 facade for {@code TextFieldWidget}: single-line editing with cursor, selection and
 * clipboard (ctrl+A/C/X/V). Subclasses usually draw themselves via {@code renderWidget}.
 */
public class TextFieldWidget extends ClickableWidget {
    private final TextRenderer renderer;
    private String text = "";
    private int maxLength = 32;
    private int cursor;
    private int selectionEnd;
    private boolean drawsBackground = true;
    private boolean editable = true;
    private Text placeholder;
    private Consumer<String> changedListener;
    private java.util.function.Predicate<String> predicate = s -> true;

    public TextFieldWidget(TextRenderer renderer, int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.renderer = renderer;
    }

    public String getText() { return text; }

    public void setText(String value) {
        String next = value == null ? "" : value.length() > maxLength ? value.substring(0, maxLength) : value;
        if (!predicate.test(next)) return;
        text = next;
        setCursorToEnd(false);
        onChanged();
    }

    public void setMaxLength(int value) {
        maxLength = value;
        if (text.length() > value) { text = text.substring(0, value); onChanged(); }
    }

    public void setDrawsBackground(boolean value) { drawsBackground = value; }
    public void setEditable(boolean value) { editable = value; }
    public void setPlaceholder(Text value) { placeholder = value; }
    public void setChangedListener(Consumer<String> listener) { changedListener = listener; }
    public void setTextPredicate(java.util.function.Predicate<String> value) { predicate = value == null ? s -> true : value; }

    public int getCursor() { return cursor; }

    public void setCursor(int value, boolean select) {
        cursor = Math.max(0, Math.min(text.length(), value));
        if (!select) selectionEnd = cursor;
    }

    public void setCursorToStart(boolean select) { setCursor(0, select); }
    public void setCursorToEnd(boolean select) { setCursor(text.length(), select); }
    public void setSelectionStart(int value) { setCursor(value, false); }
    public void setSelectionEnd(int value) { selectionEnd = Math.max(0, Math.min(text.length(), value)); }

    public String getSelectedText() {
        int a = Math.min(cursor, selectionEnd), b = Math.max(cursor, selectionEnd);
        return text.substring(a, b);
    }

    public void write(String value) {
        StringBuilder filtered = new StringBuilder();
        for (char c : value.toCharArray()) if (ChatAllowedCharacters.isAllowedCharacter(c)) filtered.append(c);
        int a = Math.min(cursor, selectionEnd), b = Math.max(cursor, selectionEnd);
        int room = maxLength - text.length() + (b - a);
        String insert = filtered.length() > room ? filtered.substring(0, Math.max(0, room)) : filtered.toString();
        String next = text.substring(0, a) + insert + text.substring(b);
        if (!predicate.test(next)) return;
        text = next;
        setCursor(a + insert.length(), false);
        onChanged();
    }

    private void erase(int direction) {
        if (!getSelectedText().isEmpty()) { write(""); return; }
        int target = Math.max(0, Math.min(text.length(), cursor + direction));
        int a = Math.min(cursor, target), b = Math.max(cursor, target);
        if (a == b) return;
        text = text.substring(0, a) + text.substring(b);
        setCursor(a, false);
        onChanged();
    }

    private void onChanged() { if (changedListener != null) changedListener.accept(text); }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!isFocused() || !visible) return false;
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        MinecraftClient.Keyboard clipboard = MinecraftClient.getInstance().keyboard;
        if (control && key == GLFW.GLFW_KEY_A) { setCursorToEnd(false); selectionEnd = 0; return true; }
        if (control && key == GLFW.GLFW_KEY_C) { clipboard.setClipboard(getSelectedText()); return true; }
        if (control && key == GLFW.GLFW_KEY_V) { if (editable) write(clipboard.getClipboard()); return true; }
        if (control && key == GLFW.GLFW_KEY_X) {
            clipboard.setClipboard(getSelectedText());
            if (editable) write("");
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE: if (editable) erase(-1); return true;
            case GLFW.GLFW_KEY_DELETE: if (editable) erase(1); return true;
            case GLFW.GLFW_KEY_LEFT: setCursor(cursor - 1, shift); return true;
            case GLFW.GLFW_KEY_RIGHT: setCursor(cursor + 1, shift); return true;
            case GLFW.GLFW_KEY_HOME: setCursorToStart(shift); return true;
            case GLFW.GLFW_KEY_END: setCursorToEnd(shift); return true;
            default: return false;
        }
    }

    @Override public boolean charTyped(char character, int modifiers) {
        if (!isFocused() || !visible || !editable || !ChatAllowedCharacters.isAllowedCharacter(character)) return false;
        write(Character.toString(character));
        return true;
    }

    @Override public void onClick(double mouseX, double mouseY) {
        int target = 0;
        double offset = mouseX - getX() - (drawsBackground ? 4 : 0);
        while (target < text.length() && renderer.getWidth(text.substring(0, target + 1)) <= offset) target++;
        setCursor(target, false);
    }

    @Override protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (drawsBackground) {
            context.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
            context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        }
        int textX = getX() + (drawsBackground ? 4 : 0), textY = getY() + (height - 8) / 2;
        if (text.isEmpty() && placeholder != null && !isFocused())
            context.drawTextWithShadow(renderer, placeholder, textX, textY, 0xFF707070);
        else context.drawTextWithShadow(renderer, renderer.trimToWidth(text, width - 8), textX, textY, 0xFFE0E0E0);
        if (isFocused() && System.currentTimeMillis() / 500 % 2 == 0) {
            int at = textX + renderer.getWidth(text.substring(0, cursor));
            context.fill(at, textY - 1, at + 1, textY + 9, 0xFFD0D0D0);
        }
    }
}
