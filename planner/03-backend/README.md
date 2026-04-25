# FastAPI Backend for Space Mission Planner

FastAPI server providing:
- Planetary ephemeris queries
- Trajectory propagation via JEOD
- Lambert transfer calculation

## Setup

```bash
pip install -r requirements.txt
uvicorn api.server:app --reload --port 8000
```

## API Endpoints

### GET /bodies
List available planetary bodies

### GET /body/{name}/state?time={epoch}
Get position + velocity of a body at a given time

### POST /propagate
Run trajectory propagation

### POST /transfer
Calculate Lambert transfer arc

## Environment

Set kernels path:
```bash
export KERNELS_PATH=/path/to/02-kernels
```

Set JEOD Python bindings:
```bash
export PYTHONPATH=/path/to/01-jeod-bindings/build:$PYTHONPATH
```