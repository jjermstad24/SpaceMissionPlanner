package com.spacemissionplanner.model;

public class Maneuver extends MissionEvent {
    private double dVx;
    private double dVy;
    private double dVz;
    private String referenceFrame = "EME2000";

    public Maneuver(String name) {
        super(name);
    }

    public Maneuver(String name, double dVx, double dVy, double dVz) {
        super(name);
        this.dVx = dVx;
        this.dVy = dVy;
        this.dVz = dVz;
    }

    public Maneuver(String name, double dVx, double dVy, double dVz, String referenceFrame) {
        super(name);
        this.dVx = dVx;
        this.dVy = dVy;
        this.dVz = dVz;
        this.referenceFrame = referenceFrame;
    }

    public double getdVx() { return dVx; }
    public void setdVx(double dVx) { this.dVx = dVx; }

    public double getdVy() { return dVy; }
    public void setdVy(double dVy) { this.dVy = dVy; }

    public double getdVz() { return dVz; }
    public void setdVz(double dVz) { this.dVz = dVz; }

    public String getReferenceFrame() { return referenceFrame; }
    public void setReferenceFrame(String referenceFrame) { this.referenceFrame = referenceFrame; }

    public double getMagnitude() {
        return Math.sqrt(dVx * dVx + dVy * dVy + dVz * dVz);
    }

    @Override
    public String toString() {
        String frame = referenceFrame.equals("EME2000") ? "I" : referenceFrame.equals("LVLH") ? "L" : referenceFrame;
        return String.format("%s (dV=%.1f m/s, %s)", name, getMagnitude(), frame);
    }
}
