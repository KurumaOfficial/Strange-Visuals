package dev.strangevisuals.clickgui.setting;

import java.util.List;
import java.util.function.Consumer;

public final class ModeSetting extends Setting {
    private final List<String> options = new java.util.ArrayList<>();
    private final Consumer<String> onChange;
    private int index;

    public ModeSetting(String name, List<String> options, int index) {
        this(name, options, index, null);
    }

    public ModeSetting(String name, List<String> options, int index, Consumer<String> onChange) {
        super(name);
        if (options != null) {
            this.options.addAll(options);
        }
        this.index = index;
        this.onChange = onChange;
    }

    public void setOptions(List<String> newOptions, String selectedOption) {
        this.options.clear();
        if (newOptions != null) {
            this.options.addAll(newOptions);
        }
        int idx = this.options.indexOf(selectedOption);
        this.index = idx >= 0 ? idx : 0;
    }

    public List<String> options() {
        return options;
    }

    public int index() {
        return index;
    }

    public String current() {
        return options.get(index);
    }

    public String value() {
        return current();
    }

    public void set(String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) {
                select(i);
                return;
            }
        }
    }

    private boolean open;

    public boolean open() {
        return open;
    }

    public void toggleOpen() {
        open = !open;
    }

    public void close() {
        open = false;
    }

    public void select(int option) {
        int oldIndex = this.index;
        index = Math.max(0, Math.min(options.size() - 1, option));
        open = false;
        if (oldIndex != index && onChange != null) {
            onChange.accept(current());
        }
    }

    @Override
    public float height() {
        return 26.0f;
    }

    public float optionRowHeight() {
        return 13.0f;
    }
}
