package dev.strangevisuals.hud;

import net.minecraft.client.gui.DrawContext;

public abstract class HudElement {
    private final String id;
    private final float defaultFractionX;
    private final float defaultFractionY;

    protected float fractionX;
    protected float fractionY;

    protected HudElement(String id, float defaultFractionX, float defaultFractionY) {
        this.id = id;
        this.defaultFractionX = defaultFractionX;
        this.defaultFractionY = defaultFractionY;
        this.fractionX = defaultFractionX;
        this.fractionY = defaultFractionY;
    }

    public String id() {
        return id;
    }

    public void loadFraction(float[] stored) {
        if (stored != null && stored.length == 2) {
            fractionX = stored[0];
            fractionY = stored[1];
        } else {
            fractionX = defaultFractionX;
            fractionY = defaultFractionY;
        }
    }

    public float[] toStoredFraction() {
        return new float[]{fractionX, fractionY};
    }

    public abstract float width();

    public abstract float height();

    public abstract void render(DrawContext context, float x, float y);

    public float centerX(int screenWidth) {
        return fractionX * screenWidth;
    }

    public float centerY(int screenHeight) {
        return fractionY * screenHeight;
    }

    public void moveTo(float centerX, float centerY, int screenWidth, int screenHeight) {
        fractionX = centerX / screenWidth;
        fractionY = centerY / screenHeight;
    }
}
