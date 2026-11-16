package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @WrapOperation(
        method = "onPlayerList",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V")
    )
    private void strangevisuals$silenceUnknownPlayerWarning(Logger logger, String format, Object arg1, Object arg2, Operation<Void> original) {
        if (format != null && format.contains("Ignoring player info update")) {
            // Suppress benign proxy / hub player list warning on servers like FunTime
            return;
        }
        original.call(logger, format, arg1, arg2);
    }
}
