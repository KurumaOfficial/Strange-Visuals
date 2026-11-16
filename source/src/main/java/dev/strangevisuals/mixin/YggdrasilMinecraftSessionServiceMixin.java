package dev.strangevisuals.mixin;

import com.mojang.authlib.SignatureState;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public abstract class YggdrasilMinecraftSessionServiceMixin {
    @Inject(method = "getPropertySignatureState", at = @At("HEAD"), cancellable = true, remap = false)
    private void strangevisuals$fixEmptySignatureState(Property property, CallbackInfoReturnable<SignatureState> cir) {
        if (property == null || property.signature() == null || property.signature().trim().isEmpty()) {
            cir.setReturnValue(SignatureState.UNSIGNED);
        }
    }
}
