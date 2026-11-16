package dev.strangevisuals.clickgui.setting;

public abstract class Setting {
    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract float height();
}
