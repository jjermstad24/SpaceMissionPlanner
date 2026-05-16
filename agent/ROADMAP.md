# Roadmap

Phases reflect **actual progress** and the **UI north star** (`agent/UI_DESIGN.md`). Phases 0–G are largely done; H onward align with the mission timeline product.

---

## Phase 0 — Foundation ✅

CMake, pybind11, gtest, Python package layout, agent docs.

---

## Phase 1 — Numerical core ✅

`StateVector`, `Epoch`, `Frame`, transforms (partial), SI units.

---

## Phase 2 — Astrodynamics core ✅ (baseline)

Two-body propagator, orbital elements, SPICE ephemeris bindings.

**Next:** maneuver execution in C++, LVLH/ECEF transforms.

---

## Phase 3 — Mission graph ✅ (baseline)

Graph, edges, `PropagatorNode`, Python execution, graph JSON v1.

**Next:** `ManeuverNode`, `compile_mission`, mission schema v2.

---

## Phase 4 — Optimization ✅ (baseline)

Parameters, objectives, gradient descent.

**Next:** bind optimizer to mission graph parameters (ΔV components, times).

---

## Phase 5 — Python integration ✅ (baseline)

Native module, notebooks path, `mission_graph_io`, templates.

---

## Phase 6 — Visualization ✅ (baseline)

`ViewerEpisode`, 3D widget, SPICE/toy ephemeris, trajectory JSON, WSL off-screen VTK.

**Next:** waypoint glyphs, burn vectors, scene clock toolbar.

---

## Phase 7 — GUI shell ✅ (baseline)

Main window, 3D page, mission graph JSON page, Run → 3D.

**Next:** timeline + inspector layout (see Phase H).

---

## Phase H — Mission timeline & clocks ✅ (baseline)

**Done**

- Mission schema v2 (`python/spacemissionplanner/mission/`)
- Clocks: TDB, UTC, mission elapsed; `resolve_all_event_times`
- `compile_mission` (waypoints + coasts); `episode_from_mission`
- GUI: timeline tree, display clock, scene epoch (TDB spin), mission JSON I/O
- `state_from_orbital_elements` in native bindings

**Remaining for H+**

- Inspector to edit events in GUI
- Scene epoch re-samples body ephemeris in 3D (decoupled from scrubber)

---

## Phase I — Inspector (inertial v1)

**Goals**

- Inspector for waypoint: ECI r/v and orbital elements
- Derived read-only fields (elements ↔ ECI)
- Apply → updates mission event; Run refreshes 3D

**Deliverables**

- No physics in Qt; conversion via native bindings

**Acceptance**

- Edit elements, run, see updated path.

---

## Phase J — Burns & staging

**Goals**

- `ManeuverNode` (ΔV in inertial frame)
- Stage mass model + rocket equation in C++
- Vehicle section in timeline; burn events in schema

**Acceptance**

- Two-stage LEO mission with one burn changes apogee predictably (test vs analytic).

---

## Phase K — Solvers & multi-waypoint targeting

**Goals**

- Lambert / targeting nodes between waypoints
- Optional optimizer hooks for free parameters

---

## Phase L — Ground track view

**Goals**

- 2D ground track (Earth first), linked to scrubber
- Subsatellite lat/lon vs time

**Non-goals for v1**

- Full map tiles; start with simple graticule / coastlines optional.

---

## Phase M — Multi-spacecraft & rendezvous

**Goals**

- Multiple vehicles in mission file
- Active craft selector; separate trajectories in 3D

---

## Phase N — Visual graph editor (optional)

Node-editor canvas for power users; timeline remains default.

---

## Future expansion

- Perturbations (J2, drag, SRP)
- Low-thrust, CR3BP
- Monte Carlo, covariance
- Formation flying
