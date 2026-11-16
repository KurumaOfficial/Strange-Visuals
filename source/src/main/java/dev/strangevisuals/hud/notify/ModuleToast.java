package dev.strangevisuals.hud.notify;

import dev.strangevisuals.ui.Spring;

final class ModuleToast {
    static final float HOLD_MS = 2600.0f;

    final String name;
    final boolean enabled;
    final long createdAt;

    final Spring appear = new Spring(0.0, 1500.0, 34.0);

    Spring stackY;
    boolean closing;
    float width = 60.0f;

    ModuleToast(String name, boolean enabled, long createdAt) {
        this.name = name;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }
}
