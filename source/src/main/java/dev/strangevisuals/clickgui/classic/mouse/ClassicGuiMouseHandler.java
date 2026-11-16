package dev.strangevisuals.clickgui.classic.mouse;

import dev.strangevisuals.clickgui.ModuleInfo;
import dev.strangevisuals.clickgui.classic.ClassicCategory;
import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.render.ClassicRenderTheme;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.setting.*;

import java.util.List;

public final class ClassicGuiMouseHandler {

    private static SliderSetting draggingSlider = null;
    private static float activeSliderTrackX = 0;
    private static float activeSliderTrackW = 94;
    private static ColorSetting draggingColorSatVal = null;
    private static ColorSetting draggingColorHue = null;
    private static float activeColorBaseX = 0;
    private static float activeColorBaseY = 0;

    private ClassicGuiMouseHandler() {
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Language switch
        if (ClassicRenderTheme.isLanguageSwitchHovered(mouseX, mouseY)) {
            ClassicRenderTheme.handleLanguageSwitchClick(mouseX, mouseY);
            return true;
        }

        // 2. Search bar
        float searchWidth = 90.0F;
        float searchHeight = 20.0F;
        float searchX = ClassicGuiState.x + (ClassicGuiState.width / 2.0F) - (searchWidth / 2.0F);
        float searchY = ClassicGuiState.y + ClassicGuiState.height + 8.0F;

        if (ClassicGuiState.isHovered(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight)) {
            ClassicGuiState.searchActive = true;
            return true;
        } else if (ClassicGuiState.searchActive) {
            ClassicGuiState.searchActive = false;
        }

        // 3. Category bar tabs
        float catBarY = ClassicGuiState.y + 30.0F;
        float xGo = 0.0F;
        for (ClassicCategory category : ClassicCategory.values()) {
            float textWidth = FontDraw.getWidth(FontDraw.FontType.MEDIUM, category.getName(), 5);
            float itemX = ClassicGuiState.x + 10.0F + xGo;
            float itemW = textWidth + 6.0F;
            float itemH = 12.0F;

            if (ClassicGuiState.isHovered(mouseX, mouseY, itemX, catBarY, itemW, itemH)) {
                if (ClassicGuiState.selectedCategory != category) {
                    ClassicGuiState.selectedCategory = category;
                    ClassicGuiState.scroll.reset();
                    ClassicGuiState.resetTransientState();
                }
                return true;
            }
            xGo += 9.0F + textWidth;
        }

        // 4. Themes tab click
        if (ClassicGuiState.selectedCategory == ClassicCategory.THEME) {
            return ClassicRenderTheme.handleThemeClick(mouseX, mouseY);
        }

        // 5. Modules & Settings clicks
        ClassicGuiState.ModulePanel panel = ClassicGuiState.modulePanelBounds();
        float modulesX = panel.x();
        float modulesY = panel.y();
        float modulesWidth = panel.width();
        float modulesHeight = panel.height();

        float listStartY = ClassicGuiState.listStartY();
        float listHeight = ClassicGuiState.listHeight();
        float clipTop = listStartY;
        float clipBottom = listStartY + listHeight;

        float scrollY = ClassicGuiState.scroll.getScroll();
        float yDown = 0.0F;

        List<ModuleInfo> visibleModules = ClassicGuiState.visibleModules();

        for (ModuleInfo module : visibleModules) {
            float up = ClassicGuiState.calcUP(module);
            float drawY = listStartY + yDown + scrollY;
            float headerH = ClassicGuiState.MODULE_HEADER_HEIGHT;

            boolean headerVisible = drawY + headerH > clipTop && drawY < clipBottom;
            boolean blockVisible = drawY + 26.0F + up > clipTop && drawY < clipBottom;

            // Module header clicked
            if (headerVisible && ClassicGuiState.isHovered(mouseX, mouseY, modulesX, drawY, modulesWidth, headerH)) {
                if (button == 0) {
                    module.toggle();
                    return true;
                } else if (button == 1) {
                    module.toggleExpanded();
                    return true;
                } else if (button == 2) {
                    module.armBinding();
                    return true;
                }
            }

            // Expanded settings clicked
            if (module.hasSettings() && module.expanded() && blockVisible) {
                float settingsTop = drawY + headerH + 2.0F;
                if (handleSettingsClick(module.leftSettings(), mouseX, mouseY, modulesX + 7, settingsTop, clipTop, clipBottom)) {
                    return true;
                }
                if (handleSettingsClick(module.rightSettings(), mouseX, mouseY, modulesX + modulesWidth / 2.0F + 2.0F, settingsTop, clipTop, clipBottom)) {
                    return true;
                }
            }

            yDown += ClassicGuiState.MODULE_VERTICAL_SPACING + up;
        }

        return false;
    }

