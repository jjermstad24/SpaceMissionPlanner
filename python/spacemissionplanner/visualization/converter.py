"""Conversion logic from mission graph outputs to viewer-ready episodes."""

from __future__ import annotations

from typing import Iterable, Sequence, Optional, Mapping

import numpy as np

from spacemissionplanner.visualization.viewer_data import ViewerEpisode

try:
    from spacemissionplanner.spacemissionplanner_native import StateVector, Epoch, Frame, CentralBodyId
    HAS_NATIVE_BINDINGS = True
except ImportError:
    HAS_NATIVE_BINDINGS = False
    StateVector = object


def episode_from_state_vectors(
    states: Sequence[StateVector],
    *,
    frame_name: Optional[str] = None,
    origin_description: str = "Mission Graph Output",
    time_scale_note: str = "TDB (Barycentric Dynamic Time)",
    body_positions_m: Optional[Mapping[str, np.ndarray]] = None,
    body_display_radius_m: Optional[Mapping[str, float]] = None,
) -> ViewerEpisode:
    """
    Convert a sequence of mission graph StateVectors to a ViewerEpisode.

    Extracts positions, times, and frame metadata from the state vectors.

    Args:
        states: Sequence of StateVector objects from the C++ backend.
        frame_name: Name of the coordinate frame (defaults to first state's frame).
        origin_description: Human-readable description of the origin.
        time_scale_note: Description of the time scale used.
        body_positions_m: Optional mapping of body_id to (T, 3) position array.
                         If not provided, creates a placeholder Sun at origin.
        body_display_radius_m: Optional mapping of body_id to display radius in meters.
                               If not provided, uses default values.
    """
    if not HAS_NATIVE_BINDINGS:
        raise RuntimeError("Native bindings not available. Install spacemissionplanner_native.")

    if not states:
        raise ValueError("Cannot create episode from empty state sequence")

    times = []
    positions = []

    for sv in states:
        epoch = sv.epoch()
        times.append(epoch.seconds_since_j2000())

        pos = sv.position()
        positions.append([pos[0], pos[1], pos[2]])

    times = np.array(times, dtype=np.float64)
    positions = np.array(positions, dtype=np.float64)

    if frame_name is None:
        frame_name = states[0].frame().name()

    tlen = times.shape[0]

    if body_positions_m is None:
        body_ids = ("Sun",)
        body_positions_m = {"Sun": np.zeros((tlen, 3), dtype=np.float64)}
        body_display_radius_m = {"Sun": 696340e3}  # Sun radius in meters

    if body_display_radius_m is None:
        body_display_radius_m = {bid: 1.0e6 for bid in body_ids}

    body_ids = tuple(body_positions_m.keys())

    return ViewerEpisode(
        frame_name=frame_name,
        origin_description=origin_description,
        time_scale_note=time_scale_note,
        times=times,
        body_ids=body_ids,
        body_positions_m=body_positions_m,
        body_display_radius_m=body_display_radius_m,
        trajectory_positions_m=positions,
        trajectory_render_mode="full_path",
    )
