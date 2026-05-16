"""Resolve mission TimeSpec values to TDB seconds since J2000."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from spacemissionplanner.mission.model import ClockDef, Mission, MissionEvent, TimeSpec

MU_EARTH = 3.986004418e14

# Approximate TAI-UTC leap seconds buffer for display-only UTC (v1).
_UTC_TO_TDB_OFFSET_S = 32.184


def _parse_utc_iso(iso: str) -> float:
    text = iso.strip().replace("Z", "+00:00")
    dt = datetime.fromisoformat(text)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    unix = dt.timestamp()
    # J2000 = 2000-01-01T12:00:00 TAI ≈ unix 946727935.816 (approx)
    j2000_unix = 946727935.816
    return unix - j2000_unix + _UTC_TO_TDB_OFFSET_S


def clock_kind_for(mission: Mission, clock_id: str) -> str:
    for c in mission.clocks:
        if c.id == clock_id:
            return c.kind
    raise ValueError(f"Unknown clock id: {clock_id!r}")


def resolve_timespec(
    mission: Mission,
    spec: TimeSpec,
    resolved: dict[str, float],
) -> float:
    kind = clock_kind_for(mission, spec.clock)

    if kind == "tdb_since_j2000":
        if spec.value is None:
            raise ValueError(f"Clock {spec.clock!r} requires absolute value")
        return float(spec.value)

    if kind == "utc":
        if spec.iso is not None:
            return _parse_utc_iso(spec.iso)
        if spec.value is not None:
            return float(spec.value)
        raise ValueError(f"UTC clock {spec.clock!r} needs iso or value")

    if kind == "mission_elapsed":
        if spec.offset_s is None or spec.relative_to is None:
            raise ValueError("mission_elapsed requires offset_s and relative_to")
        base = resolved.get(spec.relative_to)
        if base is None:
            raise ValueError(f"Cannot resolve relative_to {spec.relative_to!r} yet")
        return base + float(spec.offset_s)

    raise ValueError(f"Unsupported clock kind: {kind!r}")


def resolve_all_event_times(mission: Mission) -> dict[str, float]:
    """Map event id → TDB seconds since J2000."""
    by_id = {e.id: e for e in mission.events}
    resolved: dict[str, float] = {}
    pending = set(by_id.keys())

    for _ in range(len(pending) + 1):
        if not pending:
            break
        progressed = False
        for eid in list(pending):
            ev = by_id[eid]
            try:
                resolved[eid] = resolve_timespec(mission, ev.time, resolved)
                pending.remove(eid)
                progressed = True
            except ValueError as exc:
                if "yet" not in str(exc):
                    raise
        if not progressed and pending:
            raise ValueError(f"Could not resolve event times (cycle?): {sorted(pending)}")
    return resolved


def format_tdb(seconds: float) -> str:
    return f"TDB {seconds:.3f} s since J2000"


def format_time_in_clock(mission: Mission, clock_id: str, tdb_s: float, resolved: dict[str, float]) -> str:
    kind = clock_kind_for(mission, clock_id)
    if kind == "tdb_since_j2000":
        return format_tdb(tdb_s)
    if kind == "mission_elapsed":
        for c in mission.clocks:
            if c.id == clock_id and c.zero_event:
                zero_tdb = resolved.get(c.zero_event, 0.0)
                return f"T+{tdb_s - zero_tdb:.1f} s"
        return f"{tdb_s:.1f} s"
    return format_tdb(tdb_s)
