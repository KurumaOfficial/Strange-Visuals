package dev.strangevisuals.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;

import java.util.ArrayList;
import java.util.List;

public final class Pipelines {
    private static final List<RenderPipeline> OWNED = new ArrayList<>();
    private static boolean warmed;

    private Pipelines() {
    }

    public static RenderPipeline register(RenderPipeline pipeline) {
        OWNED.add(pipeline);
        return RenderPipelines.register(pipeline);
    }

    public static void warmUp() {
        if (warmed || RenderSystem.getDevice() == null) {
            return;
        }
        warmed = true;

        BlitPipeline.init();
        CardPipeline.init();
        CirclePipeline.init();
        ColorFieldPipeline.init();
        DownsamplePipeline.init();
        GlassPanelPipeline.init();
        GlassWindowPipeline.init();
        GlowPipeline.init();
        IconPipeline.init();
        PanelPipeline.init();
        RoundedRectPipeline.init();
        SdfPipeline.init();

        for (RenderPipeline pipeline : OWNED) {
            try {
                RenderSystem.getDevice().precompilePipeline(pipeline);
            } catch (RuntimeException failed) {
                dev.strangevisuals.StrangeVisuals.LOGGER.debug("Pipeline precompilation deferred: {}", failed.getMessage());
            }
        }
    }
}
