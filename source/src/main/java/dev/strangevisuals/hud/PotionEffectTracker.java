package dev.strangevisuals.hud;

import dev.strangevisuals.ui.Spring;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PotionEffectTracker {
    static final int SEPARATE_SLOT_COUNT = 4;
    static final int BLOCK_CAP = 12;

    private static final Map<RegistryEntry<StatusEffect>, EffectState> states = new LinkedHashMap<>();
    private static long lastFrame = System.nanoTime();
    private static final List<Map.Entry<RegistryEntry<StatusEffect>, EffectState>> visible = new ArrayList<>(BLOCK_CAP);

    private PotionEffectTracker() {
    }

    static void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float deltaSeconds = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        if (client.player != null) {
            for (StatusEffectInstance instance : client.player.getStatusEffects()) {
                RegistryEntry<StatusEffect> type = instance.getEffectType();
                EffectState state = states.get(type);
                if (state == null) {
                    state = new EffectState();
                    states.put(type, state);
                }
                state.active = true;
                int newDuration = instance.getDuration();

                if (newDuration > state.duration || state.maxDuration <= 0) {
                    state.maxDuration = newDuration;
                }
                state.duration = newDuration;
                state.amplifier = instance.getAmplifier();
                state.harmful = type.value().getCategory() == StatusEffectCategory.HARMFUL;
            }
        }

        Iterator<EffectState> iterator = states.values().iterator();
        while (iterator.hasNext()) {
            EffectState state = iterator.next();
            state.appear.step(state.active ? 1.0 : 0.0, deltaSeconds);
            if (!state.active && state.appear.get() <= 0.01) {
                iterator.remove();
                continue;
            }
            state.active = false;
        }

        visible.clear();
        for (Map.Entry<RegistryEntry<StatusEffect>, EffectState> entry : states.entrySet()) {
            if (visible.size() >= BLOCK_CAP) {
                break;
            }
            visible.add(entry);
        }
    }

    static Map.Entry<RegistryEntry<StatusEffect>, EffectState> entryAt(int index) {
        return index < visible.size() ? visible.get(index) : null;
    }

    static List<Map.Entry<RegistryEntry<StatusEffect>, EffectState>> all() {
        return visible;
    }

    static final class EffectState {
        final Spring appear = new Spring(0.0, 1300.0, 42.0);
        boolean active;
        int duration;
        int maxDuration;
        int amplifier;
        boolean harmful;
    }
}
