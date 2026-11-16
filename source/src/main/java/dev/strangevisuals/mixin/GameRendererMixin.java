package dev.strangevisuals.mixin;

import dev.strangevisuals.hands.HandEditor;
import dev.strangevisuals.hands.HandOutline;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(float fov);

    @Shadow
    private float getFov(Camera camera, float tickProgress, boolean changingFov) {
        throw new AssertionError();
    }

    @Shadow
    public abstract Camera getCamera();

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void strangevisuals$beforeHands(float tickProgress, boolean sleeping,
                                            Matrix4f positionMatrix, CallbackInfo ci) {
        if (HandEditor.active() && getCamera() != null) {
            HandEditor.setFov(getFov(getCamera(), tickProgress, true));
            HandEditor.update();
        }
        HandOutline.captureBefore();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void strangevisuals$afterHands(CallbackInfo ci) {
        HandOutline.composite();
    }
}
