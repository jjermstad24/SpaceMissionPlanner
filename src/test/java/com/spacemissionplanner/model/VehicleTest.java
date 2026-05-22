package com.spacemissionplanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    @Test
    void testDefaultConstructor() {
        Vehicle v = new Vehicle("Test Vehicle");

        assertEquals("Test Vehicle", v.getName());
        assertEquals(6871, v.getSemiMajorAxis());
        assertEquals(0.0, v.getEccentricity());
        assertEquals(45.0, v.getInclination());
        assertEquals(0.0, v.getRaan());
        assertEquals(0.0, v.getArgPeriapsis());
        assertEquals(0.0, v.getTrueAnomaly());
    }

    @Test
    void testParameterizedConstructor() {
        Vehicle v = new Vehicle("Custom", 7000, 0.1, 30.0, 45.0, 90.0, 180.0);

        assertEquals("Custom", v.getName());
        assertEquals(7000, v.getSemiMajorAxis());
        assertEquals(0.1, v.getEccentricity());
        assertEquals(30.0, v.getInclination());
        assertEquals(45.0, v.getRaan());
        assertEquals(90.0, v.getArgPeriapsis());
        assertEquals(180.0, v.getTrueAnomaly());
    }

    @Test
    void testSetters() {
        Vehicle v = new Vehicle("Test");

        v.setSemiMajorAxis(8000);
        v.setEccentricity(0.05);
        v.setInclination(60.0);
        v.setRaan(15.0);
        v.setArgPeriapsis(30.0);
        v.setTrueAnomaly(270.0);

        assertEquals(8000, v.getSemiMajorAxis());
        assertEquals(0.05, v.getEccentricity());
        assertEquals(60.0, v.getInclination());
        assertEquals(15.0, v.getRaan());
        assertEquals(30.0, v.getArgPeriapsis());
        assertEquals(270.0, v.getTrueAnomaly());
    }

    @Test
    void testToString() {
        Vehicle v = new Vehicle("Test Vehicle", 7000, 0.1, 45.0, 0.0, 0.0, 0.0);
        String display = v.toString();

        assertTrue(display.contains("Test Vehicle"));
        assertTrue(display.contains("a=7000"));
        assertTrue(display.contains("e=0.10"));
        assertTrue(display.contains("i=45"));
    }

    @Test
    void testNameSetter() {
        Vehicle v = new Vehicle("Original");

        v.setName("Renamed");

        assertEquals("Renamed", v.getName());
    }
}
