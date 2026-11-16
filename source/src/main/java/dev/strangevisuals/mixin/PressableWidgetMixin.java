package dev.strangevisuals.mixin;

import dev.strangevisuals.render.WidgetStylePipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    protected PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$customButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            return;
        }
        Screen screen = client.currentScreen;
        if (screen instanceof dev.strangevisuals.ui.editor.MainMenuEditorScreen
                || screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen
                || screen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            return;
        }

        dev.strangevisuals.ui.style.MenuStyle style = dev.strangevisuals.ui.style.StyleManager.getStyle();

        // Authentic Vanilla Theme: apply to all buttons across all menu screens
        if (style == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            Object self = this;
            if (self instanceof net.minecraft.client.gui.widget.ButtonWidget
                    || self instanceof net.minecraft.client.gui.widget.CyclingButtonWidget) {
                if (dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, this, mouseX, mouseY, delta)) {
                    ci.cancel();
                }
            }
            return;
        }

        // Modern Beta Theme:
        if (screen instanceof net.minecraft.client.gui.screen.TitleScreen) {
            return;
        } else if (screen instanceof GameMenuScreen) {
            if (!dev.strangevisuals.ui.editor.MainMenuConfig.get().customPauseScreen) {
                return;
            }
        } else if (screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                || screen instanceof PackScreen || screen instanceof SelectWorldScreen) {
            if (!dev.strangevisuals.ui.editor.MainMenuConfig.get().customSettingsScreen) {
                return;
            }
        }

        Object self = this;
        if (self instanceof net.minecraft.client.gui.widget.ButtonWidget
                || self instanceof net.minecraft.client.gui.widget.CyclingButtonWidget) {
            WidgetStylePipeline.renderButton(context, this, mouseX, mouseY, delta);
            ci.cancel();
        }
    }
}
