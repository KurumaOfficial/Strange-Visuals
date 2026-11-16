package dev.strangevisuals.mixin;

import dev.strangevisuals.clickgui.ModuleRegistry;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.RainSplashParticle;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RainSplashParticle.class)
public abstract class RainSplashParticleMixin extends BillboardParticle {
    private RainSplashParticleMixin(ClientWorld world, double x, double y, double z, Sprite sprite) {
        super(world, x, y, z, sprite);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void strangevisuals$matchRain(ClientWorld world, double x, double y, double z,
                                          Sprite sprite, CallbackInfo ci) {
        if (!ModuleRegistry.shaders().enabled()
            || "Off".equals(ModuleRegistry.rainMode().current())) {
            return;
        }
        int tint = ModuleRegistry.rainColor().rgb();

        setColor((tint >> 16 & 0xFF) / 255.0f * 0.9f,
            (tint >> 8 & 0xFF) / 255.0f * 0.9f,
            (tint & 0xFF) / 255.0f * 0.9f);
    }
}
