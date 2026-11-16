package dev.strangevisuals.hud;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.render.GlyphAtlas;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public final class PotionBlock extends HudElement {
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static final int MAX_PER_ROW = 6;
    private static final float GAP = 5.0f;
    private static final int GHOST_COUNT = 4;

    private GlyphAtlas bold;

    public PotionBlock() {
        super("potion_block", 0.045f, 0.32f);
    }

    private void ensureLoaded() {
        if (bold == null) {
            bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
        }
    }

    private boolean isActiveMode() {
        return PotionLayoutMode.get() == PotionLayoutMode.Mode.BLOCK;
    }

    private int visibleCount() {
        int count = PotionEffectTracker.all().size();
        return count > 0 ? count : (PotionSlot.isEditMode() ? GHOST_COUNT : 0);
    }

    private static int columns(int count) {
        return Math.max(1, Math.min(count, MAX_PER_ROW));
    }

    private static int rows(int count) {
        return Math.max(1, (int) Math.ceil(count / (double) MAX_PER_ROW));
    }

    @Override
    public float width() {
        if (!isActiveMode()) {
            return 0.0f;
        }
        int count = visibleCount();
        if (count == 0) {
            return 0.0f;
        }
        return columns(count) * PotionCapsuleRenderer.CAPSULE_W + (columns(count) - 1) * GAP;
    }

    @Override
    public float height() {
        if (!isActiveMode()) {
            return 0.0f;
        }
        int count = visibleCount();
        if (count == 0) {
            return 0.0f;
        }
        return rows(count) * PotionCapsuleRenderer.CAPSULE_H + (rows(count) - 1) * GAP;
    }

    @Override
    public void render(DrawContext context, float x, float y) {
        if (!isActiveMode()) {
            return;
        }
        ensureLoaded();
        List<Map.Entry<RegistryEntry<StatusEffect>, PotionEffectTracker.EffectState>> entries = PotionEffectTracker.all();

        if (entries.isEmpty()) {
            if (PotionSlot.isEditMode()) {
                for (int i = 0; i < GHOST_COUNT; i++) {
                    float cx = x + (i % MAX_PER_ROW) * (PotionCapsuleRenderer.CAPSULE_W + GAP);
                    float cy = y + (i / MAX_PER_ROW) * (PotionCapsuleRenderer.CAPSULE_H + GAP);
                    PotionCapsuleRenderer.drawGhost(context, bold, cx, cy, i % 2 == 1);
                }
            }
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            float cx = x + (i % MAX_PER_ROW) * (PotionCapsuleRenderer.CAPSULE_W + GAP);
            float cy = y + (i / MAX_PER_ROW) * (PotionCapsuleRenderer.CAPSULE_H + GAP);
            Map.Entry<RegistryEntry<StatusEffect>, PotionEffectTracker.EffectState> entry = entries.get(i);
            PotionCapsuleRenderer.draw(context, bold, cx, cy, entry.getKey(), entry.getValue());
        }
    }
}
