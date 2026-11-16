package dev.strangevisuals.clickgui.classic;

import dev.strangevisuals.clickgui.KeyNames;
import dev.strangevisuals.clickgui.ModuleInfo;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;
import dev.strangevisuals.clickgui.classic.mouse.ClassicGuiMouseHandler;
import dev.strangevisuals.clickgui.classic.render.ClassicGuiRender;
import dev.strangevisuals.clickgui.setting.KeybindSetting;
import dev.strangevisuals.clickgui.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class ClassicGuiScreen extends Screen {
    private static final float APPEAR_DURATION = 0.24F;
    private static final float CLOSE_DURATION = 0.16F;

    private long appearStartNanos;
    private long closeStartNanos = -1L;
    private float currentScale = 1.0F;
    private boolean draggingWindow = false;
    private float dragOffsetX = 0.0F;
    private float dragOffsetY = 0.0F;

    public ClassicGuiScreen() {
        super(Text.literal("Strange Visuals"));
        GuiLocalization.initialize();
        this.appearStartNanos = System.nanoTime();
    }

    @Override
    protected void init() {
        super.init();
        ClassicGuiState.updatePositions(this.width, this.height);
        this.appearStartNanos = System.nanoTime();
        this.closeStartNanos = -1L;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        float scale = 1.0F;
        float alpha = 1.0F;

        if (closeStartNanos > 0) {
            float closeT = Math.min(1.0F, (System.nanoTime() - closeStartNanos) / 1_000_000_000.0F / CLOSE_DURATION);
            float eased = easeInCubic(closeT);
            scale = 1.0F - 0.10F * eased;
            alpha = 1.0F - eased;
            if (closeT >= 1.0F) {
                closeStartNanos = -1L;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.setScreen(null);
                }
                return;
            }
        } else {
            float openT = Math.min(1.0F, (System.nanoTime() - appearStartNanos) / 1_000_000_000.0F / APPEAR_DURATION);
            float eased = easeOutCubic(openT);
            scale = 0.90F + 0.10F * eased;
            alpha = 0.05F + 0.95F * eased;
        }

        this.currentScale = scale;

        float anchorX = ClassicGuiState.x + ClassicGuiState.width / 2.0F;
        float anchorY = ClassicGuiState.y + ClassicGuiState.height / 2.0F;

        double unscaledMouseX = mouseX;
        double unscaledMouseY = mouseY;
        if (scale < 0.999F || scale > 1.001F) {
            unscaledMouseX = anchorX + (mouseX - anchorX) / scale;
            unscaledMouseY = anchorY + (mouseY - anchorY) / scale;
        }

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);

        ClassicGuiRender.render(context, unscaledMouseX, unscaledMouseY, deltaTicks);

        matrices.popMatrix();

        if (alpha < 0.999F) {
            int overlayAlpha = Math.max(0, Math.min(255, (int) ((1.0F - alpha) * 160.0F)));
            context.fill(0, 0, this.width, this.height, overlayAlpha << 24);
        }
    }

    private double[] unscaleCoordinates(double mouseX, double mouseY) {
        float anchorX = ClassicGuiState.x + ClassicGuiState.width / 2.0F;
        float anchorY = ClassicGuiState.y + ClassicGuiState.height / 2.0F;
        float s = Math.max(0.1F, this.currentScale);
        return new double[]{
            anchorX + (mouseX - anchorX) / s,
            anchorY + (mouseY - anchorY) / s
        };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double[] unscaled = unscaleCoordinates(click.x(), click.y());
        double mx = unscaled[0];
        double my = unscaled[1];
        int button = click.button();

        // Module keybind listening check
        for (ModuleInfo m : ModuleRegistry.all()) {
            if (m.binding()) {
                m.resolveBinding(button == 0 ? -100 : (button == 1 ? -99 : -98));
                return true;
            }
            if (m.hasSettings()) {
                for (Setting s : m.leftSettings()) {
                    if (s instanceof KeybindSetting k && k.listening()) {
                        k.resolve("M" + (button + 1));
                        return true;
                    }
                }
                for (Setting s : m.rightSettings()) {
                    if (s instanceof KeybindSetting k && k.listening()) {
                        k.resolve("M" + (button + 1));
                        return true;
                    }
                }
            }
        }

        if (ClassicGuiMouseHandler.mouseClicked(mx, my, button)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            if (ClassicGuiState.isHovered(mx, my, ClassicGuiState.x, ClassicGuiState.y, ClassicGuiState.width, ClassicGuiState.height)) {
                this.draggingWindow = true;
                this.dragOffsetX = (float) click.x() - ClassicGuiState.x;
                this.dragOffsetY = (float) click.y() - ClassicGuiState.y;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (this.draggingWindow) {
            float newX = (float) click.x() - this.dragOffsetX;
            float newY = (float) click.y() - this.dragOffsetY;
            ClassicGuiState.x = Math.max(0.0F, Math.min(this.width - ClassicGuiState.width, newX));
            ClassicGuiState.y = Math.max(0.0F, Math.min(this.height - ClassicGuiState.height, newY));
            ClassicGuiState.savedX = ClassicGuiState.x;
            ClassicGuiState.savedY = ClassicGuiState.y;
            return true;
        }
        double[] unscaled = unscaleCoordinates(click.x(), click.y());
        ClassicGuiMouseHandler.mouseDragged(unscaled[0], unscaled[1]);
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.draggingWindow) {
            this.draggingWindow = false;
            dev.strangevisuals.clickgui.ConfigManager.save();
        }
        ClassicGuiMouseHandler.mouseReleased(click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double[] unscaled = unscaleCoordinates(mouseX, mouseY);
        ClassicGuiState.ModulePanel panel = ClassicGuiState.modulePanelBounds();
        if (ClassicGuiState.isHovered(unscaled[0], unscaled[1], panel.x(), panel.y(), panel.width(), panel.height())) {
            ClassicGuiState.scroll.handleScroll(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        // Search active typing
        if (ClassicGuiState.searchActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!ClassicGuiState.searchQuery.isEmpty()) {
                    ClassicGuiState.searchQuery = "";
                } else {
                    ClassicGuiState.searchActive = false;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                ClassicGuiState.searchActive = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!ClassicGuiState.searchQuery.isEmpty()) {
                    ClassicGuiState.searchQuery = ClassicGuiState.searchQuery.substring(0, ClassicGuiState.searchQuery.length() - 1);
                }
                return true;
            }
            return true;
        }

        // Module keybind resolution
        for (ModuleInfo m : ModuleRegistry.all()) {
            if (m.binding()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                    m.clearBinding();
                } else {
                    m.resolveBinding(keyCode);
                }
                return true;
            }
            if (m.hasSettings()) {
                for (Setting s : m.leftSettings()) {
                    if (s instanceof KeybindSetting k && k.listening()) {
                        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                            k.clear();
                        } else {
                            k.resolve(KeyNames.of(keyCode));
                        }
                        return true;
                    }
                }
                for (Setting s : m.rightSettings()) {
                    if (s instanceof KeybindSetting k && k.listening()) {
                        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                            k.clear();
                        } else {
                            k.resolve(KeyNames.of(keyCode));
                        }
                        return true;
                    }
                }
            }
        }

        boolean hasCtrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (hasCtrl && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            ModuleRegistry.setGuiStyle("Beta");
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.setScreen(new dev.strangevisuals.clickgui.ClickGuiScreen());
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ClassicGuiState.searchActive && input.isValidChar()) {
            ClassicGuiState.searchQuery += input.asString();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        if (closeStartNanos < 0) {
            dev.strangevisuals.clickgui.ConfigManager.save();
            ClassicGuiState.resetTransientState();
            closeStartNanos = System.nanoTime();
            return;
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
