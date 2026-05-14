import numpy as np
import pytest

from spacemissionplanner.visualization.demo_ephemeris import build_demo_viewer_episode
from spacemissionplanner.visualization.viewer_data import ViewerEpisode, resample_trajectory_to_times


def test_demo_episode_validates() -> None:
    ep = build_demo_viewer_episode(n_steps=32)
    assert ep.frame_name
    assert len(ep.body_ids) >= 9
    assert "Earth" in ep.body_positions_m
    assert ep.trajectory_render_mode == "grow"


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


def test_viewer_episode_rejects_bad_render_mode() -> None:
    t = np.linspace(0, 1, 5)
    bodies = {"Sun": np.zeros((5, 3))}
    r = {"Sun": 1e9}
    traj = np.zeros((5, 3))
    with pytest.raises(ValueError, match="trajectory_render_mode"):
        ViewerEpisode(
            frame_name="X",
            origin_description="",
            time_scale_note="",
            times=t,
            body_ids=("Sun",),
            body_positions_m=bodies,
            body_display_radius_m=r,
            trajectory_positions_m=traj,
            trajectory_render_mode="invalid",  # type: ignore[arg-type]
        )


def test_resample_trajectory() -> None:
    tt = np.array([0.0, 1.0, 2.0])
    traj = np.array([[0.0, 0.0, 0.0], [1.0, 0.0, 0.0], [2.0, 0.0, 0.0]])
    tq = np.array([0.5, 1.5])
    out = resample_trajectory_to_times(traj, tt, tq)
    assert out.shape == (2, 3)
    np.testing.assert_allclose(out[0], [0.5, 0.0, 0.0])
    np.testing.assert_allclose(out[1], [1.5, 0.0, 0.0])


def test_viewer_episode_relocation() -> None:
    t = np.array([0.0, 10.0])
    bodies = {
        "Sun": np.array([[0.0, 0.0, 0.0], [0.0, 0.0, 0.0]]),
        "Earth": np.array([[100.0, 0.0, 0.0], [110.0, 0.0, 0.0]]),
    }
    radii = {"Sun": 1.0, "Earth": 1.0}
    traj = np.array([[10.0, 0.0, 0.0], [15.0, 0.0, 0.0]])
    ep = ViewerEpisode("F", "D", "N", t, ("Sun", "Earth"), bodies, radii, traj)

    new_ep = ep.relocated_to_body("Earth")
    assert new_ep.origin_description.startswith("Relocated to Earth")
    np.testing.assert_allclose(new_ep.body_positions_m["Sun"], [[-100.0, 0.0, 0.0], [-110.0, 0.0, 0.0]])
    np.testing.assert_allclose(new_ep.body_positions_m["Earth"], [[0.0, 0.0, 0.0], [0.0, 0.0, 0.0]])
    np.testing.assert_allclose(new_ep.trajectory_positions_m, [[-90.0, 0.0, 0.0], [-95.0, 0.0, 0.0]])

