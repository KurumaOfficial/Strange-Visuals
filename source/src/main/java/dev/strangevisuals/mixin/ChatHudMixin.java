package dev.strangevisuals.mixin;

import dev.strangevisuals.anim.Animations;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
        at = @At("HEAD"))
    private void strangevisuals$pushChat(DrawContext context, TextRenderer textRenderer, int currentTick,
                                         int mouseX, int mouseY, boolean focused, boolean hasScreen,
                                         CallbackInfo ci) {
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        float open = Animations.ease(Animations.chat());
        if (open < 0.999f) {
            float bottom = context.getScaledWindowHeight();
            float scale = 0.93f + 0.07f * open;
            matrices.translate(0.0f, bottom);
            matrices.scale(scale, scale);
            matrices.translate(0.0f, -bottom + (1.0f - open) * 26.0f);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
        at = @At("RETURN"))
    private void strangevisuals$popChat(DrawContext context, TextRenderer textRenderer, int currentTick,
                                        int mouseX, int mouseY, boolean focused, boolean hasScreen,
                                        CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
