package dev.strangevisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DirectConnectScreen.class)
public abstract class DirectConnectScreenMixin extends Screen {
    @Shadow @Final private static Text ENTER_IP_TEXT;

    @Shadow private TextFieldWidget addressField;
    @Shadow private ButtonWidget selectServerButton;

    protected DirectConnectScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strangevisuals$centerContent(CallbackInfo ci) {
        int blockH = 114;
        int startY = Math.max(20, (this.height - blockH) / 2);
        int centerX = this.width / 2;
        int boxX = centerX - 100;

        if (this.addressField != null) {
            this.addressField.setX(boxX);
            this.addressField.setY(startY + 34);
        }

        if (this.selectServerButton != null) {
            this.selectServerButton.setX(boxX);
            this.selectServerButton.setY(startY + 66);
        }

        for (var child : this.children()) {
            if (child instanceof ButtonWidget bw && bw != this.selectServerButton) {
                bw.setX(boxX);
                bw.setY(startY + 92);
                break;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$renderCentered(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        super.render(context, mouseX, mouseY, delta);

        int blockH = 114;
        int startY = Math.max(20, (this.height - blockH) / 2);
        int centerX = this.width / 2;
        int boxX = centerX - 100;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, ENTER_IP_TEXT, boxX + 1, startY + 22, 0xFFA0A0A0);

        if (this.addressField != null) {
            this.addressField.render(context, mouseX, mouseY, delta);
        }

        ci.cancel();
    }
}
