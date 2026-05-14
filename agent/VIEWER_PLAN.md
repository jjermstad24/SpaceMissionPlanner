# Trajectory and Solar-System Viewer — Planning Document

This document formulates a **plan** for a viewer that can **render major solar-system bodies** (informally, “all the planets”) and **show a user trajectory in that context**, before those systems are wired end-to-end in the application. It aligns with `agent/AGENTS.md` (backend-first, frames, units, notebook parity, no physics in the GUI).

---

## 1. North-star outcome

A user opens the desktop viewer (or a notebook with the same scene API) and sees:

- **Bodies**: Sun, planets, and selected small bodies (Moon, Pluto, etc.) as **time-varying positions** in a consistent **inertial frame** (e.g. ICRF / J2000) over a chosen **time span**.
- **Trajectory**: A polyline (or tube) representing **states sampled from the backend** (propagation or mission graph output), in the **same frame and epoch convention** as the bodies.
- **Interaction**: Pan, zoom, rotate, optional **time scrubber** so bodies and trajectory update together at the same simulation clock.

The viewer **does not** integrate orbits; it **only displays** data produced by deterministic backend pipelines.

---

## 2. Architectural boundaries

| Concern | Owner | Rule |
|--------|--------|------|
| Ephemeris (where planets are at time *t*) | Backend (C++ + SPICE kernels) or a thin, testable Python loader that wraps the same kernels | Same results in GUI and notebooks. |
| Trajectory samples (position/velocity vs time) | Backend / mission graph execution | GUI receives **arrays + metadata**, not force models. |
| Mesh, texture, scale, camera, picking | Frontend (PyVista / VTK inside Qt) | **No** orbital mechanics in UI code. |
| Frame, epoch, central body | Declared on every trajectory segment | Matches `AGENTS.md`: no frame-less or epoch-less states. |

**Notebook first**: any function that builds a “scene description” or sampled body positions for the GUI must be importable from Jupyter with the same inputs (time grid, frame id, body list, trajectory table).

---

## 3. Data contracts (define these early)

Stable, versionable structures (JSON schema or pydantic/dataclass + documented columns) keep the viewer decoupled from internal graph types.

### 3.1 Trajectory to viewer

Minimum per sample (or per segment with uniform metadata):

- **Time**: absolute epoch (e.g. TAI or UTC with explicit scale documented in schema).
- **Position**: Cartesian `(x, y, z)` in **meters** in a named **frame** (e.g. `J2000` relative to solar system barycenter or a declared origin).
- **Optional**: velocity, segment id, central body id for labeling.

The GUI draws a polyline through positions; velocity can support direction arrows or osculating orbit hints later.

### 3.2 Ephemeris to viewer

For each body id (NAIF ID or internal enum mapped to SPICE):

- Same **frame** and **time grid** as trajectory (or resampled on the client from a dense backend table).
- **Position** in meters (same origin policy as trajectory).

Backend batch API example (conceptual): `sample_bodies(body_ids, epochs, frame, origin)` → `(N, B, 3)` array + metadata. The viewer never calls SPICE directly unless you explicitly choose a **single** thin Python path for prototypes—with a plan to converge on the C++ path for parity.

---

## 4. Rendering and UX design problems

These are **product/engineering decisions** to schedule, not afterthoughts.

### 4.1 Scale and fidelity

- **True scale**: planetary radii vs interplanetary distances make a single-scale “solar system + trajectory” view unreadable.
- **Plan**: support **multi-scale rendering modes** (documented in UI):
  - **Orbit mode**: bodies as small glyphs; trajectory dominates.
  - **Body mode** (optional): local camera near one body with exaggerated planet radius for illustration.
  - **Hybrid**: logarithmic or dual-scale (artistic) with clear “not to scale” labeling where applicable.

### 4.2 Body appearance

- **MVP**: textured spheres or solid colors per body; no need for full PBR initially.
- **Later**: normal maps, rings (Saturn, Uranus, Neptune), simple atmosphere halo.

### 4.3 Performance

- Ephemeris: sample on a **coarse grid** for the full span; **refine** near maneuver or close approach in a second pass (backend or viewer resample).
- VTK: instancing or low-poly meshes for many bodies; avoid per-frame mesh rebuilds where possible.

### 4.4 Time and reference origin

- Single **simulation clock** driving both ephemeris queries and trajectory index.
- **Origin**: solar system barycenter vs heliocentric vs “target body centered” camera—plan explicit modes so frame strings in data match what users expect.

### 4.5 Qt integration

- Embed VTK via **PyVista** in a **Qt** widget (e.g. `pyvistaqt` or equivalent pattern) so the viewer lives inside `MainWindow` rather than a separate process—unless you deliberately choose a separate viewer for v1.

