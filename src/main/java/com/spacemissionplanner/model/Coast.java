package com.spacemissionplanner.model;

public class Coast extends MissionEvent {
    private double duration;
    private double stepSize;

    public Coast(String name) {
        super(name);
        this.duration = 600;
        this.stepSize = 60;
    }

    public Coast(String name, double duration, double stepSize) {
        super(name);
        this.duration = duration;
        this.stepSize = stepSize;
    }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public double getStepSize() { return stepSize; }
    public void setStepSize(double stepSize) { this.stepSize = stepSize; }

    @Override
    public String toString() {
        if (duration >= 3600) {
            return String.format("%s (%.1f h)", name, duration / 3600);
        }
        return String.format("%s (%.0f s)", name, duration);
    }
}
