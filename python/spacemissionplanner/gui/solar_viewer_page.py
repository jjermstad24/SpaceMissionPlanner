"""Main-window page: solar-system viewer + time scrubber + demo scene."""

from __future__ import annotations

from pathlib import Path

from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QHideEvent, QShowEvent
from PySide6.QtWidgets import (
    QFileDialog,
    QHBoxLayout,
    QLabel,
    QMessageBox,
    QPushButton,
    QSlider,
    QVBoxLayout,
    QWidget,
)

from spacemissionplanner.visualization.demo_ephemeris import build_demo_viewer_episode
from spacemissionplanner.visualization.episode_io import load_viewer_episode_from_json_path
from spacemissionplanner.visualization.solar_system_view import SolarSystemViewWidget


class SolarViewerPage(QWidget):
    def __init__(self) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.setContentsMargins(16, 16, 16, 16)
        root.setSpacing(10)

        btn_row = QHBoxLayout()
        self._btn_demo = QPushButton("Load demo")
        self._btn_demo.clicked.connect(self._on_load_demo_clicked)
        self._btn_file = QPushButton("Open trajectory JSON…")
        self._btn_file.clicked.connect(self._on_open_trajectory_json)
        btn_row.addWidget(self._btn_demo)
        btn_row.addWidget(self._btn_file)
        self._btn_play = QPushButton("Play")
        self._btn_play.clicked.connect(self._toggle_play)
        btn_row.addWidget(self._btn_play)
        btn_row.addStretch(1)
        root.addLayout(btn_row)

        self._play_timer = QTimer(self)
        self._play_timer.setInterval(50)
        self._play_timer.timeout.connect(self._on_play_tick)

        self._caption = QLabel(
            "<i>3D view loads when this tab is shown (VTK initializes after the main window is visible).</i>"
        )
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
        self._slider.sliderPressed.connect(self._stop_playback)
        self._index_label = QLabel()
        self._index_label.setMinimumWidth(160)
        self._index_label.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        row.addWidget(QLabel("Time index"))
        row.addWidget(self._slider, stretch=1)
        row.addWidget(self._index_label)
        root.addLayout(row)

        self._episode = None
        self._first_show_handled = False
        self._slider_suffix = ""

    def hideEvent(self, event: QHideEvent) -> None:
        super().hideEvent(event)
        self._stop_playback()

    def _stop_playback(self) -> None:
        if self._play_timer.isActive():
            self._play_timer.stop()
        self._btn_play.setText("Play")

    def _toggle_play(self) -> None:
        if self._play_timer.isActive():
            self._stop_playback()
            return
        if self._episode is None:
            return
        if self._slider.value() >= self._slider.maximum():
            self._slider.setValue(0)
        self._btn_play.setText("Pause")
        self._play_timer.start()

    def _on_play_tick(self) -> None:
        v = self._slider.value()
        if v >= self._slider.maximum():
            self._stop_playback()
            return
        self._slider.setValue(v + 1)

    def showEvent(self, event: QShowEvent) -> None:
        super().showEvent(event)
        if self._first_show_handled or not self.isVisible():
            return
        self._first_show_handled = True
        QTimer.singleShot(0, self._load_initial_demo)

    def _load_initial_demo(self) -> None:
        self._apply_episode(build_demo_viewer_episode(n_steps=200), "demo", None)

    def _on_load_demo_clicked(self) -> None:
        self._apply_episode(build_demo_viewer_episode(n_steps=200), "demo", None)

    def _on_open_trajectory_json(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open trajectory JSON",
            "",
            "Trajectory JSON (*.json);;All files (*)",
        )
        if not path:
            return
        try:
            ep = load_viewer_episode_from_json_path(Path(path))
        except (OSError, ValueError) as exc:
            QMessageBox.warning(self, "Could not load trajectory", str(exc))
            return
        self._apply_episode(ep, "file", Path(path))

    def _apply_episode(self, ep, source: str, path: Path | None) -> None:
        self._stop_playback()
        self._episode = ep
        if source == "demo":
            self._slider_suffix = " (demo)"
            note = "<b>Note</b>: Toy solar-system geometry; SPICE-backed ephemeris will replace this."
        else:
            self._slider_suffix = " (file)"
            note = f"<b>Source</b>: {path.name if path else '?'}"
        self._caption.setText(
            f"<b>Frame</b>: {ep.frame_name} — {ep.origin_description}<br>"
            f"<b>Time</b>: {ep.time_scale_note}<br>"
            f"{note}"
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
            self._index_label.setText(f"step {value} / t = {t:.3e} s{self._slider_suffix}")
        else:
            self._index_label.setText(f"step {value}{self._slider_suffix}")
