package dev.strangevisuals.hands;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public final class HandPanel {
    private static final Identifier TEXT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float X = 8.0f;
    private static final float Y = 8.0f;
    private static final float WIDTH = 124.0f;
    private static final float PADDING = 7.0f;
    private static final float ROW = 14.0f;
    private static final float TITLE_SIZE = 6.4f;
    private static final float LABEL_SIZE = 5.2f;

    private static final Action[] ACTIONS = {
        new Action("Reset right", Kind.RESET_RIGHT),
        new Action("Reset left", Kind.RESET_LEFT),
        new Action("Reset both", Kind.RESET_BOTH),
        new Action("Mirror to left", Kind.MIRROR)
    };

    private static GlyphAtlas text;
    private static GlyphAtlas bold;
    private static long lastFrame = System.nanoTime();

    private HandPanel() {
    }

    private static float height() {
        return PADDING + 11.0f + 2.0f * 9.0f + 5.0f + ACTIONS.length * ROW + PADDING - 4.0f;
    }

    public static boolean press(double mouseX, double mouseY) {
        if (!HandEditor.active() || !inside(mouseX, mouseY)) {
            return false;
        }
        float top = rowsTop();
        for (int i = 0; i < ACTIONS.length; i++) {
            if (mouseY >= top + i * ROW && mouseY < top + (i + 1) * ROW - 2.0f) {
                apply(ACTIONS[i].kind);
                HandPose.save();
                return true;
            }
        }

        return true;
    }

    private static void apply(Kind kind) {
        switch (kind) {
            case RESET_RIGHT -> HandPose.reset(Arm.RIGHT);
            case RESET_LEFT -> HandPose.reset(Arm.LEFT);
            case RESET_BOTH -> {
                HandPose.reset(Arm.RIGHT);
                HandPose.reset(Arm.LEFT);
            }
            case MIRROR -> HandPose.mirror();
            default -> { }
        }
    }

    public static boolean inside(double mouseX, double mouseY) {
        return mouseX >= X && mouseX < X + WIDTH && mouseY >= Y && mouseY < Y + height();
    }

    public static void render(DrawContext context, double mouseX, double mouseY) {
        if (!HandEditor.active()) {
            return;
        }
        ensureLoaded();
        long now = System.nanoTime();
        double delta = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        float h = height();
        GlassPanelPipeline.draw(context, X, Y, WIDTH, h, argb(Theme.glassTint(), 238));
        NeonEdge.frame(context, X, Y, WIDTH, h, 23, 0.9f);

        float cursor = Y + PADDING;
        MsdfText.draw(context, bold, "View Model", X + PADDING, cursor, TITLE_SIZE,
            argb(Theme.lighten(Theme.accent(), 0.3f), 255));
        cursor += 11.0f;

        cursor = readout(context, "right", Arm.RIGHT, cursor);
        cursor = readout(context, "left", Arm.LEFT, cursor);
        cursor += 5.0f;

        for (int i = 0; i < ACTIONS.length; i++) {
            Action action = ACTIONS[i];
            float top = cursor + i * ROW;
            boolean hovered = mouseX >= X + PADDING - 3.0f && mouseX < X + WIDTH - PADDING + 3.0f
                && mouseY >= top && mouseY < top + ROW - 2.0f;
            action.hover.step(hovered ? 1.0 : 0.0, delta);
            button(context, action, top, (float) action.hover.get());
        }
    }

    private static float readout(DrawContext context, String name, Arm arm, float top) {
        boolean moved = HandPose.moved(arm);
        MsdfText.draw(context, text, name, X + PADDING, top, LABEL_SIZE,
            argb(0xFFFFFF, moved ? 165 : 90));

        String value = moved
            ? String.format("%.2f  %.2f  %.2f  ×%.2f", HandPose.x(arm), HandPose.y(arm),
                HandPose.z(arm), HandPose.scale(arm))
            : "default";
        float width = MsdfText.width(text, value, LABEL_SIZE);
        MsdfText.draw(context, text, value, X + WIDTH - PADDING - width, top, LABEL_SIZE,
            argb(moved ? Theme.lighten(Theme.accent(), 0.25f) : 0xFFFFFF, moved ? 220 : 70));
        return top + 9.0f;
    }

    private static void button(DrawContext context, Action action, float top, float hover) {
        float left = X + PADDING - 3.0f;
        float width = WIDTH - (PADDING - 3.0f) * 2.0f;
        float tall = ROW - 3.0f;

        RoundedRectPipeline.draw(context, left, top, width, tall,
            argb(0xFFFFFF, Math.round(8 + hover * 26)));
        if (hover > 0.01f) {
            NeonEdge.frame(context, left, top, width, tall, 41, hover * 0.8f);
        }

        MsdfText.draw(context, text, action.label, left + 7.0f + hover * 3.0f,
            top + (tall - LABEL_SIZE) * 0.5f - 1.2f, LABEL_SIZE,
            argb(0xFFFFFF, Math.round(140 + hover * 110)));
    }

    public static void renderHint(DrawContext context, String hint, boolean held) {
        ensureLoaded();
        float size = 5.6f;
        float width = MsdfText.width(text, hint, size);
        float padding = 9.0f;
        float boxWidth = width + padding * 2.0f;
        float boxHeight = 15.0f;
        float x = (context.getScaledWindowWidth() - boxWidth) * 0.5f;
        float y = context.getScaledWindowHeight() - 46.0f;

        GlassPanelPipeline.draw(context, x, y, boxWidth, boxHeight, argb(Theme.glassTint(), 225));
        NeonEdge.frame(context, x, y, boxWidth, boxHeight, 57, held ? 1.0f : 0.6f);
        MsdfText.draw(context, text, hint, x + padding, y + (boxHeight - size) * 0.5f - 1.1f, size,
            argb(held ? Theme.lighten(Theme.accent(), 0.45f) : 0xFFFFFF, held ? 245 : 180));
    }

    private static float rowsTop() {
        return Y + PADDING + 11.0f + 18.0f + 5.0f;
    }

    private static void ensureLoaded() {
        if (text == null) {
            text = GlyphAtlas.load(TEXT_DATA, TEXT_ATLAS);
        }
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }
    }

    private static int argb(int rgb, int alpha) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    private enum Kind {
        RESET_RIGHT,
        RESET_LEFT,
        RESET_BOTH,
        MIRROR
    }

    private static final class Action {
        final String label;
        final Kind kind;
        final Spring hover = new Spring(0.0, 900.0, 48.0);

        Action(String label, Kind kind) {
            this.label = label;
            this.kind = kind;
        }
    }
}
