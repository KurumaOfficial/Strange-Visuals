package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.strangevisuals.anim.Animations;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean strangevisuals$holdThirdPerson(boolean thirdPerson) {
        float progress = Animations.perspective();
        if (progress < 0.0f) {
            return thirdPerson;
        }
        return thirdPerson || progress > 0.01f;
    }

    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean strangevisuals$holdFrontView(boolean inverseView) {
        float flip = Animations.flip();
        if (flip < 0.0f) {
            return inverseView;
        }
        return inverseView || flip > 0.005f;
    }

    @ModifyReturnValue(method = "clipToSpace", at = @At("RETURN"))
    private float strangevisuals$easeDistance(float original) {
        float progress = Animations.perspective();
        return progress < 0.0f ? original : original * progress;
    }

    @ModifyArgs(method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 2))
    private void strangevisuals$easeFlip(Args args) {
        float flip = Animations.flip();
        if (flip < 0.0f) {
            return;
        }
        float turned = args.<Float>get(0);
        float inverted = args.<Float>get(1);

        args.set(0, turned - 180.0f + 180.0f * flip);
        args.set(1, -inverted * (1.0f - 2.0f * flip));
    }
}
