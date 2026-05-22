package com.spacemissionplanner.model;

public class Vehicle extends MissionEvent {
    private CelestialBody celestialBody = CelestialBody.EARTH;
    private String referenceFrame = "ORBITAL_ELEMENTS";

    private double semiMajorAxis;
    private double eccentricity;
    private double inclination;
    private double raan;
    private double argPeriapsis;
    private double trueAnomaly;

    private double inertialX, inertialY, inertialZ;
    private double inertialVx, inertialVy, inertialVz;

    private double ecefX, ecefY, ecefZ;
    private double ecefVx, ecefVy, ecefVz;

    private double latitude, longitude, altitude;

    public Vehicle(String name) {
        super(name);
        this.semiMajorAxis = 6871;
        this.eccentricity = 0.0;
        this.inclination = 45.0;
        this.raan = 0.0;
        this.argPeriapsis = 0.0;
        this.trueAnomaly = 0.0;
    }

    public Vehicle(String name, double semiMajorAxis, double eccentricity,
                    double inclination, double raan, double argPeriapsis, double trueAnomaly) {
        super(name);
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.inclination = inclination;
        this.raan = raan;
        this.argPeriapsis = argPeriapsis;
        this.trueAnomaly = trueAnomaly;
    }

    public CelestialBody getCelestialBody() { return celestialBody; }
    public void setCelestialBody(CelestialBody celestialBody) { this.celestialBody = celestialBody; }

    public String getReferenceFrame() { return referenceFrame; }
    public void setReferenceFrame(String referenceFrame) { this.referenceFrame = referenceFrame; }

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

    public double getInertialX() { return inertialX; }
    public void setInertialX(double v) { inertialX = v; }
    public double getInertialY() { return inertialY; }
    public void setInertialY(double v) { inertialY = v; }
    public double getInertialZ() { return inertialZ; }
    public void setInertialZ(double v) { inertialZ = v; }
    public double getInertialVx() { return inertialVx; }
    public void setInertialVx(double v) { inertialVx = v; }
    public double getInertialVy() { return inertialVy; }
    public void setInertialVy(double v) { inertialVy = v; }
    public double getInertialVz() { return inertialVz; }
    public void setInertialVz(double v) { inertialVz = v; }

    public double getEcefX() { return ecefX; }
    public void setEcefX(double v) { ecefX = v; }
    public double getEcefY() { return ecefY; }
    public void setEcefY(double v) { ecefY = v; }
    public double getEcefZ() { return ecefZ; }
    public void setEcefZ(double v) { ecefZ = v; }
    public double getEcefVx() { return ecefVx; }
    public void setEcefVx(double v) { ecefVx = v; }
    public double getEcefVy() { return ecefVy; }
    public void setEcefVy(double v) { ecefVy = v; }
    public double getEcefVz() { return ecefVz; }
    public void setEcefVz(double v) { ecefVz = v; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { latitude = v; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { longitude = v; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double v) { altitude = v; }

    private String frameAbbrev() {
        switch (referenceFrame) {
            case "INERTIAL": return "I";
            case "ECEF":     return "E";
            case "LLA":      return "G";
            default:         return "K";
        }
    }

    @Override
    public String toString() {
        String abbrev = frameAbbrev();
        switch (referenceFrame) {
            case "INERTIAL":
                return String.format("%s [%s] (%.0f, %.0f, %.0f km)", name, abbrev, inertialX, inertialY, inertialZ);
            case "ECEF":
                return String.format("%s [%s] (%.0f, %.0f, %.0f km)", name, abbrev, ecefX, ecefY, ecefZ);
            case "LLA":
                return String.format("%s [%s] (%.1f\u00B0, %.1f\u00B0, %.0f km)", name, abbrev, latitude, longitude, altitude);
            default:
                return String.format("%s [%s] (a=%.0f, e=%.2f, i=%.0f\u00B0)", name, abbrev, semiMajorAxis, eccentricity, inclination);
        }
    }
}
