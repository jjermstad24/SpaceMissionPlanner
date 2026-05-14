"""Conversion logic from mission graph outputs to viewer-ready episodes."""

from __future__ import annotations

from typing import Iterable, Sequence

import numpy as np

from spacemissionplanner.visualization.viewer_data import ViewerEpisode

# Mock for type-checking the C++ StateVector; replace with actual binding reference once ready
# In practice, this would be a Protocol or a direct check against the bound type.
StateVector = object


def episode_from_state_vectors(
    states: Sequence[StateVector],
    *,
    frame_name: str = "J2000",
    origin_description: str = "Mission Graph Output",
    time_scale_note: str = "TDB (Barycentric Dynamic Time)",
    stub_body_id: str = "Sun",
) -> ViewerEpisode:
    """
    Convert a sequence of mission graph StateVectors to a ViewerEpisode.
    
    Extracts positions, times, and frame metadata from the state vectors.
    """
    # Logic:
    # 1. Iterate over sequence, extract epoch and position_m() from each StateVector.
    # 2. Package into NumPy arrays.
    # 3. Create ViewerEpisode.
    # Implementation depends on the final pybind11 API for StateVector.
    
    # Placeholder implementation:
    times = np.array([0.0], dtype=np.float64) # Replace with epoch extract
    pos = np.array([[0.0, 0.0, 0.0]], dtype=np.float64) # Replace with pos extract
    
    # Create the episode structure...
    # (Returning empty for now until C++ bindings are confirmed)
    return ViewerEpisode(
        frame_name=frame_name,
        origin_description=origin_description,
        time_scale_note=time_scale_note,
        times=times,
        body_ids=(stub_body_id,),
        body_positions_m={stub_body_id: np.zeros((1, 3))},
        body_display_radius_m={stub_body_id: 1.0e6},
        trajectory_positions_m=pos,
        trajectory_render_mode="full_path",
    )
