# File Format Conventions

## Directory Structure

Each model lives in its own directory under `cpp/`:

```
cpp/<model_name>/
├── include/          # Header files (.hh)
├── src/              # Implementation files (.cpp)
└── test/             # Unit tests (test_*.cpp)
```

## Naming Conventions

- **Headers**: `.hh` extension (not `.hpp` or `.h`)
- **Implementation**: `.cpp` extension
- **Tests**: `test_<module>.cpp` pattern
- **Aggregator headers**: `model_name.hh` (e.g., `core.hh`)

## Include Style

Headers use relative includes within their model:

```cpp
#include "epoch.hh"
#include "state_vector.hh"
```

Not:

```cpp
#include "core/epoch.hh"
```

## CMake Structure

Each model directory contains a `CMakeLists.txt` that handles:
- Library compilation
- Test building (when `BUILD_TESTS=ON`)
- Public/private include directories

## Dependencies

- Header-only models link via INTERFACE libraries
- Implementation models link via PUBLIC includes
- Tests link against the model library target

## Testing

Tests are co-located with their model in the `test/` subdirectory.
Each model enables its own tests via `if(BUILD_TESTS)` in its CMakeLists.txt.