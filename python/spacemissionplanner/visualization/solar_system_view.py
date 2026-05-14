"""PyVista + Qt 3D viewer widget (bodies + trajectory)."""

from __future__ import annotations

from typing import TYPE_CHECKING, Optional

import numpy as np
from PySide6.QtWidgets import QLabel, QVBoxLayout, QWidget

if TYPE_CHECKING:
    from spacemissionplanner.visualization.viewer_data import ViewerEpisode


def _tube_radius_from_points(points: np.ndarray) -> float:
    span = float(np.max(np.ptp(points, axis=0)))
    ext = float(np.max(np.linalg.norm(points, axis=1)))
    if not np.isfinite(span):
        span = 0.0
    if not np.isfinite(ext):
        ext = 0.0
    base = max(span * 0.012, ext * 0.005, 8e4)
    return float(np.clip(base, 8e4, 8e10))


def _cursor_radius(points: np.ndarray, k: int) -> float:
    span = float(max(np.max(np.ptp(points, axis=0)), 1.0))
    loc = float(np.linalg.norm(points[k]))
    return float(np.clip(0.025 * max(span, loc), 1.2e5, 3e9))


def _add_polyline(
    plotter,
    points: np.ndarray,
    *,
    color: str,
    line_width: int,
    tube: bool,
    name: str,
) -> None:
    import pyvista as pv

    if points.shape[0] < 2:
        return
    poly = pv.PolyData(points)
    n = points.shape[0]
    poly.lines = np.hstack([[n], np.arange(n, dtype=np.int32)])
    if tube:
        r = _tube_radius_from_points(points)
        try:
            mesh = poly.tube(radius=r)
            plotter.add_mesh(mesh, color=color, smooth_shading=True, name=name)
        except Exception:
            line = pv.lines_from_points(points)
            plotter.add_mesh(line, color=color, line_width=line_width, name=name)
    else:
        line = pv.lines_from_points(points)
        plotter.add_mesh(line, color=color, line_width=line_width, name=name)


class SolarSystemViewWidget(QWidget):
    """Renders a ``ViewerEpisode`` with a time index; bodies as spheres, trajectory as a thick line."""

    def __init__(self, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        self._layout = QVBoxLayout(self)
        self._layout.setContentsMargins(0, 0, 0, 0)
        self._plotter = None
        self._plotter_failed = False
        self._episode: Optional["ViewerEpisode"] = None
        self._time_index = 0
        self._missing = QLabel(
            "3D viewer needs PyVista and pyvistaqt.\n"
            "Install with: pip install pyvista pyvistaqt"
        )
        self._missing.setWordWrap(True)
        # Defer QtInteractor / VTK until first episode (see SolarViewerPage.showEvent) so the
        # top-level window exists — avoids X11 BadWindow on WSL/X11 when VTK configures too early.

    def has_plotter(self) -> bool:
        return self._plotter is not None

    def _ensure_plotter(self) -> None:
        if self._plotter is not None or self._plotter_failed:
            return
        try:
            import pyvista as pv
            from pyvistaqt import QtInteractor
        except ImportError:
            self._layout.addWidget(self._missing)
            self._plotter_failed = True
            return

        pv.set_plot_theme("document")
        self._plotter = QtInteractor(self)
        self._layout.addWidget(self._plotter)
        self._plotter.set_background("#0f172a")
        self._plotter.show_axes()
        rw = getattr(self._plotter, "ren_win", None) or getattr(self._plotter, "render_window", None)
        if rw is not None and hasattr(rw, "SetMultiSamples"):
            rw.SetMultiSamples(0)

    def set_episode(self, episode: "ViewerEpisode") -> None:
        self._ensure_plotter()
        self._episode = episode
        self._time_index = 0
        self._redraw()

    def set_time_index(self, idx: int) -> None:
        if self._episode is None:
            return
        self._ensure_plotter()
        n = self._episode.times.shape[0]
        self._time_index = int(np.clip(idx, 0, n - 1))
        self._redraw()

    def reset_camera(self) -> None:
        if self._plotter is not None:
            self._plotter.reset_camera()

    def _draw_trajectory(self, ep: "ViewerEpisode", k: int) -> None:
        import pyvista as pv

        full = np.asarray(ep.trajectory_positions_m, dtype=np.float64)
        if full.shape[0] < 2:
            return

        mode = ep.trajectory_render_mode

        if mode == "full_path":
            _add_polyline(self._plotter, full, color="#f8fafc", line_width=5, tube=True, name="trajectory")
            cr = _cursor_radius(full, k)
            pt = full[k]
            sph = pv.Sphere(radius=cr, center=pt, theta_resolution=20, phi_resolution=20)
            self._plotter.add_mesh(sph, color="#38bdf8", smooth_shading=True, name="trajectory_cursor")
            return

        # grow: faint full path so index 0 is not empty, plus highlighted prefix
        _add_polyline(self._plotter, full, color="#64748b", line_width=2, tube=False, name="trajectory_full")
        seg = full[: k + 1]
        if seg.shape[0] >= 2:
            _add_polyline(self._plotter, seg, color="#f8fafc", line_width=4, tube=True, name="trajectory_head")
        elif seg.shape[0] == 1:
            cr = _cursor_radius(full, 0)
            sph = pv.Sphere(radius=cr, center=seg[0], theta_resolution=18, phi_resolution=18)
            self._plotter.add_mesh(sph, color="#38bdf8", smooth_shading=True, name="trajectory_cursor")

    def _redraw(self) -> None:
        if self._plotter is None or self._episode is None:
            return

        import pyvista as pv

        from spacemissionplanner.visualization import demo_ephemeris

        ep = self._episode
        k = self._time_index
        self._plotter.clear()
        self._plotter.set_background("#0f172a")
        self._plotter.show_axes()

        preferred = (
            "Sun",
            "Jupiter",
            "Saturn",
            "Uranus",
            "Neptune",
            "Mars",
            "Earth",
            "Venus",
            "Mercury",
            "Moon",
        )
        ordered = tuple(b for b in preferred if b in ep.body_ids) + tuple(
            b for b in ep.body_ids if b not in preferred
        )
        for bid in ordered:
            pos = ep.body_positions_m[bid][k]
            r = ep.body_display_radius_m[bid]
            color = demo_ephemeris.body_color(bid)
            sph = pv.Sphere(radius=r, center=pos, theta_resolution=24, phi_resolution=24)
            self._plotter.add_mesh(sph, color=color, smooth_shading=True, name=f"body_{bid}")

        self._draw_trajectory(ep, k)

        self._plotter.render()
