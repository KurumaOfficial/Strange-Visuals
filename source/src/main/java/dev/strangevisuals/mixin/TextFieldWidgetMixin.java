package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.strangevisuals.anim.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawText"
            + "(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V",
        ordinal = 0))
    private void strangevisuals$fadeInLetter(DrawContext context, TextRenderer renderer, OrderedText text,
                                             int x, int y, int color, boolean shadow,
                                             Operation<Void> original) {
        String letter = Animations.typedLetter();
        float age = Animations.typeAge();
        MinecraftClient client = MinecraftClient.getInstance();
        if (letter == null || age < 0.0f || !(client.currentScreen instanceof ChatScreen)) {
            original.call(context, renderer, text, x, y, color, shadow);
            return;
        }

        int glyph = renderer.getWidth(letter);
        int edge = x + renderer.getWidth(text) - glyph;
        if (glyph <= 0 || edge <= x) {
            original.call(context, renderer, text, x, y, color, shadow);
            return;
        }

        context.enableScissor(x - 2, y - 4, edge, y + renderer.fontHeight + 4);
        original.call(context, renderer, text, x, y, color, shadow);
        context.disableScissor();

        float settle = Animations.ease(age);
        int alpha = Math.round(255 * settle);
        if (alpha <= 2) {
            return;
        }

        Matrix3x2fStack matrices = context.getMatrices();

        Animations.pushScale(matrices, edge + glyph * 0.5f, y + renderer.fontHeight * 0.5f,
            1.0f + (1.0f - settle) * 0.55f);
        matrices.translate(0.0f, -(1.0f - settle) * 2.5f);
        context.drawText(renderer, letter, edge, y, alpha << 24 | color & 0xFFFFFF, shadow);
        matrices.popMatrix();
    }
}
