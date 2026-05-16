"""Compile a Mission timeline into a mission Graph."""

from __future__ import annotations

from typing import Any

import numpy as np

from spacemissionplanner.mission.clocks import MU_EARTH, resolve_all_event_times
from spacemissionplanner.mission.model import Mission, MissionEvent

try:
    from spacemissionplanner.spacemissionplanner_native import (
        CentralBodyId,
        Edge,
        Epoch,
        Frame,
        Graph,
        PropagatorNode,
        StateVector,
        state_from_orbital_elements,
    )

    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False
    Graph = Any  # type: ignore[misc, assignment]


def _require_native() -> None:
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available")


def _frame_from_name(name: str) -> Frame:
    factories = {
        "J2000": Frame.J2000,
        "TOD": Frame.TOD,
    }
    if name not in factories:
        raise ValueError(f"Unsupported frame for v1 compile: {name!r}")
    return factories[name]()


def _body_from_name(name: str) -> CentralBodyId:
    return getattr(CentralBodyId, name)


def _mu_for_body(body: CentralBodyId) -> float:
    if body == CentralBodyId.earth:
        return MU_EARTH
    raise ValueError(f"No mu configured for body {body.name!r}")


def state_from_waypoint(event: MissionEvent, tdb_s: float) -> StateVector:
    _require_native()
    if event.type != "waypoint":
        raise ValueError("Not a waypoint event")
    if event.central_body is None or event.frame is None or event.representation is None:
        raise ValueError(f"Waypoint {event.id!r} missing frame, central_body, or representation")

    body = _body_from_name(event.central_body)
    frame = _frame_from_name(event.frame)
    epoch = Epoch(tdb_s)
    mu = _mu_for_body(body)

    if event.representation == "eci":
        if event.position_m is None or event.velocity_m_s is None:
            raise ValueError(f"Waypoint {event.id!r} missing ECI position/velocity")
        pos = np.asarray(event.position_m, dtype=np.float64).reshape(3)
        vel = np.asarray(event.velocity_m_s, dtype=np.float64).reshape(3)
        return StateVector(pos, vel, epoch, frame, body)

    if event.representation == "orbital_elements":
        if event.elements is None:
            raise ValueError(f"Waypoint {event.id!r} missing elements")
        el = event.elements
        return state_from_orbital_elements(
            float(el["a_m"]),
            float(el["e"]),
            float(el["i_rad"]),
            float(el["raan_rad"]),
            float(el["argp_rad"]),
            float(el["nu_rad"]),
            tdb_s,
            mu,
        )

    raise ValueError(f"Unsupported representation: {event.representation!r}")


def _coast_order(mission: Mission) -> list[MissionEvent]:
    coasts = [e for e in mission.events if e.type == "coast"]
    ordered: list[MissionEvent] = []
    remaining = {c.id: c for c in coasts}

    while remaining:
        progressed = False
        for cid, coast in list(remaining.items()):
            parent = coast.from_event
            if parent is None:
                raise ValueError(f"Coast {cid!r} missing from_event")
            parent_coast = remaining.get(parent)
            if parent_coast is None or parent_coast.id in {c.id for c in ordered}:
                ordered.append(coast)
                del remaining[cid]
                progressed = True
        if not progressed:
            raise ValueError("Coast events have unresolved dependency order")
    return ordered


def compile_mission(mission: Mission) -> Graph:
    """Build a C++ Graph from mission events (waypoints + coasts)."""
    _require_native()
    tdb = resolve_all_event_times(mission)
    graph = Graph()
    propagators: dict[str, PropagatorNode] = {}

    for coast in _coast_order(mission):
        if coast.from_event is None or coast.duration_s is None or coast.step_s is None:
            raise ValueError(f"Coast {coast.id!r} incomplete")
        step = float(coast.step_s)
        if step <= 0:
            raise ValueError("step_s must be positive")
        num_steps = max(1, int(round(float(coast.duration_s) / step)))

        parent = mission.event_by_id(coast.from_event)
        if parent is None:
            raise ValueError(f"Unknown from_event {coast.from_event!r}")

        node = PropagatorNode(coast.id, MU_EARTH)
        node.set_step_size(step)
        node.set_num_steps(num_steps)

        if parent.type == "waypoint":
            node.set_initial_state(state_from_waypoint(parent, tdb[parent.id]))
        elif parent.type == "coast":
            upstream = propagators.get(parent.id)
            if upstream is None:
                raise ValueError(f"Coast {parent.id!r} must be compiled before {coast.id!r}")
            graph.add_edge(Edge(upstream, "states", node, "initial_state"))
        else:
            raise ValueError(f"Coast cannot follow event type {parent.type!r}")

        graph.add_node(node)
        propagators[coast.id] = node

    if not propagators:
        # Single waypoint with no coast: one-shot propagator for visualization
        waypoints = [e for e in mission.events if e.type == "waypoint"]
        if len(waypoints) == 1:
            wp = waypoints[0]
            node = PropagatorNode("coast_auto", MU_EARTH)
            node.set_initial_state(state_from_waypoint(wp, tdb[wp.id]))
            node.set_step_size(60.0)
            node.set_num_steps(1)
            graph.add_node(node)
        elif not waypoints:
            raise ValueError("Mission has no coast or waypoint events to compile")

    return graph
