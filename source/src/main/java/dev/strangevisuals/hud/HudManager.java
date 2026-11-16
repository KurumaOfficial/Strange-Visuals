package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.anim.Animations;
import dev.strangevisuals.hands.HandEditor;
import dev.strangevisuals.hud.notify.NotificationManager;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.HudGlassCapture;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.Spring;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public final class HudManager {
    private static final float DOT_SPACING = 26.0f;
    private static final float SNAP_DISTANCE = 5.0f;

    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final float MENU_W = 78.0f;
    private static final float MENU_ROW_H = 16.0f;
    private static final float MENU_PAD = 4.0f;
    private static final float MENU_GAP = 6.0f;

    private static final List<HudElement> ELEMENTS = List.of(new Watermark(), new KeybindPanel(),
        new MediaPlayerHud(),
        new PotionSlot(0), new PotionSlot(1), new PotionSlot(2), new PotionSlot(3), new PotionBlock());
    private static final Spring gridReveal = new Spring(0.0, 700.0, 32.0);
    private static final Spring dragBlur = new Spring(0.0, 900.0, 40.0);

    private static HudElement dragging;
    private static float dragOffsetX;
    private static float dragOffsetY;
    private static boolean previousMouseDown;
    private static boolean previousRightMouseDown;
    private static long lastFrame = System.nanoTime();
    private static boolean loaded;

    private static boolean menuOpen;
    private static float menuX;
    private static float menuY;
    private static GlyphAtlas menuFont;

    private HudManager() {
    }

    public static void render(DrawContext context) {
        dev.strangevisuals.render.Pipelines.warmUp();
        if (!loaded) {
            Map<String, float[]> stored = HudLayoutStorage.load();
            for (HudElement element : ELEMENTS) {
                element.loadFraction(stored.get(element.id()));
            }
            loaded = true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && client.options.hudHidden) {
            return;
        }
        long now = System.nanoTime();
        float deltaSeconds = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        PotionEffectTracker.refresh();
        NameTags.render(context);

        boolean editMode = isEditScreen(client);
        gridReveal.step(editMode ? 1.0 : 0.0, deltaSeconds);
        float gridAlpha = (float) gridReveal.get();

        if (editMode) {
            updateDragging(client);
        } else {
            dragging = null;
            menuOpen = false;
        }

        dragBlur.step(dragging != null ? 1.0 : 0.0, deltaSeconds);
        float blurAmount = (float) dragBlur.get();
        if (blurAmount > 0.05f) {
            try {
                context.applyBlur();
            } catch (IllegalStateException ignored) {
                // Vanilla post-process blur is disabled or unsupported in current device context
            }
            int veilAlpha = Math.round(45 * blurAmount);
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), argb(0x000000, veilAlpha));
        }

        HudGlassCapture.refresh();

        if (gridAlpha > 0.01f) {
            renderGrid(context, gridAlpha);
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        for (HudElement element : ELEMENTS) {
            if (isPotionElement(element) && !dev.strangevisuals.ui.editor.MainMenuConfig.get().customStatusEffects) {
                continue;
            }
            float width = element.width();
            float height = element.height();
            float x = element.centerX(screenWidth) - width * 0.5f;
            float y = element.centerY(screenHeight) - height * 0.5f;
            element.render(context, x, y);

            if (editMode && element == dragging) {
                renderGuides(context, element, x, y, width, height, gridAlpha);
            }
        }

        Animations.tick();
        HandEditor.tick();
        LyricsHud.render(context);
        if (dev.strangevisuals.ui.editor.MainMenuConfig.get().customHotbar) {
            HotbarHud.render(context);
        }
        NotificationManager.render(context);

        if (editMode && menuOpen) {
            renderContextMenu(context);
        }
    }

    public static boolean isEditing() {
        return isEditScreen(MinecraftClient.getInstance());
    }

    private static boolean isEditScreen(MinecraftClient client) {
        return client.currentScreen instanceof ChatScreen;
    }

    private static boolean isPotionElement(HudElement element) {
        return element instanceof PotionSlot || element instanceof PotionBlock;
    }

    private static void updateDragging(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightMouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean freeMove = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(handle, rawX, rawY);
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float mouseX = (float) (rawX[0] * screenWidth / client.getWindow().getWidth());
        float mouseY = (float) (rawY[0] * screenHeight / client.getWindow().getHeight());

        if (rightMouseDown && !previousRightMouseDown) {
            HudElement target = findElementAt(mouseX, mouseY, screenWidth, screenHeight);
            if (isPotionElement(target)) {
                openContextMenu(target, screenWidth, screenHeight);
            } else {
                menuOpen = false;
            }
        }
        previousRightMouseDown = rightMouseDown;

        if (mouseDown && !previousMouseDown) {
            if (menuOpen) {
                handleMenuClick(mouseX, mouseY);
                previousMouseDown = mouseDown;
                return;
            }
            dragging = findElementAt(mouseX, mouseY, screenWidth, screenHeight);
            if (dragging != null) {
                dragOffsetX = mouseX - dragging.centerX(screenWidth);
                dragOffsetY = mouseY - dragging.centerY(screenHeight);
            }
        }

        if (!mouseDown) {
            if (dragging != null) {
                persist();
            }
            dragging = null;
        } else if (dragging != null) {
            float targetX = mouseX - dragOffsetX;
            float targetY = mouseY - dragOffsetY;
            if (!freeMove) {
                float halfW = dragging.width() * 0.5f;
                float halfH = dragging.height() * 0.5f;
                targetX = Math.max(halfW, Math.min(screenWidth - halfW, targetX));
                targetY = Math.max(halfH, Math.min(screenHeight - halfH, targetY));
            }
            float[] snapped = freeMove ? new float[]{targetX, targetY} : snap(dragging, targetX, targetY, screenWidth, screenHeight);
            if (!freeMove) {
                float halfW = dragging.width() * 0.5f;
                float halfH = dragging.height() * 0.5f;
                snapped[0] = Math.max(halfW, Math.min(screenWidth - halfW, snapped[0]));
                snapped[1] = Math.max(halfH, Math.min(screenHeight - halfH, snapped[1]));
            }
            dragging.moveTo(snapped[0], snapped[1], screenWidth, screenHeight);
        }

        previousMouseDown = mouseDown;
    }

    private static void openContextMenu(HudElement target, int screenWidth, int screenHeight) {
        float width = target.width();
        float height = target.height();
        float cx = target.centerX(screenWidth);
        float cy = target.centerY(screenHeight);
        float menuHeight = MENU_PAD * 2.0f + MENU_ROW_H * 2.0f;
        menuX = Math.max(2.0f, Math.min(screenWidth - MENU_W - 2.0f, cx - MENU_W * 0.5f));
        menuY = cy - height * 0.5f - MENU_GAP - menuHeight;
        if (menuY < 2.0f) {
            menuY = cy + height * 0.5f + MENU_GAP;
        }
        menuOpen = true;
    }

    private static void handleMenuClick(float mouseX, float mouseY) {
        float menuHeight = MENU_PAD * 2.0f + MENU_ROW_H * 2.0f;
        boolean inside = mouseX >= menuX && mouseX <= menuX + MENU_W && mouseY >= menuY && mouseY <= menuY + menuHeight;
        if (inside) {
            int row = (int) ((mouseY - menuY - MENU_PAD) / MENU_ROW_H);
            if (row == 0) {
                PotionLayoutMode.set(PotionLayoutMode.Mode.BLOCK);
            } else if (row == 1) {
                PotionLayoutMode.set(PotionLayoutMode.Mode.SEPARATE);
            }
        }
        menuOpen = false;
    }

    private static void renderContextMenu(DrawContext context) {
        if (menuFont == null) {
            menuFont = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }
        float menuHeight = MENU_PAD * 2.0f + MENU_ROW_H * 2.0f;
        RoundedRectPipeline.draw(context, menuX, menuY, MENU_W, menuHeight, argb(0x121318, 245));
        RoundedRectPipeline.drawOutline(context, menuX, menuY, MENU_W, menuHeight, argb(0xFFFFFF, 55));

        PotionLayoutMode.Mode current = PotionLayoutMode.get();
        drawMenuRow(context, "Block", menuX, menuY + MENU_PAD, current == PotionLayoutMode.Mode.BLOCK);
        drawMenuRow(context, "Separate", menuX, menuY + MENU_PAD + MENU_ROW_H, current == PotionLayoutMode.Mode.SEPARATE);
    }

    private static void drawMenuRow(DrawContext context, String label, float x, float y, boolean selected) {
        if (selected) {
            RoundedRectPipeline.draw(context, x + 3.0f, y + 1.0f, MENU_W - 6.0f, MENU_ROW_H - 2.0f, argb(0xFFFFFF, 24));
        }
        MsdfText.draw(context, menuFont, label, x + 9.0f, y + MENU_ROW_H * 0.5f - 3.2f, 7.0f,
            argb(0xFFFFFF, selected ? 250 : 200));
    }

    private static HudElement findElementAt(float mouseX, float mouseY, int screenWidth, int screenHeight) {
        for (int i = ELEMENTS.size() - 1; i >= 0; i--) {
            HudElement element = ELEMENTS.get(i);
            float halfW = element.width() * 0.5f;
            float halfH = element.height() * 0.5f;
            float cx = element.centerX(screenWidth);
            float cy = element.centerY(screenHeight);
            if (mouseX >= cx - halfW && mouseX <= cx + halfW && mouseY >= cy - halfH && mouseY <= cy + halfH) {
                return element;
            }
        }
        return null;
    }

    private static float[] snap(HudElement element, float centerX, float centerY, int screenWidth, int screenHeight) {
        float snappedX = centerX;
        float snappedY = centerY;
        float gridX = Math.round(centerX / DOT_SPACING) * DOT_SPACING;
        float gridY = Math.round(centerY / DOT_SPACING) * DOT_SPACING;
        if (Math.abs(centerX - gridX) < SNAP_DISTANCE) {
            snappedX = gridX;
        }
        if (Math.abs(centerY - gridY) < SNAP_DISTANCE) {
            snappedY = gridY;
        }

        float screenCenterX = screenWidth * 0.5f;
        float screenCenterY = screenHeight * 0.5f;
        if (Math.abs(centerX - screenCenterX) < SNAP_DISTANCE) {
            snappedX = screenCenterX;
        }
        if (Math.abs(centerY - screenCenterY) < SNAP_DISTANCE) {
            snappedY = screenCenterY;
        }

        for (HudElement other : ELEMENTS) {
            if (other == element) {
                continue;
            }
            float otherX = other.centerX(screenWidth);
            float otherY = other.centerY(screenHeight);
            if (Math.abs(centerX - otherX) < SNAP_DISTANCE) {
                snappedX = otherX;
            }
            if (Math.abs(centerY - otherY) < SNAP_DISTANCE) {
                snappedY = otherY;
            }
        }
        return new float[]{snappedX, snappedY};
    }

    private static void renderGrid(DrawContext context, float alpha) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int dotAlpha = Math.round(16 * alpha);
        for (float dy = DOT_SPACING * 0.5f; dy < height; dy += DOT_SPACING) {
            for (float dx = DOT_SPACING * 0.5f; dx < width; dx += DOT_SPACING) {
                context.fill(Math.round(dx) - 1, Math.round(dy) - 1, Math.round(dx) + 1, Math.round(dy) + 1,
                    argb(0xFFFFFF, dotAlpha));
            }
        }
    }

    private static final float GUIDE_REACH = 34.0f;

    private static void renderGuides(DrawContext context, HudElement element, float x, float y, float width, float height, float alpha) {
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;

        boolean onScreenCenterX = Math.abs(centerX - screenWidth * 0.5f) < 0.6f;
        boolean onScreenCenterY = Math.abs(centerY - screenHeight * 0.5f) < 0.6f;

        if (onScreenCenterX) {
            tick(context, centerX, centerY, false, argb(0xFFFFFF, Math.round(220 * alpha)));
        }
        if (onScreenCenterY) {
            tick(context, centerX, centerY, true, argb(0xFFFFFF, Math.round(220 * alpha)));
        }

        for (HudElement other : ELEMENTS) {
            if (other == element) {
                continue;
            }
            float otherX = other.centerX(screenWidth);
            float otherY = other.centerY(screenHeight);
            if (Math.abs(centerX - otherX) < 0.6f) {
                tick(context, centerX, centerY, false, argb(0xFFFFFF, Math.round(190 * alpha)));
            }
            if (Math.abs(centerY - otherY) < 0.6f) {
                tick(context, centerX, centerY, true, argb(0xFFFFFF, Math.round(190 * alpha)));
            }
        }
    }

    private static void tick(DrawContext context, float centerX, float centerY, boolean vertical, int color) {
        if (vertical) {
            GlowPipeline.line(context, centerX, centerY, 2.0f, GUIDE_REACH * 2.0f, color);
        } else {
            GlowPipeline.line(context, centerX, centerY, GUIDE_REACH * 2.0f, 2.0f, color);
        }
    }

    private static void persist() {
        Map<String, float[]> positions = new java.util.HashMap<>();
        for (HudElement element : ELEMENTS) {
            positions.put(element.id(), element.toStoredFraction());
        }
        HudLayoutStorage.save(positions);
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
