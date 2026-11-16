package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.render.GlassPanelPipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.PanelPipeline;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.ui.Spring;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public final class HotbarHud {
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final int SLOTS = 9;
    private static final float SLOT_W = 20.0f;
    private static final float BAR_H = 20.0f;
    private static final float PADDING = 4.0f;
    private static final float BOTTOM_MARGIN = 2.0f;
    private static final float NUMBER_SIZE = 7.2f;
    private static final float ITEM_LIFT = 4.0f;
    private static final float OFFHAND_GAP = 6.0f;

    private static final Spring[] slotFocus = new Spring[SLOTS];
    private static final Spring indicatorX = new Spring(0.0, 1000.0, 46.0);

    private static GlyphAtlas bold;
    private static long lastFrame = System.nanoTime();
    private static boolean initialized;

    private HotbarHud() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }

        long now = System.nanoTime();
        float deltaSeconds = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        PlayerInventory inventory = client.player.getInventory();
        int selected = inventory.getSelectedSlot();

        if (!initialized) {
            for (int i = 0; i < SLOTS; i++) {
                slotFocus[i] = new Spring(i == selected ? 1.0 : 0.0, 1200.0, 76.0);
            }
            indicatorX.step(selected, 0.0);
            initialized = true;
        }
        for (int i = 0; i < SLOTS; i++) {
            slotFocus[i].step(i == selected ? 1.0 : 0.0, deltaSeconds);
        }
        indicatorX.step(selected, deltaSeconds);

        float totalWidth = PADDING * 2.0f + SLOTS * SLOT_W;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        float x = (screenWidth - totalWidth) * 0.5f;
        float y = screenHeight - BOTTOM_MARGIN - BAR_H;

        HudSurface.panel(context, "hotbar", x, y, totalWidth, BAR_H, 1.0f);

        float indicatorCenterX = x + PADDING + ((float) indicatorX.get() + 0.5f) * SLOT_W;
        RoundedRectPipeline.draw(context, indicatorCenterX - SLOT_W * 0.5f + 1.5f, y + 2.0f,
            SLOT_W - 3.0f, BAR_H - 4.0f, argb(0xFFFFFF, 14));
        context.fill(Math.round(indicatorCenterX - 5.0f), Math.round(y + BAR_H - 4.0f),
            Math.round(indicatorCenterX + 5.0f), Math.round(y + BAR_H - 3.0f),
            argb(NeonEdge.cool(), 235));

        for (int i = 0; i < SLOTS; i++) {
            float focus = (float) slotFocus[i].get();
            float slotCenterX = x + PADDING + (i + 0.5f) * SLOT_W;
            float centerY = y + BAR_H * 0.5f;

            ItemStack stack = inventory.getStack(i);
            boolean empty = stack.isEmpty();

            if (empty && focus < 0.98f) {
                MsdfText.drawCenteredOn(context, bold, String.valueOf(i + 1), slotCenterX, centerY,
                    NUMBER_SIZE, argb(0xFFFFFF, Math.round(34 * (1.0f - focus))));
            }

            if (!empty) {
                float itemY = centerY - 8.0f - focus * ITEM_LIFT;
                if (focus > 0.02f) {
                    GlowPipeline.radial(context, slotCenterX, itemY + 8.0f, 11.0f, argb(0xFFFFFF, Math.round(45 * focus)));
                }
                int itemX = Math.round(slotCenterX - 8.0f);
                context.drawItem(stack, itemX, Math.round(itemY));
                overlay(context, client, stack, itemX, Math.round(itemY), centerY);
            }
        }

        renderOffhand(context, client, x, y, totalWidth);
    }

    private static void overlay(DrawContext context, MinecraftClient client, ItemStack stack,
                                int itemX, int itemY, float slotCenterY) {
        if (stack.getCount() > 1) {
            String count = String.valueOf(stack.getCount());
            MsdfText.drawRightOn(context, bold, count, itemX + 17.0f, itemY + 14.0f, 6.6f,
                argb(0xFFFFFF, 235));
        }

        float cooldown = client.player == null ? 0.0f
            : client.player.getItemCooldownManager().getCooldownProgress(stack,
                client.getRenderTickCounter().getTickProgress(true));
        if (cooldown > 0.0f) {
            int top = Math.round(itemY + 16.0f - 16.0f * cooldown);
            context.fill(itemX, top, itemX + 16, itemY + 16, argb(0xFFFFFF, 90));
        }

        if (!stack.isDamageable() || !stack.isDamaged()) {
            return;
        }
        float left = Math.max(0.0f, 1.0f - stack.getDamage() / (float) stack.getMaxDamage());

        float barHeight = 13.0f;
        float barX = itemX + 17.0f;
        float barTop = slotCenterY - barHeight * 0.5f;
        PanelPipeline.draw(context, barX, barTop, 1.5f, barHeight, argb(0x000000, 130));
        float filled = Math.max(1.0f, barHeight * left);
        int tone = left > 0.28f ? 0xFFFFFF : Theme.lighten(NeonEdge.warm(), 0.1f);
        PanelPipeline.draw(context, barX, barTop + barHeight - filled, 1.5f, filled,
            argb(tone, left > 0.28f ? 210 : 245));
    }

    private static void renderOffhand(DrawContext context, MinecraftClient client, float barX, float barY, float barWidth) {
        ItemStack offhand = client.player.getOffHandStack();
        if (offhand.isEmpty()) {
            return;
        }

        float islandSize = BAR_H;
        Arm mainArm = client.player.getMainArm();
        float islandX = mainArm == Arm.RIGHT ? barX - OFFHAND_GAP - islandSize : barX + barWidth + OFFHAND_GAP;

        HudSurface.plate(context, "hotbar", islandX, barY, islandSize, islandSize, 1.0f);

        int itemX = Math.round(islandX + islandSize * 0.5f - 8.0f);
        int itemY = Math.round(barY + islandSize * 0.5f - 8.0f);
        GlowPipeline.radial(context, islandX + islandSize * 0.5f, barY + islandSize * 0.5f, 11.0f, argb(0xFFFFFF, 30));
        context.drawItem(offhand, itemX, itemY);
        overlay(context, client, offhand, itemX, itemY, barY + islandSize * 0.5f);
    }

    private static int argb(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
