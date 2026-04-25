# Space Mission Planner

A space mission planning application built on NASA JSC Engineering Orbital Dynamics (JEOD) physics models with a Python backend and React frontend.

## Architecture

```
┌────────────────────┐    REST API    ┌────────────────────┐
│   React Frontend  │ ──────────── │  Python Backend  │
│   (port 3000)    │              │  (port 8000)   │
└────────────────────┘              └───────┬────────┘
                                         │
                                   pybind11
                                         │
                         ┌─────────────────────┴─────────────────────┐
                         │    JEOD C++ Physics Models    │
                         │  SpiceEphemeris           │
                         │  GravityManager          │
                         │  DynManager            │
                         │  OrbitalElements        │
                         └───────────────────────────┘
```

## Quick Start

```bash
# 1. Install CSPICE from NAIF (see 01-jeod-bindings/README.md)

# 2. Download SPICE kernels
cd 02-kernels && python download.py

# 3. Build JEOD Python bindings
cd 01-jeod-bindings && mkdir build && cd build && cmake .. && make

# 4. Start backend
cd 03-backend && pip install -r requirements.txt
uvicorn api.server:app --reload

# 5. Start frontend (in another terminal)
cd 04-frontend && npm install && npm start
```

## Project Structure

| Directory | Purpose |
|---|---|
| `01-jeod-bindings/` | JEOD C++ → Python pybind11 wrapper |
| `02-kernels/` | SPICE kernels + download script |
| `03-backend/` | FastAPI server, trajectory APIs |
| `04-frontend/` | React + Three.js visualization |

## Features

- ✓ SPICE ephemerides for planetary positions
- ✓ Full N-body gravity propagation via JEOD
- ✓ Lambert transfer arc calculation
- ✓ 3D solar system visualization
- ✓ High-fidelity mission design

## Documentation

- [JEOD Bindings Setup](01-jeod-bindings/README.md)
- [SPICE Kernels](02-kernels/README.md)
- [Backend API](03-backend/README.md)
- [Frontend Setup](04-frontend/README.md)