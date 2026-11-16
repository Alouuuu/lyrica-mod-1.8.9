package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.8.9 facade for {@code TextRenderer}, backed by the vanilla {@link FontRenderer}.
 * Text whose font is a registered TTF face (bundled Inter, Google Fonts) is measured and drawn by
 * {@link TtfText}/{@link TtfFonts}; {@code minecraft:uniform} maps to the vanilla unicode font.
 */
public final class TextRenderer {
    private static TextRenderer instance;

    public final int fontHeight;
    private final FontRenderer font;

    private TextRenderer(FontRenderer font) {
        this.font = font;
        this.fontHeight = font.FONT_HEIGHT;
    }

    static TextRenderer get() {
        FontRenderer current = Minecraft.getMinecraft().fontRendererObj;
        if (instance == null || instance.font != current) instance = new TextRenderer(current);
        return instance;
    }

    public FontRenderer handle() { return font; }

    public int getWidth(String text) { return text == null ? 0 : font.getStringWidth(text); }
    public int getWidth(Text text) {
        if (text == null) return 0;
        TtfText.Face face = TtfFonts.face(text);
        if (face != null) return (int) Math.ceil(TtfFonts.width(face, text));
        boolean old = beginUniform(text);
        try { return font.getStringWidth(text.toLegacyString()); } finally { endUniform(old); }
    }
    public int getWidth(OrderedText text) { return text == null ? 0 : getWidth(text.text()); }

    public String trimToWidth(String text, int width) { return font.trimStringToWidth(text, width); }

    /** Styled trim (upstream returns a StringVisitable; callers use {@code .getString()}). */
    public Text trimToWidth(Text text, int width) {
        if (text == null) return Text.empty();
        TtfText.Face face = TtfFonts.face(text);
        if (face != null)
            return Text.literal(TtfText.trim(face, text.getString(), width, TtfFonts.bold(text), TtfFonts.italic(text)))
                    .setStyle(text.getStyle());
        String trimmed;
        boolean old = beginUniform(text);
        try { trimmed = font.trimStringToWidth(text.toLegacyString(), width); } finally { endUniform(old); }
        String plain = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(trimmed);
        return Text.literal(plain == null ? "" : plain).setStyle(text.getStyle());
    }

    public List<OrderedText> wrapLines(Text text, int width) {
        List<OrderedText> lines = new ArrayList<>();
        TtfText.Face face = TtfFonts.face(text);
        if (face != null) {
            for (String line : TtfText.wrap(face, text.getString(), Math.max(1, width), TtfFonts.bold(text), TtfFonts.italic(text)))
                lines.add(new OrderedText(Text.literal(line).setStyle(text.getStyle())));
            return lines;
        }
        boolean old = beginUniform(text);
        try {
            Style style = text.font() == null ? Style.EMPTY : Style.EMPTY.withFont(text.font());
            for (String line : font.listFormattedStringToWidth(text.toLegacyString(), Math.max(1, width)))
                lines.add(new OrderedText(Text.literal(line).setStyle(style)));
        } finally { endUniform(old); }
        return lines;
    }

    int draw(String text, float x, float y, int color, boolean shadow) {
        // FontRenderer forces alpha 255 when alpha < 4; such text is meant to be invisible.
        if (text == null || text.isEmpty() || (color >>> 24) < 4) return (int) x;
        GlStateManager.enableBlend();
        RenderSystem.defaultBlendFunc();
        int end = font.drawString(text, x, y, color, shadow);
        GlStateManager.color(1, 1, 1, 1);
        return end;
    }

    int draw(DrawContext context, Text text, float x, float y, int color, boolean shadow) {
        if (text == null) return (int) x;
        TtfText.Face face = TtfFonts.face(text);
        if (face != null) return TtfFonts.draw(context, face, text, x, y, color, shadow);
        boolean old = beginUniform(text);
        try { return draw(text.toLegacyString(), x, y, color, shadow); } finally { endUniform(old); }
    }

    /** 1.21's text layer types; 1.8.9 world text always draws with the current GL state. */
    public enum TextLayerType { NORMAL, SEE_THROUGH, POLYGON_OFFSET }

    /**
     * World-space overload (1.21 signature). 1.8.9 draws immediately with the current GL
     * modelview, which the world {@link MatrixStack} already mirrors, so the matrix, layer and
     * light arguments are unused.
     */
    public int draw(Text text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                    VertexConsumerProvider consumers, TextLayerType layer, int background, int light) {
        return draw(consumers.context(), text, x, y, color, shadow);
    }

    private static final Identifier UNIFORM = Identifier.ofVanilla("uniform");

    private boolean beginUniform(Text text) {
        boolean old = font.getUnicodeFlag();
        Identifier id = text.font();
        if (id != null && UNIFORM.equals(id)) font.setUnicodeFlag(true);
        return old;
    }

    private void endUniform(boolean old) { font.setUnicodeFlag(old); }
}
