"""Abstraction for ephemeris sampling (SPICE or analytical)."""

from __future__ import annotations

from typing import Protocol, runtime_checkable

import numpy as np


@runtime_checkable
class EphemerisProvider(Protocol):
    """Interface for sampling celestial body positions over time."""

    def sample_bodies(
        self,
        body_ids: list[str],
        times: np.ndarray,
        *,
        frame: str = "J2000",
        origin: str = "SSB",
    ) -> dict[str, np.ndarray]:
        """Return a mapping of body_id -> (N, 3) positions in meters."""
        ...


class ToyEphemerisProvider:
    """Fallback provider using the circular coplanar model from demo_ephemeris."""

    def sample_bodies(
        self,
        body_ids: list[str],
        times: np.ndarray,
        *,
        frame: str = "J2000",
        origin: str = "SSB",
    ) -> dict[str, np.ndarray]:
        from spacemissionplanner.visualization import demo_ephemeris

        # Note: demo_ephemeris.build_demo_viewer_episode currently handles its own 
        # time grid. Here we'd ideally interpolate or re-run the logic for 'times'.
        # For now, this is a placeholder to show the architecture.
        return {}