---

## 5. Phased roadmap (suggested)

Phases are ordered so each step delivers something visible and testable.

### Phase A — Static skeleton

- Empty 3D view in the GUI tab; camera controls; grid or starfield optional.
- **Acceptance**: application runs; no ephemeris yet.

### Phase B — One trajectory, no planets

- Load trajectory contract from a **file or in-memory array** (notebook and GUI).
- Draw polyline in declared frame.
- **Acceptance**: known test trajectory (e.g. circular in simplified frame) renders correctly.

### Phase C — One body + trajectory

- Sun or Earth position from SPICE (or stub) + trajectory relative to same origin/frame.
- Validate frame alignment with a regression test (numeric tolerance on a few epochs).
- **Acceptance**: Earth glyph + LEO-like arc looks plausible.

### Phase D — “All planets” pass

- Configurable body list (default: major planets + Moon + Pluto optional).
- Batch ephemeris sampling from backend; viewer caches per time window.
- **Acceptance**: scrubbing time moves all bodies; no NaNs; documented kernel set.

### Phase E — Mission integration

- Mission graph node emits trajectory segments into the same contract.
- Viewer subscribes to “current mission state” or loads from serialized run.
- **Acceptance**: replay of a saved mission matches notebook plot.

### Phase F — Polish

- Labels, distance tools, screenshot, optional 2D map inset, performance profiling.

---

## 6. Dependencies and assets

- **Kernels**: generic SPICE `.bsp` / `.tls` (and planetary constants) versioned or documented in-repo vs downloaded via scripted fetch; checksums for reproducibility.
- **Python**: PyVista, VTK, NumPy; optional `pyvistaqt` (or chosen Qt–VTK bridge).
- **Legal**: texture maps often have license constraints—track provenance if shipping assets.

---

## 7. Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Frame mismatch (trajectory vs ephemeris) | Single frame enum in contract; backend tests that compare SPICE `sxform` to your propagation frame. |
| GUI blocks on long ephemeris sample | Worker thread or async job + progress; immutable result handed to VTK thread per Qt rules. |
| WSL / headless CI | Offscreen VTK for tests; GUI tests optional/manual. |
| Scope creep (full NAIF catalog) | Start from a **fixed body list**; plugin registry for add-ons later (`AGENTS.md` extensibility). |

---

## 8. Open questions (resolve in design reviews)

1. **Canonical origin** for “solar system view”: SSB vs Sun-centered—what do mission files store?
2. **Time scale** for file interchange: UTC vs TAI vs ephemeris time—pick one for v1 schema.
3. **Single vs dual Python paths** for SPICE in prototypes: how fast must C++ parity be enforced?
4. **Minimum hardware** target for full solar-system + long trajectory at 60 FPS (if required).

---

## 9. Relation to existing code

- `python/spacemissionplanner/visualization/TrajectoryScene` is a thin polyline helper; the plan above extends it into a **scene composer** (bodies + trajectory + time) with clear inputs.
- C++ `cspice` / propagation outputs should eventually feed the same contract via **pybind11**; until then, validated Python-side sampling is acceptable if documented as transitional.

---

## 10. Success criteria (viewer v1)

- User can enable **default major-body set** and see them move with a **time slider**.
- User can load **at least one** backend-generated trajectory and see it **simultaneously** with those bodies, with **matching frame metadata** visible in the UI or export.
- The same scene inputs can be **scripted in a notebook** without launching the GUI.

When these hold, wiring the viewer to the mission graph becomes primarily **data plumbing and UX**, not a redesign of visualization fundamentals.

---

## 11. Implementation snapshot (repository)

Initial scaffolding (toy ephemeris, not SPICE):

- **`python/spacemissionplanner/visualization/episode_io.py`** — v1 trajectory JSON loader (`load_viewer_episode_from_json_path`) and `viewer_episode_from_trajectory_arrays` for notebooks; stub origin body for scale.
- **`python/spacemissionplanner/visualization/demo_ephemeris.py`** — `build_demo_viewer_episode()` coplanar demo (Sun, planets, Moon + sample trajectory).
- **`python/spacemissionplanner/visualization/solar_system_view.py`** — `SolarSystemViewWidget` (PyVista `QtInteractor`).
- **`python/spacemissionplanner/gui/solar_viewer_page.py`** — Sidebar “3D viewer” page with time scrubber and demo load.

Next backend-facing step: replace `build_demo_viewer_episode` inputs with SPICE/C++-sampled `ViewerEpisode` while keeping the widget API stable. Trajectory-only files can already be exchanged via `episode_io` (Phase B in §5).
