package dev.strangevisuals.mixin;

import dev.strangevisuals.hud.HudManager;
import dev.strangevisuals.ui.editor.MainMenuConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void strangevisuals$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudManager.render(context);
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$cancelVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MainMenuConfig.get().customHotbar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$cancelVanillaStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MainMenuConfig.get().customStatusEffects) {
            ci.cancel();
        }
    }
}
