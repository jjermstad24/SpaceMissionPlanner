package com.spacemissionplanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaypointTest {

    @Test
    void testDefaultConstructor() {
        Waypoint wp = new Waypoint("Test Waypoint");

        assertEquals("Test Waypoint", wp.getName());
        assertEquals(6871, wp.getSemiMajorAxis());
        assertEquals(0.0, wp.getEccentricity());
        assertEquals(45.0, wp.getInclination());
        assertEquals(0.0, wp.getRaan());
        assertEquals(0.0, wp.getArgPeriapsis());
        assertEquals(0.0, wp.getTrueAnomaly());
    }

    @Test
    void testParameterizedConstructor() {
        Waypoint wp = new Waypoint("Custom", 7000, 0.1, 30.0, 45.0, 90.0, 180.0);

        assertEquals("Custom", wp.getName());
        assertEquals(7000, wp.getSemiMajorAxis());
        assertEquals(0.1, wp.getEccentricity());
        assertEquals(30.0, wp.getInclination());
        assertEquals(45.0, wp.getRaan());
        assertEquals(90.0, wp.getArgPeriapsis());
        assertEquals(180.0, wp.getTrueAnomaly());
    }

    @Test
    void testSetters() {
        Waypoint wp = new Waypoint("Test");
        
        wp.setSemiMajorAxis(8000);
        wp.setEccentricity(0.05);
        wp.setInclination(60.0);
        wp.setRaan(15.0);
        wp.setArgPeriapsis(30.0);
        wp.setTrueAnomaly(270.0);

        assertEquals(8000, wp.getSemiMajorAxis());
        assertEquals(0.05, wp.getEccentricity());
        assertEquals(60.0, wp.getInclination());
        assertEquals(15.0, wp.getRaan());
        assertEquals(30.0, wp.getArgPeriapsis());
        assertEquals(270.0, wp.getTrueAnomaly());
    }

    @Test
    void testToString() {
        Waypoint wp = new Waypoint("Test WP", 7000, 0.1, 45.0, 0.0, 0.0, 0.0);
        String display = wp.toString();

        assertTrue(display.contains("Test WP"));
        assertTrue(display.contains("a=7000"));
        assertTrue(display.contains("e=0.10"));
        assertTrue(display.contains("i=45"));
    }

    @Test
    void testNameSetter() {
        Waypoint wp = new Waypoint("Original");
        
        wp.setName("Renamed");
        
        assertEquals("Renamed", wp.getName());
    }
}