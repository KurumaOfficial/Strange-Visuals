package dev.strangevisuals.ui;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.i18n.I18n;
import dev.strangevisuals.render.BackgroundPipeline;
import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.IconPipeline;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.PanelPipeline;
import dev.strangevisuals.render.Pipelines;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.editor.CustomTextureLoader;
import dev.strangevisuals.ui.editor.MainMenuConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 100% Custom Main Menu Screen for StrangeVisuals.
 * Restores original AAA high-end typography, animations, watermark, and branding,
 * with 1:1 editor synchronicity and guaranteed responsive screen transitions.
 */
public final class MainMenuScreen extends Screen {
    private static final Identifier LOGO = StrangeVisuals.id("textures/gui/logo.png");
    private static final Identifier ICON_DATA = StrangeVisuals.id("fonts/msdf/wtmico.json");
    private static final Identifier ICON_ATLAS = StrangeVisuals.id("fonts/msdf/wtmico.png");
    private static final Identifier TITLE_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier TITLE_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");
    private static final Identifier BRAND_DATA = StrangeVisuals.id("fonts/msdf/brands.json");
    private static final Identifier BRAND_ATLAS = StrangeVisuals.id("fonts/msdf/brands.png");

    private static final int PERSON = 'P';
    private static final int PEOPLE = 'N';
    private static final int GEAR = 'O';
    private static final int POWER = 'Q';
    private static final int DISCORD = 0xF144;
    private static final int TELEGRAM = 0xF1B6;

    private static final float BRAND_H = 78.0f;
    private static final String SLOGAN_TOP = "Free to use,";
    private static final String SLOGAN_BOTTOM = "Yours to shape.";
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("dd.MM.yy");

    private static final float OPEN_MS = 460.0f;
    private static final float CLOSE_MS = 240.0f;

    private final List<Entry> entries = new ArrayList<>();
    private final Spring telegramHover = new Spring(0.0, 280.0, 20.0);
    private final Spring discordHover = new Spring(0.0, 280.0, 20.0);

    private GlyphAtlas icons;
    private GlyphAtlas titleFont;
    private GlyphAtlas brands;
    private long lastFrame = System.nanoTime();
    private long openedAt;
    private Identifier skin;
    private java.util.function.Supplier<SkinTextures> skinLookup;
    private boolean soundPlayed;
    private boolean closing;
    private long closingAt;
    private Runnable pendingAction;

    public static final class Entry {
        public final String id;
        public final int icon;
        public final String labelKey;
        public final Runnable action;
        public final Spring hover = new Spring(0.0, 280.0, 22.0);

        public Entry(String id, int icon, String labelKey, Runnable action) {
            this.id = id;
            this.icon = icon;
            this.labelKey = labelKey;
            this.action = action;
        }
    }

    public MainMenuScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        Pipelines.warmUp();
        icons = GlyphAtlas.load(ICON_DATA, ICON_ATLAS);
        titleFont = GlyphAtlas.load(TITLE_FONT_DATA, TITLE_FONT_ATLAS);
        brands = GlyphAtlas.load(BRAND_DATA, BRAND_ATLAS);

