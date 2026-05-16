# Mission File Schema (interchange)

Mission files are **JSON**, **versioned**, and **deterministic** (stable key ordering on save). Physics values are SI: meters, seconds, kilograms, radians.

Graph-only snapshots (debug) use `mission_graph` schema v1 — see `python/spacemissionplanner/mission_graph/serialization.py`.

---

## Schema v2 (target) — `mission.json`

Top-level:

```json
{
  "schema_version": 2,
  "name": "example_leo",
  "metadata": {
    "created_utc": "2026-05-16T12:00:00Z",
    "scene_origin": "SSB",
    "default_frame": "J2000"
  },
  "clocks": [
    { "id": "tdb", "kind": "tdb_since_j2000" },
    { "id": "mission", "kind": "mission_elapsed", "zero_event": "launch" }
  ],
  "vehicle": {
    "id": "sc1",
    "name": "Spacecraft 1",
    "stages": [
      {
        "id": "stage1",
        "Isp_s": 320.0,
        "propellant_mass_kg": 2000.0,
        "dry_mass_kg": 500.0
      }
    ]
  },
  "events": [
    {
      "id": "launch",
      "type": "launch",
      "time": { "clock": "tdb", "value": 0.0 }
    },
    {
      "id": "wp0",
      "type": "waypoint",
      "time": { "clock": "tdb", "value": 0.0 },
      "representation": "orbital_elements",
      "central_body": "earth",
      "frame": "J2000",
      "elements": { "a_m": 6778000.0, "e": 0.0, "i_rad": 0.9, "raan_rad": 0.0, "argp_rad": 0.0, "nu_rad": 0.0 }
    },
    {
      "id": "coast1",
      "type": "coast",
      "time": { "clock": "mission", "offset_s": 0.0, "relative_to": "launch" },
      "duration_s": 3600.0,
      "step_s": 60.0,
      "from_event": "wp0"
    }
  ]
}
```

### TimeSpec

**Absolute:**

```json
{ "clock": "tdb", "value": 0.0 }
```

**Relative:**

```json
{ "clock": "mission", "offset_s": 3600.0, "relative_to": "launch" }
```

`clock` must match an `id` in `clocks[]`. Resolution to TDB seconds is done in the compiler (`compile_mission`), not in the GUI.

### Waypoint representations (v1)

| `representation` | Fields |
|------------------|--------|
| `eci` | `position_m`, `velocity_m_s` |
| `orbital_elements` | `elements`: `a_m`, `e`, `i_rad`, `raan_rad`, `argp_rad`, `nu_rad` |

Both require `frame` (inertial name) and `central_body`.

### Coast events

- `from_event`: prior waypoint or burn terminus.
- `duration_s`, `step_s` (or `num_steps`).
- Compiles to `PropagatorNode` in the mission graph.

### Burn events (schema v2; node TBD)

```json
{
  "id": "tli",
  "type": "burn",
  "time": { "clock": "mission", "offset_s": 7200.0, "relative_to": "launch" },
  "frame": "J2000",
  "delta_v_m_s": [100.0, 0.0, 0.0],
  "stage_id": "stage1"
}
```

v1 UI may omit burns; schema reserves fields.

---

## Trajectory-only JSON (viewer)

Remains schema v1 in `episode_io.py` — trajectory samples for visualization without full mission semantics. Do not conflate with `mission.json`.

---

## Migration

| From | To |
|------|-----|
| `mission_graph` v1 (graph nodes only) | Import as single-coast mission or debug graph |
| `mission` v2 | Superset; compiler emits graph v1 internally |

---

## Validation rules

- Every event has a unique `id`.
- `relative_to` must reference an existing event.
- Waypoints must include `frame`, `central_body`, `epoch` (directly or via resolved `time`).
- No frames other than inertial in v1 validators.
