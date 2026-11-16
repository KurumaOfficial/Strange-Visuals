package dev.strangevisuals;

import dev.strangevisuals.account.AccountStorage;
import dev.strangevisuals.clickgui.ClickGuiScreen;
import dev.strangevisuals.clickgui.classic.ClassicGuiScreen;
import dev.strangevisuals.cosmetic.Cosmetics;
import dev.strangevisuals.hands.HandPose;
import dev.strangevisuals.render.AmbienceRenderer;
import dev.strangevisuals.render.ScreenFilterRenderer;
import dev.strangevisuals.render.HitBubbleRenderer;
import dev.strangevisuals.render.KillShaderRenderer;
import dev.strangevisuals.render.PanoramaManager;
import dev.strangevisuals.render.RainFall;
import dev.strangevisuals.render.RainRenderer;
import dev.strangevisuals.clickgui.MenuCloseEffect;
import dev.strangevisuals.clickgui.ModuleInfo;
import dev.strangevisuals.clickgui.ModuleRegistry;
import dev.strangevisuals.mixin.FogRendererAccessor;
import dev.strangevisuals.player.FakePlayer;
import dev.strangevisuals.player.HitEffects;
import dev.strangevisuals.player.KillEffects;
import dev.strangevisuals.hud.NameTags;
import dev.strangevisuals.player.TargetMarks;
import dev.strangevisuals.clickgui.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Core entry point for the StrangeVisuals client mod (Minecraft 1.21.11 / Fabric).
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Sound and keybinding registration</li>
 *   <li>Configuration, account, and hand pose persistence lifecycle</li>
 *   <li>World and post-processing render pipeline attachments</li>
 *   <li>Client tick event handling and module keybind polling</li>
 * </ul>
 */
public final class StrangeVisuals implements ClientModInitializer {
    public static final String MOD_ID = "strangevisuals";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("StrangeVisuals");

    public static final Identifier OPEN_SOUND_ID = Identifier.of(MOD_ID, "ui.open");
    public static final Identifier BACK_SOUND_ID = Identifier.of(MOD_ID, "ui.back");
    public static final Identifier TOGGLE_ON_SOUND_ID = Identifier.of(MOD_ID, "ui.toggle_on");
    public static final Identifier TOGGLE_OFF_SOUND_ID = Identifier.of(MOD_ID, "ui.toggle_off");
    public static final SoundEvent OPEN_SOUND = SoundEvent.of(OPEN_SOUND_ID);
    public static final SoundEvent BACK_SOUND = SoundEvent.of(BACK_SOUND_ID);
    public static final SoundEvent TOGGLE_ON_SOUND = SoundEvent.of(TOGGLE_ON_SOUND_ID);
    public static final SoundEvent TOGGLE_OFF_SOUND = SoundEvent.of(TOGGLE_OFF_SOUND_ID);

    public static final KeyBinding.Category STRANGEVISUALS_CATEGORY =
        KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding clickGuiKey;
    private static KeyBinding switchPanoramaKey;
    private static boolean fogSuppressed;

    public static KeyBinding getSwitchPanoramaKey() {
        return switchPanoramaKey;
    }

