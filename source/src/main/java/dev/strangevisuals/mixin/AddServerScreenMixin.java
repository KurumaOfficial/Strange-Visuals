package dev.strangevisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AddServerScreen.class)
public abstract class AddServerScreenMixin extends Screen {
    @Shadow @Final private static Text ENTER_NAME_TEXT;
    @Shadow @Final private static Text ENTER_IP_TEXT;

    @Shadow private TextFieldWidget serverNameField;
    @Shadow private TextFieldWidget addressField;
    @Shadow private ButtonWidget addButton;

    protected AddServerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strangevisuals$centerContent(CallbackInfo ci) {
        int blockH = 174;
        int startY = Math.max(16, (this.height - blockH) / 2);
        int centerX = this.width / 2;
        int boxX = centerX - 100;

        if (this.serverNameField != null) {
            this.serverNameField.setX(boxX);
            this.serverNameField.setY(startY + 32);
        }

        if (this.addressField != null) {
            this.addressField.setX(boxX);
            this.addressField.setY(startY + 70);
        }

        for (var child : this.children()) {
            if (child instanceof CyclingButtonWidget<?> cb) {
                cb.setX(boxX);
                cb.setY(startY + 98);
            } else if (child instanceof ButtonWidget bw && bw != this.addButton) {
                bw.setX(boxX);
                bw.setY(startY + 150);
            }
        }

        if (this.addButton != null) {
            this.addButton.setX(boxX);
            this.addButton.setY(startY + 124);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$renderCentered(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        super.render(context, mouseX, mouseY, delta);

        int blockH = 174;
        int startY = Math.max(16, (this.height - blockH) / 2);
        int centerX = this.width / 2;
        int boxX = centerX - 100;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, ENTER_NAME_TEXT, boxX + 1, startY + 20, 0xFFA0A0A0);
        context.drawTextWithShadow(this.textRenderer, ENTER_IP_TEXT, boxX + 1, startY + 58, 0xFFA0A0A0);

        if (this.serverNameField != null) {
            this.serverNameField.render(context, mouseX, mouseY, delta);
        }
        if (this.addressField != null) {
            this.addressField.render(context, mouseX, mouseY, delta);
        }

        ci.cancel();
    }
}