    private static boolean handleSettingsClick(List<Setting> settings, double mouseX, double mouseY, float x, float y, float clipTop, float clipBottom) {
        if (settings == null) return false;
        float up = 0.0F;

        for (Setting setting : settings) {
            float xSettings = x;
            float ySettings = y + up;

            if (setting instanceof BooleanSetting bool) {
                if (isHoveredClipped(mouseX, mouseY, xSettings + 88, ySettings + 4, 10, 10, clipTop, clipBottom)) {
                    bool.toggle();
                    return true;
                }
                up += 20.0F;
            } else if (setting instanceof SliderSetting slider) {
                if (isHoveredClipped(mouseX, mouseY, xSettings + 4, ySettings + 11, 94, 6, clipTop, clipBottom)) {
                    draggingSlider = slider;
                    activeSliderTrackX = xSettings + 4;
                    activeSliderTrackW = 94;
                    updateSliderFraction(slider, mouseX, activeSliderTrackX, activeSliderTrackW);
                    return true;
                }
                up += 20.0F;
            } else if (setting instanceof ModeSetting mode) {
                float btnX = xSettings + 52.0F;
                float btnY = ySettings + 3.0F;
                float btnW = 44.0F;
                float btnH = 12.0F;

                if (isHoveredClipped(mouseX, mouseY, btnX, btnY, btnW, btnH, clipTop, clipBottom)) {
                    mode.toggleOpen();
                    return true;
                }
                if (mode.open()) {
                    float itemH = 11.0F;
                    float dropY = btnY + btnH + 1.0F;
                    for (int i = 0; i < mode.options().size(); i++) {
                        float rowY = dropY + 1.0F + i * itemH;
                        if (isHoveredClipped(mouseX, mouseY, btnX, rowY, btnW, itemH, clipTop, clipBottom)) {
                            mode.select(i);
                            return true;
                        }
                    }
                    up += mode.options().size() * itemH + 4.0F;
                }
                up += 20.0F;
            } else if (setting instanceof ColorSetting color) {
                if (isHoveredClipped(mouseX, mouseY, xSettings + 88, ySettings + 4, 10, 10, clipTop, clipBottom)) {
                    color.toggleOpen();
                    return true;
                }
                if (color.open()) {
                    float xColor = xSettings + 5;
                    float yColor = ySettings + 16;
                    int size = 82;

                    // 2D Saturation / Value drag
                    if (isHoveredClipped(mouseX, mouseY, xColor + 5, yColor + 5, size, 60, clipTop, clipBottom)) {
                        draggingColorSatVal = color;
                        activeColorBaseX = xColor + 5;
                        activeColorBaseY = yColor + 5;
                        updateColorSatVal(color, mouseX, mouseY, activeColorBaseX, activeColorBaseY, size, 60);
                        return true;
                    }

                    // 1D Hue bar drag
                    if (isHoveredClipped(mouseX, mouseY, xColor + 5, yColor + 69, size, 4, clipTop, clipBottom)) {
                        draggingColorHue = color;
                        activeColorBaseX = xColor + 5;
                        updateColorHue(color, mouseX, activeColorBaseX, size);
                        return true;
                    }
                    up += 80.0F;
                }
                up += 20.0F;
            } else if (setting instanceof KeybindSetting bind) {
                if (isHoveredClipped(mouseX, mouseY, xSettings + 58, ySettings + 4, 40, 10, clipTop, clipBottom)) {
                    bind.arm();
                    return true;
                }
                up += 20.0F;
            } else {
                up += 20.0F;
            }
        }
        return false;
    }

    public static void mouseDragged(double mouseX, double mouseY) {
        if (draggingSlider != null) {
            updateSliderFraction(draggingSlider, mouseX, activeSliderTrackX, activeSliderTrackW);
        }
        if (draggingColorSatVal != null) {
            updateColorSatVal(draggingColorSatVal, mouseX, mouseY, activeColorBaseX, activeColorBaseY, 82, 60);
        }
        if (draggingColorHue != null) {
            updateColorHue(draggingColorHue, mouseX, activeColorBaseX, 82);
        }
    }

    public static void mouseReleased(int button) {
        draggingSlider = null;
        draggingColorSatVal = null;
        draggingColorHue = null;
    }

    private static void updateSliderFraction(SliderSetting slider, double mouseX, float trackX, float trackW) {
        float fraction = (float) ((mouseX - trackX) / trackW);
        slider.setFraction(Math.max(0.0F, Math.min(1.0F, fraction)));
    }

    private static void updateColorSatVal(ColorSetting color, double mouseX, double mouseY, float baseX, float baseY, float w, float h) {
        float relX = (float) Math.max(0.0F, Math.min(w, mouseX - baseX));
        float relY = (float) Math.max(0.0F, Math.min(h, mouseY - baseY));
        float saturation = relX / w;
        float value = 1.0F - (relY / h);
        color.setSaturationValue(saturation, value);
    }

    private static void updateColorHue(ColorSetting color, double mouseX, float baseX, float w) {
        float relX = (float) Math.max(0.0F, Math.min(w, mouseX - baseX));
        float hue = relX / w;
        color.setHue(hue);
    }

    private static boolean isHoveredClipped(double mouseX, double mouseY, float x, float y, float width, float height, float clipTop, float clipBottom) {
        return mouseY >= clipTop && mouseY <= clipBottom && ClassicGuiState.isHovered(mouseX, mouseY, x, y, width, height);
    }
}
