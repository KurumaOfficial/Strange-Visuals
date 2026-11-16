package dev.strangevisuals.render;

import dev.strangevisuals.clickgui.ModuleRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.AbstractWorldRenderContext;
import net.minecraft.client.MinecraftClient;

public final class AmbienceRenderer {
    private record Look(float contrast, int tint, float tintAmount, float glowCutoff) {
    }

    private static final Look[] LOOKS = {
        new Look(1.00f, 0xFFFFFF, 0.00f, 0.72f),
        new Look(1.12f, 0x6E8CFF, 0.30f, 0.60f),
        new Look(1.18f, 0x3F5BD1, 0.48f, 0.52f),
        new Look(1.06f, 0xFFB25E, 0.34f, 0.64f),
        new Look(0.92f, 0xC9A8FF, 0.42f, 0.38f)
    };

    private AmbienceRenderer() {
    }

    public static void render(AbstractWorldRenderContext context) {
        if (!ModuleRegistry.ambience().enabled()) {
            // Release fullscreen VRAM as soon as the module is off (1440p/4K = hundreds of MB).
            AmbiencePipeline.shutdown();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        int choice = Math.max(0, Math.min(LOOKS.length - 1, ModuleRegistry.gradeLook().index()));
        Look look = LOOKS[choice];

        AmbiencePipeline.run(
            ModuleRegistry.gradeExposure().value() / 100.0f,
            look.contrast(),
            ModuleRegistry.gradeSaturation().value() / 100.0f,
            ModuleRegistry.gradeVignette().value() / 100.0f,
            look.tint(),
            look.tintAmount(),
            look.glowCutoff(),
            ModuleRegistry.bloomStrength().value() / 100.0f);
    }
}
