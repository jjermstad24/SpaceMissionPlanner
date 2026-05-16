"""Built-in mission graph templates."""

from __future__ import annotations

import math

import numpy as np

try:
    from spacemissionplanner.spacemissionplanner_native import (
        CentralBodyId,
        Edge,
        Epoch,
        Frame,
        Graph,
        PropagatorNode,
        StateVector,
    )

    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False


def _require_native() -> None:
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available; build the C++ extension first")


def earth_orbit_graph(
    *,
    name: str = "earth_prop",
    altitude_km: float = 500.0,
    step_size: float = 60.0,
    num_steps: int = 200,
) -> Graph:
    """Circular Earth orbit propagator graph (single node, no edges)."""
    _require_native()
    mu = 3.986004418e14
    radius = 6371e3 + altitude_km * 1000.0
    v_mag = math.sqrt(mu / radius)
    state = StateVector(
        np.array([radius, 0.0, 0.0]),
        np.array([0.0, v_mag, 0.0]),
        Epoch.J2000(),
        Frame.J2000(),
        CentralBodyId.earth,
    )
    node = PropagatorNode(name, mu)
    node.set_initial_state(state)
    node.set_step_size(step_size)
    node.set_num_steps(num_steps)
    graph = Graph()
    graph.add_node(node)
    return graph


def two_segment_earth_orbit_graph(
    *,
    phase1_name: str = "phase1",
    phase2_name: str = "phase2",
    altitude_km: float = 500.0,
    phase1_steps: int = 100,
    phase2_steps: int = 100,
    step_size: float = 60.0,
) -> Graph:
    """Two chained Earth-orbit segments: phase2 continues from phase1 final state."""
    _require_native()
    mu = 3.986004418e14
    radius = 6371e3 + altitude_km * 1000.0
    v_mag = math.sqrt(mu / radius)
    state = StateVector(
        np.array([radius, 0.0, 0.0]),
        np.array([0.0, v_mag, 0.0]),
        Epoch.J2000(),
        Frame.J2000(),
        CentralBodyId.earth,
    )

    phase1 = PropagatorNode(phase1_name, mu)
    phase1.set_initial_state(state)
    phase1.set_step_size(step_size)
    phase1.set_num_steps(phase1_steps)

    phase2 = PropagatorNode(phase2_name, mu)
    phase2.set_step_size(step_size)
    phase2.set_num_steps(phase2_steps)

    graph = Graph()
    graph.add_node(phase1)
    graph.add_node(phase2)
    graph.add_edge(Edge(phase1, "states", phase2, "initial_state"))
    return graph
