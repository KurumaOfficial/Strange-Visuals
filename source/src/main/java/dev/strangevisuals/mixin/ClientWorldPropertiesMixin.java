package dev.strangevisuals.mixin;

import dev.strangevisuals.clickgui.Ambience;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void strangevisuals$overrideTime(CallbackInfoReturnable<Long> cir) {
        long vanilla = cir.getReturnValue();
        long override = Ambience.timeOfDay(vanilla);
        if (override != vanilla) {
            cir.setReturnValue(override);
        }
    }
}
