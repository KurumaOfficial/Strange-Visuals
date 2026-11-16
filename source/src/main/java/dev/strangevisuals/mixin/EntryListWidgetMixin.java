package dev.strangevisuals.mixin;

import dev.strangevisuals.ui.style.MenuStyle;
import dev.strangevisuals.ui.style.StyleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    @Inject(method = "drawMenuListBackground", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$customListBackground(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && StyleManager.getStyle() == MenuStyle.VANILLA) {
            EntryListWidget<?> self = (EntryListWidget<?>) (Object) this;
            context.fill(self.getX(), self.getY(), self.getRight(), self.getBottom(), client.world == null ? 0x33000000 : 0x44000000);
            ci.cancel();
        }
    }
}
