package dev.strangevisuals.mixin;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.i18n.I18n;
import dev.strangevisuals.render.BackgroundPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.RoundedRectPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageScreen.class)
public abstract class MessageScreenMixin extends Screen {
    @Shadow private NarratedMultilineTextWidget textWidget;

    @Unique private static final Identifier LOGO = StrangeVisuals.id("textures/gui/logo.png");
    @Unique private static final Identifier TITLE_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    @Unique private static final Identifier TITLE_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");
    @Unique private static final Identifier TEXT_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    @Unique private static final Identifier TEXT_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");

    @Unique private GlyphAtlas strangevisuals$titleFont;
    @Unique private GlyphAtlas strangevisuals$textFont;

    protected MessageScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strangevisuals$hideVanillaTextWidget(CallbackInfo ci) {
        if (!dev.strangevisuals.ui.editor.MainMenuConfig.get().customLoadingScreen) {
            return;
        }
        if (this.textWidget != null) {
            this.textWidget.visible = false;
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$customRenderSaving(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!dev.strangevisuals.ui.editor.MainMenuConfig.get().customLoadingScreen) {
            return;
        }
        if (strangevisuals$titleFont == null) {
            dev.strangevisuals.render.Pipelines.warmUp();
            strangevisuals$titleFont = GlyphAtlas.load(TITLE_FONT_DATA, TITLE_FONT_ATLAS);
            strangevisuals$textFont = GlyphAtlas.load(TEXT_FONT_DATA, TEXT_FONT_ATLAS);
        }

        long time = System.currentTimeMillis();
        if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            strangevisuals$renderVanillaSaving(context, delta, time);
            ci.cancel();
            return;
        }

        BackgroundPipeline.draw(1.0f);
        context.fillGradient(0, 0, width, height, 0x66000000, 0x99020406);

        float cx = width * 0.5f;
        float cy = height * 0.44f;

        // Stable ambient glow - smooth slow breathing, 0 tremble
        float glowRadius = 75.0f + (float) Math.sin(time * 0.001) * 6.0f;
        GlowPipeline.radial(context, cx, cy - 24.0f, glowRadius, withAlpha(NeonEdge.cool(), 50));

        // Crisp stable logo - no jittering
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO,
            Math.round(cx - 23.0f), Math.round(cy - 47.0f), 0.0f, 0.0f,
            46, 46, 120, 120, 120, 120, 0xFFFFFFFF);

        if (strangevisuals$titleFont != null) {
            String titleText = this.getTitle() != null && !this.getTitle().getString().isBlank()
                ? this.getTitle().getString() : I18n.get("saving.title");
            MsdfText.drawCentered(context, strangevisuals$titleFont, titleText, cx, cy + 18.0f, 11.5f, 0xFFFFFFFF);
        }
        if (strangevisuals$textFont != null) {
            String subText = I18n.get("saving.subtitle");
            MsdfText.drawCentered(context, strangevisuals$textFont, subText, cx, cy + 34.0f, 7.8f, NeonEdge.cool());
        }

        // Sleek sliding indeterminate bar
        float barW = 220.0f;
        float barH = 4.0f;
        float barX = cx - barW * 0.5f;
        float barY = cy + 48.0f;
        RoundedRectPipeline.draw(context, barX, barY, barW, barH, 0xEE141822);
        RoundedRectPipeline.drawOutline(context, barX, barY, barW, barH, 0x44252E3E);

        float sweep = (float) ((time * 0.0009) % 1.0);
        float beamW = barW * 0.35f;
        float beamX = barX + sweep * (barW - beamW);
        RoundedRectPipeline.draw(context, beamX, barY, beamW, barH, NeonEdge.cool());

        ci.cancel();
    }

    @Unique
    private void strangevisuals$renderVanillaSaving(DrawContext context, float delta, long time) {
        try {
            this.renderPanoramaBackground(context, delta);
            context.fillGradient(0, 0, width, height, 0x1A000000, 0x30000000);
        } catch (Throwable t) {
            dev.strangevisuals.render.VanillaStylePipeline.renderDirtBackground(context, width, height);
        }

        float cx = width * 0.5f;
        float cy = height * 0.44f;

        int iconSize = 24;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, dev.strangevisuals.render.VanillaStylePipeline.ICONS_VIDEO,
            Math.round(cx - iconSize * 0.5f), Math.round(cy - 38.0f), 0.0f, 40.0f,
            iconSize, iconSize, 20, 20, 240, 60, 0xFFFFFFFF);

        String titleText = this.getTitle() != null && !this.getTitle().getString().isBlank()
            ? this.getTitle().getString() : I18n.get("saving.title");
        context.drawCenteredTextWithShadow(textRenderer, titleText, Math.round(cx), Math.round(cy), 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, I18n.get("saving.subtitle"), Math.round(cx), Math.round(cy + 16.0f), 0xFFFFFFA0);

        int barW = 200;
        int barH = 12;
        int barX = Math.round(cx - barW * 0.5f);
        int barY = Math.round(cy + 34.0f);
        context.fill(barX, barY, barX + barW, barY + barH, 0xFF000000);
        dev.strangevisuals.render.VanillaStylePipeline.drawBorder(context, barX, barY, barW, barH, 0xFF555555);

        float sweep = (float) ((time * 0.0009) % 1.0);
        int beamW = (int) (barW * 0.35f);
        int beamX = barX + 1 + (int) (sweep * (barW - 2 - beamW));
        context.fill(beamX, barY + 1, beamX + beamW, barY + barH - 1, 0xFF55FF55);
    }

    @Unique
    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
