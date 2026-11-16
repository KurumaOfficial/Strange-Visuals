package dev.strangevisuals.ui;

import dev.strangevisuals.macro.Macro;
import dev.strangevisuals.macro.MacroManager;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.style.MenuStyle;
import dev.strangevisuals.ui.style.StyleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MacroScreen extends Screen {
    private final Screen parent;
    private Macro listeningMacro = null;
    private boolean isAddingModal = false;
    private int addingKey = GLFW.GLFW_KEY_UNKNOWN;
    private boolean addingKeyListening = false;
    private TextFieldWidget addCommandField;
    private int scrollOffset = 0;

    public MacroScreen(Screen parent) {
        super(Text.translatable("strangevisuals.macro.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelW = 340;
        int panelX = (width - panelW) / 2;

        if (isAddingModal) {
            // Modal dialog for adding a new macro
            int modalW = 280;
            int modalH = 140;
            int modalX = (width - modalW) / 2;
            int modalY = (height - modalH) / 2;

            addDrawableChild(ButtonWidget.builder(
                addingKeyListening ? Text.translatable("strangevisuals.macro.press_key")
                    : (addingKey > 0 ? Text.literal(new Macro(addingKey, "").getKeyDisplayName()) : Text.translatable("strangevisuals.macro.select_key")),
                button -> {
                    addingKeyListening = true;
                    clearAndInit();
                }
            ).dimensions(modalX + 20, modalY + 36, modalW - 40, 20).build());

            addCommandField = new TextFieldWidget(textRenderer, modalX + 20, modalY + 66, modalW - 40, 20, Text.translatable("strangevisuals.macro.command"));
            addCommandField.setMaxLength(128);
            addCommandField.setPlaceholder(Text.translatable("strangevisuals.macro.command_placeholder"));
            addDrawableChild(addCommandField);

            addDrawableChild(ButtonWidget.builder(Text.translatable("strangevisuals.macro.save"), button -> {
                String cmd = addCommandField.getText().trim();
                if (addingKey > 0 && !cmd.isEmpty()) {
                    MacroManager.add(addingKey, cmd);
                    isAddingModal = false;
                    addingKey = GLFW.GLFW_KEY_UNKNOWN;
                    addingKeyListening = false;
                    clearAndInit();
                }
            }).dimensions(modalX + 20, modalY + 98, 115, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.translatable("strangevisuals.macro.cancel"), button -> {
                isAddingModal = false;
                addingKey = GLFW.GLFW_KEY_UNKNOWN;
                addingKeyListening = false;
                clearAndInit();
            }).dimensions(modalX + 145, modalY + 98, 115, 20).build());
            return;
        }

        int listTop = 50;
        int listBottom = height - 52;
        int visibleCount = (listBottom - listTop) / 28;
        List<Macro> macros = MacroManager.getMacros();

        int maxScroll = Math.max(0, macros.size() - visibleCount);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        int cardY = listTop;
        for (int i = scrollOffset; i < macros.size() && cardY + 24 <= listBottom; i++) {
            Macro macro = macros.get(i);
            int currentY = cardY;

            // Key button
            boolean isListening = (listeningMacro == macro);
            String keyText = isListening ? "???" : macro.getKeyDisplayName();
            addDrawableChild(ButtonWidget.builder(Text.literal(keyText), button -> {
                listeningMacro = isListening ? null : macro;
                clearAndInit();
            }).dimensions(panelX + 4, currentY + 2, 48, 20).build());

            // Toggle button
            Text toggleText = macro.isEnabled()
                ? Text.translatable("strangevisuals.macro.on")
                : Text.translatable("strangevisuals.macro.off");
            addDrawableChild(ButtonWidget.builder(toggleText, button -> {
                macro.setEnabled(!macro.isEnabled());
                MacroManager.save();
                clearAndInit();
            }).dimensions(panelX + panelW - 74, currentY + 2, 38, 20).build());

            // Delete button
            addDrawableChild(ButtonWidget.builder(Text.literal("✕"), button -> {
                MacroManager.remove(macro);
                clearAndInit();
            }).dimensions(panelX + panelW - 32, currentY + 2, 28, 20).build());

            cardY += 28;
        }

        // Bottom action buttons
        int btnW = 140;
        int btnGap = 12;
        int bottomX = (width - (btnW * 2 + btnGap)) / 2;
        int bottomY = height - 36;

        addDrawableChild(ButtonWidget.builder(Text.translatable("strangevisuals.macro.add_btn"), button -> {
            isAddingModal = true;
            addingKey = GLFW.GLFW_KEY_UNKNOWN;
            addingKeyListening = true;
            clearAndInit();
        }).dimensions(bottomX, bottomY, btnW, 20).build());

        addDrawableChild(ButtonWidget.builder(net.minecraft.screen.ScreenTexts.BACK, button -> close())
            .dimensions(bottomX + btnW + btnGap, bottomY, btnW, 20).build());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (isAddingModal && addingKeyListening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                addingKeyListening = false;
                clearAndInit();
                return true;
            }
            addingKey = keyCode;
            addingKeyListening = false;
            clearAndInit();
            return true;
        }

        if (listeningMacro != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                listeningMacro.setKey(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                listeningMacro.setKey(keyCode);
            }
            listeningMacro = null;
            MacroManager.save();
            clearAndInit();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isAddingModal) {
            int maxScroll = Math.max(0, MacroManager.getMacros().size() - 5);
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                clearAndInit();
                return true;
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuStyle style = StyleManager.getStyle();
        if (style == MenuStyle.VANILLA) {
            dev.strangevisuals.render.VanillaStylePipeline.renderOptionsBackground(context, width, height);
            context.fillGradient(0, 0, width, height, 0x44000000, 0x77000000);
        } else {
            dev.strangevisuals.render.BackgroundPipeline.draw(1.0f);
            context.fillGradient(0, 0, width, height, 0x66000000, 0x99020406);
            GlowPipeline.radial(context, width * 0.5f, 60.0f, 130.0f, 0x2200E5FF);
        }

        // Header Title & Subtitle
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("strangevisuals.macro.header"), width / 2, 16, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("strangevisuals.macro.subtitle"), width / 2, 30, 0xFFA0A8B8);

        int panelW = 340;
        int panelX = (width - panelW) / 2;
        int listTop = 50;
        int listBottom = height - 52;
        int visibleCount = (listBottom - listTop) / 28;
        List<Macro> macros = MacroManager.getMacros();

        if (!isAddingModal) {
            if (macros.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("strangevisuals.macro.empty_title"), width / 2, height / 2 - 10, 0xFF888899);
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("strangevisuals.macro.empty_subtitle"), width / 2, height / 2 + 6, 0xFF55FF55);
            } else {
                int cardY = listTop;
                for (int i = scrollOffset; i < macros.size() && cardY + 24 <= listBottom; i++) {
                    Macro macro = macros.get(i);
                    boolean isHovered = mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= cardY && mouseY <= cardY + 24;

                    int bg = isHovered ? 0xDD1B2232 : 0xDD111622;
                    int outline = macro.isEnabled() ? (isHovered ? NeonEdge.cool() : 0xFF2A374E) : 0xFF222632;
                    RoundedRectPipeline.draw(context, panelX, cardY, panelW, 24, bg);
                    RoundedRectPipeline.drawOutline(context, panelX, cardY, panelW, 24, outline);

                    // Command text display in center
                    String cmdText = macro.getCommand();
                    int maxCmdW = panelW - 136;
                    String trimmedCmd = textRenderer.trimToWidth(cmdText, maxCmdW);
                    int cmdColor = macro.isEnabled() ? 0xFFFFFFFF : 0xFF7A8294;
                    context.drawTextWithShadow(textRenderer, trimmedCmd, panelX + 58, cardY + 8, cmdColor);

                    cardY += 28;
                }
            }
        } else {
            // Render modal backdrop
            int modalW = 280;
            int modalH = 140;
            int modalX = (width - modalW) / 2;
            int modalY = (height - modalH) / 2;

            RoundedRectPipeline.draw(context, modalX, modalY, modalW, modalH, 0xFA0E121B);
            RoundedRectPipeline.drawOutline(context, modalX, modalY, modalW, modalH, NeonEdge.cool());
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("strangevisuals.macro.modal_title"), width / 2, modalY + 14, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
