package dev.strangevisuals.mixin;

import dev.strangevisuals.clickgui.ModuleRegistry;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void strangevisuals$fogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance,
                                         float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        if (!ModuleRegistry.ambience().enabled() || !ModuleRegistry.ambienceFog().value()) {
            return;
        }
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
            return;
        }
        int rgb = ModuleRegistry.fogColor().rgb();
        float tr = (rgb >> 16 & 0xFF) / 255.0f;
        float tg = (rgb >> 8 & 0xFF) / 255.0f;
        float tb = (rgb & 0xFF) / 255.0f;
        Vector4f orig = cir.getReturnValue();
        orig.set(
            orig.x + (tr - orig.x) * 0.75f,
            orig.y + (tg - orig.y) * 0.75f,
            orig.z + (tb - orig.z) * 0.75f,
            orig.w
        );
        cir.setReturnValue(orig);
    }
}
