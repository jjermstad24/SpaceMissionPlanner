# Current Status

**Last updated:** planning refresh — mission timeline UI direction captured in `agent/UI_DESIGN.md`.

---

## Implemented (production path)

| Area | What works |
|------|------------|
| Core | `StateVector`, `Epoch`, `Frame`, central body |
| Astro | Two-body propagator, orbital elements ↔ state |
| Mission graph | `Graph`, `PropagatorNode`, topo `run_graph`, edge wiring |
| Serialization | Graph JSON v1 (`mission_graph/serialization.py`) |
| Templates | Single + two-segment Earth orbit graphs |
| Visualization | `ViewerEpisode`, 3D viewer, scrubber, SPICE/demo ephemeris |
| GUI | 3D page, mission graph JSON page, **Run && view in 3D** |
| Platform | Native module detection, WSL off-screen VTK |

**Tests:** 35 Python passed, 1 skipped; C++ mission_graph 6/6.

---

## Not implemented (documented target)

| Area | Target doc |
|------|------------|
| Mission timeline editor | `UI_DESIGN.md` |
| Clock kinds (TDB, UTC, mission elapsed) | `MISSION_SCHEMA.md` |
| Mission schema v2 + compiler | `MISSION_SCHEMA.md`, `ROADMAP.md` Phase H |
| Inspector (ECI / elements) | `ROADMAP.md` Phase I |
| Burns, Isp, mass, staging | `ROADMAP.md` Phase J |
| Non-inertial input modes | `UI_DESIGN.md` §4 |
| Ground track view | `VIEWER_PLAN.md` Phase L |
| Multi-spacecraft | `ROADMAP.md` Phase M |
| Visual node editor | `ROADMAP.md` Phase N |

---

## Design decisions (locked for planning)

1. **Clocks:** multiple kinds for display and event times; canonical internal axis is TDB.
2. **Event times:** same clock options (absolute + relative).
3. **Frames:** inertial only for v1 implementation.
4. **Spacecraft:** one vehicle v1; rendezvous later.
5. **Views:** 3D primary; ground track later.

---

## Suggested next implementation slice (Phase H)

1. `mission/clocks.py` — resolve `TimeSpec` → TDB  
2. `mission/compile.py` — waypoint + coast → `Graph`  
3. Mission JSON v2 load/save (subset: ECI + elements + coast)  
4. GUI toolbar: clock + scene epoch  
5. Timeline widget (tree) replacing plain list on mission page  

No new physics required for H beyond existing `PropagatorNode`.

---

## Quick commands

```bash
make -j4
.venv/bin/pytest python/tests/ -q
.venv/bin/smp-gui
```

```python
from spacemissionplanner.mission_graph.templates import two_segment_earth_orbit_graph
from spacemissionplanner.mission_graph import run_graph
from spacemissionplanner.visualization.mission_graph_io import episode_from_graph

g = two_segment_earth_orbit_graph()
run_graph(g)
ep = episode_from_graph(g, run=False)
```
