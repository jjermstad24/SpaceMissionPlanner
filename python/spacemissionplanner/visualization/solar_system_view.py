"""PyVista + Qt 3D viewer widget (bodies + trajectory)."""

from __future__ import annotations

from typing import TYPE_CHECKING, Optional

import numpy as np
from PySide6.QtWidgets import QLabel, QVBoxLayout, QWidget

if TYPE_CHECKING:
    from spacemissionplanner.visualization.viewer_data import ViewerEpisode


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

        traj = ep.trajectory_positions_m[: k + 1]
        if traj.shape[0] >= 2:
            poly = pv.PolyData(traj)
            n = traj.shape[0]
            poly.lines = np.hstack([[n], np.arange(n, dtype=np.int32)])
            scale_r = float(np.clip(0.006 * np.max(np.linalg.norm(traj, axis=1)), 1e8, 5e10))
            try:
                mesh = poly.tube(radius=scale_r)
                self._plotter.add_mesh(mesh, color="#f8fafc", smooth_shading=True, name="trajectory")
            except Exception:
                line = pv.lines_from_points(traj)
                self._plotter.add_mesh(line, color="#f8fafc", line_width=4, name="trajectory")

        self._plotter.render()
