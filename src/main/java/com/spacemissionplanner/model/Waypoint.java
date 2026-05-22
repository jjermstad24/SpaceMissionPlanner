package com.spacemissionplanner.model;

public class Waypoint {
    private String name;
    private double semiMajorAxis;
    private double eccentricity;
    private double inclination;
    private double raan;
    private double argPeriapsis;
    private double trueAnomaly;

    public Waypoint(String name) {
        this.name = name;
        this.semiMajorAxis = 6871;
        this.eccentricity = 0.0;
        this.inclination = 45.0;
        this.raan = 0.0;
        this.argPeriapsis = 0.0;
        this.trueAnomaly = 0.0;
    }

    public Waypoint(String name, double semiMajorAxis, double eccentricity,
                    double inclination, double raan, double argPeriapsis, double trueAnomaly) {
        this.name = name;
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.inclination = inclination;
        this.raan = raan;
        this.argPeriapsis = argPeriapsis;
        this.trueAnomaly = trueAnomaly;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getSemiMajorAxis() { return semiMajorAxis; }
    public void setSemiMajorAxis(double semiMajorAxis) { this.semiMajorAxis = semiMajorAxis; }

    public double getEccentricity() { return eccentricity; }
    public void setEccentricity(double eccentricity) { this.eccentricity = eccentricity; }

    public double getInclination() { return inclination; }
    public void setInclination(double inclination) { this.inclination = inclination; }

    public double getRaan() { return raan; }
    public void setRaan(double raan) { this.raan = raan; }

    public double getArgPeriapsis() { return argPeriapsis; }
    public void setArgPeriapsis(double argPeriapsis) { this.argPeriapsis = argPeriapsis; }

    public double getTrueAnomaly() { return trueAnomaly; }
    public void setTrueAnomaly(double trueAnomaly) { this.trueAnomaly = trueAnomaly; }

    public String toDisplayString() {
        return String.format("%s (a=%.0f, e=%.2f, i=%.0f°)", name, semiMajorAxis, eccentricity, inclination);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}