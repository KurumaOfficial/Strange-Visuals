package dev.strangevisuals.ui;

public final class Spring {
    private final double stiffness;
    private final double damping;
    private double value;
    private double velocity;

    public Spring(double initial, double stiffness, double damping) {
        this.value = initial;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void step(double target, double deltaSeconds) {
        double dt = Math.min(deltaSeconds, 1.0 / 60.0);
        int steps = Math.max(1, (int) Math.ceil(deltaSeconds / dt));
        for (int i = 0; i < steps; i++) {
            velocity += (target - value) * stiffness * dt;
            velocity -= velocity * Math.min(1.0, damping * dt);
            value += velocity * dt;
        }
        if (Math.abs(target - value) < 0.0005 && Math.abs(velocity) < 0.0005) {
            value = target;
            velocity = 0.0;
        }
    }

    public void reset(double target) {
        value = target;
        velocity = 0.0;
    }

    public double get() {
        return value;
    }
}
