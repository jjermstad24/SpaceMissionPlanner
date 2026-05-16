"""Built-in mission templates (schema v2)."""

from __future__ import annotations

from spacemissionplanner.mission.model import (
    ClockDef,
    Mission,
    MissionEvent,
    StageDef,
    TimeSpec,
    VehicleDef,
)


def default_leo_mission() -> Mission:
    """LEO waypoint + one-hour coast (matches MISSION_SCHEMA example)."""
    return Mission(
        name="example_leo",
        metadata={"scene_origin": "SSB", "default_frame": "J2000"},
        clocks=[
            ClockDef(id="tdb", kind="tdb_since_j2000"),
            ClockDef(id="mission", kind="mission_elapsed", zero_event="launch"),
        ],
        vehicle=VehicleDef(
            id="sc1",
            name="Spacecraft 1",
            stages=[StageDef(id="stage1", Isp_s=320.0, propellant_mass_kg=2000.0, dry_mass_kg=500.0)],
        ),
        events=[
            MissionEvent(id="launch", type="launch", time=TimeSpec(clock="tdb", value=0.0)),
            MissionEvent(
                id="wp0",
                type="waypoint",
                time=TimeSpec(clock="tdb", value=0.0),
                representation="orbital_elements",
                central_body="earth",
                frame="J2000",
                elements={
                    "a_m": 6778000.0,
                    "e": 0.0,
                    "i_rad": 0.9,
                    "raan_rad": 0.0,
                    "argp_rad": 0.0,
                    "nu_rad": 0.0,
                },
            ),
            MissionEvent(
                id="coast1",
                type="coast",
                time=TimeSpec(clock="mission", offset_s=0.0, relative_to="launch"),
                duration_s=3600.0,
                step_s=60.0,
                from_event="wp0",
            ),
        ],
    )


def two_phase_leo_mission() -> Mission:
    """Two coast segments chained through graph edges."""
    m = default_leo_mission()
    m.name = "two_phase_leo"
    m.events.append(
        MissionEvent(
            id="coast2",
            type="coast",
            time=TimeSpec(clock="mission", offset_s=3600.0, relative_to="launch"),
            duration_s=1800.0,
            step_s=60.0,
            from_event="coast1",
        )
    )
    return m
