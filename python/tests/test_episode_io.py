from pathlib import Path

import json

import numpy as np
import pytest

from spacemissionplanner.visualization.episode_io import (
    load_viewer_episode_from_json_path,
    viewer_episode_from_trajectory_arrays,
)


def test_viewer_episode_from_arrays_circular() -> None:
    t = np.linspace(0.0, 100.0, 20)
    ang = 2.0 * np.pi * t / 100.0
    r = 7.0e6
    p = np.column_stack([r * np.cos(ang), r * np.sin(ang), np.zeros_like(ang)])
    ep = viewer_episode_from_trajectory_arrays(
        t,
        p,
        frame_name="TEST",
        origin_description="unit test",
        time_scale_note="s",
    )
    assert ep.trajectory_positions_m.shape == (20, 3)
    assert ep.body_positions_m["Sun"].shape == (20, 3)
    assert ep.trajectory_render_mode == "full_path"


def test_rejects_non_monotonic_times() -> None:
    t = np.array([0.0, 2.0, 1.0])
    p = np.zeros((3, 3))
    with pytest.raises(ValueError, match="strictly increasing"):
        viewer_episode_from_trajectory_arrays(t, p, frame_name="X", origin_description="", time_scale_note="")


def test_load_sample_json() -> None:
    root = Path(__file__).resolve().parents[2]
    path = root / "examples" / "viewer" / "sample_trajectory.json"
    ep = load_viewer_episode_from_json_path(path)
    assert ep.times.shape[0] == 22
    assert ep.trajectory_positions_m.shape == (22, 3)


def test_json_schema_version_enforced(tmp_path: Path) -> None:
    bad = {"schema_version": 99, "frame_name": "x", "origin_description": "y", "time_scale_note": "z", "times_s": [0, 1], "positions_m": [[0, 0, 0], [1, 0, 0]]}
    tmp = tmp_path / "bad.json"
    tmp.write_text(json.dumps(bad), encoding="utf-8")
    with pytest.raises(ValueError, match="schema_version"):
        load_viewer_episode_from_json_path(tmp)


def test_json_bad_trajectory_render_mode(tmp_path: Path) -> None:
    bad = {
        "schema_version": 1,
        "frame_name": "x",
        "origin_description": "y",
        "time_scale_note": "z",
        "times_s": [0, 1],
        "positions_m": [[0, 0, 0], [1, 0, 0]],
        "trajectory_render_mode": "nope",
    }
    p = tmp_path / "bad_mode.json"
    p.write_text(json.dumps(bad), encoding="utf-8")
    with pytest.raises(ValueError, match="trajectory_render_mode"):
        load_viewer_episode_from_json_path(p)
