package dev.strangevisuals.render;

import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;

public final class SkyRenderer {
    private static float clock;
    private static long lastFrame = System.nanoTime();

    private SkyRenderer() {
    }

    public static void render() {
        if (!ModuleRegistry.shaderSky().enabled()) {
            lastFrame = System.nanoTime();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        long now = System.nanoTime();
        float delta = (float) Math.min((now - lastFrame) / 1_000_000_000.0, 0.1);
        lastFrame = now;
        clock += delta * ModuleRegistry.skySpeed().value();

        ShaderSkyPipeline.draw(clock,
            ModuleRegistry.skyIntensity().value() / 100.0f,
            ModuleRegistry.skyStars().value() / 100.0f,
            ModuleRegistry.skyMode().index(),
            ModuleRegistry.skyScale().value(),
            Theme.skyAccent(), Theme.skySecondary());
    }
}
