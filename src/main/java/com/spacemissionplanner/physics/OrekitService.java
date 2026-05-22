package com.spacemissionplanner.physics;

import com.spacemissionplanner.model.CelestialBody;
import com.spacemissionplanner.util.ErrorHandler;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;

import java.util.ArrayList;
import java.util.List;

public class OrekitService {

    private final Frame inertialFrame;
    private final Frame itrf;
    private final java.util.Map<CelestialBody, OneAxisEllipsoid> ellipsoidCache = new java.util.HashMap<>();
    private CelestialBody body = CelestialBody.EARTH;
    private java.util.Set<CelestialBody> gravityEnabled = new java.util.HashSet<>();

    private static final double MOON_A = 384400000;
    private static final double MOON_PERIOD = 27.321661 * 86400;
    private static final double MOON_N = 2 * Math.PI / MOON_PERIOD;
    private static final double MOON_M0 = Math.toRadians(135.0);
    private static final double MOON_I = Math.toRadians(23.44);

    public OrekitService() {
        this.inertialFrame = FramesFactory.getEME2000();
        configureDataLoading();
        this.itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        gravityEnabled.add(CelestialBody.EARTH);
        gravityEnabled.add(CelestialBody.MOON);
    }

    private void configureDataLoading() {
        java.io.File orekitData = new java.io.File("orekit-data");
        if (orekitData.exists() && orekitData.isDirectory()) {
            ErrorHandler.runSafe(() ->
                DataContext.getDefault().getDataProvidersManager().addProvider(
                    new DirectoryCrawler(orekitData)),
                "Load OreKit ephemeris data"
            );
        } else {
            ErrorHandler.info("orekit-data directory not found — using analytic ephemeris");
        }
    }

    public void setCelestialBody(CelestialBody body) {
        this.body = body;
    }

    public CelestialBody getCelestialBody() {
        return body;
    }

    public void setGravityEnabled(CelestialBody body, boolean enabled) {
        if (enabled) {
            gravityEnabled.add(body);
        } else {
            gravityEnabled.remove(body);
        }
    }

    private double getEffectiveGm() {
        if (gravityEnabled.contains(body)) {
            return body.getGm();
        }
        return CelestialBody.EARTH.getGm();
    }

    public Orbit createOrbit(double a, double e, double i, double raan, double argPe, double trueAnomaly, AbsoluteDate date) {
        return new KeplerianOrbit(
            a, e,
            Math.toRadians(i),
            Math.toRadians(raan),
            Math.toRadians(argPe),
            Math.toRadians(trueAnomaly),
            PositionAngleType.TRUE,
            inertialFrame,
            date != null ? date : AbsoluteDate.J2000_EPOCH,
            getEffectiveGm()
        );
    }

    public Orbit createOrbit(double a, double e, double i, double raan, double argPe, double trueAnomaly) {
        return createOrbit(a, e, i, raan, argPe, trueAnomaly, AbsoluteDate.J2000_EPOCH);
    }

    public Propagator createPropagator(Orbit orbit) {
        return new KeplerianPropagator(orbit);
    }

    public List<TrajectoryPoint> propagate(Orbit orbit, double durationSeconds, int steps) {
        Propagator propagator = createPropagator(orbit);
        List<TrajectoryPoint> points = new ArrayList<>();
        double stepSize = durationSeconds / steps;
        AbsoluteDate startDate = orbit.getDate();

        for (int i = 0; i <= steps; i++) {
            AbsoluteDate targetDate = startDate.shiftedBy(i * stepSize);
            SpacecraftState state = propagator.propagate(targetDate);
            PVCoordinates pv = state.getPVCoordinates();

            points.add(new TrajectoryPoint(
                targetDate,
                pv.getPosition().getX(),
                pv.getPosition().getY(),
                pv.getPosition().getZ(),
                pv.getVelocity().getX(),
                pv.getVelocity().getY(),
                pv.getVelocity().getZ()
            ));
        }

        return points;
    }

