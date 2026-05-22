package com.spacemissionplanner.model;

public enum CelestialBody {
    EARTH("Earth", 3.986004418e14, 6371000.0, 6378137.0, 1.0 / 298.257223563),
    MOON("Moon", 4.9028695e12, 1737400.0, 1737400.0, 0.0);

    private final String displayName;
    private final double gm;
    private final double meanRadius;
    private final double ellipsoidA;
    private final double flattening;

    CelestialBody(String displayName, double gm, double meanRadius, double ellipsoidA, double flattening) {
        this.displayName = displayName;
        this.gm = gm;
        this.meanRadius = meanRadius;
        this.ellipsoidA = ellipsoidA;
        this.flattening = flattening;
    }

    public String getDisplayName() { return displayName; }
    public double getGm() { return gm; }
    public double getMeanRadius() { return meanRadius; }
    public double getEllipsoidA() { return ellipsoidA; }
    public double getFlattening() { return flattening; }
}
