package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.strangevisuals.ui.AltManagerScreen;
import dev.strangevisuals.ui.MainMenuScreen;
import dev.strangevisuals.ui.style.MenuStyle;
import dev.strangevisuals.ui.style.StyleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final Identifier CUSTOM_EDITION_TEXTURE = Identifier.of("strangevisuals", "textures/gui/vanilla/edition.png");

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$replace(CallbackInfo ci) {
        if (StyleManager.getStyle() != MenuStyle.BETA) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof MainMenuScreen)) {
            client.send(() -> client.setScreen(new MainMenuScreen()));
        }
        ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strangevisuals$customizeVanillaTitleScreen(CallbackInfo ci) {
        if (StyleManager.getStyle() != MenuStyle.VANILLA) {
            return;
        }

        int buttonShiftY = 18;
        List<Element> toRemove = new ArrayList<>();
        int realmsX = this.width / 2 - 100;
        int realmsY = this.height / 4 + 48 + 48;
        int realmsW = 200;
        int realmsH = 20;
        boolean foundRealms = false;

        for (Element child : new ArrayList<>(this.children())) {
            // 1. Mojang copyright text at bottom right
            if (child instanceof PressableTextWidget) {
                toRemove.add(child);
                continue;
            }

            if (child instanceof ClickableWidget widget) {
                Text msg = widget.getMessage();
                String str = msg != null ? msg.getString().toLowerCase(Locale.ROOT) : "";

                if (str.contains("mojang") || str.contains("copyright")) {
                    toRemove.add(child);
                    continue;
                }

                // 2. Accessibility button
                boolean isAccessibility = false;
                if (msg != null && msg.getContent() instanceof TranslatableTextContent ttc) {
                    String key = ttc.getKey();
                    if ("narrator.button.accessibility".equals(key) || "options.accessibility".equals(key)) {
                        isAccessibility = true;
                    }
                }
                if (!isAccessibility && widget instanceof TextIconButtonWidget && widget.getX() > this.width / 2) {
                    isAccessibility = true;
                }
                if (!isAccessibility && (str.contains("accessibility") || str.contains("доступност") || str.contains("доступність"))) {
                    isAccessibility = true;
                }
                if (isAccessibility) {
                    toRemove.add(child);
                    continue;
                }

                // 3. Minecraft Realms button -> replace with Alt Manager
                boolean isRealms = false;
                if (msg != null && msg.getContent() instanceof TranslatableTextContent ttc) {
                    if ("menu.online".equals(ttc.getKey())) {
                        isRealms = true;
                    }
                }
                if (!isRealms && (str.contains("realms") || str.contains("реалмс"))) {
                    isRealms = true;
                }

                if (isRealms) {
                    foundRealms = true;
                    realmsX = widget.getX();
                    realmsY = widget.getY();
                    realmsW = widget.getWidth();
                    realmsH = widget.getHeight();
                    toRemove.add(child);
                    continue;
                }

                // Shift main menu buttons downwards to accommodate enlarged 2x logo
                if (widget.getY() >= this.height / 4 + 30 && widget.getY() < this.height - 30) {
                    widget.setY(widget.getY() + buttonShiftY);
                }
            }
        }

        for (Element el : toRemove) {
            this.remove(el);
        }

        // Add Alt Manager button in place of Realms (shifted downwards by buttonShiftY)
        if (foundRealms) {
            ButtonWidget altManagerBtn = ButtonWidget.builder(
                Text.translatable("strangevisuals.menu.alt_manager"),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new AltManagerScreen(this));
                    }
                }
            ).dimensions(realmsX, realmsY + buttonShiftY, realmsW, realmsH).build();
            this.strangevisuals$altManagerBtn = altManagerBtn;
            this.addDrawableChild(altManagerBtn);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private ButtonWidget strangevisuals$altManagerBtn;

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V")
    )
    private void strangevisuals$drawScaledLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha, Operation<Void> original) {
        if (StyleManager.getStyle() != MenuStyle.VANILLA) {
            original.call(logoDrawer, context, screenWidth, alpha);
            return;
        }

        // Make logo 1.5x bigger (up to 1.5x, clamped gracefully to avoid screen clipping)
        float targetScale = 1.5f;
        float maxScaleWidth = (screenWidth - 24.0f) / 256.0f;
        float maxScaleHeight = (this.height * 0.35f) / 52.0f;
        float scale = Math.max(1.0f, Math.min(targetScale, Math.min(maxScaleWidth, maxScaleHeight)));

        float a = logoDrawer.shouldIgnoreAlpha() ? 1.0f : alpha;
        int color = ColorHelper.getWhite(a);

        // 1. Draw scaled Minecraft logo
        int logoW = (int) (256 * scale);
        int logoH = (int) (44 * scale);
        int logoX = screenWidth / 2 - logoW / 2;
        int logoY = Math.max(16, (int) (24 * scale - 12));

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            LogoDrawer.LOGO_TEXTURE,
            logoX, logoY,
            0.0f, 0.0f,
            logoW, logoH,
            256, 44,
            256, 64,
            color
        );

        // 2. Draw custom "STRANGE VISUALS" edition texture
        int editionW = (int) (160 * scale);
        int editionH = (int) (16 * scale);
        int editionX = screenWidth / 2 - editionW / 2;
        int editionY = (int) (logoY + 37 * scale);

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            CUSTOM_EDITION_TEXTURE,
            editionX, editionY,
            0.0f, 0.0f,
            editionW, editionH,
            160, 16,
            256, 32,
            color
        );
    }

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V")
    )
    private void strangevisuals$suppressSplashText(SplashTextRenderer splash, DrawContext context, int screenWidth, TextRenderer textRenderer, float alpha, Operation<Void> original) {
        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            // Suppress splash text to keep the 2x logo clean and uncluttered
            return;
        }
        original.call(splash, context, screenWidth, textRenderer, alpha);
    }

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V")
    )
    private void strangevisuals$drawVersionText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, Operation<Void> original) {
        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            // Strip any "(модифицировано)", "(modded)", or parenthesized mod status
            String cleanText = text.replaceAll("(?i)\\s*\\([^)]*(?:modded|модифиц)[^)]*\\)", "")
                                   .replace("(модифицировано)", "")
                                   .replace("(модифицированная)", "")
                                   .replace("(Modded)", "")
                                   .replaceAll("\\s+", " ")
                                   .trim();
            String svText = cleanText.endsWith(" SV") ? cleanText : cleanText + " SV";
            original.call(context, textRenderer, svText, x, y, color);
            return;
        }
        original.call(context, textRenderer, text, x, y, color);
    }

    @Inject(method = "isRealmsNotificationsGuiDisplayed", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$hideRealmsNotifications(CallbackInfoReturnable<Boolean> cir) {
        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            cir.setReturnValue(false);
        }
    }


    @Inject(method = "render", at = @At("TAIL"))
    private void strangevisuals$renderActiveAccountUnderButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (StyleManager.getStyle() != MenuStyle.VANILLA) {
            return;
        }
        ClickableWidget target = this.strangevisuals$altManagerBtn;
        if (target == null) {
            for (var child : this.children()) {
                if (child instanceof ClickableWidget cw) {
                    Text msg = cw.getMessage();
                    if (msg != null) {
                        String s = msg.getString().toLowerCase(Locale.ROOT);
                        if (s.contains("аккаунт") || s.contains("акаунт") || s.contains("alt manager")) {
                            target = cw;
                            break;
                        }
                    }
                }
            }
        }
        if (target != null) {
            String name = this.client != null && this.client.getSession() != null
                    ? this.client.getSession().getUsername() : "Player";
            int textY = target.getY() + target.getHeight() + 2;
            context.drawCenteredTextWithShadow(this.textRenderer, "§a" + name, this.width / 2, textY, 0xFF55FF55);
        }
    }
}
