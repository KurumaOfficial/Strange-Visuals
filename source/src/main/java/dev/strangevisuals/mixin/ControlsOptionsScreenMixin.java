package dev.strangevisuals.mixin;

import dev.strangevisuals.ui.MacroScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsOptionsScreen.class)
public abstract class ControlsOptionsScreenMixin extends GameOptionsScreen {
    protected ControlsOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Inject(
        method = "addOptions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/OptionListWidget;addWidgetEntry(Lnet/minecraft/client/gui/widget/ClickableWidget;Lnet/minecraft/client/gui/widget/ClickableWidget;)V",
            shift = At.Shift.AFTER
        )
    )
    private void strangevisuals$addMacroButton(CallbackInfo ci) {
        ButtonWidget macroBtn = ButtonWidget.builder(
            Text.translatable("strangevisuals.gui.macros"),
            btn -> {
                if (this.client != null) {
                    this.client.setScreen(new MacroScreen(this));
                }
            }
        ).width(310).build();
        this.body.addWidgetEntry(macroBtn, null);
    }
}
