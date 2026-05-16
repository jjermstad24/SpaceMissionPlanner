# Contributing

Read **`agent/AGENTS.md`** first. Product and UI direction live in **`agent/UI_DESIGN.md`**; do not implement GUI features that contradict it without updating the doc.

---

## Priorities

1. Modularity and deterministic execution  
2. Backend correctness before Qt polish  
3. Notebook parity for every user-facing capability  
4. Versioned JSON with tests  

---

## Coding standards

### C++

- C++20, RAII, no `using namespace`
- SI units in public APIs
- Physics and frame conversion in `cpp/`, not in `gui/`

### Python

- PEP 8, typed public APIs where practical
- No hidden mission state in module globals

### Formatting

- `clang-format` (C++)
- `black` / `isort` (Python) where configured

---

## Testing

Required for new features:

- Unit tests (gtest / pytest)
- Numeric regression when behavior is quantitative
- Serialization round-trip when adding schema fields

---

## Pull requests

Include:

- What changed and why (link to roadmap phase if applicable)
- Tests
- Updates to `agent/current_status.md` when shipping a phase
- Updates to `agent/MISSION_SCHEMA.md` when changing interchange format

---

## Architecture rules

Do not:

- Put simulation logic in GUI code  
- Hardcode frames (beyond documented v1 inertial set) or units  
- Bypass mission graph execution for propagation  
- Introduce mutable global simulation state  

---

## Documentation

New subsystems should update:

- `agent/ARCHITECTURE.md` — if layers or data flow change  
- `agent/ROADMAP.md` — if phase scope changes  
- `agent/UI_DESIGN.md` — if user-visible behavior changes  

Notebook examples are encouraged for any public API.

---

## Performance

Correctness and clear abstractions first; optimize propagation and visualization after benchmarks justify it.
