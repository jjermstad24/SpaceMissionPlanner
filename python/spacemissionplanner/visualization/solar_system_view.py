"""PyVista + Qt 3D viewer widget (bodies + trajectory)."""

from __future__ import annotations

from typing import TYPE_CHECKING, Optional

import numpy as np
from PySide6.QtCore import Qt
from PySide6.QtGui import QImage, QPixmap, QResizeEvent
from PySide6.QtWidgets import QLabel, QVBoxLayout, QWidget

from spacemissionplanner.gui.vtk_platform import resolve_vtk_display_mode

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
        self._display_mode = resolve_vtk_display_mode()
        self._frame_label: QLabel | None = None
        self._episode: Optional["ViewerEpisode"] = None
        self._time_index = 0
        self._missing = QLabel(
            "3D viewer needs PyVista and pyvistaqt.\n"
            "Install with: pip install pyvista pyvistaqt"
        )
        self._missing.setWordWrap(True)
        # Defer VTK until first episode (see SolarViewerPage.showEvent) so the top-level window exists.

    def display_mode(self) -> str:
        return self._display_mode

    def has_plotter(self) -> bool:
        return self._plotter is not None

    def _render_size(self) -> tuple[int, int]:
        w = max(self.width(), 320)
        h = max(self.height(), 240)
        return w, h

    def _ensure_plotter(self) -> None:
        if self._plotter is not None or self._plotter_failed:
            return
        try:
            import pyvista as pv
        except ImportError:
            self._layout.addWidget(self._missing)
            self._plotter_failed = True
            return

        pv.set_plot_theme("document")

        if self._display_mode == "offscreen":
            w, h = self._render_size()
            self._plotter = pv.Plotter(off_screen=True, window_size=(w, h))
            self._frame_label = QLabel(self)
            self._frame_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
            self._frame_label.setMinimumSize(320, 240)
            self._frame_label.setStyleSheet("background-color: #0f172a;")
            self._layout.addWidget(self._frame_label)
        else:
            try:
                from pyvistaqt import QtInteractor
            except ImportError:
                self._layout.addWidget(self._missing)
                self._plotter_failed = True
                return

            self._plotter = QtInteractor(self, multi_samples=0)
            self._layout.addWidget(self._plotter)
            rw = getattr(self._plotter, "ren_win", None) or getattr(self._plotter, "render_window", None)
            if rw is not None and hasattr(rw, "SetMultiSamples"):
                rw.SetMultiSamples(0)

        self._plotter.set_background("#0f172a")
        self._plotter.show_axes()

    def _present_offscreen_frame(self) -> None:
        if self._plotter is None or self._frame_label is None:
            return
        w, h = self._render_size()
        self._plotter.window_size = (w, h)
        img = np.asarray(self._plotter.screenshot(None, return_img=True), dtype=np.uint8)
        if img.ndim != 3 or img.shape[0] < 1 or img.shape[1] < 1:
            return
        img = np.ascontiguousarray(img)
        ih, iw, ch = img.shape
        if ch == 4:
            fmt = QImage.Format.Format_RGBA8888
            stride = 4 * iw
        else:
            fmt = QImage.Format.Format_RGB888
            stride = 3 * iw
        qimg = QImage(img.data, iw, ih, stride, fmt).copy()
        self._frame_label.setPixmap(QPixmap.fromImage(qimg))

    def resizeEvent(self, event: QResizeEvent) -> None:
        super().resizeEvent(event)
        if self._display_mode == "offscreen" and self._plotter is not None and self._episode is not None:
            self._redraw()

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
            if self._display_mode == "offscreen" and self._episode is not None:
                self._redraw()

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

        if self._display_mode == "offscreen":
            self._present_offscreen_frame()
        else:
            self._plotter.render()
