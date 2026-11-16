package dev.strangevisuals.mixin;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.render.SkyRenderer;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
    private static boolean strangevisuals$active() {
        return ModuleRegistry.shaderSky().enabled();
    }

    @Inject(method = "renderTopSky", at = @At("TAIL"))
    private void strangevisuals$paintSky(int color, CallbackInfo ci) {
        SkyRenderer.render();
    }

    @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$glowingSky(MatrixStack matrices, float tickProgress, int color, CallbackInfo ci) {
        if (strangevisuals$active()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$skyDark(CallbackInfo ci) {
        if (strangevisuals$active()) {
            ci.cancel();
        }
    }
}
