# Viewer Plan — 3D Scene & Ground Track

Visualization planning for the **center viewport** and future **ground track**. Product context: `agent/UI_DESIGN.md`. Contracts: `ViewerEpisode` in code, `agent/MISSION_SCHEMA.md` for missions.

---

## North-star behavior

At any **scene epoch** (user-chosen clock + value):

1. **Bodies** appear at correct positions (SPICE or validated fallback).
2. **Trajectory** shows full path; scrubber highlights current point.
3. **Waypoints / burns** appear as glyphs (burns as inertial ΔV arrows in v1).
4. **One scrubber** drives bodies, trajectory index, and labels.

The viewer **never** propagates; it displays backend samples.

---

## Architectural boundaries

| Concern | Owner |
|---------|--------|
| Ephemeris sampling | C++ SPICE / `ephemeris.py` |
| Trajectory samples | `run_graph` → `episode_from_graph` / `episode_from_mission` |
| Meshes, camera, scale | PyVista + Qt |
| Clock display | GUI converts TDB ↔ display clock |

---

## Data contract: `ViewerEpisode`

Stable fields (extend carefully):

- `frame_name`, `origin_description`, `time_scale_note`
- `times` — TDB seconds since J2000 (canonical scrubber axis)
- `body_ids`, `body_positions_m`, `body_display_radius_m`
- `trajectory_positions_m`, `trajectory_render_mode`

Optional future fields:

- `waypoint_positions_m`, `waypoint_labels`
- `burns[]` — point, `delta_v_m_s`, frame
- `display_clock_hint` — UI only

Trajectory-only JSON (`episode_io.py` v1) remains for quick load without a full mission.

---

## 3D viewport

### Scale modes

| Mode | Use |
|------|-----|
| Orbit | Small body glyphs; trajectory readable |
| Body-centric | Camera near Earth/Moon; exaggerated radius |

Label when not true scale.

### Rendering MVP (current + next)

| Done | Next |
|------|------|
| Bodies as spheres, trajectory tube/line | Waypoint markers |
| Time scrubber, grow/full path | Burn vectors |
| Off-screen VTK on WSL | Pick → select timeline event |
| Demo + SPICE ephemeris | Scene epoch toolbar |

### Qt / VTK

- Prefer embedded `QtInteractor` where GL works.
- WSL: `SMP_GUI_VTK_MODE=auto` → off-screen frames in `QLabel`.
- `AA_ShareOpenGLContexts` when embedding.

---

## Clocks in the viewer

- Scrubber internal axis: **TDB seconds** (matches `ViewerEpisode.times`).
- Toolbar: user selects **display clock** (TDB, UTC, mission elapsed).
- Status bar shows current time in display clock + TDB for debug.
- **Scene epoch** (body positions) may differ from scrubber when “live ephemeris” mode is off—document which epoch bodies use.

---

## Ground track view (future — Phase L)

**Goal:** 2D plot of spacecraft sub-point on a central body (Earth first).

```text
┌─────────────────────────────┐
│  Lat                      │
│    ╲   trajectory ground   │
│     ╲  track               │
│  Lon ─────────────────     │
└─────────────────────────────┘
```

- Input: same `ViewerEpisode` + `central_body` (Earth).
- Convert ECI → geodetic per sample (C++, not Qt).
- Linked scrubber cursor with 3D view.
- v1 ground track: line plot only (no map server).

---

## Phased delivery (viewer-specific)

| Step | Content | Status |
|------|---------|--------|
| A | Empty 3D + camera | ✅ |
| B | Trajectory JSON load | ✅ |
| C | One body + trajectory | ✅ |
| D | Multi-body SPICE/demo | ✅ partial |
| E | Mission graph → episode | ✅ |
| F | JSON graph save/load | ✅ |
| G | Multi-segment merge | ✅ |
| H | Scene clock toolbar | planned |
| I | Waypoint/burn glyphs | planned |
| L | Ground track dock | future |

---

## Risks

| Risk | Mitigation |
|------|------------|
| Frame mismatch | Single `frame_name` on episode; tests on sample epochs |
| WSL GL crash | Off-screen mode default on WSL |
| Scale confusion | UI mode labels |
| Ephemeris blocking UI | Background sample → episode |

---

## Success criteria (viewer tied to UI v1)

- Clock/epoch controls update **all bodies** consistently.
- Scrubber syncs trajectory marker and time label.
- Mission run from timeline refreshes 3D without restart.
- Notebook can build identical `ViewerEpisode` from same mission file.

---

## Code map

| File | Role |
|------|------|
| `visualization/viewer_data.py` | `ViewerEpisode` |
| `visualization/solar_system_view.py` | 3D widget |
| `visualization/ephemeris.py` | Body sampling |
| `visualization/mission_graph_io.py` | Graph → episode |
| `gui/solar_viewer_page.py` | Page + scrubber |
| `gui/vtk_platform.py` | WSL VTK mode |
