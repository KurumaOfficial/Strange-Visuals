package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.media.MediaArt;
import dev.strangevisuals.media.MediaWatcher;
import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.PanelPipeline;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class MediaPlayerHud extends HudElement {
    private static final Identifier TEXT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float WIDTH = 152.0f;
    private static final float HEIGHT = 34.0f;
    private static final float PADDING = 5.0f;
    private static final float ART = 24.0f;

    private static final float RIGHT_INSET = 10.0f;
    private static final float TITLE_SIZE = 6.6f;
    private static final float ARTIST_SIZE = 5.4f;

    private GlyphAtlas text;
    private GlyphAtlas bold;
    private final Spring appear = new Spring(0.0, 900.0, 48.0);
    private long lastFrame = System.nanoTime();

    private float spin;
    private final Spring spinRate = new Spring(0.0, 320.0, 40.0);
    private float scroll;

    public MediaPlayerHud() {
        super("media", 0.5f, 0.9f);
    }

    @Override
    public float width() {
        return WIDTH;
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    public void render(DrawContext context, float x, float y) {
        if (!ModuleRegistry.mediaPlayer().enabled()) {
            return;
        }
        if (text == null) {
            text = GlyphAtlas.load(TEXT_DATA, TEXT_ATLAS);
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }
        MediaWatcher.ensureRunning();

        MediaWatcher.Track track = MediaWatcher.current();
        MediaArt.follow(track, ModuleRegistry.albumArt().value());
        boolean show = track.present() || HudManager.isEditing();

        long now = System.nanoTime();
        float delta = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;
        appear.step(show ? 1.0 : 0.0, delta);
        float ease = (float) appear.get();
        if (ease <= 0.01f) {
            return;
        }

        int alpha = Math.round(255 * ease);
        y += (1.0f - ease) * 8.0f;

        spinRate.step(track.playing() ? 1.0 : 0.0, delta);
        spin += delta * 40.0f * (float) spinRate.get();

        HudSurface.panel(context, "media", x, y, WIDTH, HEIGHT, ease);

        float artX = x + PADDING;
        float artY = y + (HEIGHT - ART) * 0.5f;
        drawArt(context, artX, artY, ease, alpha);

        float contentX = artX + ART + 8.0f;
        float contentRight = x + WIDTH - RIGHT_INSET;
        float contentW = contentRight - contentX;

        String artist = track.present() ? track.artist() : "not playing";
        String title = track.present() ? track.title() : "nothing on";

        if (!artist.isBlank()) {
            MsdfText.draw(context, text, clip(text, artist, ARTIST_SIZE, contentW - 44.0f), contentX, y + 6.0f,
                ARTIST_SIZE, argb(0xFFFFFF, Math.round(105 * ease)));
        }
        drawTransport(context, contentRight - 30.0f, y + 9.0f, track.playing(), ease);

        float titleY = y + HEIGHT - 15.0f;
        drawScrollingTitle(context, title, contentX, titleY, contentW, delta, ease);

        float railY = y + HEIGHT - 7.0f;
        PanelPipeline.draw(context, contentX, railY, contentW, 1.2f, argb(0xFFFFFF, Math.round(26 * ease)));
        if (track.length() > 0L) {
            float progress = Math.max(0.0f, Math.min(1.0f, track.position() / (float) track.length()));
            PanelPipeline.draw(context, contentX, railY, contentW * progress, 1.2f,
                argb(NeonEdge.cool(), Math.round(215 * ease)));
        }
    }

    private void drawArt(DrawContext context, float artX, float artY, float ease, int alpha) {
        float discX = artX + ART * 0.5f;
        float discY = artY + ART * 0.5f;

        RoundedRectPipeline.draw(context, artX, artY, ART, ART, argb(0xFFFFFF, Math.round(18 * ease)));
        if (MediaArt.available()) {
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(discX, discY);
            matrices.rotate((float) Math.toRadians(spin));
            matrices.translate(-discX, -discY);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, MediaArt.textureId(),
                Math.round(artX), Math.round(artY), 0.0f, 0.0f, Math.round(ART), Math.round(ART),
                Math.round(ART), Math.round(ART), argb(0xFFFFFF, alpha));
            matrices.popMatrix();
        } else {
            GlowPipeline.radial(context, discX, discY, ART * 0.52f, argb(NeonEdge.cool(), Math.round(60 * ease)));
            CirclePipeline.draw(context, discX, discY, ART * 0.30f, argb(0xFFFFFF, Math.round(40 * ease)));
        }
        CirclePipeline.draw(context, discX, discY, ART * 0.11f, argb(0x101114, alpha));
        CirclePipeline.draw(context, discX, discY, ART * 0.05f, argb(NeonEdge.cool(), Math.round(220 * ease)));
        RoundedRectPipeline.drawOutline(context, artX, artY, ART, ART, argb(0xFFFFFF, Math.round(34 * ease)));
    }

    private void drawScrollingTitle(DrawContext context, String title, float x, float y, float maxWidth,
                                    float delta, float ease) {
        float titleWidth = MsdfText.width(bold, title, TITLE_SIZE);
        int ink = argb(0xFFFFFF, Math.round(242 * ease));

        if (titleWidth <= maxWidth) {
            scroll = 0.0f;
            MsdfText.draw(context, bold, title, x, y, TITLE_SIZE, ink);
            return;
        }

        float span = titleWidth + 24.0f;
        scroll = (scroll + delta * 16.0f) % span;
        context.enableScissor(Math.round(x), Math.round(y - 2.0f),
            Math.round(x + maxWidth), Math.round(y + TITLE_SIZE + 3.0f));
        MsdfText.draw(context, bold, title, x - scroll, y, TITLE_SIZE, ink);
        MsdfText.draw(context, bold, title, x - scroll + span, y, TITLE_SIZE, ink);
        context.disableScissor();
    }

    private void drawTransport(DrawContext context, float x, float centerY, boolean playing, float ease) {
        int ink = argb(0xFFFFFF, Math.round(215 * ease));
        float gap = 13.0f;

        drawSkip(context, x, centerY, true, ink);
        if (playing) {
            PanelPipeline.draw(context, x + gap - 2.4f, centerY - 3.6f, 1.8f, 7.2f, ink);
            PanelPipeline.draw(context, x + gap + 0.8f, centerY - 3.6f, 1.8f, 7.2f, ink);
        } else {
            drawTriangle(context, x + gap - 2.0f, centerY, 6.6f, false, ink);
        }
        drawSkip(context, x + gap * 2.0f, centerY, false, ink);
    }

    private void drawSkip(DrawContext context, float x, float centerY, boolean back, int ink) {
        float barX = back ? x - 3.2f : x + 2.4f;
        PanelPipeline.draw(context, barX, centerY - 3.3f, 1.5f, 6.6f, ink);
        drawTriangle(context, back ? x + 2.8f : x - 2.8f, centerY, 5.8f, back, ink);
    }

    private void drawTriangle(DrawContext context, float tipX, float centerY, float size, boolean pointLeft, int ink) {
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            float halfHeight = size * 0.5f * (1.0f - t);
            float offset = size * 0.62f * t;
            float px = pointLeft ? tipX - offset : tipX + offset;
            PanelPipeline.draw(context, px, centerY - halfHeight, 1.2f, halfHeight * 2.0f, ink);
        }
    }

    private static String clip(GlyphAtlas atlas, String value, float size, float maxWidth) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (MsdfText.width(atlas, value, size) <= maxWidth) {
            return value;
        }
        String trimmed = value;
        while (trimmed.length() > 1 && MsdfText.width(atlas, trimmed + "...", size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