    @Override
    public void onInitializeClient() {
        Registry.register(Registries.SOUND_EVENT, OPEN_SOUND_ID, OPEN_SOUND);
        Registry.register(Registries.SOUND_EVENT, BACK_SOUND_ID, BACK_SOUND);
        Registry.register(Registries.SOUND_EVENT, TOGGLE_ON_SOUND_ID, TOGGLE_ON_SOUND);
        Registry.register(Registries.SOUND_EVENT, TOGGLE_OFF_SOUND_ID, TOGGLE_OFF_SOUND);
        AccountStorage.load();
        dev.strangevisuals.macro.MacroManager.load();
        HandPose.load();
        ConfigManager.load();
        dev.strangevisuals.ui.editor.MainMenuConfig.load();
        ModuleRegistry.syncFromMenuConfig();

        LOGGER.info("[StrangeVisuals] Initialized successfully. Active Style: {}",
            dev.strangevisuals.ui.style.StyleManager.getStyle().displayName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.save();
            dev.strangevisuals.macro.MacroManager.save();
            dev.strangevisuals.ui.editor.MainMenuConfig.save();
            AccountStorage.save();
            HandPose.save();
        }, "strangevisuals-shutdown"));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TargetMarks.clear();
            KillEffects.clear();
            FakePlayer.clear();
            // Free fullscreen VRAM: every post pipeline holds its own scene-size
            // buffers (Ambience x4, Rain, Kill, Bubble, Filter, Hands x4, HUD glass).
            // Without this they survive world switches at 1440p/4K indefinitely.
            try {
                dev.strangevisuals.render.AmbiencePipeline.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown AmbiencePipeline: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.RainRenderer.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown RainRenderer: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.KillShaderRenderer.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown KillShaderRenderer: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.HitBubbleRenderer.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown HitBubbleRenderer: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.ScreenFilterRenderer.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown ScreenFilterRenderer: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.ScreenCapture.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown ScreenCapture: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.render.HudGlassCapture.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown HudGlassCapture: {}", t.getMessage());
            }
            try {
                dev.strangevisuals.hands.HandOutline.shutdown();
            } catch (Throwable t) {
                LOGGER.warn("Failed to shutdown HandOutline: {}", t.getMessage());
            }
            ConfigManager.save();
        });

        clickGuiKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.strangevisuals.clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT, STRANGEVISUALS_CATEGORY));

        switchPanoramaKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.strangevisuals.next_panorama", GLFW.GLFW_KEY_INSERT, STRANGEVISUALS_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(StrangeVisuals::onTick);
        WorldRenderEvents.AFTER_ENTITIES.register(MenuCloseEffect::render);
        WorldRenderEvents.AFTER_ENTITIES.register(Cosmetics::render);
        WorldRenderEvents.AFTER_ENTITIES.register(RainFall::render);
        WorldRenderEvents.AFTER_ENTITIES.register(TargetMarks::render);
        WorldRenderEvents.AFTER_ENTITIES.register(HitEffects::render);
        WorldRenderEvents.AFTER_ENTITIES.register(KillEffects::render);
        WorldRenderEvents.AFTER_ENTITIES.register(NameTags::capture);
        WorldRenderEvents.END_MAIN.register(RainRenderer::render);
        WorldRenderEvents.END_MAIN.register(HitBubbleRenderer::render);
        WorldRenderEvents.END_MAIN.register(KillShaderRenderer::render);
        WorldRenderEvents.END_MAIN.register(AmbienceRenderer::render);
        WorldRenderEvents.END_MAIN.register(ScreenFilterRenderer::renderWorld);
    }

    private static void onTick(MinecraftClient client) {
        PanoramaManager.tick();

        while (switchPanoramaKey.wasPressed()) {
            PanoramaManager.triggerTransition();
        }

        while (clickGuiKey.wasPressed()) {
            if (client.currentScreen instanceof ClassicGuiScreen classic) {
                classic.close();
            } else if (client.currentScreen instanceof ClickGuiScreen beta) {
                beta.close();
            } else if (client.currentScreen == null) {
                if (ModuleRegistry.isClassicGui()) {
                    client.setScreen(new ClassicGuiScreen());
                } else {
                    client.setScreen(new ClickGuiScreen());
                }
            }
        }
        updateFog();
        FakePlayer.tick(client);
    }

    private static void updateFog() {
        boolean wanted = ModuleRegistry.shaderSky().enabled()
            && !(ModuleRegistry.ambience().enabled() && ModuleRegistry.ambienceFog().value());
        if (wanted == fogSuppressed) {
            return;
        }
        fogSuppressed = wanted;
        FogRendererAccessor.setFogEnabled(!wanted);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
