package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.hands.HandEditor;
import dev.strangevisuals.render.BackgroundPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.PanoramaManager;
import dev.strangevisuals.ui.Theme;
import dev.strangevisuals.ui.editor.CustomTextureLoader;
import dev.strangevisuals.ui.editor.MainMenuConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "init()V", at = @At("HEAD"))
    private void strangevisuals$playOpenSound(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                || screen instanceof PackScreen || screen instanceof SelectWorldScreen
                || screen instanceof GameMenuScreen
                || screen instanceof dev.strangevisuals.ui.AltManagerScreen) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSoundManager() != null) {
                client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.OPEN_SOUND, 0.9f, 1.0f));
            }
        }
    }

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$clearViewWhilePlacing(DrawContext context, CallbackInfo ci) {
        if (HandEditor.holding()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$customBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = (Screen) (Object) this;

        if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            if (screen instanceof GameMenuScreen) {
                context.fillGradient(0, 0, width, height, 0x88000000, 0xAA000000);
                ci.cancel();
                return;
            }
            if (screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                    || screen instanceof PackScreen || screen instanceof SelectWorldScreen
                    || screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
                    || screen instanceof dev.strangevisuals.ui.AltManagerScreen
                    || screen instanceof net.minecraft.client.gui.screen.world.CreateWorldScreen
                    || screen instanceof net.minecraft.client.gui.screen.world.EditWorldScreen
                    || screen instanceof net.minecraft.client.gui.screen.multiplayer.AddServerScreen
                    || screen instanceof net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen) {
                if (client.world != null) {
                    context.fillGradient(0, 0, width, height, 0x99000000, 0xCC000000);
                } else {
                    PanoramaManager.render(context, screen, width, height, delta);
                    context.fillGradient(0, 0, width, height, 0x1A000000, 0x30000000);
                }
                ci.cancel();
                return;
            }
            return;
        }

        if (screen instanceof GameMenuScreen) {
            if (!MainMenuConfig.get().customPauseScreen) {
                return;
            }
            // In-game pause menu: frosted glass blur & dynamic theme ambient glow over the world
            context.fillGradient(0, 0, width, height, 0x8805070B, 0xAA080B12);
            GlowPipeline.radial(context, width * 0.5f, height * 0.5f, width * 0.45f, (0x25 << 24) | (Theme.accent() & 0xFFFFFF));
            ci.cancel();
            return;
        }

        if (screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                || screen instanceof PackScreen || screen instanceof SelectWorldScreen
                || screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
                || screen instanceof dev.strangevisuals.ui.AltManagerScreen) {
            if (!MainMenuConfig.get().customSettingsScreen) {
                return;
            }
            if (client.world != null) {
                // In-game settings: world blur & dark glass overlay with theme accent glow
                context.fillGradient(0, 0, width, height, 0x9905070B, 0xCC080B12);
                GlowPipeline.radial(context, width * 0.5f, height * 0.5f, width * 0.45f, (0x22 << 24) | (Theme.accent() & 0xFFFFFF));
            } else {
                // Title screen / main menu settings: animated shader or custom photo
                MainMenuConfig menuCfg = MainMenuConfig.get();
                if (menuCfg.backgroundMode == MainMenuConfig.BackgroundMode.CUSTOM_IMAGE) {
                    Identifier customTex = CustomTextureLoader.getCustomTexture();
                    if (customTex != null) {
                        context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height, 0xFFFFFFFF);
                    } else {
                        BackgroundPipeline.draw(1.0f);
                    }
                    int alpha = Math.round(menuCfg.imageDarkening * 255);
                    context.fillGradient(0, 0, width, height, (alpha << 24), (alpha << 24) | 0x05070A);
                } else {
                    BackgroundPipeline.draw(1.0f);
                }

                if (menuCfg.shaderOverlayEnabled && menuCfg.shaderEffect != MainMenuConfig.ShaderEffect.NONE) {
                    int glowAlpha = Math.round(menuCfg.shaderIntensity * 60);
                    GlowPipeline.radial(context, width * 0.5f, height * 0.5f, width * 0.45f, (glowAlpha << 24) | (Theme.accent() & 0xFFFFFF));
                }
                context.fillGradient(0, 0, width, height, 0x44000000, 0x88020406);
            }
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "renderPanoramaBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(Lnet/minecraft/client/gui/DrawContext;IIZ)V")
    )
    private void strangevisuals$wrapPanoramaRender(RotatingCubeMapRenderer originalRenderer, DrawContext context, int width, int height, boolean rotate, Operation<Void> original) {
        PanoramaManager.render(context, width, height, 0.0f);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$handleScreenKey(net.minecraft.client.input.KeyInput input, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (StrangeVisuals.getSwitchPanoramaKey() != null && StrangeVisuals.getSwitchPanoramaKey().matchesKey(input)) {
            Screen self = (Screen) (Object) this;
            if (self.getFocused() instanceof net.minecraft.client.gui.widget.TextFieldWidget) {
                return;
            }
            PanoramaManager.triggerTransition();
            cir.setReturnValue(true);
        }
    }
}
