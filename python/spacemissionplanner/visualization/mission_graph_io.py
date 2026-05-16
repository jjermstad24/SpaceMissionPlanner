"""Mission graph to viewer integration."""

from __future__ import annotations

from typing import Optional

import numpy as np

from spacemissionplanner.visualization.viewer_data import ViewerEpisode

try:
    from spacemissionplanner.spacemissionplanner_native import (
        StateVector, Epoch, Frame, CentralBodyId, PropagatorNode, Graph
    )
    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False


MU_EARTH = 3.986004418e14


def build_propagator_episode(
    initial_position: tuple[float, float, float],
    initial_velocity: tuple[float, float, float],
    initial_epoch: Epoch,
    frame: Frame,
    central_body: CentralBodyId,
    step_size: float = 60.0,
    num_steps: int = 200,
    body_positions_m: Optional[dict[str, np.ndarray]] = None,
    body_display_radius_m: Optional[dict[str, float]] = None,
) -> ViewerEpisode:
    """Build a ViewerEpisode from a PropagatorNode execution.

    Args:
        initial_position: (x, y, z) in meters
        initial_velocity: (vx, vy, vz) in m/s
        initial_epoch: Starting epoch
        frame: Coordinate frame
        central_body: Central body for propagation (e.g., Earth)
        step_size: Time step in seconds
        num_steps: Number of propagation steps
        body_positions_m: Optional pre-computed body positions (body_id -> (T, 3) array)
        body_display_radius_m: Optional body display radii

    Returns:
        ViewerEpisode ready for visualization
    """
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available")

    from spacemissionplanner.visualization.converter import episode_from_state_vectors
    from spacemissionplanner.visualization.ephemeris import get_default_body_positions
    import numpy as np

    position = np.array(initial_position, dtype=np.float64)
    velocity = np.array(initial_velocity, dtype=np.float64)

    initial_state = StateVector(position, velocity, initial_epoch, frame, central_body)

    node = PropagatorNode("propagator")
    node.set_initial_state(initial_state)
    node.set_step_size(step_size)
    node.set_num_steps(num_steps)

    success = node.compute()
    if not success:
        raise RuntimeError("PropagatorNode compute failed")

    import numpy as np

    states = node.get_states()
    epochs = node.get_epochs()
    positions = node.get_positions()

    times = np.array(epochs, dtype=np.float64)

    if body_positions_m is None:
        body_positions_m = get_default_body_positions(times, frame.name())

    if body_display_radius_m is None:
        body_display_radius_m = {
            "Sun": 696340e3,
            "Earth": 6371e3,
            "Moon": 1737.4e3,
        }

    return episode_from_state_vectors(
        states,
        frame_name=frame.name(),
        origin_description=f"Two-body propagation about {central_body.name}",
        time_scale_note="TDB (seconds since J2000)",
        body_positions_m=body_positions_m,
        body_display_radius_m=body_display_radius_m,
    )


def episode_from_graph(
    graph: Graph,
    *,
    run: bool = True,
    origin_description: str = "Mission graph trajectory",
) -> ViewerEpisode:
    """Build a multi-segment ``ViewerEpisode`` from propagator nodes in a graph."""
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available")

    from spacemissionplanner.mission_graph.execution import run_graph, topological_order
    from spacemissionplanner.visualization.converter import episode_from_state_vectors
    from spacemissionplanner.visualization.ephemeris import get_default_body_positions

    if run:
        run_graph(graph)

    merged_states = []
    for name in topological_order(graph):
        node = graph.get_node(name)
        if node is None or not isinstance(node, PropagatorNode):
            continue
        segment = node.get_states()
        if not segment:
            raise RuntimeError(f"Propagator {name!r} has no output states")
        if not merged_states:
            merged_states.extend(segment)
        else:
            merged_states.extend(segment[1:])

    if not merged_states:
        raise RuntimeError("Graph has no propagator output to visualize")

    frame_name = merged_states[0].frame().name()
    times = np.array([s.epoch().seconds_since_j2000() for s in merged_states], dtype=np.float64)
    body_positions_m = get_default_body_positions(times, frame_name)
    body_display_radius_m = {
        "Sun": 696340e3,
        "Earth": 6371e3,
        "Moon": 1737.4e3,
    }

    return episode_from_state_vectors(
        merged_states,
        frame_name=frame_name,
        origin_description=origin_description,
        time_scale_note="TDB (seconds since J2000)",
        body_positions_m=body_positions_m,
        body_display_radius_m=body_display_radius_m,
    )


def build_earth_orbit_episode(
    altitude_km: float = 500.0,
    inclination_deg: float = 0.0,
    num_steps: int = 200,
    step_size: float = 60.0,
) -> ViewerEpisode:
    """Build a ViewerEpisode for a circular Earth orbit.

    Args:
        altitude_km: Orbital altitude in kilometers
        inclination_deg: Orbital inclination in degrees
        num_steps: Number of time steps
        step_size: Time step in seconds

    Returns:
        ViewerEpisode for visualization
    """
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available")

    import math

    R_EARTH = 6371e3
    MU = 3.986004418e14

    radius = R_EARTH + altitude_km * 1000.0
    v_mag = math.sqrt(MU / radius)

    inclination_rad = math.radians(inclination_deg)
    vy = v_mag * math.cos(inclination_rad)
    vz = v_mag * math.sin(inclination_rad)

    initial_epoch = Epoch.J2000()
    frame = Frame.J2000()

    position = (radius, 0.0, 0.0)
    velocity = (0.0, vy, vz)

    return build_propagator_episode(
        initial_position=position,
        initial_velocity=velocity,
        initial_epoch=initial_epoch,
        frame=frame,
        central_body=CentralBodyId.earth,
        step_size=step_size,
        num_steps=num_steps,
    )