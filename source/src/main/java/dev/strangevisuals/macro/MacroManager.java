package dev.strangevisuals.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MacroManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_macros.json");
    private static final Type LIST_TYPE = new TypeToken<List<Macro>>() {}.getType();

    private static final List<Macro> MACROS = new ArrayList<>();

    private MacroManager() {
    }

    public static synchronized void load() {
        MACROS.clear();
        if (!Files.isRegularFile(FILE)) {
            // Provide sensible defaults on first run
            MACROS.add(new Macro(org.lwjgl.glfw.GLFW.GLFW_KEY_H, "/home"));
            MACROS.add(new Macro(org.lwjgl.glfw.GLFW.GLFW_KEY_K, "/spawn"));
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            List<Macro> list = GSON.fromJson(reader, LIST_TYPE);
            if (list != null) {
                MACROS.addAll(list);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to load macros, starting empty: {}", e.getMessage());
        }
    }

    public static synchronized void save() {
        Path parent = FILE.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to create directories for macros: {}", e.getMessage());
                return;
            }
        }
        Path tempFile = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(MACROS, LIST_TYPE, writer);
            }
            try {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.error("Failed to save macros: {}", e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {}
        }
    }

    public static List<Macro> getMacros() {
        return MACROS;
    }

    public static void add(int key, String command) {
        MACROS.add(new Macro(key, command));
        save();
    }

    public static void remove(Macro macro) {
        MACROS.remove(macro);
        save();
    }

    public static void onKeyPressed(int keyCode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null || client.currentScreen != null) {
            return;
        }

        for (Macro macro : MACROS) {
            if (macro.isEnabled() && macro.getKey() == keyCode) {
                String cmd = macro.getCommand().trim();
                if (cmd.isEmpty()) {
                    continue;
                }
                if (cmd.startsWith("/")) {
                    client.getNetworkHandler().sendChatCommand(cmd.substring(1));
                } else {
                    client.getNetworkHandler().sendChatMessage(cmd);
                }
                client.inGameHud.setOverlayMessage(Text.literal("§7[§bМакрос§7] §f" + cmd), false);
            }
        }
    }
}
