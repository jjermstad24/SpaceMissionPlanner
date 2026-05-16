# AGENTS.md

Guidance for human contributors and coding agents working in this repository.

**Product vision:** `agent/UI_DESIGN.md`  
**System structure:** `agent/ARCHITECTURE.md`  
**Delivery order:** `agent/ROADMAP.md`  
**Mission JSON:** `agent/MISSION_SCHEMA.md`  
**3D / ground views:** `agent/VIEWER_PLAN.md`  
**Implementation snapshot:** `agent/current_status.md`

---

## Purpose

Modular astrodynamics and mission design framework:

- Graph-based mission construction (C++ execution)
- Extensible propagation and optimization
- Notebook-first APIs
- Qt GUI that **orchestrates** the backend
- Deterministic, reproducible runs

---

## Architectural principles

### Backend first

All mission functionality exists without the GUI. Physics, graph execution, file I/O, and compilation from mission → graph live in C++ / Python libraries—not in Qt widgets.

### Notebook first

Every GUI capability must be callable from Python and Jupyter with the same data contracts.

### Deterministic execution

No hidden global simulation state. Graph runs must be reproducible from mission file + version.

### Extensibility

Propagators, frames, clocks, node types, and views register through interfaces—avoid hardcoding one mission type.

---

## Canonical state

Internal state is Cartesian with metadata:

- Position, velocity (m, m/s)
- Epoch (TDB seconds since J2000 internally)
- Frame (inertial names in v1)
- Central body

```
x = [x, y, z, vx, vy, vz] + epoch + frame + central_body
```

User-facing inputs (elements, geodetic, etc.) **convert at the boundary** to this type in C++.

---

## Time and clocks

- Support **multiple clock kinds** for display and event tagging (see `MISSION_SCHEMA.md`).
- Resolve to canonical TDB for propagation and SPICE.
- GUI shows selected display clock; scrubber maps to canonical time array.

---

## Frames (phased)

- **v1:** Inertial only (e.g. J2000 / ECI).
- **Later:** ECEF, geodetic, LVLH—for inspector input and burns—not for hiding canonical state.

---

## Spacecraft

- **v1:** Single spacecraft.
- **Later:** Multiple spacecraft for rendezvous (separate timelines or craft selector).

---

## Units

SI: meters, seconds, kilograms, radians.

---

## Mission graph

Nodes: explicit inputs/outputs, cache invalidation, lazy evaluation.  
User-facing **timeline** compiles to a graph; raw graph JSON is advanced/debug.

---

## Serialization

- JSON with `schema_version`
- Deterministic save (sorted keys)
- Mission schema v2 (target) vs graph snapshot v1 (current)

---

## Testing

- C++: gtest
- Python: pytest
- Every feature: unit tests + regression where numeric

---

## Forbidden patterns

- Physics or frame math in GUI code
- Hardcoded units or frames in mission logic
- Mutable global simulation state
- Bypassing graph invalidation
- Tight coupling of VTK rendering to propagator internals

---

## Tech stack

| Layer | Stack |
|-------|--------|
| Core | C++20, Eigen |
| Astro | two-body, SPICE (cspice) |
| Graph / optimize | mission_graph, optimization |
| Bindings | pybind11 |
| Python | wrappers, visualization, mission_graph |
| GUI | PySide6, PyVista/VTK |

---

## Preferred implementation order

1. Core state, epochs, inertial frames  
2. Propagation + elements I/O  
3. Mission schema + compile → graph  
4. Clock resolution + timeline execution  
5. 3D scene (bodies + trajectory + scrubber)  
6. Inspector (ECI, elements)  
7. Burns, staging, solvers  
8. Ground track view  
9. Multi-spacecraft  

Details: `agent/ROADMAP.md`.
