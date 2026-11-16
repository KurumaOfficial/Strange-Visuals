package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.RoundedRectPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class NameTags {
    private static final Identifier TEXT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float HEIGHT = 18.0f;
    private static final float PAD = 5.0f;
    private static final float FACE = 11.0f;
    private static final float GAP = 5.0f;
    private static final float NAME_SIZE = 7.4f;
    private static final float SMALL_SIZE = 6.4f;

    private static final float RANGE = 64.0f;

    private static final Matrix4f viewProjection = new Matrix4f();
    private static final Vector4f projected = new Vector4f();
    private static Vec3d eye = Vec3d.ZERO;
    private static boolean ready;

    private static final class PlayerEntry {
        AbstractClientPlayerEntity player;
        double distSq;
    }
    private static final java.util.List<PlayerEntry> playerPool = new java.util.ArrayList<>();
    private static final java.util.List<PlayerEntry> activeEntries = new java.util.ArrayList<>();
    private static final java.util.List<String> rankPieces = new java.util.ArrayList<>();
    private static final java.util.List<Integer> rankColours = new java.util.ArrayList<>();

    private static GlyphAtlas text;
    private static GlyphAtlas bold;

    private NameTags() {
    }

    public static void capture(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null || context.matrices() == null) {
            ready = false;
            return;
        }

        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(
            client.options.getFov().getValue());
        viewProjection.set(projection).mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
        eye = camera.getCameraPos();
        ready = true;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ready || client.world == null || client.player == null
            || !ModuleRegistry.nameTags().enabled() || client.options.hudHidden) {
            return;
        }
        if (text == null) {
            text = GlyphAtlas.load(TEXT_DATA, TEXT_ATLAS);
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }

        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        float scaleSetting = ModuleRegistry.nameTagScale().value();

        activeEntries.clear();
        java.util.List<AbstractClientPlayerEntity> players = client.world.getPlayers();
        while (playerPool.size() < players.size()) {
            playerPool.add(new PlayerEntry());
        }
        for (int i = 0; i < players.size(); i++) {
            AbstractClientPlayerEntity p = players.get(i);
            PlayerEntry entry = playerPool.get(i);
            entry.player = p;
            entry.distSq = p.squaredDistanceTo(eye);
            activeEntries.add(entry);
        }
        activeEntries.sort((a, b) -> Double.compare(b.distSq, a.distSq));
        for (PlayerEntry entry : activeEntries) {
            AbstractClientPlayerEntity player = entry.player;
            if (player == client.player || player.isSpectator()) {
                continue;
            }

            if (player.isInvisibleTo(client.player)) {
                continue;
            }

            Vec3d head = player.getLerpedPos(tickDelta).add(0.0, player.getHeight() + 0.38, 0.0);
            double away = head.distanceTo(eye);
            if (away > RANGE) {
                continue;
            }

            if (!client.player.canSee(player)) {
                continue;
            }
            if (!project(context, head)) {
                continue;
            }

            float scale = clamp((float) (9.0 / Math.max(away, 1.0)), 0.5f, 1.15f) * scaleSetting;
            draw(context, client, player, projected.x, projected.y, scale);
        }
    }

    private static boolean project(DrawContext context, Vec3d at) {
        projected.set((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z), 1.0f);
        viewProjection.transform(projected);
        if (projected.w <= 0.05f) {
            return false;
        }
        float ndcX = projected.x / projected.w;
        float ndcY = projected.y / projected.w;
        projected.x = (ndcX * 0.5f + 0.5f) * context.getScaledWindowWidth();
        projected.y = (0.5f - ndcY * 0.5f) * context.getScaledWindowHeight();
        return true;
    }

    private static void draw(DrawContext context, MinecraftClient client,
                             AbstractClientPlayerEntity player, float screenX, float screenY,
                             float scale) {
        String name = player.getName().getString();
        collect(prefixOf(player), rankPieces, rankColours);
        ItemStack held = player.getMainHandStack();

        boolean known = player.getMaxHealth() > 0.0f;
        String health = known ? String.valueOf(Math.max(0, Math.round(player.getHealth()))) : "???";

        float rankWidth = rankPieces.isEmpty() ? 0.0f : runsWidth(rankPieces, SMALL_SIZE) + GAP;
        float nameWidth = MsdfText.width(bold, name, NAME_SIZE);
        float healthWidth = MsdfText.width(bold, health, SMALL_SIZE);
        boolean showItem = !held.isEmpty() && ModuleRegistry.nameTagItem().value();
        float itemWidth = showItem ? 12.0f + GAP : 0.0f;
        float width = PAD + FACE + GAP + rankWidth + nameWidth + GAP + itemWidth
            + 8.0f + 2.0f + healthWidth + PAD;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(screenX, screenY);
        matrices.scale(scale, scale);
        matrices.translate(-width * 0.5f, -HEIGHT);

        HudSurface.panel(context, "nametags", 0.0f, 0.0f, width, HEIGHT, 1.0f);

        float centerY = HEIGHT * 0.5f;
        float cursor = PAD;

        Identifier skin = player.getSkin() != null ? player.getSkin().body().texturePath()
            : DefaultSkinHelper.getTexture();
        int faceAlpha = 255;
        int size = Math.round(FACE);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(cursor),
            Math.round(centerY - FACE * 0.5f), 8.0f, 8.0f, size, size, 8, 8, 64, 64,
            argb(0xFFFFFF, faceAlpha));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(cursor),
            Math.round(centerY - FACE * 0.5f), 40.0f, 8.0f, size, size, 8, 8, 64, 64,
            argb(0xFFFFFF, faceAlpha));
        cursor += FACE + GAP;

        if (!rankPieces.isEmpty()) {
            drawRuns(context, rankPieces, rankColours, cursor, centerY, SMALL_SIZE, 240);
            cursor += rankWidth;
        }
        MsdfText.drawOnExact(context, bold, name, cursor, centerY, NAME_SIZE, argb(0xFFFFFF, 245));
        cursor += nameWidth + GAP;

        if (showItem) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(cursor, centerY - 6.0f);
            context.getMatrices().scale(0.75f, 0.75f);
            context.drawItem(held, 0, 0);
            context.getMatrices().popMatrix();
            cursor += itemWidth;
        }

        heart(context, cursor + 4.5f, centerY, !known, player);
        MsdfText.drawOnExact(context, bold, health, cursor + 11.0f, centerY, SMALL_SIZE,
            argb(known ? healthColour(player) : 0xFFFFFF, known ? 245 : 130));

        matrices.popMatrix();
    }

    private static void heart(DrawContext context, float cx, float cy, boolean hidden,
                              AbstractClientPlayerEntity player) {
        int colour = hidden ? 0x9AA0AC : healthColour(player);
        int alpha = hidden ? 120 : 245;

        float lift = cy - 0.35f;
        CirclePipeline.draw(context, cx - 1.35f, lift - 0.85f, 1.75f, argb(colour, alpha));
        CirclePipeline.draw(context, cx + 1.35f, lift - 0.85f, 1.75f, argb(colour, alpha));
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(cx, lift);
        matrices.rotate((float) Math.toRadians(45.0));
        RoundedRectPipeline.draw(context, -1.9f, -1.9f, 3.8f, 3.8f, argb(colour, alpha));
        matrices.popMatrix();
    }

    private static int healthColour(AbstractClientPlayerEntity player) {
        float share = Math.max(0.0f, Math.min(1.0f,
            player.getHealth() / Math.max(1.0f, player.getMaxHealth())));
        if (share >= 0.6f) {
            return mix(0xE8D65B, 0x6FE39A, Math.min(1.0f, (share - 0.6f) / 0.4f));
        }
        return mix(0xFF5C5C, 0xE8D65B, Math.min(1.0f, (share - 0.2f) / 0.4f));
    }

    private static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    private static void collect(Text source, java.util.List<String> pieces,
                                java.util.List<Integer> colours) {
        pieces.clear();
        colours.clear();
        source.visit((style, part) -> {
            if (part.isEmpty()) {
                return java.util.Optional.empty();
            }
            pieces.add(part);
            TextColor colour = style.getColor();
            colours.add(colour == null ? 0xFFFFFF : colour.getRgb());
            return java.util.Optional.empty();
        }, Style.EMPTY);
    }

    private static float runsWidth(java.util.List<String> pieces, float size) {
        float sum = 0.0f;
        for (String piece : pieces) {
            sum += MsdfText.width(bold, piece, size);
        }
        return sum;
    }

    private static float drawRuns(DrawContext context, java.util.List<String> pieces,
                                  java.util.List<Integer> colours, float x, float centerY,
                                  float size, int alpha) {
        float cursor = x;
        for (int i = 0; i < pieces.size(); i++) {
            String piece = pieces.get(i);
            MsdfText.drawOnExact(context, bold, piece, cursor, centerY, size,
                argb(colours.get(i), alpha));
            cursor += MsdfText.width(bold, piece, size);
        }
        return cursor - x;
    }

    private static Text prefixOf(AbstractClientPlayerEntity player) {
        Team team = player.getScoreboardTeam();
        return team == null ? Text.empty() : team.getPrefix();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean replaces(Text ignored) {
        return ModuleRegistry.nameTags().enabled();
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
