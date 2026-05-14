
# AGENTS.md

## Purpose

This repository is a modular astrodynamics and mission design framework focused on:
- graph-based mission construction
- extensible trajectory propagation
- mission optimization
- notebook interoperability
- GUI-driven workflows
- high-performance numerical execution

The repository is intended to support autonomous coding agents and human contributors simultaneously.

---

# Architectural Principles

## Backend First

All mission functionality must exist independently from the GUI.

The GUI is a frontend orchestration layer only.

Mission execution, physics, optimization, and serialization must never depend on GUI systems.

---

## Notebook First

Every capability exposed in the GUI must also be available through Python APIs and Jupyter notebooks.

Notebook workflows are first-class citizens.

---

## Deterministic Execution

Mission graph execution must be deterministic and reproducible.

No hidden mutable global state is allowed.

---

## Extensibility

All major systems must support future extension:
- propagators
- force models
- optimizers
- visualization systems
- import/export tools

Use interfaces and plugin registration wherever possible.

---

# Core Technology Stack

## Backend
- C++20
- Eigen
- SPICE
- pybind11
- gtest

## Frontend
- Python
- PySide6 / Qt
- PyVista / VTK
- Jupyter notebooks

---

# Mission Graph Philosophy

Mission definitions are represented as:
- nodes
- edges
- dependency graphs

Nodes must:
- declare explicit inputs
- declare explicit outputs
- support cache invalidation
- support lazy evaluation

---

# State Representation

Canonical internal state vectors are Cartesian:

x = [x, y, z, vx, vy, vz]

All states must include:
- frame
- epoch
- central body

Frame-less or epoch-less states are forbidden.

---

# Units

Internal canonical units:
- meters
- seconds
- kilograms
- radians

---

# Propagation Requirements

Propagators must:
- be stateless
- be deterministic
- be thread-safe
- avoid global mutable state

---

# Optimization Requirements

Optimization systems must support:
- objective abstraction
- constraint abstraction
- parameter abstraction

Initial optimizer:
- gradient descent

Future optimizers must integrate through interfaces only.

---

# Serialization

Mission files:
- use JSON
- must contain schema versions
- must remain deterministic and portable

---

# Testing

Every new feature requires:
- unit tests
- deterministic validation
- regression coverage where applicable

C++ tests use gtest.
Python tests use pytest.

---

# Forbidden Patterns

Do not:
- place physics logic in GUI code
- hardcode frame assumptions
- hardcode units
- use mutable global simulation state
- tightly couple rendering and simulation
- bypass graph invalidation systems

---

# Preferred Development Strategy

Implement incrementally:
1. core math/state systems
2. propagation
3. mission graph
4. optimization
5. bindings
6. visualization
7. GUI tooling
