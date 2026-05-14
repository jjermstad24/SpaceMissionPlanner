import numpy as np
import pytest

from spacemissionplanner.visualization.scene import TrajectoryScene, pyvista_available


def test_scene_positions_roundtrip() -> None:
    pts = np.array([[0.0, 0.0, 0.0], [1.0, 0.0, 0.0]], dtype=np.float64)
    scene = TrajectoryScene(pts)
    assert scene.positions().shape == (2, 3)


def test_scene_rejects_bad_shape() -> None:
    with pytest.raises(ValueError):
        TrajectoryScene(np.zeros((2, 2)))


@pytest.mark.skipif(not pyvista_available(), reason="PyVista not installed")
def test_scene_pyvista_polyline() -> None:
    pts = np.array([[0.0, 0.0, 0.0], [7e6, 0.0, 0.0]], dtype=np.float64)
    poly = TrajectoryScene(pts).to_pyvista_polyline()
    assert poly.n_points == 2
