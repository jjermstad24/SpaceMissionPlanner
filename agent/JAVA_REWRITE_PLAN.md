# Java Rewrite Plan

**Goal:** Build a mission planning application using Java + OreKit + JavaFX

**Starting point:** Clean slate (existing code deleted)

**Initial scope:** Rich GUI with orbit propagation

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Build | Maven |
| IDE | VS Code |
| Physics | OreKit 13.x (jpype or direct) |
| GUI | JavaFX 17 |
| 3D Visualization | JavaFX 3D or custom |
| Time handling | OreKit AbsoluteDate |

---

## Project Structure

```
space-mission-planner/
├── pom.xml                 # Maven config
├── src/
│   └── main/
│       ├── java/com/example/spacemissionplanner/
│       │   ├── Main.java              # Entry point
│       │   ├── OrekitService.java     # Physics wrapper
│       │   ├── MissionModel.java      # Mission state
│       │   ├── ui/
│       │   │   ├── MainController.java
│       │   │   ├── OrbitInputPanel.java
│       │   │   ├── TimelinePanel.java
│       │   │   └── Viewer3D.java
│       │   └── physics/
│       │       └── OrbitPropagator.java
│       └── resources/
│           └── oreekit-data.zip       # OreKit data files
```

---

## Milestones

### Phase 1: Foundation
- [x] Create Maven project with pom.xml
- [x] Add OreKit dependency
- [x] Add JavaFX dependency
- [x] Verify empty shell compiles and runs

### Phase 2: Physics Core
- [x] OreKit initialization
- [x] Create orbit from Keplerian elements
- [x] Run propagator (Keplerian)
- [x] Return trajectory (time, position, velocity)

### Phase 3: Basic GUI
- [x] Main window with JavaFX
- [x] Input panel: semi-major axis, eccentricity, inclination, RAAN, argument of periapsis, true anomaly
- [x] Run button
- [x] Display results in console/log
- [x] Timeline (ListView)
- [x] 2D orbit visualization (Canvas)

### Phase 4: 3D Visualization
- [x] Pure Java 3D renderer (no OpenGL required)
- [x] Draw Earth wireframe
- [x] Draw orbit path
- [x] Draw spacecraft marker (animated)
- [x] Camera controls (X/Y rotation buttons)

### Phase 5: Timeline & Mission Model
- [ ] Timeline panel (list of waypoints)
- [ ] Add/remove waypoints
- [ ] Connect waypoints with coast arcs
- [ ] "Run Mission" executes all waypoints in sequence

---

## OreKit Setup Notes

1. **Installation:** Add to pom.xml:
   ```xml
   <dependency>
       <groupId>org.orekit</groupId>
       <artifactId>orekit</artifactId>
       <version>13.1</version>
   </dependency>
   ```

2. **Data files:** Need `orekit-data.zip` (planetary ephemerides)
   - Download from https://gitlab.orekit.org/orekit/orekit-data
   - Configure via `Orekit.init()` with data path

---

## GUI Layout (Target)

```
┌──────────────────────────────────────────────────────────────┐
│  Menu Bar: File | Mission | Run                              │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────────────┐  ┌────────────────────────────┐ │
│  │   TIMELINE             │  │   3D VIEWER                 │ │
│  │   [Waypoint 1]         │  │                            │ │
│  │   [Waypoint 2]         │  │       (Earth + Orbit)      │ │
│  │   [+ Add Waypoint]     │  │                            │ │
│  │                        │  │                            │ │
│  │                        │  │                            │ │
│  └────────────────────────┘  └────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────┐  │
│  │  ORBIT INPUT (selected waypoint)                     │  │
│  │  a: [    ] e: [    ] i: [    ]                       │  │
│  │  Ω: [    ] ω: [    ] ν: [    ]                       │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│  [Run Mission]  Time: 2025-01-01T00:00:00Z  Status: Ready   │
└──────────────────────────────────────────────────────────────┘
```

---

## Implementation Notes

- Use JavaFX `Line` or `MeshView` for 3D orbit drawing
- Use `Earth` as central body (CelestialBodyFactory.getEarth())
- Use `FramesFactory.getEME2000()` for inertial frame
- Propagator: start with `KeplerianPropagator`, upgrade to `NumericalPropagator` later
- Store waypoints in an `ObservableList` for GUI binding
- Use JavaFX `Task` for background propagation

---

## Next Steps

See `agent/JAVA_REWRITE_PLAN.md` for detailed tasks.
Proceed to Phase 1: Create project structure and verify shell compiles.