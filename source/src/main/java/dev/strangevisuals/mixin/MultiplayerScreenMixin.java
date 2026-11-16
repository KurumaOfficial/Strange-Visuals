package dev.strangevisuals.mixin;

import dev.strangevisuals.ui.CustomMultiplayerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin {
    @Shadow private Screen parent;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$replaceMultiplayer(CallbackInfo ci) {
        if (dev.strangevisuals.ui.style.StyleManager.getStyle() != dev.strangevisuals.ui.style.MenuStyle.BETA) {
            return;
        }
        if (!dev.strangevisuals.ui.editor.MainMenuConfig.get().customMultiplayerScreen) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new CustomMultiplayerScreen(this.parent));
        ci.cancel();
    }
}
