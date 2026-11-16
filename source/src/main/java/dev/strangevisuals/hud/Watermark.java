package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;

public final class Watermark extends HudElement {
    private static final Identifier LOGO = StrangeVisuals.id("textures/gui/logo.png");
    private static final Identifier TEXT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float HEIGHT = 17.0f;
    private static final float PADDING = 7.0f;
    private static final float LOGO_SIZE = 10.0f;
    private static final float NAME_SIZE = 8.4f;
    private static final float QUIET_SIZE = 5.8f;
    private static final float GAP = 8.0f;

    private GlyphAtlas text;
    private GlyphAtlas bold;
    private final Spring nameWidth = new Spring(0, 1100, 42);
    private long lastTick = System.nanoTime();
    private float totalWidth = 150.0f;
    private float fpsSlot = -1.0f;

    public Watermark() {
        super("watermark", 0.045f, 0.045f);
    }

    private void ensureLoaded() {
        if (text == null) {
            text = GlyphAtlas.load(TEXT_DATA, TEXT_ATLAS);
        }
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }
        if (fpsSlot < 0.0f) {
            fpsSlot = MsdfText.width(bold, "999", QUIET_SIZE);
        }
    }

    @Override
    public float width() {
        return totalWidth;
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    private static final float ASCENDER = 0.952f;
    private static final float CAP_HEIGHT = 0.70f;

    private static float baselineFor(float centerY, float size) {
        return centerY + CAP_HEIGHT * 0.5f * size;
    }

    private static float topFor(float centerY, float size) {
        return baselineFor(centerY, size) - ASCENDER * size;
    }

    @Override
    public void render(DrawContext context, float x, float y) {
        ensureLoaded();
        MinecraftClient client = MinecraftClient.getInstance();

        String username = client.getSession() != null ? client.getSession().getUsername() : "Player";
        String fps = String.valueOf(client.getCurrentFps());
        String ping = currentPing(client) + "ms";
        String server = client.getCurrentServerEntry() != null
            ? client.getCurrentServerEntry().address : "singleplayer";

        long now = System.nanoTime();
        float deltaSeconds = (float) Math.min((now - lastTick) / 1_000_000_000.0, 0.05);
        lastTick = now;

        nameWidth.step(MsdfText.width(bold, username, NAME_SIZE), deltaSeconds);
        float nameSpan = (float) nameWidth.get();

        float unitWidth = MsdfText.width(text, "fps", QUIET_SIZE);
        float quiet = fpsSlot + 1.5f + unitWidth + GAP
            + MsdfText.width(text, ping, QUIET_SIZE) + GAP
            + MsdfText.width(text, server, QUIET_SIZE);
        totalWidth = PADDING + LOGO_SIZE + GAP + nameSpan + GAP + quiet + PADDING;

        HudSurface.panel(context, "watermark", x, y, totalWidth, HEIGHT, 1.0f);

        float centerY = y + HEIGHT * 0.5f;
        float cursor = x + PADDING;

        float logoY = centerY - LOGO_SIZE * 0.5f;
        GlowPipeline.radial(context, cursor + LOGO_SIZE * 0.5f, centerY, LOGO_SIZE * 1.2f,
            argb(NeonEdge.cool(), 75));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, Math.round(cursor), Math.round(logoY),
            0.0f, 0.0f, Math.round(LOGO_SIZE), Math.round(LOGO_SIZE), 120, 120, 120, 120, argb(0xFFFFFF, 255));
        cursor += LOGO_SIZE + GAP;

        MsdfText.draw(context, bold, username, cursor, topFor(centerY, NAME_SIZE), NAME_SIZE,
            argb(Theme.lighten(NeonEdge.cool(), 0.18f), 255));
        cursor += nameSpan + GAP;

        float fpsWidth = MsdfText.width(bold, fps, QUIET_SIZE);
        MsdfText.draw(context, bold, fps, cursor + fpsSlot - fpsWidth, topFor(centerY, QUIET_SIZE), QUIET_SIZE,
            argb(0xFFFFFF, 205));
        cursor += fpsSlot + 1.5f;
        MsdfText.draw(context, text, "fps", cursor, topFor(centerY, QUIET_SIZE), QUIET_SIZE,
            argb(0xFFFFFF, 90));
        cursor += unitWidth + GAP;

        cursor = drawQuiet(context, ping, cursor, centerY);
        drawQuiet(context, server, cursor, centerY);
    }

    private float drawQuiet(DrawContext context, String value, float cursor, float centerY) {
        float ruleX = cursor - GAP * 0.5f;
        context.fill(Math.round(ruleX), Math.round(centerY - 4.0f), Math.round(ruleX) + 1,
            Math.round(centerY + 4.0f), argb(0xFFFFFF, 30));
        MsdfText.draw(context, text, value, cursor, topFor(centerY, QUIET_SIZE), QUIET_SIZE,
            argb(0xFFFFFF, 150));
        return cursor + MsdfText.width(text, value, QUIET_SIZE) + GAP;
    }
    private static int currentPing(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static int argb(int rgb, int alpha) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }
}