    public List<TrajectoryPoint> computeCoastArc(TrajectoryPoint endPoint, 
            double targetA, double targetE, double targetI, 
            double targetRaan, double targetArgPe, double targetTa) {
        List<TrajectoryPoint> coastPoints = new ArrayList<>();
        
        double duration = 600;
        int steps = 20;
        double stepSize = duration / steps;
        
        Orbit startOrbit = new CartesianOrbit(
            new PVCoordinates(
                new Vector3D(endPoint.x, endPoint.y, endPoint.z),
                new Vector3D(endPoint.vx, endPoint.vy, endPoint.vz)
            ),
            inertialFrame, endPoint.date, getEffectiveGm()
        );
        
        Propagator propagator = createPropagator(startOrbit);
        
        for (int i = 1; i <= steps; i++) {
            AbsoluteDate targetDate = endPoint.date.shiftedBy(i * stepSize);
            SpacecraftState state = propagator.propagate(targetDate);
            PVCoordinates pv = state.getPVCoordinates();
            
            coastPoints.add(new TrajectoryPoint(
                targetDate,
                pv.getPosition().getX(),
                pv.getPosition().getY(),
                pv.getPosition().getZ(),
                pv.getVelocity().getX(),
                pv.getVelocity().getY(),
                pv.getVelocity().getZ()
            ));
        }
        
        return coastPoints;
    }

    public TrajectoryPoint createTrajectoryPoint(double a, double e, double i, double raan, double argPe, double ta, AbsoluteDate date) {
        Orbit orbit = createOrbit(a, e, i, raan, argPe, ta, date);
        PVCoordinates pv = orbit.getPVCoordinates();
        return new TrajectoryPoint(
            orbit.getDate(),
            pv.getPosition().getX(),
            pv.getPosition().getY(),
            pv.getPosition().getZ(),
            pv.getVelocity().getX(),
            pv.getVelocity().getY(),
            pv.getVelocity().getZ()
        );
    }

    public TrajectoryPoint createTrajectoryPoint(double a, double e, double i, double raan, double argPe, double ta) {
        return createTrajectoryPoint(a, e, i, raan, argPe, ta, AbsoluteDate.J2000_EPOCH);
    }

    public TrajectoryPoint createTrajectoryPointFromInertial(double xKm, double yKm, double zKm,
                                                              double vxKmS, double vyKmS, double vzKmS,
                                                              AbsoluteDate date) {
        AbsoluteDate d = date != null ? date : AbsoluteDate.J2000_EPOCH;
        return new TrajectoryPoint(
            d,
            xKm * 1000, yKm * 1000, zKm * 1000,
            vxKmS * 1000, vyKmS * 1000, vzKmS * 1000
        );
    }

    public TrajectoryPoint createTrajectoryPointFromInertial(double xKm, double yKm, double zKm,
                                                              double vxKmS, double vyKmS, double vzKmS) {
        return createTrajectoryPointFromInertial(xKm, yKm, zKm, vxKmS, vyKmS, vzKmS, AbsoluteDate.J2000_EPOCH);
    }

