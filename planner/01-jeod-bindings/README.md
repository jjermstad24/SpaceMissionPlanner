# JEOD Python Bindings

Python bindings for NASA JEOD (JSC Engineering Orbital Dynamics) physics models.

## Prerequisites

1. **CSPICE Library**
   
   Download and compile from NAIF toolkit:
   ```bash
   https://naif.jpl.nasa.gov/naif/toolkit.html
   ```

2. **Set environment variable:**
   ```bash
   export JEOD_SPICE_DIR=/path/to/spice
   ```

3. **Download SPICE kernels:**
   ```bash
   cd ../02-kernels && python download.py
   ```

## Build

```bash
mkdir build && cd build
cmake .. -DJEOD_DIR=/path/to/jeod -DJEOD_SPICE_DIR=/path/to/spice
make
```

This produces `libpyjeod.so` which can be imported in Python:

```python
import pyjeod
# Use: pyjeod.SpiceEphemeris, pyjeod.GravityManager, etc.
```

## Wrapped Classes

| Class | Purpose |
|---|---|
| `SpiceEphemeris` | Planetary ephemerides via SPICE |
| `GravityManager` | N-body gravity sources |
| `DynManager` | Propagation controller |
| `DynBody` | Spacecraft state |
| `RefFrame` | Reference frame queries |
| `OrbitalElements` | Keplerian ↔ Cartesian conversion |
| `TimeManager` | Time system |

## Integration

After building, ensure the library is in your Python path:

```bash
export PYTHONPATH=/path/to/01-jeod-bindings/build:$PYTHONPATH
```