package dev.strangevisuals.clickgui.setting;

public final class SliderSetting extends Setting {
    private final float min;
    private final float max;
    private final float step;
    private final String suffix;
    private float value;

    public SliderSetting(String name, float min, float max, float step, float value, String suffix) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = value;
        this.suffix = suffix;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public float value() {
        return value;
    }

    public float fraction() {
        return (value - min) / (max - min);
    }

    public void setFraction(float fraction) {
        float raw = min + fraction * (max - min);
        float stepped = Math.round(raw / step) * step;
        value = Math.max(min, Math.min(max, stepped));
    }

    public void setValue(float value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public String display() {
        String number = step < 1.0f ? String.format("%.2f", value) : String.valueOf((int) value);
        return suffix == null ? number : number + suffix;
    }

    @Override
    public float height() {
        return 20.0f;
    }
}