    public TrajectoryPoint createTrajectoryPointFromECEF(double xKm, double yKm, double zKm,
                                                          double vxKmS, double vyKmS, double vzKmS,
                                                          AbsoluteDate date) {
        try {
            AbsoluteDate d = date != null ? date : AbsoluteDate.J2000_EPOCH;
            Transform t = itrf.getTransformTo(inertialFrame, d);
            PVCoordinates pvECEF = new PVCoordinates(
                new Vector3D(xKm * 1000, yKm * 1000, zKm * 1000),
                new Vector3D(vxKmS * 1000, vyKmS * 1000, vzKmS * 1000)
            );
            PVCoordinates pvEME2000 = t.transformPVCoordinates(pvECEF);
            return new TrajectoryPoint(
                d,
                pvEME2000.getPosition().getX(),
                pvEME2000.getPosition().getY(),
                pvEME2000.getPosition().getZ(),
                pvEME2000.getVelocity().getX(),
                pvEME2000.getVelocity().getY(),
                pvEME2000.getVelocity().getZ()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert ECEF to EME2000", e);
        }
    }

    public TrajectoryPoint createTrajectoryPointFromECEF(double xKm, double yKm, double zKm,
                                                          double vxKmS, double vyKmS, double vzKmS) {
        return createTrajectoryPointFromECEF(xKm, yKm, zKm, vxKmS, vyKmS, vzKmS, AbsoluteDate.J2000_EPOCH);
    }

    public TrajectoryPoint createTrajectoryPointFromLLA(double latDeg, double lonDeg, double altKm, AbsoluteDate date) {
        try {
            OneAxisEllipsoid bodyEllipsoid = ellipsoidCache.computeIfAbsent(body,
                b -> new OneAxisEllipsoid(b.getEllipsoidA(), b.getFlattening(), itrf));
            AbsoluteDate d = date != null ? date : AbsoluteDate.J2000_EPOCH;
            GeodeticPoint geo = new GeodeticPoint(Math.toRadians(latDeg), Math.toRadians(lonDeg), altKm * 1000);
            Vector3D posECEF = bodyEllipsoid.transform(geo);
            PVCoordinates pvECEF = new PVCoordinates(posECEF, Vector3D.ZERO);
            Transform t = itrf.getTransformTo(inertialFrame, d);
            PVCoordinates pvEME2000 = t.transformPVCoordinates(pvECEF);
            return new TrajectoryPoint(
                d,
                pvEME2000.getPosition().getX(),
                pvEME2000.getPosition().getY(),
                pvEME2000.getPosition().getZ(),
                pvEME2000.getVelocity().getX(),
                pvEME2000.getVelocity().getY(),
                pvEME2000.getVelocity().getZ()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert LLA to EME2000", e);
        }
    }

    public TrajectoryPoint createTrajectoryPointFromLLA(double latDeg, double lonDeg, double altKm) {
        return createTrajectoryPointFromLLA(latDeg, lonDeg, altKm, AbsoluteDate.J2000_EPOCH);
    }

    public TrajectoryPoint applyDeltaV(TrajectoryPoint point, double dVx, double dVy, double dVz) {
        return applyDeltaV(point, dVx, dVy, dVz, "EME2000");
    }

    public TrajectoryPoint applyDeltaV(TrajectoryPoint point, double dVx, double dVy, double dVz, String frame) {
        double fx, fy, fz;

        switch (frame) {
            case "LVLH": {
                Vector3D r = new Vector3D(point.x, point.y, point.z);
                Vector3D v = new Vector3D(point.vx, point.vy, point.vz);
                Vector3D R = r.normalize();
                Vector3D N = r.crossProduct(v).normalize();
                Vector3D T = N.crossProduct(R);
                Vector3D dV = new Vector3D(
                    R.getX() * dVx + T.getX() * dVy + N.getX() * dVz,
                    R.getY() * dVx + T.getY() * dVy + N.getY() * dVz,
                    R.getZ() * dVx + T.getZ() * dVy + N.getZ() * dVz
                );
                fx = dV.getX();
                fy = dV.getY();
                fz = dV.getZ();
                break;
            }
            default:
                fx = dVx;
                fy = dVy;
                fz = dVz;
        }

        return new TrajectoryPoint(
            point.date,
            point.x, point.y, point.z,
            point.vx + fx, point.vy + fy, point.vz + fz
        );
    }

    public List<TrajectoryPoint> propagateWithCoast(TrajectoryPoint start, double durationSeconds, int steps) {
        return propagateArc(start, durationSeconds, steps);
    }

    public List<TrajectoryPoint> propagateBackward(TrajectoryPoint start, double durationSeconds, int steps) {
        return propagateArc(start, -durationSeconds, steps);
    }

    public List<TrajectoryPoint> propagateArc(TrajectoryPoint start, double durationSeconds, int steps) {
        List<TrajectoryPoint> points = new ArrayList<>();
        double gm = getEffectiveGm();
        Orbit startOrbit = new CartesianOrbit(
            new PVCoordinates(
                new Vector3D(start.x, start.y, start.z),
                new Vector3D(start.vx, start.vy, start.vz)
            ),
            inertialFrame, start.date, gm
        );
        Propagator propagator = createPropagator(startOrbit);
        double stepSize = durationSeconds / steps;
        for (int i = 1; i <= steps; i++) {
            AbsoluteDate targetDate = start.date.shiftedBy(i * stepSize);
            SpacecraftState state = propagator.propagate(targetDate);
            PVCoordinates pv = state.getPVCoordinates();
            points.add(new TrajectoryPoint(
                targetDate,
                pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
                pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ()
            ));
        }
        return points;
    }

    public TrajectoryPoint propagateToDate(TrajectoryPoint start, AbsoluteDate targetDate) {
        double gm = getEffectiveGm();
        Orbit startOrbit = new CartesianOrbit(
            new PVCoordinates(
                new Vector3D(start.x, start.y, start.z),
                new Vector3D(start.vx, start.vy, start.vz)
            ),
            inertialFrame, start.date, gm
        );
        Propagator propagator = createPropagator(startOrbit);
        SpacecraftState state = propagator.propagate(targetDate);
        PVCoordinates pv = state.getPVCoordinates();
        return new TrajectoryPoint(
            targetDate,
            pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
            pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ()
        );
    }

    public static class MeetResult {
        public final TrajectoryPoint forwardEnd;
        public final TrajectoryPoint backwardEnd;
        public final double positionError;
        public final double velocityError;
        public final double totalError;

        public MeetResult(TrajectoryPoint forwardEnd, TrajectoryPoint backwardEnd) {
            this.forwardEnd = forwardEnd;
            this.backwardEnd = backwardEnd;
            double dx = forwardEnd.x - backwardEnd.x;
            double dy = forwardEnd.y - backwardEnd.y;
            double dz = forwardEnd.z - backwardEnd.z;
            this.positionError = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double dvx = forwardEnd.vx - backwardEnd.vx;
            double dvy = forwardEnd.vy - backwardEnd.vy;
            double dvz = forwardEnd.vz - backwardEnd.vz;
            this.velocityError = Math.sqrt(dvx * dvx + dvy * dvy + dvz * dvz);
            this.totalError = positionError + velocityError * 1000;
        }
    }

    public MeetResult computeMeetError(TrajectoryPoint pointA, TrajectoryPoint pointB) {
        return new MeetResult(pointA, pointB);
    }

    public MeetResult propagateToMeet(TrajectoryPoint forwardPoint, AbsoluteDate forwardTargetDate,
                                       TrajectoryPoint backwardPoint, AbsoluteDate backwardTargetDate) {
        TrajectoryPoint forwardEnd = propagateToDate(forwardPoint, forwardTargetDate);
        TrajectoryPoint backwardEnd = propagateToDate(backwardPoint, backwardTargetDate);
        return new MeetResult(forwardEnd, backwardEnd);
    }

    public static class TransferOptimizerResult {
        public final double[] bestParams;
        public final double bestTotalDeltaV;
        public final double departureDeltaV;
        public final double arrivalDeltaV;
        public final double transferDuration;

        public TransferOptimizerResult(double[] bestParams, double bestTotalDeltaV,
                                        double departureDeltaV, double arrivalDeltaV,
                                        double transferDuration) {
            this.bestParams = bestParams;
            this.bestTotalDeltaV = bestTotalDeltaV;
            this.departureDeltaV = departureDeltaV;
            this.arrivalDeltaV = arrivalDeltaV;
            this.transferDuration = transferDuration;
        }
    }

    public TransferOptimizerResult optimizeTransfer(TrajectoryPoint startPoint, AbsoluteDate startDate,
                                                     TrajectoryPoint endPoint, AbsoluteDate endDate,
                                                     String[] varyParams, double[][] paramBounds,
                                                     int stepsPerParam) {
        double[] best = new double[varyParams.length];
        double bestTotalDV = Double.MAX_VALUE;
        double bestDepDV = 0, bestArrDV = 0, bestDuration = 0;

        int totalSteps = (int) Math.pow(stepsPerParam + 1, varyParams.length);
        for (int s = 0; s < totalSteps; s++) {
            double[] current = new double[varyParams.length];
            int tmp = s;
            for (int i = 0; i < varyParams.length; i++) {
                double range = paramBounds[i][1] - paramBounds[i][0];
                current[i] = paramBounds[i][0] + range * (tmp % (stepsPerParam + 1)) / stepsPerParam;
                tmp /= (stepsPerParam + 1);
            }

            // Create modified end point (first 6 params = Keplerian elements)
            Orbit orbit = new KeplerianOrbit(
                current[0] * 1000,
                current[1],
                current[2],
                current[3],
                current[4],
                current[5],
                PositionAngleType.TRUE,
                inertialFrame,
                endPoint.date,
                getEffectiveGm()
            );
            PVCoordinates pv = orbit.getPVCoordinates();
            TrajectoryPoint modifiedEnd = new TrajectoryPoint(endPoint.date,
                pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ(),
                pv.getVelocity().getX(), pv.getVelocity().getY(), pv.getVelocity().getZ());

            // Propagate start and modified end to meet time
            TrajectoryPoint startAtMeet = propagateToDate(startPoint, startDate);
            TrajectoryPoint endAtMeet = propagateToDate(modifiedEnd, endDate);

            // Use Lambert solver to compute the delta-V for this transfer
            TransferBurnResult burn = computeTransferBurn(startAtMeet, endAtMeet);
            double totalDV = burn.dVMagnitude + burn.arrivalDVMagnitude;

            if (totalDV < bestTotalDV) {
                bestTotalDV = totalDV;
                bestDepDV = burn.dVMagnitude;
                bestArrDV = burn.arrivalDVMagnitude;
                bestDuration = burn.transferDuration;
                System.arraycopy(current, 0, best, 0, current.length);
            }
        }

        return new TransferOptimizerResult(best, bestTotalDV, bestDepDV, bestArrDV, bestDuration);
    }

    // --- Transfer burn (Lambert solver) ---

    public static class TransferBurnResult {
        public final double dVx, dVy, dVz;
        public final double dVMagnitude;
        public final double arrivalDVx, arrivalDVy, arrivalDVz;
        public final double arrivalDVMagnitude;
        public final double transferDuration;

        public TransferBurnResult(Vector3D v1Current, Vector3D v1Transfer,
                                  Vector3D v2Current, Vector3D v2Transfer,
                                  double transferDuration) {
            Vector3D dV1 = v1Transfer.subtract(v1Current);
            Vector3D dV2 = v2Transfer.subtract(v2Current);
            this.dVx = dV1.getX(); this.dVy = dV1.getY(); this.dVz = dV1.getZ();
            this.dVMagnitude = dV1.getNorm();
            this.arrivalDVx = dV2.getX(); this.arrivalDVy = dV2.getY(); this.arrivalDVz = dV2.getZ();
            this.arrivalDVMagnitude = dV2.getNorm();
            this.transferDuration = transferDuration;
        }
    }

    private static double[] stumpff(double z) {
        double C, S;
        if (Math.abs(z) < 1e-8) {
            C = 0.5 - z / 24.0 + z * z / 720.0;
            S = 1.0 / 6.0 - z / 120.0 + z * z / 5040.0;
        } else if (z > 0) {
            double sqrtZ = Math.sqrt(z);
            C = (1 - Math.cos(sqrtZ)) / z;
            S = (sqrtZ - Math.sin(sqrtZ)) / (z * sqrtZ);
        } else {
            double sqrtNegZ = Math.sqrt(-z);
            C = (Math.cosh(sqrtNegZ) - 1) / (-z);
            S = (Math.sinh(sqrtNegZ) - sqrtNegZ) / (-z * sqrtNegZ);
        }
        return new double[]{C, S};
    }

    public TransferBurnResult computeTransferBurn(TrajectoryPoint from, TrajectoryPoint to) {
        double dt = to.date.durationFrom(from.date);
        double absDt = Math.abs(dt);
        double gm = getEffectiveGm();

        Vector3D r1 = new Vector3D(from.x, from.y, from.z);
        Vector3D r2 = new Vector3D(to.x, to.y, to.z);
        Vector3D v1Current = new Vector3D(from.vx, from.vy, from.vz);
        Vector3D v2Current = new Vector3D(to.vx, to.vy, to.vz);

        double r1Mag = r1.getNorm();
        double r2Mag = r2.getNorm();

        if (absDt < 1e-6) {
            return new TransferBurnResult(v1Current, v2Current, v2Current, v1Current, absDt);
        }

        // Transfer angle
        double cosDeltaNu = r1.dotProduct(r2) / (r1Mag * r2Mag);
        cosDeltaNu = Math.max(-1.0, Math.min(1.0, cosDeltaNu));
        double deltaNu = Math.acos(cosDeltaNu);

        // Determine short way / long way from angular momentum direction
        Vector3D h1 = r1.crossProduct(v1Current);
        Vector3D cross = r1.crossProduct(r2);
        boolean longWay = cross.dotProduct(h1) < 0;

        if (longWay) {
            deltaNu = 2 * Math.PI - deltaNu;
        }

        double sinDeltaNu = Math.sin(deltaNu);
        double A = sinDeltaNu * Math.sqrt(r1Mag * r2Mag / (1.0 - Math.cos(deltaNu)));

        // Solve universal variable z via Newton iteration
        double z = 0.0;
        double tol = 1e-12;
        int maxIter = 1000;

        for (int iter = 0; iter < maxIter; iter++) {
            double[] ss = stumpff(z);
            double C = ss[0];
            double S = ss[1];

            if (C <= 1e-12) {
                z += 0.1;
                continue;
            }

            double sqrtC = Math.sqrt(C);
            double y = r1Mag + r2Mag + A * (z * S - 1.0) / sqrtC;

            if (y < 0) {
                z += 0.1;
                continue;
            }

            double sqrtY = Math.sqrt(y);
            double x = sqrtY / sqrtC;
            double t = (x * x * x * S + A * sqrtY) / Math.sqrt(gm);

            double f = 1.0 - y / r1Mag;
            double g = A * sqrtY / Math.sqrt(gm);
            double gDot = 1.0 - y / r2Mag;

            double residual = t - absDt;
            if (Math.abs(residual) < tol || Math.abs(g) < 1e-15) {
                if (Math.abs(g) < 1e-15) {
                    break; // singular transfer
                }
                Vector3D v1Transfer = r2.subtract(r1.scalarMultiply(f)).scalarMultiply(1.0 / g);
                Vector3D v2Transfer = r2.scalarMultiply(gDot).subtract(r1).scalarMultiply(1.0 / g);
                return new TransferBurnResult(v1Current, v1Transfer, v2Current, v2Transfer, absDt);
            }

            // Finite difference Newton step
            double dz = 1e-8;
            double[] ss2 = stumpff(z + dz);
            double C2 = ss2[0];
            double S2 = ss2[1];
            double sqrtC2 = Math.sqrt(C2);
            double y2 = r1Mag + r2Mag + A * ((z + dz) * S2 - 1.0) / sqrtC2;
            double sqrtY2 = Math.sqrt(Math.max(0, y2));
            double x2 = sqrtY2 / sqrtC2;
            double t2 = (x2 * x2 * x2 * S2 + A * sqrtY2) / Math.sqrt(gm);
            double dtdz = (t2 - t) / dz;

            if (Math.abs(dtdz) < 1e-20) {
                z += 0.1;
                continue;
            }

            z = z - residual / dtdz;
        }

        // Fallback: return simple velocity difference
        return new TransferBurnResult(v1Current, v2Current, v2Current, v1Current, absDt);
    }

    public Vector3D getMoonPosition(AbsoluteDate date) {
        AbsoluteDate d = date != null ? date : AbsoluteDate.J2000_EPOCH;
        try {
            org.orekit.bodies.CelestialBody moon = CelestialBodyFactory.getMoon();
            return moon.getPVCoordinates(d, inertialFrame).getPosition();
        } catch (Exception e) {
            ErrorHandler.warn("DE ephemeris unavailable for Moon position, using analytic fallback", e);
            return computeAnalyticMoonPosition(d);
        }
    }

    private Vector3D computeAnalyticMoonPosition(AbsoluteDate d) {
        double m = MOON_M0 + MOON_N * d.durationFrom(AbsoluteDate.J2000_EPOCH);
        double cosM = Math.cos(m);
        double sinM = Math.sin(m);
        return new Vector3D(
            MOON_A * cosM,
            MOON_A * sinM * Math.cos(MOON_I),
            MOON_A * sinM * Math.sin(MOON_I)
        );
    }

    public void getMoonPV(AbsoluteDate date, Vector3D[] outPos, Vector3D[] outVel) {
        AbsoluteDate d = date != null ? date : AbsoluteDate.J2000_EPOCH;
        try {
            org.orekit.bodies.CelestialBody moon = CelestialBodyFactory.getMoon();
            PVCoordinates pv = moon.getPVCoordinates(d, inertialFrame);
            outPos[0] = pv.getPosition();
            outVel[0] = pv.getVelocity();
        } catch (Exception e) {
            ErrorHandler.warn("DE ephemeris unavailable for Moon PV, using analytic fallback", e);
            outPos[0] = computeAnalyticMoonPosition(d);
            outVel[0] = new Vector3D(0, 0, 0);
        }
    }

    public List<TrajectoryPoint> translateToBodyCentered(List<TrajectoryPoint> points,
                                                          CelestialBody relativeBody,
                                                          CelestialBody targetBody) {
        if (relativeBody == targetBody || points == null) return points;
        List<TrajectoryPoint> result = new ArrayList<>();
        Vector3D[] relPosArr = new Vector3D[1];
        Vector3D[] relVelArr = new Vector3D[1];
        Vector3D[] tgtPosArr = new Vector3D[1];
        Vector3D[] tgtVelArr = new Vector3D[1];
        for (TrajectoryPoint p : points) {
            getBodyPV(relativeBody, p.date, relPosArr, relVelArr);
            getBodyPV(targetBody, p.date, tgtPosArr, tgtVelArr);
            result.add(new TrajectoryPoint(p.date,
                p.x + relPosArr[0].getX() - tgtPosArr[0].getX(),
                p.y + relPosArr[0].getY() - tgtPosArr[0].getY(),
                p.z + relPosArr[0].getZ() - tgtPosArr[0].getZ(),
                p.vx + relVelArr[0].getX() - tgtVelArr[0].getX(),
                p.vy + relVelArr[0].getY() - tgtVelArr[0].getY(),
                p.vz + relVelArr[0].getZ() - tgtVelArr[0].getZ()));
        }
        return result;
    }

    private void getBodyPV(CelestialBody body, AbsoluteDate date, Vector3D[] outPos, Vector3D[] outVel) {
        if (body == CelestialBody.EARTH) {
            outPos[0] = Vector3D.ZERO;
            outVel[0] = Vector3D.ZERO;
        } else {
            getMoonPV(date, outPos, outVel);
        }
    }

    public List<TrajectoryPoint> getCelestialBodyTrajectory(CelestialBody body, AbsoluteDate start, double durationSeconds, int steps) {
        List<TrajectoryPoint> points = new ArrayList<>();
        double stepSize = durationSeconds / steps;
        if (body == CelestialBody.EARTH) {
            for (int i = 0; i <= steps; i++) {
                AbsoluteDate d = start.shiftedBy(i * stepSize);
                Vector3D moonPos = getMoonPosition(d);
                points.add(new TrajectoryPoint(d,
                    -moonPos.getX(), -moonPos.getY(), -moonPos.getZ(), 0, 0, 0));
            }
        } else {
            for (int i = 0; i <= steps; i++) {
                AbsoluteDate d = start.shiftedBy(i * stepSize);
                Vector3D pos = getMoonPosition(d);
                points.add(new TrajectoryPoint(d,
                    pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0));
            }
        }
        return points;
    }

    public static class TrajectoryPoint {
        public final AbsoluteDate date;
        public final double x, y, z;
        public final double vx, vy, vz;

        public TrajectoryPoint(AbsoluteDate date, double x, double y, double z,
                               double vx, double vy, double vz) {
            this.date = date;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }

        @Override
        public String toString() {
            return String.format("Point[t=%.1f, pos=(%.1f, %.1f, %.1f) km, vel=(%.1f, %.1f, %.1f) km/s]",
                date.durationFrom(AbsoluteDate.J2000_EPOCH),
                x / 1000, y / 1000, z / 1000,
                vx / 1000, vy / 1000, vz / 1000);
        }
    }

    public static void main(String[] args) {
        OrekitService service = new OrekitService();

        double earthRadius = 6371e3;
        double altitude = 500e3;
        double a = earthRadius + altitude;

        Orbit orbit = service.createOrbit(a, 0.0, 45.0, 0.0, 0.0, 0.0);
        System.out.println("Created orbit: " + orbit);

        List<TrajectoryPoint> trajectory = service.propagate(orbit, 5400, 100);

        System.out.println("\nTrajectory (first 5 points):");
        for (int i = 0; i < 5 && i < trajectory.size(); i++) {
            System.out.println(trajectory.get(i));
        }

        System.out.println("\n... and " + (trajectory.size() - 5) + " more points");
        System.out.println("\nOreKit physics service is working!");
    }
}