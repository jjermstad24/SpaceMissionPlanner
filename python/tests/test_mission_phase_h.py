"""Mission schema v2: clocks, compile, episode."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

pytest.importorskip("spacemissionplanner.spacemissionplanner_native")

from spacemissionplanner.mission import (
    compile_mission,
    default_leo_mission,
    load_mission_json,
    resolve_all_event_times,
    save_mission_json,
    two_phase_leo_mission,
)
from spacemissionplanner.mission_graph.execution import run_graph
from spacemissionplanner.visualization.mission_graph_io import episode_from_mission


def test_resolve_mission_elapsed() -> None:
    mission = default_leo_mission()
    tdb = resolve_all_event_times(mission)
    assert tdb["launch"] == 0.0
    assert tdb["wp0"] == 0.0
    assert tdb["coast1"] == 0.0


def test_compile_and_run_leo() -> None:
    mission = default_leo_mission()
    graph = compile_mission(mission)
    assert graph.get_node("coast1") is not None
    ran = run_graph(graph)
    assert ran == ["coast1"]


def test_two_phase_compile_chain() -> None:
    mission = two_phase_leo_mission()
    graph = compile_mission(mission)
    assert len(graph.get_edges()) == 1
    run_graph(graph)


def test_mission_json_round_trip(tmp_path: Path) -> None:
    path = tmp_path / "mission.json"
    mission = default_leo_mission()
    save_mission_json(path, mission)
    loaded = load_mission_json(path)
    assert loaded.name == mission.name
    assert len(loaded.events) == len(mission.events)


def test_episode_from_mission() -> None:
    mission = default_leo_mission()
    ep = episode_from_mission(mission)
    assert ep.trajectory_positions_m.shape[0] >= 60
