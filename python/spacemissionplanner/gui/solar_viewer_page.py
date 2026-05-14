"""Main-window page: solar-system viewer + time scrubber + demo scene."""

from __future__ import annotations

from PySide6.QtCore import Qt, QTimer
from PySide6.QtWidgets import (
    QHBoxLayout,
    QLabel,
    QSlider,
    QVBoxLayout,
    QWidget,
)

from spacemissionplanner.visualization.demo_ephemeris import build_demo_viewer_episode
from spacemissionplanner.visualization.solar_system_view import SolarSystemViewWidget


class SolarViewerPage(QWidget):
    def __init__(self) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.setContentsMargins(16, 16, 16, 16)
        root.setSpacing(10)

        self._caption = QLabel()
        self._caption.setWordWrap(True)
        self._caption.setStyleSheet("color: #52525b; font-size: 12px;")
        root.addWidget(self._caption)

        self._view = SolarSystemViewWidget(self)
        root.addWidget(self._view, stretch=1)

        row = QHBoxLayout()
        self._slider = QSlider(Qt.Horizontal)
        self._slider.setMinimum(0)
        self._slider.setMaximum(0)
        self._slider.valueChanged.connect(self._on_slider)
        self._index_label = QLabel()
        self._index_label.setMinimumWidth(120)
        self._index_label.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        row.addWidget(QLabel("Time index"))
        row.addWidget(self._slider, stretch=1)
        row.addWidget(self._index_label)
        root.addLayout(row)

        self._episode = None
        self._load_demo_episode()

    def _load_demo_episode(self) -> None:
        ep = build_demo_viewer_episode(n_steps=200)
        self._episode = ep
        self._caption.setText(
            f"<b>Frame</b>: {ep.frame_name} — {ep.origin_description}<br>"
            f"<b>Time</b>: {ep.time_scale_note}<br>"
            "<b>Note</b>: Toy geometry for the viewer shell; SPICE-backed ephemeris will replace this."
        )
        self._view.set_episode(ep)
        n = ep.times.shape[0]
        self._slider.blockSignals(True)
        self._slider.setMaximum(max(0, n - 1))
        self._slider.setValue(0)
        self._slider.blockSignals(False)
        self._on_slider(0)
        QTimer.singleShot(0, self._view.reset_camera)

    def _on_slider(self, value: int) -> None:
        self._view.set_time_index(value)
        if self._episode is not None:
            t = self._episode.times[value]
            self._index_label.setText(f"step {value} / t = {t:.3e} s (demo)")
        else:
            self._index_label.setText(f"step {value}")
