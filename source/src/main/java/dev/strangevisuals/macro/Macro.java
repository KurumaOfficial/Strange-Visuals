package dev.strangevisuals.macro;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class Macro {
    private String id;
    private int key;
    private String command;
    private boolean enabled;

    public Macro(int key, String command) {
        this(UUID.randomUUID().toString(), key, command, true);
    }

    public Macro(String id, int key, String command, boolean enabled) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.key = key;
        this.command = command != null ? command : "";
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyDisplayName() {
        if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) {
            return "НЕТ";
        }
        try {
            return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
        } catch (Throwable t) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("Could not get localized key name for GLFW key {}: {}", key, t.getMessage());
            return "KEY " + key;
        }
    }
}
