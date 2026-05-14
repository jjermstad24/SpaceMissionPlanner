
# ARCHITECTURE.md

# System Overview

The project is divided into five major layers:

1. Numerical Core
2. Astrodynamics Layer
3. Mission Graph Layer
4. Optimization Layer
5. Frontend Layer

---

# Repository Structure

repo/
├── cpp/
│   ├── core/
│   ├── astro/
│   ├── optimization/
│   ├── mission_graph/
│   ├── cspice/
│   └── bindings/
│
├── python/
│   ├── notebooks/
│   ├── wrappers/
│   ├── visualization/
│   └── gui/
│
├── tests/
├── examples/
├── docs/
├── schemas/
└── assets/

---

# Numerical Core

Responsibilities:
- vector math
- matrix operations
- epochs
- frames
- coordinate transforms
- units

Dependencies:
- Eigen

---

# Astrodynamics Layer

Responsibilities:
- propagation
- orbital mechanics
- maneuver execution
- event detection
- force models

Initial propagator:
- two-body propagation

Future extensions:
- J2
- N-body
- SRP
- low thrust
- atmospheric drag

---

# Mission Graph Layer

Responsibilities:
- node management
- dependency tracking
- execution scheduling
- lazy evaluation
- serialization

Node categories:
- orbit nodes
- maneuver nodes
- propagation nodes
- optimization nodes
- event nodes

---

# Optimization Layer

Responsibilities:
- optimization variables
- constraints
- objectives
- solver orchestration

Initial optimizer:
- gradient descent

Future extensibility:
- SQP
- CMA-ES
- collocation
- multiple shooting

---

# Frontend Layer

Responsibilities:
- notebook integration
- plotting
- graph editing
- user interaction

The frontend must never contain mission physics.

---

# Python Bindings

Bindings use:
- pybind11

Python APIs must expose:
- mission construction
- propagation
- optimization
- visualization hooks

---

# GUI Design

Framework:
- PySide6 / Qt

Features:
- graph editing
- orbit visualization
- maneuver editing
- trajectory playback

The GUI must only orchestrate backend systems.

---

# Threading Philosophy

Requirements:
- task-based execution
- deterministic behavior
- immutable propagation inputs

---

# Serialization

Mission files use:
- JSON
- schema versioning

Mission serialization must remain stable across versions whenever possible.
