package dev.strangevisuals.clickgui.setting;

import dev.strangevisuals.ui.Spring;

import java.util.function.Consumer;

public final class BooleanSetting extends Setting {
    private boolean value;
    private final Consumer<Boolean> onChange;
    public final Spring glow = new Spring(0.0, 900.0, 46.0);

    public BooleanSetting(String name, boolean value) {
        this(name, value, null);
    }

    public BooleanSetting(String name, boolean value, Consumer<Boolean> onChange) {
        super(name);
        this.value = value;
        this.onChange = onChange;
    }

    public boolean value() {
        return value;
    }

    public void toggle() {
        value = !value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public void setValue(boolean value) {
        this.value = value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public void setValueSilently(boolean value) {
        this.value = value;
    }

    @Override
    public float height() {
        return 13.0f;
    }
}
