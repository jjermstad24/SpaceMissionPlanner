package com.spacemissionplanner.physics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.orbits.Orbit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrekitServiceTest {

    private OrekitService service;
    private static final double EARTH_RADIUS_KM = 6371.0;

    @BeforeEach
    void setUp() {
        service = new OrekitService();
    }

    @Test
    void testCreateOrbit() {
        double a = (EARTH_RADIUS_KM + 500) * 1000;
        Orbit orbit = service.createOrbit(a, 0.0, 45.0, 0.0, 0.0, 0.0);

        assertNotNull(orbit);
        assertEquals(a, orbit.getA());
        assertEquals(0.0, orbit.getE(), 1e-10);
        assertEquals(Math.toRadians(45.0), orbit.getI(), 1e-10);
    }

    @Test
    void testPropagate() {
        double a = (EARTH_RADIUS_KM + 500) * 1000;
        Orbit orbit = service.createOrbit(a, 0.0, 45.0, 0.0, 0.0, 0.0);

        List<OrekitService.TrajectoryPoint> trajectory = service.propagate(orbit, 5400, 100);

        assertNotNull(trajectory);
        assertEquals(101, trajectory.size());

        OrekitService.TrajectoryPoint first = trajectory.get(0);
        assertNotNull(first.date);
        assertNotNull(first.x);
        assertNotNull(first.y);
        assertNotNull(first.z);
    }

    @Test
    void testPropagateCircularOrbit() {
        double a = (EARTH_RADIUS_KM + 400) * 1000;
        Orbit orbit = service.createOrbit(a, 0.0, 0.0, 0.0, 0.0, 0.0);

        List<OrekitService.TrajectoryPoint> trajectory = service.propagate(orbit, 5600, 50);

        assertNotNull(trajectory);
        assertEquals(51, trajectory.size());

        double x0 = trajectory.get(0).x;
        double y0 = trajectory.get(0).y;
        double z0 = trajectory.get(0).z;
        double r0 = Math.sqrt(x0*x0 + y0*y0 + z0*z0);

        double lastX = trajectory.get(trajectory.size() - 1).x;
        double lastY = trajectory.get(trajectory.size() - 1).y;
        double lastZ = trajectory.get(trajectory.size() - 1).z;
        double rLast = Math.sqrt(lastX*lastX + lastY*lastY + lastZ*lastZ);

        assertEquals(r0, rLast, a * 0.01);
    }

    @Test
    void testMultipleOrbits() {
        double a1 = (EARTH_RADIUS_KM + 400) * 1000;
        Orbit orbit1 = service.createOrbit(a1, 0.0, 45.0, 0.0, 0.0, 0.0);
        List<OrekitService.TrajectoryPoint> traj1 = service.propagate(orbit1, 3000, 30);
        assertEquals(31, traj1.size());

        double a2 = (EARTH_RADIUS_KM + 600) * 1000;
        Orbit orbit2 = service.createOrbit(a2, 0.1, 60.0, 30.0, 45.0, 90.0);
        List<OrekitService.TrajectoryPoint> traj2 = service.propagate(orbit2, 3600, 40);
        assertEquals(41, traj2.size());

        assertNotSame(traj1, traj2);
    }

    @Test
    void testTrajectoryPoint() {
        List<OrekitService.TrajectoryPoint> trajectory = service.propagate(
            service.createOrbit((EARTH_RADIUS_KM + 500) * 1000, 0.0, 45.0, 0.0, 0.0, 0.0),
            100, 10
        );

        OrekitService.TrajectoryPoint point = trajectory.get(0);
        String str = point.toString();

        assertNotNull(str);
        assertTrue(str.contains("Point"));
    }
}