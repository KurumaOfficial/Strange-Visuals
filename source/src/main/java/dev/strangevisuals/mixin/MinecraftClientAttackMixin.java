package dev.strangevisuals.mixin;

import dev.strangevisuals.player.FakePlayer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientAttackMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$hitDummy(CallbackInfoReturnable<Boolean> info) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (FakePlayer.tryHit(client)) {
            info.setReturnValue(true);
        }
    }
}
