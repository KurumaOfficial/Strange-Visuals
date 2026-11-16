package dev.strangevisuals.mixin;

import dev.strangevisuals.player.FakePlayer;
import dev.strangevisuals.player.HitEffects;
import dev.strangevisuals.player.KillEffects;
import dev.strangevisuals.player.TargetMarks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$hitDummy(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (FakePlayer.hit(MinecraftClient.getInstance(), target)) {
            ci.cancel();
            return;
        }

        TargetMarks.noteHit(target);

        KillEffects.watch(target);

        Vec3d impact = MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hit
            && hit.getEntity() == target ? hit.getPos() : null;
        HitEffects.add(target, player.getAttackCooldownProgress(0.5f), impact);
    }
}
