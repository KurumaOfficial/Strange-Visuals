package dev.strangevisuals.mixin;

import dev.strangevisuals.clickgui.ModuleRegistry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogModifier.class)
public abstract class AtmosphericFogModifierMixin {
    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void strangevisuals$fog(FogData data, Camera camera, ClientWorld world, float viewDistance,
                                    RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!ModuleRegistry.ambience().enabled() || !ModuleRegistry.ambienceFog().value()) {
            return;
        }
        float end = ModuleRegistry.fogDistance().value();
        float start = end * 0.35f;
        data.environmentalStart = start;
        data.environmentalEnd = end;
        data.renderDistanceStart = start;
        data.renderDistanceEnd = end;
        data.skyEnd = end * 1.6f;
        data.cloudEnd = end * 1.6f;
    }
}
