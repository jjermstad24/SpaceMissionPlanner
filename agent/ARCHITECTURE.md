# Architecture

## System layers

```text
┌─────────────────────────────────────────────────────────┐
│  GUI (PySide6) — timeline, inspector, 3D, scrubber    │
├─────────────────────────────────────────────────────────┤
│  Python — mission compile, clocks, visualization, I/O   │
├─────────────────────────────────────────────────────────┤
│  pybind11 — StateVector, Graph, Propagator, SPICE, …    │
├─────────────────────────────────────────────────────────┤
│  mission_graph │ optimization │ astro │ core │ spice    │
└─────────────────────────────────────────────────────────┘
```

The GUI **never** integrates trajectories or applies ΔV. It edits a `Mission`, calls `run_graph`, and displays `ViewerEpisode` samples.

---

## Repository layout

```text
cpp/
  core/           epochs, frames, StateVector
  astro/          two_body propagator, orbital elements
  mission_graph/  Graph, PropagatorNode, …
  optimization/   parameters, objectives, solvers
  spice/          ephemeris (kernels)
  bindings/python/

python/spacemissionplanner/
  mission_graph/  execution, serialization, templates
  visualization/  ViewerEpisode, 3D widget, ephemeris
  gui/            MainWindow, pages
  wrappers/       native extension discovery

agent/            design docs (this folder)
```

---

## Data flow (target)

```text
User edits Mission (timeline + vehicle)
        │
        ▼
  compile_mission()  ──►  Graph (C++)
        │
        ▼
  run_graph()  ──►  states[], epochs[]
        │
        ▼
  episode_from_mission()  ──►  ViewerEpisode
        │
        ▼
  SolarSystemViewWidget  +  time scrubber
```

**Clock resolution** happens before compile: all `TimeSpec` → TDB seconds.  
**Ephemeris** samples bodies on the same TDB grid as the trajectory.

---

## Core (`cpp/core`)

- `Epoch` — TDB since J2000 (extend for clock service later)
- `Frame` — inertial set in v1; body-fixed later
- `StateVector` — canonical mission state
- `transforms` — frame conversions (expand for ECEF/LVLH)

---

## Astrodynamics (`cpp/astro`)

- Two-body Keplerian propagator
- Orbital elements ↔ state
- Future: perturbations, maneuvers as first-class ops

---

## Mission graph (`cpp/mission_graph`)

Execution DAG:

| Node (existing / planned) | Role |
|---------------------------|------|
| `PropagatorNode` | Coast segment |
| `ManeuverNode` | ΔV + mass (planned) |
| `StageNode` | Mass stack / jettison (planned) |
| Solver nodes | Lambert, targeting (planned) |

Python `execution.run_graph()` performs topological run and wires `states → initial_state` edges.

---

## Mission model (Python, planned)

| Module | Role |
|--------|------|
| `mission/model.py` | Mission, Event, Vehicle dataclasses |
| `mission/clocks.py` | Clock registry, TimeSpec → TDB |
| `mission/compile.py` | Mission → Graph |
| `mission_graph/serialization.py` | Graph snapshot v1 (debug) |

Schema: `agent/MISSION_SCHEMA.md`.

---

## Visualization

- **`ViewerEpisode`** — bodies + trajectory arrays + frame metadata
- **`solar_system_view`** — PyVista/Qt (off-screen on WSL)
- **Future:** `ground_track_view` — 2D body-fixed plot, linked scrubber

See `agent/VIEWER_PLAN.md`.

---

## GUI structure (target)

| Region | Widget responsibility |
|--------|---------------------|
| Toolbar | Mission name, **clock selector**, scene epoch, Run |
| Left dock | Timeline tree (vehicle + events) |
| Center | 3D viewport |
| Right dock | Inspector (representation + derived fields) |
| Bottom | Time scrubber |

Current app uses sidebar pages as a stepping stone toward this layout.

---

## Native extension

Built module: `spacemissionplanner.spacemissionplanner_native`  
Discovery: `wrappers.backend.native_extension_status()`.

---

## Threading

- Propagation: deterministic, thread-safe nodes
- Long ephemeris sample: background worker → immutable `ViewerEpisode` → UI thread render

---

## Serialization

| Artifact | Schema | Purpose |
|----------|--------|---------|
| `mission.json` | v2 (target) | User mission |
| `graph.json` | v1 | Debug / interchange |
| `trajectory.json` | v1 | Viewer-only samples |

---

## Related docs

- UI: `agent/UI_DESIGN.md`
- Roadmap: `agent/ROADMAP.md`
- Code style: `agent/FILE_FORMAT.md`
