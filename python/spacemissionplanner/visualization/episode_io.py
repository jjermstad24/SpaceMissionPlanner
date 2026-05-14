"""Load ``ViewerEpisode`` from portable files (trajectory-only v1 JSON)."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Mapping, Union, cast

import numpy as np

from spacemissionplanner.visualization.viewer_data import TrajectoryRenderMode, ViewerEpisode

PathLike = Union[str, Path]

TRAJECTORY_JSON_SCHEMA_VERSION = 1


def viewer_episode_from_trajectory_arrays(
    times_s: np.ndarray,
    positions_m: np.ndarray,
    *,
    frame_name: str,
    origin_description: str,
    time_scale_note: str,
    stub_body_id: str = "Sun",
    trajectory_render_mode: TrajectoryRenderMode = "full_path",
) -> ViewerEpisode:
    """Build a ``ViewerEpisode`` from trajectory samples only.

    Adds a single **stub** body (default ``Sun``) fixed at the origin with a display radius
    derived from the trajectory extent so the scene has a scale reference. No ephemeris.
    """
    t = np.asarray(times_s, dtype=np.float64).reshape(-1)
    p = np.asarray(positions_m, dtype=np.float64)
    if p.ndim != 2 or p.shape[1] != 3:
        raise ValueError("positions_m must have shape (N, 3)")
    if t.shape[0] != p.shape[0]:
        raise ValueError("times_s and positions_m must have the same length")
    if t.shape[0] < 2:
        raise ValueError("need at least two samples")
    if not np.all(np.isfinite(t)) or not np.all(np.isfinite(p)):
        raise ValueError("times and positions must be finite")
    d = np.diff(t)
    if not np.all(d > 0.0):
        raise ValueError("times_s must be strictly increasing")

    span = float(np.max(np.ptp(p, axis=0)))
    if not np.isfinite(span) or span <= 0.0:
        span = float(np.max(np.linalg.norm(p, axis=1)))
    if not np.isfinite(span) or span <= 0.0:
        span = 1.0
    glyph_r = max(1.0e6, 0.04 * span)

    n = t.shape[0]
    zeros = np.zeros((n, 3), dtype=np.float64)
    body_ids = (stub_body_id,)
    positions: dict[str, np.ndarray] = {stub_body_id: zeros}
    radii: dict[str, float] = {stub_body_id: glyph_r}

    return ViewerEpisode(
        frame_name=frame_name,
        origin_description=origin_description,
        time_scale_note=time_scale_note,
        times=t,
        body_ids=body_ids,
        body_positions_m=positions,
        body_display_radius_m=radii,
        trajectory_positions_m=p.copy(),
        trajectory_render_mode=trajectory_render_mode,
    )


def load_viewer_episode_from_json_path(path: PathLike) -> ViewerEpisode:
    """Load a v1 trajectory JSON file into a ``ViewerEpisode``.

    Expected keys:

    - ``schema_version`` (int, must be 1)
    - ``frame_name`` (str)
    - ``origin_description`` (str)
    - ``time_scale_note`` (str)
    - ``times_s`` (list of numbers, strictly increasing)
    - ``positions_m`` (list of [x,y,z] in meters, same length as ``times_s``)
    - ``stub_body_id`` (optional str, default ``Sun``)
    - ``trajectory_render_mode`` (optional, ``full_path`` or ``grow``; default ``full_path`` for file loads)
    """
    p = Path(path)
    raw = p.read_text(encoding="utf-8")
    try:
        data: Mapping[str, Any] = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid JSON in {p}: {exc}") from exc

    ver = data.get("schema_version")
    if ver != TRAJECTORY_JSON_SCHEMA_VERSION:
        raise ValueError(f"unsupported schema_version {ver!r}; expected {TRAJECTORY_JSON_SCHEMA_VERSION}")

    required = ("frame_name", "origin_description", "time_scale_note", "times_s", "positions_m")
    missing = [k for k in required if k not in data]
    if missing:
        raise ValueError(f"missing keys: {', '.join(missing)}")

    times = np.asarray(data["times_s"], dtype=np.float64).reshape(-1)
    pos = np.asarray(data["positions_m"], dtype=np.float64)
    mode_raw = data.get("trajectory_render_mode", "full_path")
    if mode_raw not in ("grow", "full_path"):
        raise ValueError("trajectory_render_mode must be 'grow' or 'full_path'")
    return viewer_episode_from_trajectory_arrays(
        times,
        pos,
        frame_name=str(data["frame_name"]),
        origin_description=str(data["origin_description"]),
        time_scale_note=str(data["time_scale_note"]),
        stub_body_id=str(data.get("stub_body_id", "Sun")),
        trajectory_render_mode=cast(TrajectoryRenderMode, mode_raw),
    )
