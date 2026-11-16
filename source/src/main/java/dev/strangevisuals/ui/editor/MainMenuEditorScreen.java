package dev.strangevisuals.ui.editor;

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
import dev.strangevisuals.render.VanillaStylePipeline;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import dev.strangevisuals.ui.style.MenuStyle;
import dev.strangevisuals.ui.style.StyleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive in-game Visual Main Menu Editor with 1:1 Parity and real-time live preview.
 * Features procedural shader dropdown selection (6 distinct shaders), photo background selector,
 * multi-layer overlay blending (Photo + Shader), opacity/darkening sliders, element drag & drop,
 * and snap-to-grid controls.
 */
public final class MainMenuEditorScreen extends Screen {
    private static final Identifier LOGO = StrangeVisuals.id("textures/gui/logo.png");
    private static final Identifier TITLE_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier TITLE_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");
    private static final Identifier TEXT_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier ICON_DATA = StrangeVisuals.id("fonts/msdf/wtmico.json");
    private static final Identifier ICON_ATLAS = StrangeVisuals.id("fonts/msdf/wtmico.png");

    private static final float DOT_SPACING = 24.0f;
    private static final float SNAP_THRESHOLD = 8.0f;

    private static final class SnapGuide {
        final boolean vertical;
        final float pos;
        final float min;
        final float max;

        SnapGuide(boolean vertical, float pos, float min, float max) {
            this.vertical = vertical;
            this.pos = pos;
            this.min = min;
            this.max = max;
        }
    }

    private final List<SnapGuide> activeGuides = new ArrayList<>();

    private final Screen parent;
    private GlyphAtlas titleFont;
    private GlyphAtlas textFont;
    private GlyphAtlas icons;

    private MainMenuConfig config;
    private String selectedElementId = null;
    private String draggingElementId = null;
    private boolean resizingElement = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float startScale;
    private double startMouseY;

    // Dropdown popups
    private boolean shaderDropdownOpen = false;
    private boolean photoDropdownOpen = false;
    private boolean screensModalOpen = false;
    private float shaderMenuX, shaderMenuY, shaderMenuW;
    private float photoMenuX, photoMenuY, photoMenuW;
    private float screensMenuX, screensMenuY, screensMenuW;

    private final Spring gridReveal = new Spring(0.0, 700.0, 32.0);
    private long lastFrameTime = System.nanoTime();

    private String toastMessage = null;
    private long toastExpiresAt = 0L;

    public MainMenuEditorScreen(Screen parent) {
        this(parent, false);
    }

    public MainMenuEditorScreen(Screen parent, boolean openScreensModal) {
        super(Text.literal("Main Menu Editor"));
        this.parent = parent;
        this.screensModalOpen = openScreensModal;
        this.config = MainMenuConfig.get();
    }

