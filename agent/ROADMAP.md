
# ROADMAP.md

# Phase 0 — Foundation

## Goals
- establish repository layout
- configure build systems
- configure testing
- establish coding standards

## Deliverables
- CMake configuration
- pybind11 integration
- gtest integration
- CI pipeline
- formatting tools

---

# Phase 1 — Numerical Core

## Goals
- create foundational abstractions

## Deliverables
- state vectors
- epochs
- frames
- transforms
- unit conventions

---

# Phase 2 — Astrodynamics Core

## Goals
- implement initial propagation systems

## Deliverables
- two-body propagator
- maneuver execution
- event detection
- SPICE integration

---

# Phase 3 — Mission Graph

## Goals
- implement graph execution model

## Deliverables
- node interfaces
- edge interfaces
- dependency invalidation
- lazy execution
- graph serialization

---

# Phase 4 — Optimization

## Goals
- implement optimization framework

## Deliverables
- parameter systems
- objective systems
- constraint systems
- gradient descent optimizer

---

# Phase 5 — Python Integration

## Goals
- notebook interoperability

## Deliverables
- pybind11 wrappers
- notebook examples
- plotting APIs

---

# Phase 6 — Visualization

## Goals
- trajectory rendering

## Deliverables
- orbit plotting
- trajectory rendering
- maneuver visualization
- playback tools

---

# Phase 7 — GUI

## Goals
- graph-based mission editor

## Deliverables
- node editor
- graph manipulation
- property panels
- live recomputation

---

# Future Expansion

Potential future systems:
- N-body propagation
- CR3BP
- low-thrust optimization
- launch vehicle ascent
- covariance analysis
- Monte Carlo systems
- rendezvous planning
- formation flying
