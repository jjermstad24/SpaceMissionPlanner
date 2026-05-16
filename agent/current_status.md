# Current Status

**Last updated:** Phase H — mission timeline, clocks, compile, GUI.

---

## Implemented

| Area | What works |
|------|------------|
| Mission v2 | `spacemissionplanner.mission` — model, clocks, compile, JSON I/O, templates |
| Clocks | TDB absolute, mission elapsed (relative), UTC (iso/value) |
| Compile | Waypoints (ECI, orbital elements) + chained coasts → `Graph` |
| Native | `state_from_orbital_elements` binding |
| Viewer | `episode_from_mission(mission)` |
| GUI mission page | Timeline tree, display clock, scene epoch TDB, New LEO / 2-phase, mission JSON, graph debug |
| Prior | Graph v1, multi-segment, 3D viewer, WSL VTK, Run → 3D |

**Tests:** 40 Python passed, 1 skipped (after Phase H).

---

## Try it

```bash
make -j4
.venv/bin/smp-gui
```

**Mission timeline** tab → **New LEO mission** → **Run && view in 3D**.

```python
from spacemissionplanner.mission import default_leo_mission, compile_mission
from spacemissionplanner.mission_graph import run_graph
from spacemissionplanner.visualization.mission_graph_io import episode_from_mission

m = default_leo_mission()
run_graph(compile_mission(m))
ep = episode_from_mission(m, run=False)
```

---

## Not yet implemented

| Item | Phase |
|------|--------|
| Inspector (edit waypoint in GUI) | I |
| Scene epoch drives body ephemeris independently of scrubber | H+ |
| Burns, Isp, staging nodes | J |
| ECEF / geodetic / LVLH inspector | later |
| Ground track view | L |
| Multi-spacecraft | M |

---

## Design docs

See `agent/UI_DESIGN.md`, `agent/MISSION_SCHEMA.md`, `agent/ROADMAP.md`.