    @Override
    protected void init() {
        Pipelines.warmUp();
        if (titleFont == null) {
            titleFont = GlyphAtlas.load(TITLE_FONT_DATA, TITLE_FONT_ATLAS);
            textFont = GlyphAtlas.load(TEXT_FONT_DATA, TEXT_FONT_ATLAS);
            icons = GlyphAtlas.load(ICON_DATA, ICON_ATLAS);
        }
        MainMenuConfig.load();
        this.config = MainMenuConfig.get();

        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.OPEN_SOUND, 0.9f, 1.0f));
        }
    }

    public static float getElementBaseWidth(String id, int screenWidth) {
        return switch (id) {
            case "LOGO" -> 160.0f;
            case "SLOGAN" -> 140.0f;
            case "PLAYER_CARD" -> 140.0f;
            default -> MainMenuConfig.columnWidth(screenWidth);
        };
    }

    public static float getElementBaseWidth(String id) {
        return getElementBaseWidth(id, 800);
    }

    public static float getElementBaseHeight(String id) {
        return switch (id) {
            case "LOGO" -> 36.0f;
            case "SLOGAN" -> 28.0f;
            case "PLAYER_CARD" -> 32.0f;
            default -> 28.0f;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrameTime) / 1_000_000_000.0, 0.1);
        lastFrameTime = now;
        gridReveal.step(1.0, dt);

        // 1. Render Active Background in Real-Time 60fps
        renderActiveBackground(context);

        // 2. Render Dotted Grid Overlay
        renderGrid(context, (float) gridReveal.get());

        // 3. Render Elements with Bounding Boxes & Resize Handles
        renderElements(context, mouseX, mouseY);

        // 4. Render Top Toolbar
        renderToolbar(context, mouseX, mouseY);

        // 5. Render Active Dropdowns if open (drawn on top of toolbar & elements)
        if (shaderDropdownOpen) {
            renderShaderDropdown(context, mouseX, mouseY);
        } else if (photoDropdownOpen) {
            renderPhotoDropdown(context, mouseX, mouseY);
        } else if (screensModalOpen) {
            renderScreensModal(context, mouseX, mouseY);
        }

        // 6. Render Bottom Help Hint
        renderHelpBar(context);

        // 7. Toast Notification
        renderToast(context);
    }

    private void renderActiveBackground(DrawContext context) {
        float opened = 1.0f;
        if (config.backgroundMode == MainMenuConfig.BackgroundMode.CUSTOM_IMAGE) {
            Identifier customTex = CustomTextureLoader.getCustomTexture();
            if (customTex != null) {
                int imgAlpha = Math.round(config.imageOpacity * 255);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height, withAlpha(0xFFFFFF, imgAlpha));
            } else {
                BackgroundPipeline.draw(opened, config.shaderPreset.id, config.shaderIntensity, config.imageDarkening, 1.0f);
            }
            int darkAlpha = Math.round(config.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
        } else if (config.backgroundMode == MainMenuConfig.BackgroundMode.IMAGE_AND_SHADER) {
            Identifier customTex = CustomTextureLoader.getCustomTexture();
            if (customTex != null) {
                int imgAlpha = Math.round(config.imageOpacity * 255);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height, withAlpha(0xFFFFFF, imgAlpha));
            }
            int darkAlpha = Math.round(config.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
            BackgroundPipeline.draw(opened, config.shaderPreset.id, config.shaderIntensity, 0.0f, config.shaderOpacity);
        } else if (config.backgroundMode == MainMenuConfig.BackgroundMode.PANORAMA) {
            dev.strangevisuals.render.PanoramaManager.render(context, dev.strangevisuals.render.PanoramaManager.getProceduralMainMenuType(), width, height, 0.0f);
            int darkAlpha = Math.round(config.imageDarkening * 255);
            if (darkAlpha > 0) {
                context.fill(0, 0, width, height, (darkAlpha << 24));
            }
        } else {
            BackgroundPipeline.draw(opened, config.shaderPreset.id, config.shaderIntensity, config.imageDarkening, 1.0f);
        }

        if (config.shaderOverlayEnabled && config.shaderEffect != MainMenuConfig.ShaderEffect.NONE) {
            int glowAlpha = Math.round(config.shaderIntensity * 45);
            GlowPipeline.radial(context, width * 0.5f, height * 0.5f, width * 0.45f, withAlpha(NeonEdge.cool(), glowAlpha));
        }
    }

    private void renderGrid(DrawContext context, float alpha) {
        if (alpha <= 0.01f) return;
        int color = withAlpha(0x3A4458, Math.round(38 * alpha));
        for (float x = DOT_SPACING; x < width; x += DOT_SPACING) {
            for (float y = DOT_SPACING; y < height; y += DOT_SPACING) {
                CirclePipeline.draw(context, x, y, 1.0f, color);
            }
        }
        int crosshairColor = withAlpha(NeonEdge.cool(), Math.round(28 * alpha));
        PanelPipeline.draw(context, width * 0.5f - 0.5f, 0, 1.0f, height, crosshairColor);
        PanelPipeline.draw(context, 0, height * 0.5f - 0.5f, width, 1.0f, crosshairColor);
    }

    private void renderElements(DrawContext context, int mouseX, int mouseY) {
        List<String> elementIds = new ArrayList<>(List.of("LOGO", "SLOGAN", "SINGLEPLAYER", "MULTIPLAYER", "SETTINGS"));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            elementIds.add("MODS");
        }
        elementIds.add("QUIT");
        elementIds.add("PLAYER_CARD");

        int numButtons = FabricLoader.getInstance().isModLoaded("modmenu") ? 5 : 4;

        for (String id : elementIds) {
            MainMenuConfig.ElementConfig el = config.getElement(id);
            if (!el.visible) continue;

            float elW = getElementBaseWidth(id, width) * el.scale;
            float elH = getElementBaseHeight(id) * el.scale;
            float elX = el.getX(width);
            float elY = el.getY(height, numButtons);

            boolean isHovered = !shaderDropdownOpen && !photoDropdownOpen && !screensModalOpen && mouseX >= elX && mouseX <= elX + elW && mouseY >= elY && mouseY <= elY + elH;
            boolean isSelected = id.equals(selectedElementId);

            // Draw Element Content
            drawElementContent(context, id, elX, elY, elW, elH, el.scale);

            // Selection / Hover Outlines
            if (isSelected) {
                RoundedRectPipeline.drawOutline(context, elX - 2.0f, elY - 2.0f, elW + 4.0f, elH + 4.0f, NeonEdge.cool());

                // Resize Handle
                float handleSize = 8.0f;
                float hx = elX + elW - handleSize * 0.5f;
                float hy = elY + elH - handleSize * 0.5f;
                RoundedRectPipeline.draw(context, hx, hy, handleSize, handleSize, 0xFFFFFFFF);
                RoundedRectPipeline.drawOutline(context, hx, hy, handleSize, handleSize, NeonEdge.cool());

                // Scale badge
                String badge = String.format(java.util.Locale.ROOT, "%.1fx", el.scale);
                if (titleFont != null && titleFont.canRender(badge)) {
                    MsdfText.drawOn(context, titleFont, badge, elX + 2.0f, elY - 7.0f, 6.5f, NeonEdge.cool());
                } else {
                    context.drawTextWithShadow(textRenderer, badge, Math.round(elX + 2.0f), Math.round(elY - 11.0f), NeonEdge.cool());
                }
            } else if (isHovered) {
                RoundedRectPipeline.drawOutline(context, elX - 1.0f, elY - 1.0f, elW + 2.0f, elH + 2.0f, withAlpha(Theme.accent(), 102));
            }
        }

        // Render Alignment Guide Lines
        for (SnapGuide g : activeGuides) {
            int guideCol = withAlpha(Theme.accent(), 190);
            if (g.vertical) {
                PanelPipeline.draw(context, g.pos - 0.5f, g.min, 1.0f, Math.max(8.0f, g.max - g.min), guideCol);
            } else {
                PanelPipeline.draw(context, g.min, g.pos - 0.5f, Math.max(8.0f, g.max - g.min), 1.0f, guideCol);
            }
        }
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

    private void drawElementContent(DrawContext context, String id, float x, float y, float w, float h, float scale) {
        switch (id) {
            case "LOGO" -> {
                float logoSize = 34.0f * scale;
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(x + logoSize * 0.5f, y + logoSize * 0.5f);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO,
                    Math.round(-logoSize * 0.5f), Math.round(-logoSize * 0.5f), 0.0f, 0.0f,
                    Math.round(logoSize), Math.round(logoSize), 120, 120, 120, 120, 0xFFFFFFFF);
                context.getMatrices().popMatrix();

                float textX = x + logoSize + 12.0f * scale;
                spaced(context, "STRANGE", textX, y + 9.0f * scale, 11.0f * scale, 1.4f * scale, 0xFFFFFFFF);
                spaced(context, "VISUALS", textX, y + 23.0f * scale, 11.0f * scale, 1.4f * scale, withAlpha(NeonEdge.cool(), 255));
            }
            case "SLOGAN" -> {
                spaced(context, "Free to use,", x, y, 12.5f * scale, 0.0f, 0xFFFFFFFF);
                float baseline = y + 15.0f * scale;
                org.joml.Matrix3x2fStack matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(0.0f, baseline);
                matrices.mul(new org.joml.Matrix3x2f(1.0f, 0.0f, -0.17f, 1.0f, 0.0f, 0.0f));
                matrices.translate(0.0f, -baseline);
                spaced(context, "Yours to shape.", x, y + 15.0f * scale, 12.5f * scale, 0.0f, withAlpha(NeonEdge.cool(), 255));
                matrices.popMatrix();
            }
            case "SINGLEPLAYER" -> drawButtonPreview(context, x, y, w, h, "strangevisuals.menu.singleplayer", 'P', scale);
            case "MULTIPLAYER" -> drawButtonPreview(context, x, y, w, h, "strangevisuals.menu.multiplayer", 'N', scale);
            case "SETTINGS" -> drawButtonPreview(context, x, y, w, h, "strangevisuals.menu.settings", 'O', scale);
            case "MODS" -> drawButtonPreview(context, x, y, w, h, "strangevisuals.menu.mods", 'K', scale);
            case "QUIT" -> drawButtonPreview(context, x, y, w, h, "strangevisuals.menu.quit", 'Q', scale);
            case "PLAYER_CARD" -> {
                float plate = 22.0f * scale;
                RoundedRectPipeline.draw(context, x, y, plate, plate, withAlpha(0xFFFFFF, 18));
                RoundedRectPipeline.drawOutline(context, x, y, plate, plate, withAlpha(0xFFFFFF, 28));

                Identifier skin = DefaultSkinHelper.getTexture();
                if (client != null && client.getGameProfile() != null) {
                    skin = DefaultSkinHelper.getSkinTextures(client.getGameProfile()).body().texturePath();
                }
                int face = Math.round(plate - 8.0f * scale);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 4.0f * scale),
                    Math.round(y + 4.0f * scale), 8.0f, 8.0f, face, face, 8, 8, 64, 64, 0xFFFFFFFF);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, Math.round(x + 4.0f * scale),
                    Math.round(y + 4.0f * scale), 40.0f, 8.0f, face, face, 8, 8, 64, 64, 0xFFFFFFFF);

                String name = MinecraftClient.getInstance().getSession() != null
                    ? MinecraftClient.getInstance().getSession().getUsername() : "Player";
                if (titleFont != null && titleFont.canRender(name)) {
                    MsdfText.drawOn(context, titleFont, name, x + plate + 9.0f * scale, y + 8.0f * scale, 8.4f * scale, 0xFFFFFFFF);
                } else {
                    context.drawTextWithShadow(textRenderer, name, Math.round(x + plate + 9.0f * scale), Math.round(y + 4.0f * scale), 0xFFFFFFFF);
                }
                if (titleFont != null) {
                    MsdfText.drawOn(context, titleFont, "v1.0  ·  Today", x + plate + 9.0f * scale, y + 18.0f * scale, 6.2f * scale, withAlpha(0xFFFFFF, 105));
                }
            }
        }
    }

    private void drawButtonPreview(DrawContext context, float x, float y, float w, float h, String key, int iconGlyph, float scale) {
        String label = I18n.get(key);
        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            VanillaStylePipeline.renderButton(context, Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                label, VanillaStylePipeline.getIconFor(label), true, false);
            return;
        }
        RoundedRectPipeline.draw(context, x, y, w, h, withAlpha(0x0E0F14, 150));
        RoundedRectPipeline.drawOutline(context, x, y, w, h, withAlpha(0xFFFFFF, 20));
        if (icons != null) {
            IconPipeline.draw(context, icons, iconGlyph, x + 17.0f * scale, y + h * 0.5f, 6.2f * scale, withAlpha(0xB9BFCC, 205));
        }
        if (titleFont != null && titleFont.canRender(label)) {
            MsdfText.drawOn(context, titleFont, label, x + 30.0f * scale, y + h * 0.5f, 8.0f * scale, withAlpha(0xD7DCE6, 215));
        } else {
            context.drawTextWithShadow(textRenderer, label, Math.round(x + 30.0f * scale), Math.round(y + (h - 8.0f) * 0.5f), 0xFFD7DCE6);
        }
    }

    private void autoAlignColumn() {
        float colX = MainMenuConfig.columnX(width);
        float brandTop = MainMenuConfig.getDefaultY("LOGO", height, 5);
        config.getElement("LOGO").setPos(colX, brandTop, width, height);

        float sloganTop = MainMenuConfig.getDefaultY("SLOGAN", height, 5);
        config.getElement("SLOGAN").setPos(colX, sloganTop, width, height);

        float curY = brandTop + MainMenuConfig.BRAND_TOTAL_H + MainMenuConfig.BRAND_GAP;
        List<String> btns = new ArrayList<>(List.of("SINGLEPLAYER", "MULTIPLAYER", "SETTINGS"));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            btns.add("MODS");
        }
        btns.add("QUIT");

        for (String b : btns) {
            config.getElement(b).setPos(colX, curY, width, height);
            curY += MainMenuConfig.ROW_H + MainMenuConfig.ROW_GAP;
        }

        config.getElement("PLAYER_CARD").setPos(colX, curY + MainMenuConfig.IDENTITY_GAP, width, height);
        MainMenuConfig.save();
        showToast("✓ Column aligned");
    }

    private void renderToolbar(DrawContext context, int mouseX, int mouseY) {
        float barW = Math.min(width - 20.0f, 740.0f);
        float barH = 40.0f;
        float barX = (width - barW) * 0.5f;
        float barY = 8.0f;

        // Premium frosted acrylic obsidian glass
        RoundedRectPipeline.draw(context, barX, barY, barW, barH, 0xF60B0E14);
        RoundedRectPipeline.drawOutline(context, barX, barY, barW, barH, withAlpha(Theme.accent(), 60));
        PanelPipeline.draw(context, barX + 16.0f, barY + 1.2f, barW - 32.0f, 1.2f, withAlpha(Theme.accent(), 160));

        float btnY = barY + 8.0f;
        float btnH = 24.0f;
        float curX = barX + 8.0f;

        // 1. Presets Selector: Beta & Vanilla
        float betaW = 38.0f;
        drawToolBtn(context, "Beta", curX, btnY, betaW, btnH, mouseX, mouseY, StyleManager.getStyle() == MenuStyle.BETA ? NeonEdge.cool() : 0xFF2C3242);
        curX += betaW + 4.0f;

        float vanW = 46.0f;
        drawToolBtn(context, "Vanilla", curX, btnY, vanW, btnH, mouseX, mouseY, StyleManager.getStyle() == MenuStyle.VANILLA ? 0xFF55FF55 : 0xFF2C3242);
        curX += vanW + 4.0f;

        // 2. Background Mode Toggle: [Shader] / [Photo] / [Combo]
        String modeStr = switch (config.backgroundMode) {
            case SHADER -> "Mode: Shader";
            case CUSTOM_IMAGE -> "Mode: Photo";
            case IMAGE_AND_SHADER -> "Mode: Both";
            case PANORAMA -> "Mode: 3D Panorama";
        };
        float modeW = 96.0f;
        drawToolBtn(context, modeStr, curX, btnY, modeW, btnH, mouseX, mouseY, NeonEdge.cool());
        curX += modeW + 4.0f;

        // 3. Shader Preset Dropdown Button
        String shaderBtnText = "▼ " + config.shaderPreset.defaultName;
        shaderMenuX = curX;
        shaderMenuY = btnY + btnH + 4.0f;
        shaderMenuW = 108.0f;
        drawToolBtn(context, shaderBtnText, curX, btnY, shaderMenuW, btnH, mouseX, mouseY, shaderDropdownOpen ? NeonEdge.cool() : 0xFF2C3242);
        curX += shaderMenuW + 4.0f;

        // 4. Photo Selector Dropdown Button
        String currentImgName = config.customImagePath.isEmpty() ? "Default" : Path.of(config.customImagePath).getFileName().toString();
        if (currentImgName.length() > 8) currentImgName = currentImgName.substring(0, 7) + "…";
        String photoBtnText = "▼ " + currentImgName;
        photoMenuX = curX;
        photoMenuY = btnY + btnH + 4.0f;
        photoMenuW = 92.0f;
        drawToolBtn(context, photoBtnText, curX, btnY, photoMenuW, btnH, mouseX, mouseY, photoDropdownOpen ? NeonEdge.cool() : 0xFF2C3242);
        curX += photoMenuW + 4.0f;

        // 5. Open Folder Button
        float fldW = 24.0f;
        drawToolBtn(context, "📂", curX, btnY, fldW, btnH, mouseX, mouseY);
        curX += fldW + 4.0f;

        // 6. Darkening Adjuster
        String darkText = String.format(java.util.Locale.ROOT, "◐ %d%%", Math.round(config.imageDarkening * 100));
        float darkW = 50.0f;
        drawToolBtn(context, darkText, curX, btnY, darkW, btnH, mouseX, mouseY);
        curX += darkW + 4.0f;

        // 7. Shader Opacity Adjuster (in Combo or Shader mode)
        String opText = String.format(java.util.Locale.ROOT, "✨ %d%%", Math.round(config.shaderOpacity * 100));
        float opW = 50.0f;
        drawToolBtn(context, opText, curX, btnY, opW, btnH, mouseX, mouseY);
        curX += opW + 4.0f;

        // 8. Auto-Align Column Button
        float alignW = 50.0f;
        drawToolBtn(context, "☵ Align", curX, btnY, alignW, btnH, mouseX, mouseY);
        curX += alignW + 4.0f;

        // 9. Screens / Themes Modal Toggle Button
        float screensW = 66.0f;
        screensMenuX = curX;
        screensMenuY = btnY + btnH + 4.0f;
        screensMenuW = 230.0f;
        drawToolBtn(context, "⚙ Screens", curX, btnY, screensW, btnH, mouseX, mouseY, screensModalOpen ? NeonEdge.cool() : 0xFF2C3242);
        curX += screensW + 4.0f;

        // 10. Reset Button
        float rstW = 48.0f;
        drawToolBtn(context, I18n.get("editor.reset"), curX, btnY, rstW, btnH, mouseX, mouseY);

        // 11. Save & Exit Button (right aligned)
        float saveW = 58.0f;
        drawToolBtn(context, I18n.get("editor.save"), barX + barW - saveW - 8.0f, btnY, saveW, btnH, mouseX, mouseY, 0xFF34D399);
    }

    private void renderShaderDropdown(DrawContext context, int mouseX, int mouseY) {
        MainMenuConfig.ShaderPreset[] presets = MainMenuConfig.ShaderPreset.values();
        float itemH = 17.0f;
        float totalH = presets.length * itemH + 6.0f;
        float drawY = shaderMenuY;
        if (drawY + totalH > this.height - 4.0f) {
            drawY = Math.max(4.0f, this.height - totalH - 4.0f);
        }

        RoundedRectPipeline.draw(context, shaderMenuX, drawY, shaderMenuW, totalH, 0xF80B0D14);
        RoundedRectPipeline.drawOutline(context, shaderMenuX, drawY, shaderMenuW, totalH, NeonEdge.cool());

        for (int i = 0; i < presets.length; i++) {
            MainMenuConfig.ShaderPreset preset = presets[i];
            float itemY = drawY + 3.0f + i * itemH;
            boolean hov = mouseX >= shaderMenuX && mouseX <= shaderMenuX + shaderMenuW && mouseY >= itemY && mouseY <= itemY + itemH;
            boolean active = config.shaderPreset == preset;

            if (active || hov) {
                RoundedRectPipeline.draw(context, shaderMenuX + 2.0f, itemY, shaderMenuW - 4.0f, itemH, hov ? 0xEE1E2638 : 0xDD121722);
            }
            if (active) {
                RoundedRectPipeline.draw(context, shaderMenuX + 3.0f, itemY + 2.0f, 2.0f, itemH - 4.0f, NeonEdge.cool());
            }

            int textCol = active ? NeonEdge.cool() : (hov ? 0xFFFFFFFF : 0xFFCCD5E4);
            if (titleFont != null && titleFont.canRender(preset.defaultName)) {
                MsdfText.drawOn(context, titleFont, preset.defaultName, shaderMenuX + 8.0f, itemY + itemH * 0.5f, 6.8f, textCol);
            } else {
                context.drawTextWithShadow(textRenderer, preset.defaultName, Math.round(shaderMenuX + 8.0f), Math.round(itemY + 4.5f), textCol);
            }
        }
    }

    private void renderPhotoDropdown(DrawContext context, int mouseX, int mouseY) {
        List<String> images = CustomTextureLoader.getAvailableImages();
        float itemH = 22.0f;
        float totalH = Math.max(1, images.size()) * itemH + 6.0f;

        RoundedRectPipeline.draw(context, photoMenuX, photoMenuY, photoMenuW, totalH, 0xF80B0D14);
        RoundedRectPipeline.drawOutline(context, photoMenuX, photoMenuY, photoMenuW, totalH, NeonEdge.cool());

        if (images.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No photos found", Math.round(photoMenuX + 8.0f), Math.round(photoMenuY + 9.0f), 0xFF8892A4);
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            String imgName = images.get(i);
            float itemY = photoMenuY + 3.0f + i * itemH;
            boolean hov = mouseX >= photoMenuX && mouseX <= photoMenuX + photoMenuW && mouseY >= itemY && mouseY <= itemY + itemH;
            boolean active = config.customImagePath.endsWith(imgName);

            if (active || hov) {
                RoundedRectPipeline.draw(context, photoMenuX + 2.0f, itemY, photoMenuW - 4.0f, itemH, hov ? 0xEE1E2638 : 0xDD121722);
            }
            if (active) {
                RoundedRectPipeline.draw(context, photoMenuX + 3.0f, itemY + 3.0f, 2.0f, itemH - 6.0f, NeonEdge.cool());
            }

            String display = imgName.length() > 14 ? imgName.substring(0, 13) + "…" : imgName;
            int textCol = active ? NeonEdge.cool() : (hov ? 0xFFFFFFFF : 0xFFCCD5E4);
            if (titleFont != null && titleFont.canRender(display)) {
                MsdfText.drawOn(context, titleFont, display, photoMenuX + 8.0f, itemY + itemH * 0.5f, 7.2f, textCol);
            } else {
                context.drawTextWithShadow(textRenderer, display, Math.round(photoMenuX + 8.0f), Math.round(itemY + 6.5f), textCol);
            }
        }
    }

    private void renderScreensModal(DrawContext context, int mouseX, int mouseY) {
        float modalX = Math.max(8.0f, Math.min(width - screensMenuW - 8.0f, screensMenuX - 40.0f));
        float modalY = screensMenuY;
        float modalH = 242.0f;

        // Card background
        RoundedRectPipeline.draw(context, modalX, modalY, screensMenuW, modalH, 0xF80B0D14);
        RoundedRectPipeline.drawOutline(context, modalX, modalY, screensMenuW, modalH, Theme.accent());
        PanelPipeline.draw(context, modalX + 12.0f, modalY + 1.2f, screensMenuW - 24.0f, 1.2f, Theme.accent());

        // Title
        String title = I18n.get("editor.screens_modal");
        if (titleFont != null && titleFont.canRender(title)) {
            MsdfText.drawCentered(context, titleFont, title, modalX + screensMenuW * 0.5f, modalY + 13.0f, 7.5f, 0xFFFFFFFF);
        } else {
            context.drawTextWithShadow(textRenderer, title, Math.round(modalX + (screensMenuW - textRenderer.getWidth(title)) * 0.5f), Math.round(modalY + 8.0f), 0xFFFFFFFF);
        }

        // 7 Toggle Rows
        drawScreenToggleRow(context, modalX, modalY + 26.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_loading"), config.customLoadingScreen, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 56.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_multiplayer"), config.customMultiplayerScreen, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 86.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_settings"), config.customSettingsScreen, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 116.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_pause"), config.customPauseScreen, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 146.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_title"), !config.useVanillaTitleScreen, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 176.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_hotbar"), config.customHotbar, mouseX, mouseY);
        drawScreenToggleRow(context, modalX, modalY + 206.0f, screensMenuW, 26.0f,
            I18n.get("editor.screen_status_effects"), config.customStatusEffects, mouseX, mouseY);
    }

    private void drawScreenToggleRow(DrawContext context, float x, float y, float w, float h, String label, boolean state, int mouseX, int mouseY) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (hov) {
            RoundedRectPipeline.draw(context, x + 4.0f, y, w - 8.0f, h, 0x18FFFFFF);
        }

        int labelCol = hov ? 0xFFFFFFFF : 0xC0C6D4;
        if (titleFont != null && titleFont.canRender(label)) {
            MsdfText.drawOn(context, titleFont, label, x + 10.0f, y + h * 0.5f, 6.8f, labelCol);
        } else {
            context.drawTextWithShadow(textRenderer, label, Math.round(x + 10.0f), Math.round(y + (h - 8.0f) * 0.5f), labelCol);
        }

        // Switch pill
        float pillW = 34.0f;
        float pillH = 15.0f;
        float pillX = x + w - pillW - 10.0f;
        float pillY = y + (h - pillH) * 0.5f;

        if (state) {
            RoundedRectPipeline.draw(context, pillX, pillY, pillW, pillH, Theme.accent());
            CirclePipeline.draw(context, pillX + pillW - 8.0f, pillY + pillH * 0.5f, 5.0f, 0xFFFFFFFF);
            if (titleFont != null) {
                MsdfText.drawOn(context, titleFont, "ON", pillX + 5.0f, pillY + pillH * 0.5f, 5.5f, 0xFF000000);
            }
        } else {
            RoundedRectPipeline.draw(context, pillX, pillY, pillW, pillH, 0xFF202636);
            RoundedRectPipeline.drawOutline(context, pillX, pillY, pillW, pillH, 0xFF353E54);
            CirclePipeline.draw(context, pillX + 8.0f, pillY + pillH * 0.5f, 5.0f, 0xFF6B7280);
            if (titleFont != null) {
                MsdfText.drawOn(context, titleFont, "OFF", pillX + 15.0f, pillY + pillH * 0.5f, 5.5f, 0xFF9CA3AF);
            }
        }
    }

    private void drawToolBtn(DrawContext context, String label, float x, float y, float w, float h, int mouseX, int mouseY) {
        drawToolBtn(context, label, x, y, w, h, mouseX, mouseY, 0xFF2C3242);
    }

    private void drawToolBtn(DrawContext context, String label, float x, float y, float w, float h, int mouseX, int mouseY, int borderColor) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        RoundedRectPipeline.draw(context, x, y, w, h, hov ? 0xEE1E2432 : 0xDD13161F);
        RoundedRectPipeline.drawOutline(context, x, y, w, h, hov ? NeonEdge.cool() : borderColor);
        int textColor = hov ? 0xFFFFFFFF : 0xFFE4EBF5;
        if (titleFont != null && titleFont.canRender(label)) {
            MsdfText.drawCenteredOn(context, titleFont, label, x + w * 0.5f, y + h * 0.5f, 7.5f, textColor);
        } else {
            int textY = Math.round(y + (h - 8.0f) * 0.5f);
            context.drawCenteredTextWithShadow(textRenderer, label, Math.round(x + w * 0.5f), textY, textColor);
        }
    }

    private void renderHelpBar(DrawContext context) {
        float hbarW = 420.0f;
        float hbarH = 20.0f;
        float hbarX = (width - hbarW) * 0.5f;
        float hbarY = height - 26.0f;

        RoundedRectPipeline.draw(context, hbarX, hbarY, hbarW, hbarH, 0xD00A0C10);
        RoundedRectPipeline.drawOutline(context, hbarX, hbarY, hbarW, hbarH, 0xFF1D212B);
        String hint = I18n.get("editor.drag_hint");
        if (titleFont != null && titleFont.canRender(hint)) {
            MsdfText.drawCenteredOn(context, titleFont, hint, hbarX + hbarW * 0.5f, hbarY + hbarH * 0.5f, 7.0f, 0xFF8892A4);
        } else {
            int textY = Math.round(hbarY + (hbarH - 8.0f) * 0.5f);
            context.drawCenteredTextWithShadow(textRenderer, hint, Math.round(hbarX + hbarW * 0.5f), textY, 0xFF8892A4);
        }
    }

    private void renderToast(DrawContext context) {
        if (toastMessage == null || Util.getMeasuringTimeMs() > toastExpiresAt) {
            toastMessage = null;
            return;
        }
        int tw = textRenderer.getWidth(toastMessage) + 24;
        int th = 20;
        int tx = (width - tw) / 2;
        int ty = 54;
        RoundedRectPipeline.draw(context, tx, ty, tw, th, 0xEE162E20);
        RoundedRectPipeline.drawOutline(context, tx, ty, tw, th, 0xFF34D399);
        if (titleFont != null && titleFont.canRender(toastMessage)) {
            MsdfText.drawCenteredOn(context, titleFont, toastMessage, tx + tw * 0.5f, ty + th * 0.5f, 7.5f, 0xFFFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, toastMessage, tx + tw / 2, ty + 6, 0xFFFFFFFF);
        }
    }

    private void showToast(String msg) {
        this.toastMessage = msg;
        this.toastExpiresAt = Util.getMeasuringTimeMs() + 2200L;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        // Handle open dropdown clicks first
        if (shaderDropdownOpen) {
            MainMenuConfig.ShaderPreset[] presets = MainMenuConfig.ShaderPreset.values();
            float itemH = 17.0f;
            float totalH = presets.length * itemH + 6.0f;
            float drawY = shaderMenuY;
            if (drawY + totalH > this.height - 4.0f) {
                drawY = Math.max(4.0f, this.height - totalH - 4.0f);
            }
            if (inRect(click.x(), click.y(), shaderMenuX, drawY, shaderMenuW, totalH)) {
                for (int i = 0; i < presets.length; i++) {
                    float itemY = drawY + 3.0f + i * itemH;
                    if (click.y() >= itemY && click.y() <= itemY + itemH) {
                        playClick();
                        config.shaderPreset = presets[i];
                        MainMenuConfig.save();
                        shaderDropdownOpen = false;
                        showToast("✓ Shader: " + presets[i].defaultName);
                        return true;
                    }
                }
            }
            shaderDropdownOpen = false;
            return true;
        }

        if (photoDropdownOpen) {
            List<String> images = CustomTextureLoader.getAvailableImages();
            float itemH = 22.0f;
            float totalH = Math.max(1, images.size()) * itemH + 6.0f;
            if (inRect(click.x(), click.y(), photoMenuX, photoMenuY, photoMenuW, totalH)) {
                for (int i = 0; i < images.size(); i++) {
                    float itemY = photoMenuY + 3.0f + i * itemH;
                    if (click.y() >= itemY && click.y() <= itemY + itemH) {
                        playClick();
                        config.customImagePath = images.get(i);
                        CustomTextureLoader.forceReload();
                        MainMenuConfig.save();
                        photoDropdownOpen = false;
                        showToast("✓ Photo: " + images.get(i));
                        return true;
                    }
                }
            }
            photoDropdownOpen = false;
            return true;
        }

        if (screensModalOpen) {
            float modalX = Math.max(8.0f, Math.min(width - screensMenuW - 8.0f, screensMenuX - 40.0f));
            float modalY = screensMenuY;
            float modalH = 242.0f;

            if (inRect(click.x(), click.y(), modalX, modalY, screensMenuW, modalH)) {
                if (click.y() >= modalY + 26.0f && click.y() <= modalY + 52.0f) {
                    playClick();
                    config.customLoadingScreen = !config.customLoadingScreen;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Loading screens: " + (config.customLoadingScreen ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 56.0f && click.y() <= modalY + 82.0f) {
                    playClick();
                    config.customMultiplayerScreen = !config.customMultiplayerScreen;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Server list: " + (config.customMultiplayerScreen ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 86.0f && click.y() <= modalY + 112.0f) {
                    playClick();
                    config.customSettingsScreen = !config.customSettingsScreen;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Settings theme: " + (config.customSettingsScreen ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 116.0f && click.y() <= modalY + 142.0f) {
                    playClick();
                    config.customPauseScreen = !config.customPauseScreen;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Pause theme: " + (config.customPauseScreen ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 146.0f && click.y() <= modalY + 172.0f) {
                    playClick();
                    config.useVanillaTitleScreen = !config.useVanillaTitleScreen;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Custom Main Menu: " + (!config.useVanillaTitleScreen ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 176.0f && click.y() <= modalY + 202.0f) {
                    playClick();
                    config.customHotbar = !config.customHotbar;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Custom Hotbar: " + (config.customHotbar ? "ON" : "OFF"));
                    return true;
                }
                if (click.y() >= modalY + 206.0f && click.y() <= modalY + 232.0f) {
                    playClick();
                    config.customStatusEffects = !config.customStatusEffects;
                    MainMenuConfig.save();
                    dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
                    showToast("Status Effects HUD: " + (config.customStatusEffects ? "ON" : "OFF"));
                    return true;
                }
                return true;
            }
            screensModalOpen = false;
            return true;
        }

        float barW = Math.min(width - 20.0f, 740.0f);
        float barH = 40.0f;
        float barX = (width - barW) * 0.5f;
        float barY = 8.0f;
        float btnY = barY + 8.0f;
        float btnH = 24.0f;
        float curX = barX + 8.0f;

        // 1. Beta & Vanilla Presets
        float betaW = 38.0f;
        if (inRect(click.x(), click.y(), curX, btnY, betaW, btnH)) {
            playClick();
            config.applyPreset(MenuStyle.BETA);
            StyleManager.setStyle(MenuStyle.BETA);
            MainMenuConfig.save();
            showToast("✓ Preset: Beta");
            return true;
        }
        curX += betaW + 4.0f;

        float vanW = 46.0f;
        if (inRect(click.x(), click.y(), curX, btnY, vanW, btnH)) {
            playClick();
            config.applyPreset(MenuStyle.VANILLA);
            StyleManager.setStyle(MenuStyle.VANILLA);
            MainMenuConfig.save();
            showToast("✓ Preset: Vanilla");
            return true;
        }
        curX += vanW + 4.0f;

        // 2. Mode Toggle
        float modeW = 82.0f;
        if (inRect(click.x(), click.y(), curX, btnY, modeW, btnH)) {
            playClick();
            config.backgroundMode = switch (config.backgroundMode) {
                case SHADER -> MainMenuConfig.BackgroundMode.CUSTOM_IMAGE;
                case CUSTOM_IMAGE -> MainMenuConfig.BackgroundMode.IMAGE_AND_SHADER;
                case IMAGE_AND_SHADER -> MainMenuConfig.BackgroundMode.PANORAMA;
                case PANORAMA -> MainMenuConfig.BackgroundMode.SHADER;
            };
            MainMenuConfig.save();
            return true;
        }
        curX += modeW + 4.0f;

        // 3. Shader Selector Dropdown
        if (inRect(click.x(), click.y(), curX, btnY, shaderMenuW, btnH)) {
            playClick();
            shaderDropdownOpen = !shaderDropdownOpen;
            photoDropdownOpen = false;
            screensModalOpen = false;
            return true;
        }
        curX += shaderMenuW + 4.0f;

        // 4. Photo Selector Dropdown
        if (inRect(click.x(), click.y(), curX, btnY, photoMenuW, btnH)) {
            playClick();
            photoDropdownOpen = !photoDropdownOpen;
            shaderDropdownOpen = false;
            screensModalOpen = false;
            return true;
        }
        curX += photoMenuW + 4.0f;

        // 5. Open Folder
        float fldW = 24.0f;
        if (inRect(click.x(), click.y(), curX, btnY, fldW, btnH)) {
            playClick();
            Path dir = CustomTextureLoader.getDefaultCustomImageDirectory();
            Util.getOperatingSystem().open(dir.toFile());
            return true;
        }
        curX += fldW + 4.0f;

        // 6. Darkening
        float darkW = 50.0f;
        if (inRect(click.x(), click.y(), curX, btnY, darkW, btnH)) {
            playClick();
            config.imageDarkening = (config.imageDarkening + 0.15f) % 0.90f;
            MainMenuConfig.save();
            return true;
        }
        curX += darkW + 4.0f;

        // 7. Shader Opacity
        float opW = 50.0f;
        if (inRect(click.x(), click.y(), curX, btnY, opW, btnH)) {
            playClick();
            config.shaderOpacity = (config.shaderOpacity + 0.15f);
            if (config.shaderOpacity > 1.0f) config.shaderOpacity = 0.25f;
            MainMenuConfig.save();
            return true;
        }
        curX += opW + 4.0f;

        // 8. Align Column
        float alignW = 50.0f;
        if (inRect(click.x(), click.y(), curX, btnY, alignW, btnH)) {
            playClick();
            autoAlignColumn();
            return true;
        }
        curX += alignW + 4.0f;

        // 9. Screens / Themes Modal Toggle Button
        float screensW = 66.0f;
        if (inRect(click.x(), click.y(), curX, btnY, screensW, btnH)) {
            playClick();
            screensModalOpen = !screensModalOpen;
            shaderDropdownOpen = false;
            photoDropdownOpen = false;
            return true;
        }
        curX += screensW + 4.0f;

        // 10. Reset
        float rstW = 48.0f;
        if (inRect(click.x(), click.y(), curX, btnY, rstW, btnH)) {
            playClick();
            config.applyPreset(StyleManager.getStyle());
            MainMenuConfig.save();
            showToast("✓ Reset to default");
            return true;
        }

        // 11. Save & Exit
        float saveW = 58.0f;
        if (inRect(click.x(), click.y(), barX + barW - saveW - 8.0f, btnY, saveW, btnH)) {
            playSaveSound();
            MainMenuConfig.save();
            showToast("✓ Saved successfully!");
            close();
            return true;
        }

        // Element selection & dragging (in reverse render order)
        List<String> elementIds = new ArrayList<>(List.of("PLAYER_CARD", "QUIT"));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            elementIds.add("MODS");
        }
        elementIds.addAll(List.of("SETTINGS", "MULTIPLAYER", "SINGLEPLAYER", "SLOGAN", "LOGO"));

        for (String id : elementIds) {
            MainMenuConfig.ElementConfig el = config.getElement(id);
            if (!el.visible) continue;
            float elW = getElementBaseWidth(id, width) * el.scale;
            float elH = getElementBaseHeight(id) * el.scale;
            float elX = el.getX(width);
            float elY = el.getY(height, 5);

            // Check corner resize handle
            if (id.equals(selectedElementId)) {
                float handleSize = 12.0f;
                float hx = elX + elW - handleSize * 0.5f;
                float hy = elY + elH - handleSize * 0.5f;
                if (click.x() >= hx && click.x() <= hx + handleSize && click.y() >= hy && click.y() <= hy + handleSize) {
                    resizingElement = true;
                    startScale = el.scale;
                    startMouseY = click.y();
                    return true;
                }
            }

            if (click.x() >= elX && click.x() <= elX + elW && click.y() >= elY && click.y() <= elY + elH) {
                selectedElementId = id;
                draggingElementId = id;
                dragOffsetX = (float) click.x() - elX;
                dragOffsetY = (float) click.y() - elY;
                playClick();
                return true;
            }
        }

        selectedElementId = null;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingElementId = null;
        resizingElement = false;
        activeGuides.clear();
        MainMenuConfig.save();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (resizingElement && selectedElementId != null) {
            MainMenuConfig.ElementConfig el = config.getElement(selectedElementId);
            float diff = (float) ((click.y() - startMouseY) * 0.01);
            el.scale = Math.max(0.5f, Math.min(2.5f, startScale + diff));
            return true;
        }

        if (draggingElementId != null) {
            MainMenuConfig.ElementConfig el = config.getElement(draggingElementId);
            float targetX = (float) click.x() - dragOffsetX;
            float targetY = (float) click.y() - dragOffsetY;
            float elW = getElementBaseWidth(draggingElementId, width) * el.scale;
            float elH = getElementBaseHeight(draggingElementId) * el.scale;

            activeGuides.clear();

            // 1. Screen Center X Snap
            if (Math.abs((targetX + elW * 0.5f) - width * 0.5f) < SNAP_THRESHOLD) {
                targetX = width * 0.5f - elW * 0.5f;
                activeGuides.add(new SnapGuide(true, width * 0.5f, 0, height));
            }
            // 2. Screen Center Y Snap
            if (Math.abs((targetY + elH * 0.5f) - height * 0.5f) < SNAP_THRESHOLD) {
                targetY = height * 0.5f - elH * 0.5f;
                activeGuides.add(new SnapGuide(false, height * 0.5f, 0, width));
            }
            // 3. Flagship Standard Column X Snap
            float defaultColX = MainMenuConfig.columnX(width);
            if (Math.abs(targetX - defaultColX) < SNAP_THRESHOLD) {
                targetX = defaultColX;
                activeGuides.add(new SnapGuide(true, defaultColX, 0, height));
            }

            // 4. Cross-element alignment snapping
            List<String> allIds = List.of("LOGO", "SLOGAN", "SINGLEPLAYER", "MULTIPLAYER", "SETTINGS", "MODS", "QUIT", "PLAYER_CARD");
            for (String otherId : allIds) {
                if (otherId.equals(draggingElementId)) continue;
                MainMenuConfig.ElementConfig other = config.getElement(otherId);
                if (!other.visible) continue;
                float oW = getElementBaseWidth(otherId, width) * other.scale;
                float oH = getElementBaseHeight(otherId) * other.scale;
                float oX = other.getX(width);
                float oY = other.getY(height, 5);

                // Left edge snap
                if (Math.abs(targetX - oX) < 6.0f) {
                    targetX = oX;
                    activeGuides.add(new SnapGuide(true, oX, Math.min(targetY, oY), Math.max(targetY + elH, oY + oH)));
                }
                // Right edge snap
                if (Math.abs((targetX + elW) - (oX + oW)) < 6.0f) {
                    targetX = oX + oW - elW;
                    activeGuides.add(new SnapGuide(true, oX + oW, Math.min(targetY, oY), Math.max(targetY + elH, oY + oH)));
                }
                // Center X snap
                if (Math.abs((targetX + elW * 0.5f) - (oX + oW * 0.5f)) < 6.0f) {
                    targetX = oX + (oW - elW) * 0.5f;
                    activeGuides.add(new SnapGuide(true, oX + oW * 0.5f, Math.min(targetY, oY), Math.max(targetY + elH, oY + oH)));
                }
                // Top edge snap
                if (Math.abs(targetY - oY) < 6.0f) {
                    targetY = oY;
                    activeGuides.add(new SnapGuide(false, oY, Math.min(targetX, oX), Math.max(targetX + elW, oX + oW)));
                }
                // Vertical gap snap (e.g. 6px gap below another element)
                float gapBelow = oY + oH + 6.0f;
                if (Math.abs(targetY - gapBelow) < 6.0f) {
                    targetY = gapBelow;
                    activeGuides.add(new SnapGuide(false, gapBelow, Math.min(targetX, oX), Math.max(targetX + elW, oX + oW)));
                }
            }

            el.setPos(targetX, targetY, width, height);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.BACK_SOUND, 0.9f, 1.0f));
        }
        MainMenuConfig.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedElementId != null) {
            MainMenuConfig.ElementConfig el = config.getElement(selectedElementId);
            float factor = (float) (verticalAmount * 0.05);
            el.scale = Math.max(0.5f, Math.min(2.5f, el.scale + factor));
            MainMenuConfig.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        if (shaderDropdownOpen || photoDropdownOpen || screensModalOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                shaderDropdownOpen = false;
                photoDropdownOpen = false;
                screensModalOpen = false;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (selectedElementId != null) {
            MainMenuConfig.ElementConfig el = config.getElement(selectedElementId);
            float pxX = el.getX(width);
            float pxY = el.getY(height);
            if (keyCode == GLFW.GLFW_KEY_UP) { el.setPos(pxX, pxY - 2.0f, width, height); MainMenuConfig.save(); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { el.setPos(pxX, pxY + 2.0f, width, height); MainMenuConfig.save(); return true; }
            if (keyCode == GLFW.GLFW_KEY_LEFT) { el.setPos(pxX - 2.0f, pxY, width, height); MainMenuConfig.save(); return true; }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) { el.setPos(pxX + 2.0f, pxY, width, height); MainMenuConfig.save(); return true; }
            if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
                el.scale = Math.min(2.5f, el.scale + 0.05f);
                MainMenuConfig.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                el.scale = Math.max(0.5f, el.scale - 0.05f);
                MainMenuConfig.save();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private static boolean inRect(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int withAlpha(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | (rgb & 0xFFFFFF);
    }

    private void playClick() {
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f));
        }
    }

    private void playSaveSound() {
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1.0f));
        }
    }
}
