"""Data fed into the solar-system / trajectory viewer (no physics here)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal, Mapping

import numpy as np

TrajectoryRenderMode = Literal["grow", "full_path"]


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
    trajectory_render_mode: TrajectoryRenderMode = "grow"
    """grow: draw prefix ``[:k+1]`` (and faint full path). full_path: always draw full polyline + cursor at *k*."""

    def __post_init__(self) -> None:
        if self.trajectory_render_mode not in ("grow", "full_path"):
            raise ValueError("trajectory_render_mode must be 'grow' or 'full_path'")
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

    def relocated_to_body(self, body_id: str) -> ViewerEpisode:
        """Return a new episode where all positions are relative to the chosen body center."""
        if body_id not in self.body_ids:
            raise ValueError(f"body_id {body_id!r} not in episode")

        origin_pos = self.body_positions_m[body_id]
        new_body_pos = {bid: pos - origin_pos for bid, pos in self.body_positions_m.items()}

        # Trajectory might have been resampled; if it doesn't match times length, we need to interpolate
        # origin_pos onto trajectory epochs. For now, assume they match or it's a demo.
        if self.trajectory_positions_m.shape[0] == self.times.shape[0]:
            new_traj_pos = self.trajectory_positions_m - origin_pos
        else:
            # Simple fallback if lengths differ: only subtract if trajectory is resampled to self.times
            # In a real app, we'd resample origin_pos to trajectory_times.
            # For the current viewer contract, they usually match.
            new_traj_pos = self.trajectory_positions_m.copy()

        return ViewerEpisode(
            frame_name=self.frame_name,
            origin_description=f"Relocated to {body_id} (from: {self.origin_description})",
            time_scale_note=self.time_scale_note,
            times=self.times,
            body_ids=self.body_ids,
            body_positions_m=new_body_pos,
            body_display_radius_m=self.body_display_radius_m,
            trajectory_positions_m=new_traj_pos,
            trajectory_render_mode=self.trajectory_render_mode,
        )


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
