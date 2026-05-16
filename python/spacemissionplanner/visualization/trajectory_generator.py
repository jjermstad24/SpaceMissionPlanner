"""Generate trajectories from C++ propagator for visualization."""

from __future__ import annotations

import numpy as np

from spacemissionplanner.visualization.converter import episode_from_state_vectors
from spacemissionplanner.visualization.viewer_data import ViewerEpisode


def generate_keplerian_trajectory(
    initial_position: np.ndarray,
    initial_velocity: np.ndarray,
    epoch_seconds: float,
    central_body_gm: float,
    duration_seconds: float,
    n_steps: int,
    central_body_name: str = "Earth",
    frame_name: str = "J2000",
) -> ViewerEpisode:
    """
    Generate a trajectory using Keplerian two-body propagation.

    Args:
        initial_position: (3,) position in meters
        initial_velocity: (3,) velocity in m/s
        epoch_seconds: Initial epoch in seconds since J2000
        central_body_gm: Gravitational parameter (GM) in m^3/s^2
        duration_seconds: Time span to propagate
        n_steps: Number of output points
        central_body_name: Name of central body for display
        frame_name: Name of the coordinate frame

    Returns:
        ViewerEpisode ready for visualization
    """
    from spacemissionplanner.spacemissionplanner_native import (
        StateVector, Epoch, Frame, CentralBodyId, propagate_keplerian
    )

    body_id_map = {
        "Sun": CentralBodyId.sun,
        "Mercury": CentralBodyId.mercury,
        "Venus": CentralBodyId.venus,
        "Earth": CentralBodyId.earth,
        "Moon": CentralBodyId.moon,
        "Mars": CentralBodyId.mars,
        "Jupiter": CentralBodyId.jupiter,
        "Saturn": CentralBodyId.saturn,
        "Uranus": CentralBodyId.uranus,
        "Neptune": CentralBodyId.neptune,
        "Pluto": CentralBodyId.pluto,
    }
    central_body = body_id_map.get(central_body_name, CentralBodyId.earth)

    initial_epoch = Epoch(epoch_seconds)
    frame = Frame.J2000()

    initial_state = StateVector(
        initial_position.astype(np.float64),
        initial_velocity.astype(np.float64),
        initial_epoch,
        frame,
        central_body,
    )

    states = []
    time_span = np.linspace(0, duration_seconds, n_steps)

    for dt in time_span:
        state = propagate_keplerian(initial_state, dt, central_body_gm)
        states.append(state)

    return episode_from_state_vectors(
        states,
        frame_name=frame_name,
        origin_description=f"Keplerian propagation around {central_body_name}",
        time_scale_note="Seconds since J2000 (TDB)",
    )


def generate_circular_orbit_trajectory(
    altitude_m: float,
    central_body_name: str,
    central_body_gm: float,
    n_orbits: float = 1.0,
    n_steps: int = 100,
    frame_name: str = "J2000",
) -> ViewerEpisode:
    """
    Generate a circular orbit trajectory.

    Args:
        altitude_m: Altitude above central body surface in meters
        central_body_name: Name of central body
        central_body_gm: Gravitational parameter in m^3/s^2
        n_orbits: Number of complete orbits to generate
        n_steps: Number of output points
        frame_name: Coordinate frame name

    Returns:
        ViewerEpisode ready for visualization
    """
    body_radii = {
        "Sun": 696340e3,
        "Mercury": 2440e3,
        "Venus": 6052e3,
        "Earth": 6371e3,
        "Moon": 1737e3,
        "Mars": 3390e3,
        "Jupiter": 69911e3,
        "Saturn": 58232e3,
        "Uranus": 25362e3,
        "Neptune": 24622e3,
        "Pluto": 1188e3,
    }

    radius = body_radii.get(central_body_name, 6371e3) + altitude_m
    velocity = np.sqrt(central_body_gm / radius)

    pos = np.array([radius, 0.0, 0.0])
    vel = np.array([0.0, velocity, 0.0])

    period = 2 * np.pi * np.sqrt(radius**3 / central_body_gm)
    duration = n_orbits * period

    return generate_keplerian_trajectory(
        initial_position=pos,
        initial_velocity=vel,
        epoch_seconds=0.0,
        central_body_gm=central_body_gm,
        duration_seconds=duration,
        n_steps=n_steps,
        central_body_name=central_body_name,
        frame_name=frame_name,
    )