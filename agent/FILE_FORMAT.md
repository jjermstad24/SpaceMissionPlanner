# Code & Repository Conventions

C++ layout and style for this repo. **Mission JSON interchange** is documented in `agent/MISSION_SCHEMA.md` (not this file).

---

## Directory structure (C++ models)

Each model lives under `cpp/<model_name>/`:

```text
cpp/<model_name>/
├── include/          # Headers (.hh)
├── src/              # Implementation (.cpp)
└── test/             # Unit tests (test_*.cpp)
```

---

## Naming

| Kind | Pattern |
|------|---------|
| Headers | `.hh` |
| Sources | `.cpp` |
| Tests | `test_<module>.cpp` |
| Aggregator | `<model>.hh` (e.g. `core.hh`) |

---

## Includes

Relative includes within the model:

```cpp
#include "epoch.hh"
#include "state_vector.hh"
```

Not `#include "core/epoch.hh"` from inside the model.

---

## CMake

Each model has `CMakeLists.txt`:

- Library target
- Tests when `BUILD_TESTS=ON`
- PUBLIC include directories as needed

---

## Dependencies

- Header-only: `INTERFACE` libraries
- Compiled models: `PUBLIC` link to dependencies
- Tests: link model + gtest

---

## Style rules

- **No `using namespace`** in headers or implementation.
- Explicit `smp::` prefixes.
- SI units in APIs unless clearly documented otherwise.

```cpp
smp::core::StateVector state;  // correct
using namespace smp::core;      // forbidden
```

---

## Python

- Package: `python/spacemissionplanner/`
- Tests: `python/tests/`
- Native module: `spacemissionplanner_native` (CMake output name)

---

## Agent documentation

| File | Contents |
|------|----------|
| `AGENTS.md` | Rules for agents |
| `ARCHITECTURE.md` | Layers and data flow |
| `UI_DESIGN.md` | Product UI |
| `MISSION_SCHEMA.md` | JSON mission format |
| `VIEWER_PLAN.md` | 3D + ground track |
| `ROADMAP.md` | Phases |
| `current_status.md` | Snapshot of implementation |
