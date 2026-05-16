# UI Design — Mission Timeline & 3D Scene

This document is the **product north star** for the desktop app and notebook parity. Implementation should follow `AGENTS.md` (backend-first, no physics in Qt).

---

## 1. User goals

The user works in a **time-aware solar-system scene** and builds a **mission as an ordered sequence of events**:

1. See **major bodies** at a chosen **epoch** (planets, Moon, etc.).
2. Define **waypoints** and **maneuvers** using familiar input formats.
3. Generate a **trajectory** that connects those constraints (coast segments first; solvers later).
4. Model **staging** with physical parameters (Isp, mass, ΔV).
5. Scrub time and inspect state in 3D (and eventually on a ground track).

---

## 2. Layout (target)

```text
┌──────────────────────────────────────────────────────────────────┐
│ Mission: <name>   Clock: [TDB ▼]  Epoch/value: [____]  Run Export │
├─────────────┬──────────────────────────────┬─────────────────────┤
│  Timeline   │      3D viewport             │  Inspector          │
│             │  bodies @ scene epoch        │  (selected event)   │
│ ▼ Vehicle   │  full trajectory (faint)     │                     │
│   Stage 1   │  highlight to scrub time     │  Representation:    │
│ ▼ Sequence  │  waypoint / burn glyphs      │  [ECI ▼] (v1 only)  │
│   Waypoint  │                              │  fields…            │
│   Burn      │                              │  Derived (readonly) │
│   Coast     │                              │                     │
├─────────────┴──────────────────────────────┴─────────────────────┤
│ Time scrubber ─────────●──────────────────────────────────────────│
└──────────────────────────────────────────────────────────────────┘
```

**Left — Timeline** is the primary editor (not a raw JSON graph).  
**Center — 3D** is the primary spatial view.  
**Right — Inspector** edits the selected timeline item.  
**Bottom — Scrubber** drives ephemeris, trajectory index, and glyphs together.

A **ground track** view (2D map / lat-lon plot) is a **later** dock or tab—not required for v1.

---

## 3. Clocks and time (decision record)

### 3.1 Multiple clock **kinds**

The UI and mission file must support **pluggable clock kinds** for display and for tagging events. v1 implements a subset; schema reserves extensibility.

| Clock kind | Meaning | Typical use |
|------------|---------|-------------|
| `tdb_since_j2000` | Seconds since J2000 TDB | SPICE, ephemeris, backend canonical |
| `utc` | UTC calendar datetime | Operations, reports |
| `mission_elapsed` | Seconds since a defined zero event | “T+”, stage timing |
| `et` | Ephemeris time (SPICE ET) | Kernel-native interchange |

**Rule:** Backend propagation and `StateVector` epochs remain **TDB-relative seconds** internally until a dedicated clock service exists in C++. The GUI converts for display and converts user input back to canonical epoch on **Apply**.

### 3.2 Event times use the same clock options

Each timeline event carries a `TimeSpec`:

- **Absolute:** `{ "clock": "tdb_since_j2000", "value": 0.0 }`
- **Relative:** `{ "clock": "mission_elapsed", "offset_s": 3600, "relative_to": "launch" }`

The **scene epoch** (where planets are drawn) uses the same clock system: user picks clock + value; viewer resolves to TDB for ephemeris queries.

### 3.3 Single simulation scrubber

One scrubber maps to a canonical time array (TDB samples). Labels in the status bar reflect the **display clock** selected in the toolbar.

---

## 4. Coordinate representations (phased)

### v1 — Inertial only

All editing and propagation use **inertial** Cartesian state:

- **ECI / J2000** position and velocity (primary inspector mode).
- **Orbital elements** (a, e, i, Ω, ω, ν) with central body — converted via C++ `two_body` (already present).

Burns and coasts assume inertial unless noted otherwise in a later phase.

### Later — Additional inspector modes

