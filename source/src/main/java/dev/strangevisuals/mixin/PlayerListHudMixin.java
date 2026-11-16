package dev.strangevisuals.mixin;

import dev.strangevisuals.anim.Animations;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void strangevisuals$pushTab(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard,
                                        ScoreboardObjective objective, CallbackInfo ci) {
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        float open = Animations.ease(Animations.tab());
        if (open < 0.999f) {
            float scale = 0.88f + 0.12f * open;
            matrices.translate(scaledWindowWidth * 0.5f, 0.0f);
            matrices.scale(scale, scale);
            matrices.translate(-scaledWindowWidth * 0.5f, -(1.0f - open) * 34.0f);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void strangevisuals$popTab(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard,
                                       ScoreboardObjective objective, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
