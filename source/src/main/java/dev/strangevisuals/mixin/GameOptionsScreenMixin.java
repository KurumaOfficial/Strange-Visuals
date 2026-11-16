package dev.strangevisuals.mixin;

import dev.strangevisuals.i18n.I18n;
import dev.strangevisuals.ui.editor.MainMenuConfig;
import dev.strangevisuals.ui.editor.MainMenuEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptionsScreen.class)
public abstract class GameOptionsScreenMixin extends Screen {
    @Shadow @Final public ThreePartsLayoutWidget layout;

    protected GameOptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initFooter", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$addVideoFooter(CallbackInfo ci) {
        if (!((Object) this instanceof VideoOptionsScreen)) {
            return;
        }

        ci.cancel();
        this.layout.setFooterHeight(60);

        DirectionalLayoutWidget footerLayout = DirectionalLayoutWidget.vertical();
        footerLayout.spacing(4);

        DirectionalLayoutWidget row1 = DirectionalLayoutWidget.horizontal();
        row1.spacing(8);

        // 1. Cycle UI Style (Beta <-> Vanilla)
        ButtonWidget toggleBtn = ButtonWidget.builder(strangevisuals$getToggleMessage(), btn -> {
            MainMenuConfig cfg = MainMenuConfig.get();
            dev.strangevisuals.ui.style.MenuStyle current = dev.strangevisuals.ui.style.StyleManager.getStyle();
            dev.strangevisuals.ui.style.MenuStyle next = current == dev.strangevisuals.ui.style.MenuStyle.BETA
                ? dev.strangevisuals.ui.style.MenuStyle.VANILLA
                : dev.strangevisuals.ui.style.MenuStyle.BETA;
            dev.strangevisuals.ui.style.StyleManager.setStyle(next);
            cfg.applyPreset(next);
            cfg.save();
            dev.strangevisuals.clickgui.ModuleRegistry.syncFromMenuConfig();
            btn.setMessage(strangevisuals$getToggleMessage());
        }).width(150).build();
        toggleBtn.setTooltip(Tooltip.of(Text.literal(I18n.get("strangevisuals.video.style_toggle_tooltip"))));
        row1.add(toggleBtn);

        // 2. Menu Settings & Style Editor button (Bright Green)
        ButtonWidget editorBtn = ButtonWidget.builder(
                Text.literal("★ " + I18n.get("strangevisuals.video.editor_btn")).formatted(Formatting.GREEN),
                btn -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.setScreen(new MainMenuEditorScreen(this, false));
                    }
                }
        ).width(150).build();
        editorBtn.setTooltip(Tooltip.of(Text.literal(I18n.get("strangevisuals.video.editor_tooltip"))));
        row1.add(editorBtn);

        footerLayout.add(row1, p -> p.alignHorizontalCenter());

        // 3. Centered Done button
        ButtonWidget doneBtn = ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close()).width(200).build();
        footerLayout.add(doneBtn, p -> p.alignHorizontalCenter());

        this.layout.addFooter(footerLayout);
    }

    @Unique
    private static Text strangevisuals$getToggleMessage() {
        dev.strangevisuals.ui.style.MenuStyle style = dev.strangevisuals.ui.style.StyleManager.getStyle();
        return Text.literal("★ Style: " + style.displayName()).formatted(Formatting.GREEN);
    }
}
