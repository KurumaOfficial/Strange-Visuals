package dev.strangevisuals.clickgui.setting;

public final class KeybindSetting extends Setting {
    private String label;
    private boolean listening;

    public KeybindSetting(String name, String label) {
        super(name);
        this.label = label;
    }

    public String label() {
        return listening ? "..." : label;
    }

    public boolean listening() {
        return listening;
    }

    public void arm() {
        listening = true;
    }

    public void resolve(String keyName) {
        label = keyName;
        listening = false;
    }

    public void clear() {
        label = "NONE";
        listening = false;
    }

    @Override
    public float height() {
        return 13.0f;
    }
}