| Mode | User enters | Backend converts to `StateVector` |
|------|-------------|-------------------------------------|
| Geodetic | lon, lat, alt, body | ECEF → ECI @ epoch |
| ECEF | r, v | frame transform → ECI |
| LVLH | relative r/Δv vs reference traj | LVLH frame in `core/frames` |

Inspector shows **read-only derived** values in other representations for trust and debugging.

**Do not** build separate top-level tabs per frame; use one inspector with a **representation** dropdown.

---

## 5. Timeline event types

| Type | Purpose | v1 |
|------|---------|-----|
| `waypoint` | Desired position/state at a time | ECI + elements |
| `coast` | Propagate from previous state for duration or to next event | `PropagatorNode` |
| `burn` | Apply ΔV; update mass | schema only → `ManeuverNode` |
| `stage_sep` | Jettison stage, update mass model | later |
| `launch` | Mission clock zero, initial mass | later |

**Trajectory through waypoints (v1):** ordered **coast** segments between user-specified boundary states—not full Lambert/targeting yet.

**Trajectory through waypoints (later):** solver nodes (Lambert, targeting, optimizer).

---

## 6. Vehicle & staging

**v1:** Single spacecraft, optional single-stage mass properties on burns (schema + UI stub acceptable).

**Later:** Multiple spacecraft for rendezvous; each has its own timeline or a “active craft” selector.

Stage model (conceptual):

```text
Vehicle
  mass_dry_kg, name
  stages[]
    Isp_s, propellant_mass_kg, dry_mass_kg
    burns[] → deplete propellant via rocket equation in C++
```

GUI shows a **Vehicle** section in the timeline; inspector edits stage/burn props. Execution remains mission-graph nodes in C++.

---

## 7. Execution model (behind the UI)

Users edit a **Mission** (timeline + vehicle). The app compiles to a **mission graph** for execution:

```text
Mission (JSON, versioned)
    → compile_mission_graph()
    → Graph (C++ nodes/edges)
    → run_graph()
    → ViewerEpisode / trajectory samples
```

The graph view (nodes/edges JSON) remains an **advanced**/debug surface, not the default editor.

---

## 8. Views

### 8.1 3D (required)

- Bodies from ephemeris at scene epoch.
- Trajectory polyline; grow or full-path modes.
- Waypoint markers; burn vectors (later).
- WSL: off-screen VTK fallback (`SMP_GUI_VTK_MODE`).

### 8.2 Ground track (future)

- Unfold trajectory on body-fixed ground grid (Earth first).
- Linked cursor with 3D scrubber.
- Optional: map tile or simple equirectangular outline.

Not in v1 scope; reserve dock ID and `ViewerEpisode` fields if needed.

---

## 9. Mapping from today’s app

| Current | Evolves to |
|---------|------------|
| Sidebar “3D viewer” | Center **3D viewport** (always available) |
| Sidebar “Mission graph” | **Timeline** + inspector (+ debug graph JSON) |
| `PropagatorNode` + templates | `coast` events |
| `episode_from_graph` | post-run scene update |
| `serialization.py` v1 | `Mission` schema v2 superset |

---

## 10. Open questions (track in design reviews)

1. Canonical **scene origin** for solar-system view: SSB vs Sun-centered (stored in mission metadata).
2. Default **display clock** in GUI: TDB vs mission elapsed.
3. Minimum **body set** for ephemeris (major planets + Moon).
4. When to require **C++ SPICE** vs Python fallback for body positions.

---

## 11. Success criteria (UI v1)

- User selects **clock + epoch** and sees bodies update.
- User adds **≥2 waypoints** (ECI or elements), runs mission, sees **continuous trajectory** in 3D.
- User scrubs time; body positions and trajectory marker stay synchronized.
- Same mission loads in a **notebook** without the GUI.

See `agent/ROADMAP.md` for phased delivery and `agent/MISSION_SCHEMA.md` for file format.
