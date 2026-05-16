"""Mission graph JSON serialization round-trip."""

from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
import pytest

from spacemissionplanner.mission_graph.serialization import (
    MISSION_GRAPH_JSON_SCHEMA_VERSION,
    graph_from_dict,
    graph_to_dict,
    load_graph_json,
    save_graph_json,
)

pytest.importorskip("spacemissionplanner.spacemissionplanner_native")

from spacemissionplanner.spacemissionplanner_native import (  # noqa: E402
    CentralBodyId,
    Epoch,
    Frame,
    Graph,
    PropagatorNode,
    StateVector,
)


def _earth_orbit_graph() -> Graph:
    mu = 3.986004418e14
    r = 6371e3 + 500e3
    v = math.sqrt(mu / r)
    state = StateVector(
        np.array([r, 0.0, 0.0]),
        np.array([0.0, v, 0.0]),
        Epoch.J2000(),
        Frame.J2000(),
        CentralBodyId.earth,
    )
    node = PropagatorNode("earth_prop", mu)
    node.set_initial_state(state)
    node.set_step_size(60.0)
    node.set_num_steps(10)
    graph = Graph()
    graph.add_node(node)
    return graph


def test_graph_round_trip_dict() -> None:
    graph = _earth_orbit_graph()
    data = graph_to_dict(graph)
    assert data["schema_version"] == MISSION_GRAPH_JSON_SCHEMA_VERSION
    assert len(data["nodes"]) == 1
    assert data["nodes"][0]["type"] == "propagator"

    restored = graph_from_dict(data)
    node = restored.get_node("earth_prop")
    assert node is not None
    assert isinstance(node, PropagatorNode)
    assert node.num_steps() == 10
    assert node.step_size() == 60.0
    assert node.has_initial_state()


def test_graph_json_file_round_trip(tmp_path: Path) -> None:
    graph = _earth_orbit_graph()
    path = tmp_path / "mission.json"
    save_graph_json(path, graph)
    loaded = load_graph_json(path)
    node = loaded.get_node("earth_prop")
    assert node is not None
    node.compute()
    orig = graph.get_node("earth_prop")
    assert orig is not None
    orig.compute()
    assert np.allclose(node.get_positions(), orig.get_positions())


def test_two_segment_round_trip() -> None:
    from spacemissionplanner.mission_graph.templates import two_segment_earth_orbit_graph

    graph = two_segment_earth_orbit_graph(phase1_steps=5, phase2_steps=5)
    data = graph_to_dict(graph)
    assert len(data["edges"]) == 1
    restored = graph_from_dict(data)
    assert len(restored.get_edges()) == 1


def test_deterministic_json_keys(tmp_path: Path) -> None:
    path = tmp_path / "mission.json"
    save_graph_json(path, _earth_orbit_graph())
    parsed = json.loads(path.read_text(encoding="utf-8"))
    assert list(parsed.keys()) == sorted(parsed.keys())
