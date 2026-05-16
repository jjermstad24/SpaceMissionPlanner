"""Mission graph execution and multi-segment episodes."""

from __future__ import annotations

import pytest

pytest.importorskip("spacemissionplanner.spacemissionplanner_native")

from spacemissionplanner.mission_graph.execution import run_graph, topological_order
from spacemissionplanner.mission_graph.templates import earth_orbit_graph, two_segment_earth_orbit_graph
from spacemissionplanner.spacemissionplanner_native import PropagatorNode
from spacemissionplanner.visualization.mission_graph_io import episode_from_graph


def test_topological_order_two_segment() -> None:
    graph = two_segment_earth_orbit_graph(phase1_steps=5, phase2_steps=5)
    order = topological_order(graph)
    assert order == ["phase1", "phase2"]


def test_run_single_propagator() -> None:
    graph = earth_orbit_graph(num_steps=8)
    ran = run_graph(graph)
    assert ran == ["earth_prop"]
    node = graph.get_node("earth_prop")
    assert node is not None
    assert len(node.get_states()) == 9


def test_multi_segment_episode_length() -> None:
    graph = two_segment_earth_orbit_graph(phase1_steps=10, phase2_steps=15)
    ep = episode_from_graph(graph)
    # 11 + 15 points (drop duplicate join)
    assert ep.times.shape[0] == 26
    assert ep.trajectory_positions_m.shape == (26, 3)


def test_multi_segment_positions_continuous() -> None:
    graph = two_segment_earth_orbit_graph(phase1_steps=20, phase2_steps=20)
    run_graph(graph)
    p1 = graph.get_node("phase1")
    p2 = graph.get_node("phase2")
    assert p1 is not None and p2 is not None
    assert isinstance(p1, PropagatorNode) and isinstance(p2, PropagatorNode)
    end1 = p1.get_positions()[-1]
    start2 = p2.get_positions()[0]
    assert (abs(end1[0] - start2[0]) + abs(end1[1] - start2[1]) + abs(end1[2] - start2[2])) < 1.0