        MainMenuConfig.load();
        if (MainMenuConfig.get().useVanillaTitleScreen) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.send(() -> mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen()));
            }
            return;
        }

        entries.clear();
        entries.add(new Entry("SINGLEPLAYER", PERSON, "strangevisuals.menu.singleplayer", this::openSingleplayer));
        entries.add(new Entry("MULTIPLAYER", PEOPLE, "strangevisuals.menu.multiplayer", this::openMultiplayer));
        entries.add(new Entry("SETTINGS", GEAR, "strangevisuals.menu.settings", this::openSettings));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            entries.add(new Entry("MODS", 'K', "strangevisuals.menu.mods", this::openModMenu));
        }
        entries.add(new Entry("QUIT", POWER, "strangevisuals.menu.quit", this::quit));

        openedAt = Util.getMeasuringTimeMs();
        closing = false;
        pendingAction = null;
        soundPlayed = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MainMenuConfig menuCfg = MainMenuConfig.get();
        if (menuCfg.useVanillaTitleScreen) {
            if (client != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen)) {
                client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            }
            return;
        }

        long now = System.nanoTime();
        double deltaSeconds = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        float vis = visibility();
        if (menuCfg.backgroundMode == MainMenuConfig.BackgroundMode.CUSTOM_IMAGE) {
            Identifier customTex = CustomTextureLoader.getCustomTexture();
            if (customTex != null) {
                int imgAlpha = Math.round(menuCfg.imageOpacity * 255);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height, withAlpha(0xFFFFFF, imgAlpha));
            } else {
                BackgroundPipeline.draw(vis, menuCfg.shaderPreset.id, menuCfg.shaderIntensity, menuCfg.imageDarkening, 1.0f);
            }
            int darkAlpha = Math.round(menuCfg.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
        } else if (menuCfg.backgroundMode == MainMenuConfig.BackgroundMode.IMAGE_AND_SHADER) {
            Identifier customTex = CustomTextureLoader.getCustomTexture();
            if (customTex != null) {
                int imgAlpha = Math.round(menuCfg.imageOpacity * 255);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height, withAlpha(0xFFFFFF, imgAlpha));
            }
            int darkAlpha = Math.round(menuCfg.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
            BackgroundPipeline.draw(vis, menuCfg.shaderPreset.id, menuCfg.shaderIntensity, 0.0f, menuCfg.shaderOpacity);
        } else if (menuCfg.backgroundMode == MainMenuConfig.BackgroundMode.PANORAMA) {
            dev.strangevisuals.render.PanoramaManager.render(context, dev.strangevisuals.render.PanoramaManager.getProceduralMainMenuType(), width, height, delta);
            int darkAlpha = Math.round(menuCfg.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
        } else {
            BackgroundPipeline.draw(vis, menuCfg.shaderPreset.id, menuCfg.shaderIntensity, menuCfg.imageDarkening, 1.0f);
        }

        if (menuCfg.shaderOverlayEnabled && menuCfg.shaderEffect != MainMenuConfig.ShaderEffect.NONE) {
            int glowAlpha = Math.round(menuCfg.shaderIntensity * 60);
            GlowPipeline.radial(context, width * 0.5f, height * 0.5f, width * 0.45f, withAlpha(NeonEdge.cool(), glowAlpha));
        }

        if (!soundPlayed && client != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.OPEN_SOUND, 0.9f, 1.0f));
            soundPlayed = true;
        }

        float visible = visibility();
        if (closing && (visible <= 0.02f || (Util.getMeasuringTimeMs() - closingAt) >= CLOSE_MS)) {
            Runnable action = pendingAction;
            pendingAction = null;
            if (action != null) {
                action.run();
            }
            return;
        }

        context.fillGradient(0, 0, width, height, 0x00000000, 0x66020403);


        // 1. Watermark ambient logo
        renderMark(context, visible);

        // 2. Logo & Slogan (Independent customizable objects)
        renderLogo(context, menuCfg, visible);
        renderSlogan(context, menuCfg, visible);

        // 3. Entries (Action buttons)
        renderEntries(context, menuCfg, mouseX, mouseY, deltaSeconds, visible);

        // 4. Player identity card
        renderPlayerCard(context, menuCfg, visible);

        // 5. Social icons
        renderSocialCredits(context, mouseX, mouseY, deltaSeconds, visible);

    }

    private float visibility() {
        long now = Util.getMeasuringTimeMs();
        if (closing) {
            float t = (float) Math.min((now - closingAt) / CLOSE_MS, 1.0);
            return 1.0f - t * t;
        }
        float t = (float) Math.min((now - openedAt) / OPEN_MS, 1.0);
        float inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }

    private void requestTransition(Runnable action) {
        if (closing) {
            return;
        }
        closing = true;
        closingAt = Util.getMeasuringTimeMs();
        pendingAction = action;
    }

    private float step(float visible, float delay) {
        float t = Math.max(0.0f, Math.min(1.0f, (visible - delay) / Math.max(0.08f, 1.0f - delay)));
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    private void renderMark(DrawContext context, float visible) {
        if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            return;
        }
        float appear = step(visible, 0.0f);
        float time = Util.getMeasuringTimeMs() / 1000.0f;
        float breathe = 1.0f + (float) Math.sin(time * 0.5) * 0.012f;
        float size = Math.min(height * 0.62f, width * 0.34f) * breathe * (1.04f - 0.04f * appear);
        float cx = width * 0.68f;
        float cy = height * 0.5f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(size / 120.0f, size / 120.0f);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, -60, -60, 0.0f, 0.0f,
            120, 120, 120, 120, withAlpha(0xFFFFFF, Math.round(16 * appear)));
        context.getMatrices().popMatrix();
    }

    private void renderLogo(DrawContext context, MainMenuConfig menuCfg, float visible) {
        MainMenuConfig.ElementConfig el = menuCfg.getElement("LOGO");
        if (!el.visible) return;

        float x = el.getX(width);
        float top = el.getY(height, entries.size());
        float scale = el.scale;

        float logoAppear = step(visible, 0.02f);
        if (logoAppear <= 0.001f) {
            return;
        }

        float pop = 1.0f + (float) Math.sin(Math.min(1.0f, logoAppear) * Math.PI) * 0.14f;
        float logoSize = 34.0f * scale;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + logoSize * 0.5f, top + logoSize * 0.5f);
        context.getMatrices().scale(pop, pop);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO,
            Math.round(-logoSize * 0.5f), Math.round(-logoSize * 0.5f), 0.0f, 0.0f,
            Math.round(logoSize), Math.round(logoSize), 120, 120, 120, 120,
            withAlpha(0xFFFFFF, Math.round(255 * logoAppear)));
        context.getMatrices().popMatrix();

        if (dev.strangevisuals.ui.style.StyleManager.getStyle() != dev.strangevisuals.ui.style.MenuStyle.VANILLA && logoAppear < 0.999f) {
            GlowPipeline.radial(context, x + logoSize * 0.5f, top + logoSize * 0.5f,
                logoSize * (0.6f + logoAppear * 2.2f),
                withAlpha(NeonEdge.cool(), Math.round(90 * (1.0f - logoAppear))));
        }

        int brandCol = dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA
            ? 0xFFFFAA00 : NeonEdge.cool();

        float textX = x + logoSize + 12.0f * scale;
        reveal(context, "STRANGE", textX, top + 9.0f * scale, 11.0f * scale, 1.4f * scale, 0xFFFFFF, 250,
            step(visible, 0.08f));
        reveal(context, "VISUALS", textX, top + 23.0f * scale, 11.0f * scale, 1.4f * scale, brandCol, 250,
            step(visible, 0.13f));
    }

    private void renderSlogan(DrawContext context, MainMenuConfig menuCfg, float visible) {
        MainMenuConfig.ElementConfig el = menuCfg.getElement("SLOGAN");
        if (!el.visible) return;

        float x = el.getX(width);
        float sloganY = el.getY(height, entries.size());
        float scale = el.scale;

        int sloganBottomCol = dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA
            ? 0xFFDDDDDD : NeonEdge.cool();

        reveal(context, SLOGAN_TOP, x, sloganY, 12.5f * scale, 0.0f, 0xFFFFFF, 210, step(visible, 0.2f));
        float lean = step(visible, 0.26f);
        float baseline = sloganY + 15.0f * scale;
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(0.0f, baseline);
        matrices.mul(new Matrix3x2f(1.0f, 0.0f, -0.17f, 1.0f, 0.0f, 0.0f));
        matrices.translate(0.0f, -baseline);
        reveal(context, SLOGAN_BOTTOM, x, sloganY + 15.0f * scale, 12.5f * scale, 0.0f, sloganBottomCol, 235, lean);
        matrices.popMatrix();
    }

    private void reveal(DrawContext context, String line, float x, float top, float size,
                        float tracking, int colour, int alpha, float progress) {
        if (progress <= 0.001f) {
            return;
        }
        float lineHeight = size * 1.35f;
        if (progress >= 0.999f) {
            spaced(context, line, x, top, size, tracking, withAlpha(colour, alpha));
            return;
        }
        context.enableScissor(Math.round(x - 6.0f), Math.round(top - 2.0f),
            Math.round(x + width), Math.round(top + lineHeight));
        spaced(context, line, x, top + (1.0f - progress) * lineHeight, size, tracking,
            withAlpha(colour, Math.round(alpha * Math.min(1.0f, progress * 1.6f))));
        context.disableScissor();
    }

    private void spaced(DrawContext context, String line, float x, float top, float size,
                        float tracking, int argb) {
        if (titleFont == null) return;
        if (tracking <= 0.0f) {
            MsdfText.draw(context, titleFont, line, x, top, size, argb);
            return;
        }
        float cursor = x;
        for (int i = 0; i < line.length(); i++) {
            String glyph = line.substring(i, i + 1);
            MsdfText.draw(context, titleFont, glyph, cursor, top, size, argb);
            cursor += titleFont.width(glyph, size) + tracking;
        }
    }

    private void renderEntries(DrawContext context, MainMenuConfig menuCfg, int mouseX, int mouseY, double deltaSeconds, float visible) {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            MainMenuConfig.ElementConfig el = menuCfg.getElement(entry.id);
            if (!el.visible) continue;

            float x = el.getX(width);
            float y = el.getY(height, entries.size());
            float scale = el.scale;
            float w = MainMenuConfig.columnWidth(width) * scale;
            float h = 28.0f * scale;

            float appear = step(visible, 0.24f + i * 0.045f);
            if (appear <= 0.001f) {
                continue;
            }

            boolean hovered = !closing && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            entry.hover.step(hovered ? 1.0 : 0.0, deltaSeconds);
            float hover = (float) entry.hover.get();

            float slide = (1.0f - appear) * 34.0f - hover * 6.0f;
            float rowX = x - slide;

            if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
                String label = I18n.get(entry.labelKey);
                dev.strangevisuals.render.VanillaStylePipeline.IconRef iconRef = dev.strangevisuals.render.VanillaStylePipeline.getIconFor(label);
                dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(rowX), Math.round(y), Math.round(w), Math.round(h),
                    label, iconRef, true, hovered);
                continue;
            }

            RoundedRectPipeline.draw(context, rowX, y, w, h,
                withAlpha(0x0E0F14, Math.round((150 + hover * 60) * appear)));
            RoundedRectPipeline.drawOutline(context, rowX, y, w, h,
                withAlpha(mix(0xFFFFFF, NeonEdge.cool(), hover), Math.round((20 + hover * 75) * appear)));
            if (hover > 0.01f) {
                PanelPipeline.draw(context, rowX + 2.0f, y + 4.0f * scale, 1.6f, h - 8.0f * scale,
                    withAlpha(NeonEdge.cool(), Math.round(235 * hover * appear)));
            }

            if (appear > 0.05f && appear < 0.995f) {
                float across = appear * (w + 50.0f) - 25.0f;
                GlowPipeline.lineVertical(context, rowX + across, y + h * 0.5f, 30.0f, h,
                    withAlpha(NeonEdge.cool(), Math.round(90 * (1.0f - appear))));
            }

            if (icons != null) {
                IconPipeline.draw(context, icons, entry.icon, rowX + 17.0f * scale, y + h * 0.5f, 6.2f * scale,
                    withAlpha(mix(0xB9BFCC, 0xFFFFFF, hover), Math.round((205 + hover * 50) * appear)));
            }

            String label = I18n.get(entry.labelKey);
            int labelCol = withAlpha(mix(0xD7DCE6, 0xFFFFFF, hover), Math.round((215 + hover * 40) * appear));
            if (titleFont != null && titleFont.canRender(label)) {
                MsdfText.drawOn(context, titleFont, label, rowX + 30.0f * scale, y + h * 0.5f, 8.0f * scale, labelCol);
            } else {
                context.drawTextWithShadow(textRenderer, label, Math.round(rowX + 30.0f * scale), Math.round(y + (h - 8.0f) * 0.5f), labelCol);
            }
        }
    }

    private void renderPlayerCard(DrawContext context, MainMenuConfig menuCfg, float visible) {
        MainMenuConfig.ElementConfig el = menuCfg.getElement("PLAYER_CARD");
        if (!el.visible) return;

        float x = el.getX(width);
        float top = el.getY(height, entries.size());
        float scale = el.scale;

        float appear = step(visible, 0.42f);
        if (appear <= 0.001f) {
            return;
        }

        float plate = 22.0f * scale;

        if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            context.fill(Math.round(x), Math.round(top), Math.round(x + plate), Math.round(top + plate), 0x88000000);
            Identifier skin = skinTexture();
            int face = Math.round(plate - 4.0f * scale);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 2.0f * scale),
                Math.round(top + 2.0f * scale), 8.0f, 8.0f, face, face, 8, 8, 64, 64, 0xFFFFFFFF);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 2.0f * scale),
                Math.round(top + 2.0f * scale), 40.0f, 8.0f, face, face, 8, 8, 64, 64, 0xFFFFFFFF);

            String name = client != null && client.getSession() != null
                ? client.getSession().getUsername() : "Player";
            context.drawTextWithShadow(textRenderer, name, Math.round(x + plate + 8.0f * scale), Math.round(top + 2.0f * scale), 0xFFFFFFFF);
            String verDate = version() + "  ·  " + TODAY.format(LocalDate.now());
            context.drawTextWithShadow(textRenderer, verDate, Math.round(x + plate + 8.0f * scale), Math.round(top + 12.0f * scale), 0xFFAAAAAA);
            return;
        }

        RoundedRectPipeline.draw(context, x, top, plate, plate,
            withAlpha(0xFFFFFF, Math.round(18 * appear)));
        RoundedRectPipeline.drawOutline(context, x, top, plate, plate,
            withAlpha(0xFFFFFF, Math.round(28 * appear)));

        Identifier skin = skinTexture();
        int alpha = Math.round(255 * appear);
        int face = Math.round(plate - 8.0f * scale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 4.0f * scale),
            Math.round(top + 4.0f * scale), 8.0f, 8.0f, face, face, 8, 8, 64, 64,
            withAlpha(0xFFFFFF, alpha));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 4.0f * scale),
            Math.round(top + 4.0f * scale), 40.0f, 8.0f, face, face, 8, 8, 64, 64,
            withAlpha(0xFFFFFF, alpha));

        String name = client != null && client.getSession() != null
            ? client.getSession().getUsername() : "Player";
        int nameCol = withAlpha(0xFFFFFF, Math.round(240 * appear));
        if (titleFont != null && titleFont.canRender(name)) {
            MsdfText.drawOn(context, titleFont, name, x + plate + 9.0f * scale, top + 8.0f * scale, 8.4f * scale, nameCol);
        } else {
            context.drawTextWithShadow(textRenderer, name, Math.round(x + plate + 9.0f * scale), Math.round(top + 8.0f * scale - 4.0f), nameCol);
        }
        String verDate = version() + "  ·  " + TODAY.format(LocalDate.now());
        int verCol = withAlpha(0xFFFFFF, Math.round(105 * appear));
        if (titleFont != null && titleFont.canRender(verDate)) {
            MsdfText.drawOn(context, titleFont, verDate, x + plate + 9.0f * scale, top + 18.0f * scale, 6.2f * scale, verCol);
        } else {
            context.drawTextWithShadow(textRenderer, verDate, Math.round(x + plate + 9.0f * scale), Math.round(top + 18.0f * scale - 4.0f), verCol);
        }
    }

    private void renderSocialCredits(DrawContext context, int mouseX, int mouseY, double deltaSeconds, float visible) {
        float margin = 16.0f;
        float iconRadius = 9.0f;
        float discordX = width - margin - iconRadius;
        float telegramX = discordX - iconRadius * 2.0f - 8.0f;
        float iconY = height - margin - iconRadius;

        boolean discordHovered = !closing && distance(mouseX, mouseY, discordX, iconY) <= iconRadius;
        boolean telegramHovered = !closing && distance(mouseX, mouseY, telegramX, iconY) <= iconRadius;
        discordHover.step(discordHovered ? 1.0 : 0.0, deltaSeconds);
        telegramHover.step(telegramHovered ? 1.0 : 0.0, deltaSeconds);

        drawSocialIcon(context, discordX, iconY, iconRadius, DISCORD, (float) discordHover.get(), visible);
        drawSocialIcon(context, telegramX, iconY, iconRadius, TELEGRAM, (float) telegramHover.get(), visible);
    }

    private void drawSocialIcon(DrawContext context, float cx, float cy, float radius, int codepoint, float hover, float visible) {
        float scale = 1.0f + hover * 0.12f;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-cx, -cy);
        CirclePipeline.draw(context, cx, cy, radius,
            withAlpha(0x0E0F14, Math.round((150 + hover * 60) * visible)));
        IconPipeline.draw(context, brands, codepoint, cx, cy, radius * 0.9f,
            withAlpha(mix(0xB9BFCC, 0xFFFFFF, hover), Math.round((190 + hover * 65) * visible)));
        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (closing || click.button() != 0) {
            return super.mouseClicked(click, doubleClick);
        }
        float margin = 16.0f;
        float iconRadius = 9.0f;
        float discordX = width - margin - iconRadius;
        float telegramX = discordX - iconRadius * 2.0f - 8.0f;
        float iconY = height - margin - iconRadius;

        if (distance(click.x(), click.y(), discordX, iconY) <= iconRadius) {
            playClick();
            Util.getOperatingSystem().open("https://discord.gg/strangevisuals");
            return true;
        }
        if (distance(click.x(), click.y(), telegramX, iconY) <= iconRadius) {
            playClick();
            Util.getOperatingSystem().open("https://t.me/strangevisuals");
            return true;
        }

        MainMenuConfig menuCfg = MainMenuConfig.get();

        // Check entries
        for (Entry entry : entries) {
            MainMenuConfig.ElementConfig el = menuCfg.getElement(entry.id);
            if (!el.visible) continue;
            float x = el.getX(width);
            float y = el.getY(height, entries.size());
            float w = MainMenuConfig.columnWidth(width) * el.scale;
            float h = 28.0f * el.scale;
            if (click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h) {
                playClick();
                entry.action.run();
                return true;
            }
        }

        // Check player card click (opens Alt Manager)
        MainMenuConfig.ElementConfig cardEl = menuCfg.getElement("PLAYER_CARD");
        if (cardEl.visible) {
            float x = cardEl.getX(width);
            float y = cardEl.getY(height, entries.size());
            float w = 140.0f * cardEl.scale;
            float h = 32.0f * cardEl.scale;
            if (click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h) {
                playClick();
                openAltManager();
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }


    private void openSingleplayer() {
        requestTransition(() -> {
            if (client != null) client.setScreen(new SelectWorldScreen(this));
        });
    }

    private void openMultiplayer() {
        requestTransition(() -> {
            if (client != null) {
                if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.BETA
                        || MainMenuConfig.get().customMultiplayerScreen) {
                    client.setScreen(new CustomMultiplayerScreen(this));
                } else {
                    client.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(this));
                }
            }
        });
    }

    private void openAltManager() {
        requestTransition(() -> {
            if (client != null) client.setScreen(new AltManagerScreen(this));
        });
    }

    private void openSettings() {
        requestTransition(() -> {
            if (client != null) client.setScreen(new OptionsScreen(this, client.options));
        });
    }

    private void openModMenu() {
        requestTransition(() -> {
            if (client != null && FabricLoader.getInstance().isModLoaded("modmenu")) {
                try {
                    Object screen = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen")
                        .getConstructor(Screen.class)
                        .newInstance(this);
                    if (screen instanceof Screen s) {
                        client.setScreen(s);
                    }
                } catch (Exception e) {
                    StrangeVisuals.LOGGER.warn("Failed to open ModMenu screen: {}", e.getMessage());
                }
            }
        });
    }

    private void quit() {
        requestTransition(() -> {
            if (client != null) {
                client.scheduleStop();
            }
        });
    }

    private String version() {
        return FabricLoader.getInstance()
            .getModContainer(StrangeVisuals.MOD_ID)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
    }

    private Identifier skinTexture() {
        if (skin != null) {
            return skin;
        }
        if (client == null || client.getGameProfile() == null) {
            return DefaultSkinHelper.getTexture();
        }

        SkinTextures textures = null;
        try {
            if (skinLookup == null) {
                skinLookup = client.getSkinProvider()
                    .supplySkinTextures(client.getGameProfile(), false);
            }
            textures = skinLookup.get();
        } catch (RuntimeException unavailable) {
            StrangeVisuals.LOGGER.debug("Skin lookup not yet available: {}", unavailable.getMessage());
            skinLookup = null;
        }
        if (textures == null) {
            return DefaultSkinHelper.getSkinTextures(client.getGameProfile()).body().texturePath();
        }
        skin = textures.body().texturePath();
        return skin;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static int withAlpha(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }

    private static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void playClick() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f));
        }
    }
}
