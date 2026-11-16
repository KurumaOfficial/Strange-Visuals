package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.media.Lyrics;
import dev.strangevisuals.media.MediaWatcher;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class LyricsHud {
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float LIFE = 7.0f;
    private static final float FADE = 0.7f;
    private static final int MAX_ALIVE = 4;

    private static GlyphAtlas bold;
    private static final Random RANDOM = new Random();
    private static final List<Standing> ALIVE = new ArrayList<>();

    private static Lyrics.Line lastTaken;
    private static int counter;

    private LyricsHud() {
    }

    private record Standing(String text, Vec3d anchor, long shownAt) {
    }

    public static void render(DrawContext context) {
        if (!ModuleRegistry.lyricsOn()) {
            ALIVE.clear();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.getWindow() == null) {
            return;
        }
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }

        MediaWatcher.ensureRunning();
        MediaWatcher.Track track = MediaWatcher.current();
        Lyrics.follow(track);

        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        Lyrics.Line line = Lyrics.at(track.position(), 6000L);
        if (line != null && line != lastTaken) {
            lastTaken = line;
            take(line, camera);
        }

        long now = System.currentTimeMillis();
        Iterator<Standing> iterator = ALIVE.iterator();
        while (iterator.hasNext()) {
            Standing standing = iterator.next();
            float age = (now - standing.shownAt()) / 1000.0f;
            if (age > LIFE) {
                iterator.remove();
                continue;
            }
            float fade = Math.min(1.0f, age / FADE) * Math.min(1.0f, (LIFE - age) / FADE);
            if (fade > 0.01f) {
                draw(context, client, camera, standing, fade);
            }
        }
    }

    private static void take(Lyrics.Line line, Camera camera) {
        int every = Math.max(1, Math.round(ModuleRegistry.lyricsSpacing().value()));
        if (counter++ % every != 0) {
            return;
        }

        float yaw = (float) Math.toRadians(camera.getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);

        double side = (counter % 2 == 0 ? 1.0 : -1.0) * (2.4 + RANDOM.nextDouble() * 1.8);
        double ahead = 5.0 + RANDOM.nextDouble() * 2.5;
        double lift = -0.4 + RANDOM.nextDouble() * 1.6;

        Vec3d eye = camera.getCameraPos();
        Vec3d anchor = eye.add(forwardX * ahead + rightX * side, lift, forwardZ * ahead + rightZ * side);

        ALIVE.add(new Standing(line.text(), anchor, System.currentTimeMillis()));
        while (ALIVE.size() > MAX_ALIVE) {
            ALIVE.remove(0);
        }
    }

    private static void draw(DrawContext context, MinecraftClient client, Camera camera,
                             Standing standing, float fade) {
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        float cosPitch = (float) Math.cos(pitch);

        double fX = -(Math.sin(yaw) * cosPitch);
        double fY = -Math.sin(pitch);
        double fZ = Math.cos(yaw) * cosPitch;
        double rX = -Math.cos(yaw);
        double rZ = -Math.sin(yaw);

        double uX = -(rZ * fY);
        double uY = rZ * fX - rX * fZ;
        double uZ = rX * fY;

        Vec3d rel = standing.anchor().subtract(camera.getCameraPos());
        double viewZ = rel.x * fX + rel.y * fY + rel.z * fZ;
        if (viewZ <= 0.2) {
            return;
        }
        double viewX = rel.x * rX + rel.z * rZ;
        double viewY = rel.x * uX + rel.y * uY + rel.z * uZ;

        float width = context.getScaledWindowWidth();
        float height = context.getScaledWindowHeight();
        float aspect = width / Math.max(1.0f, height);
        float tanHalfFov = (float) Math.tan(Math.toRadians(client.options.getFov().getValue()) * 0.5);

        float ndcX = (float) (viewX / (viewZ * tanHalfFov * aspect));
        float ndcY = (float) (viewY / (viewZ * tanHalfFov));
        if (Math.abs(ndcX) > 3.0f || Math.abs(ndcY) > 3.0f) {
            return;
        }
        float screenX = (ndcX * 0.5f + 0.5f) * width;
        float screenY = (0.5f - ndcY * 0.5f) * height;

        float worldHeight = ModuleRegistry.lyricsSize().value() * 0.75f;
        float size = worldHeight / (2.0f * (float) viewZ * tanHalfFov) * height;
        if (size < 3.0f) {
            return;
        }

        int colour = ModuleRegistry.lyricsColour().rgb();
        String text = standing.text();
        int alpha = Math.round(248 * fade);

        screenY -= (1.0f - fade) * size * 0.4f;

        GlowPipeline.radial(context, screenX, screenY + size * 0.35f,
            MsdfText.width(bold, text, size) * 0.5f + size,
            argb(colour, Math.round(75 * fade)));
        MsdfText.drawCentered(context, bold, text, screenX + size * 0.06f, screenY + size * 0.06f, size,
            argb(0x000000, Math.round(170 * fade)));
        MsdfText.drawCentered(context, bold, text, screenX, screenY, size,
            argb(Theme.lighten(colour, 0.15f), alpha));
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
