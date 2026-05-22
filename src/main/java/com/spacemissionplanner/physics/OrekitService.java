package com.spacemissionplanner.physics;

import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class OrekitService {

    private final Frame inertialFrame;
    private static final double EARTH_SEMIMAJOR_AXIS = 6378137.0;
    private static final double EARTH_FLATTENING = 1.0 / 298.257223563;
    private static final double EARTH_GM = 3.986004418e14;

    public OrekitService() {
        this.inertialFrame = FramesFactory.getEME2000();
    }

    public Orbit createOrbit(double a, double e, double i, double raan, double argPe, double trueAnomaly) {
        return new KeplerianOrbit(
            a, e,
            Math.toRadians(i),
            Math.toRadians(raan),
            Math.toRadians(argPe),
            Math.toRadians(trueAnomaly),
            PositionAngleType.TRUE,
            inertialFrame,
            AbsoluteDate.J2000_EPOCH,
            EARTH_GM
        );
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
        
        Orbit startOrbit = new KeplerianOrbit(
            endPoint.x, endPoint.y, endPoint.z,
            endPoint.vx, endPoint.vy, endPoint.vz,
            PositionAngleType.TRUE,
            inertialFrame, endPoint.date, EARTH_GM
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