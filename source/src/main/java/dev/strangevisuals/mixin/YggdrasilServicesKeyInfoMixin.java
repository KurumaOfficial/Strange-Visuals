package dev.strangevisuals.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = YggdrasilServicesKeyInfo.class, remap = false)
public abstract class YggdrasilServicesKeyInfoMixin {
    @Inject(method = "validateProperty", at = @At("HEAD"), cancellable = true, remap = false)
    private void strangevisuals$fixEmptySignatureValidation(Property property, CallbackInfoReturnable<Boolean> cir) {
        if (property == null || property.signature() == null || property.signature().trim().isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}
