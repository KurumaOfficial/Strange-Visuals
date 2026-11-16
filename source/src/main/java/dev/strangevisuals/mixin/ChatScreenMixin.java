package dev.strangevisuals.mixin;

import dev.strangevisuals.anim.Animations;
import dev.strangevisuals.hands.HandEditor;
import dev.strangevisuals.hands.HandPanel;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.NeonEdge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private String strangevisuals$previous = "";

    @Inject(method = "onChatFieldUpdate", at = @At("HEAD"))
    private void strangevisuals$noteTyping(String text, CallbackInfo ci) {
        boolean appended = text.length() == strangevisuals$previous.length() + 1
            && text.startsWith(strangevisuals$previous);
        strangevisuals$previous = text;
        Animations.typed(appended ? text.substring(text.length() - 1) : null);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void strangevisuals$pushInput(DrawContext context, int mouseX, int mouseY, float delta,
                                          CallbackInfo ci) {
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        float open = Animations.ease(Animations.chat());
        if (open < 0.999f) {
            float bottom = context.getScaledWindowHeight();
            matrices.translate(0.0f, bottom);
            matrices.scale(1.0f, 0.35f + 0.65f * open);
            matrices.translate(0.0f, -bottom + (1.0f - open) * 10.0f);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void strangevisuals$popInput(DrawContext context, int mouseX, int mouseY, float delta,
                                         CallbackInfo ci) {
        strangevisuals$typeLight(context);
        context.getMatrices().popMatrix();

        HandEditor.renderHandles(context, mouseX, mouseY);
        HandPanel.render(context, mouseX, mouseY);
        HandEditor.renderOverlay(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$grabHand(Click click, boolean doubled,
                                         CallbackInfoReturnable<Boolean> info) {
        if (HandPanel.press(click.x(), click.y()) || HandEditor.press(click.x(), click.y(), click.button())) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$sizeHand(double mouseX, double mouseY, double horizontal,
                                         double vertical, CallbackInfoReturnable<Boolean> info) {
        if (HandEditor.scroll(mouseX, mouseY, vertical)) {
            info.setReturnValue(true);
        }
    }

    private void strangevisuals$typeLight(DrawContext context) {
        if (chatField == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float left = chatField.getX();
        float right = left + chatField.getWidth();
        float middle = chatField.getY() + chatField.getHeight() * 0.5f;

        float open = Animations.ease(Animations.chat());
        if (open < 0.999f) {
            float sweep = left + (right - left) * open;
            GlowPipeline.line(context, (left + sweep) * 0.5f, middle, sweep - left,
                chatField.getHeight() * 0.9f,
                Math.round(70 * (1.0f - open)) << 24 | NeonEdge.cool() & 0xFFFFFF);
        }

        float pulse = Animations.typePulse();
        if (pulse <= 0.01f) {
            return;
        }
        float caret = Math.min(left + client.textRenderer.getWidth(chatField.getText()), right - 2.0f);
        GlowPipeline.radial(context, caret, middle, 7.0f + pulse * 5.0f,
            Math.round(190 * pulse) << 24 | NeonEdge.cool() & 0xFFFFFF);
    }
}
