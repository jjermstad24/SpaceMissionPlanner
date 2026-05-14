import numpy as np
import pytest

from spacemissionplanner.visualization.demo_ephemeris import build_demo_viewer_episode
from spacemissionplanner.visualization.viewer_data import ViewerEpisode, resample_trajectory_to_times


def test_demo_episode_validates() -> None:
    ep = build_demo_viewer_episode(n_steps=32)
    assert ep.frame_name
    assert len(ep.body_ids) >= 9
    assert "Earth" in ep.body_positions_m


def test_viewer_episode_rejects_bad_trajectory() -> None:
    t = np.linspace(0, 1, 5)
    bodies = {"Sun": np.zeros((5, 3))}
    r = {"Sun": 1e9}
    with pytest.raises(ValueError):
        ViewerEpisode(
            frame_name="X",
            origin_description="",
            time_scale_note="",
            times=t,
            body_ids=("Sun",),
            body_positions_m=bodies,
            body_display_radius_m=r,
            trajectory_positions_m=np.zeros((1, 3)),
        )


def test_resample_trajectory() -> None:
    tt = np.array([0.0, 1.0, 2.0])
    traj = np.array([[0.0, 0.0, 0.0], [1.0, 0.0, 0.0], [2.0, 0.0, 0.0]])
    tq = np.array([0.5, 1.5])
    out = resample_trajectory_to_times(traj, tt, tq)
    assert out.shape == (2, 3)
    np.testing.assert_allclose(out[0], [0.5, 0.0, 0.0])
    np.testing.assert_allclose(out[1], [1.5, 0.0, 0.0])
