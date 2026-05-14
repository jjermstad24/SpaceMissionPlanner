"""Data fed into the solar-system / trajectory viewer (no physics here)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Mapping

import numpy as np


@dataclass(frozen=True)
class ViewerEpisode:
    """One synchronized animation: bodies and trajectory share the same logical times."""

    frame_name: str
    origin_description: str
    time_scale_note: str
    times: np.ndarray  # (T,) monotonic, same interpretation as backend export
    body_ids: tuple[str, ...]
    body_positions_m: Mapping[str, np.ndarray]  # each (T, 3), meters in frame_name
    body_display_radius_m: Mapping[str, float]  # exaggerated glyph radii for visibility
    trajectory_positions_m: np.ndarray  # (T, 3) or (T_traj, 3); if T_traj != T, resample before use

    def __post_init__(self) -> None:
        t = np.asarray(self.times, dtype=np.float64)
        if t.ndim != 1:
            raise ValueError("times must be 1-D")
        tlen = t.shape[0]
        if tlen < 2:
            raise ValueError("times must have length >= 2")
        for bid in self.body_ids:
            arr = np.asarray(self.body_positions_m[bid], dtype=np.float64)
            if arr.shape != (tlen, 3):
                raise ValueError(f"body_positions_m[{bid!r}] must have shape ({tlen}, 3), got {arr.shape}")
            rad = float(self.body_display_radius_m[bid])
            if rad <= 0.0 or not np.isfinite(rad):
                raise ValueError(f"body_display_radius_m[{bid!r}] must be finite and > 0")
        traj = np.asarray(self.trajectory_positions_m, dtype=np.float64)
        if traj.ndim != 2 or traj.shape[1] != 3:
            raise ValueError("trajectory_positions_m must have shape (N, 3)")
        if traj.shape[0] < 2:
            raise ValueError("trajectory must have at least two samples")


def resample_trajectory_to_times(
    traj: np.ndarray,
    traj_times: np.ndarray,
    target_times: np.ndarray,
) -> np.ndarray:
    """Linear interpolation of trajectory positions onto target_times (each 1-D, same units)."""
    tq = np.asarray(target_times, dtype=np.float64)
    tt = np.asarray(traj_times, dtype=np.float64)
    p = np.asarray(traj, dtype=np.float64)
    if p.shape[0] != tt.shape[0]:
        raise ValueError("traj and traj_times length mismatch")
    out = np.empty((tq.shape[0], 3), dtype=np.float64)
    for axis in range(3):
        out[:, axis] = np.interp(tq, tt, p[:, axis], left=np.nan, right=np.nan)
    if np.any(~np.isfinite(out)):
        raise ValueError("interpolation produced non-finite values; check time overlap")
    return out
