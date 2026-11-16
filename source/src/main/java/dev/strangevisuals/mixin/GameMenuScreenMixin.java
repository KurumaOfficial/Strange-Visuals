package dev.strangevisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "addFeedbackAndBugsButtons", at = @At("HEAD"), cancellable = true)
    private static void removeFeedbackAndBugs(Screen screen, GridWidget.Adder adder, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void customizeGameMenu(CallbackInfo ci) {
        // 1. Remove feedback and bug reporting buttons unconditionally
        List<ClickableWidget> toRemove = new ArrayList<>();
        for (var el : this.children()) {
            if (el instanceof ClickableWidget widget) {
                Text msg = widget.getMessage();
                if (msg != null) {
                    boolean isFeedbackOrBug = false;
                    if (msg.getContent() instanceof TranslatableTextContent ttc) {
                        String k = ttc.getKey();
                        if ("menu.sendFeedback".equals(k) || "menu.reportBugs".equals(k)) {
                            isFeedbackOrBug = true;
                        }
                    }
                    if (!isFeedbackOrBug) {
                        String str = msg.getString();
                        if (str != null) {
                            String lower = str.toLowerCase(Locale.ROOT);
                            if (lower.contains("нашли ошибку")
                                    || lower.contains("оставить отзыв")
                                    || lower.contains("отзыв")
                                    || lower.contains("feedback")
                                    || lower.contains("bugs")
                                    || lower.contains("report bug")) {
                                isFeedbackOrBug = true;
                            }
                        }
                    }
                    if (isFeedbackOrBug) {
                        toRemove.add(widget);
                    }
                }
            }
        }
        for (ClickableWidget w : toRemove) {
            this.remove(w);
        }

        // 2. Server menu customizations: remove player reporting, achievements, and stats; add Keybinds button
        boolean isServer = this.client != null && !this.client.isInSingleplayer();
        if (!isServer) {
            return;
        }

        ClickableWidget returnBtn = null;
        ClickableWidget advBtn = null;
        ClickableWidget statsBtn = null;
        ClickableWidget optionsBtn = null;
        ClickableWidget reportsBtn = null;

        for (var el : new ArrayList<>(this.children())) {
            if (el instanceof ClickableWidget widget) {
                Text msg = widget.getMessage();
                String text = msg != null ? msg.getString().toLowerCase(Locale.ROOT) : "";
                String key = (msg != null && msg.getContent() instanceof TranslatableTextContent ttc) ? ttc.getKey() : "";

                if (key.equals("menu.returnToGame") || text.contains("вернуться в игру") || text.contains("вернуться к игре") || text.contains("return to game")) {
                    returnBtn = widget;
                } else if (key.equals("gui.advancements") || text.contains("достижения") || text.contains("advancement")) {
                    advBtn = widget;
                } else if (key.equals("gui.stats") || text.contains("статистика") || text.contains("statistic") || text.contains("stats")) {
                    statsBtn = widget;
                } else if (key.equals("menu.options") || text.contains("настройки") || text.contains("options")) {
                    optionsBtn = widget;
                } else if (key.equals("menu.playerReporting") || text.contains("жалобы") || text.contains("отчет") || text.contains("player reporting")) {
                    reportsBtn = widget;
                }
            }
        }

        if (advBtn != null) {
            this.remove(advBtn);
        }
        if (statsBtn != null) {
            this.remove(statsBtn);
        }
        if (reportsBtn != null) {
            this.remove(reportsBtn);
        }

        int centerX = this.width / 2;
        int row0Y = returnBtn != null ? returnBtn.getY() : (this.height / 4 + 8);
        int row1Y = advBtn != null ? advBtn.getY() : (row0Y + 24);
        int buttonW = 98;
        int buttonH = 20;

        // Move Options button to Row 1 left
        if (optionsBtn != null) {
            optionsBtn.setX(centerX - 102);
            optionsBtn.setY(row1Y);
        }

        // Add "Макросы" button to Row 1 right
        ButtonWidget macrosBtn = ButtonWidget.builder(
            Text.translatable("strangevisuals.gui.macros"),
            b -> {
                if (this.client != null) {
                    this.client.setScreen(new dev.strangevisuals.ui.MacroScreen(this));
                }
            }
        ).dimensions(centerX + 4, row1Y, buttonW, buttonH).build();

        this.addDrawableChild(macrosBtn);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void strangevisuals$renderPlayerModel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null || this.client.player == null) {
            return;
        }

        int centerX = this.width / 2;
        int rightStart = centerX + 104;
        int availW = this.width - rightStart;

        if (availW < 50) {
            return;
        }

        // Positioned further to the right, comfortably away from the buttons
        int modelCenterX = rightStart + Math.max(76, (int) (availW * 0.48f));
        int modelCenterY = this.height / 2 - 26;

        int size = Math.min(65, Math.max(45, (int) (this.height * 0.22f)));
        int modelW = (int) (size * 1.5f);
        int modelH = (int) (size * 2.4f);

        int x1 = modelCenterX - modelW / 2;
        int x2 = modelCenterX + modelW / 2;
        int y1 = modelCenterY - modelH / 2;
        int y2 = modelCenterY + modelH / 2;

        try {
            InventoryScreen.drawEntity(context, x1, y1, x2, y2, size, 0.0625f, (float) mouseX, (float) mouseY, this.client.player);
        } catch (Throwable t) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("GameMenu player preview draw failed: {}", t.getMessage());
        }
    }
}
