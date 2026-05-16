"""Mission timeline panel (schema v2) with compile → graph → 3D."""

from __future__ import annotations

from collections.abc import Callable
from pathlib import Path
from typing import Optional

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QComboBox,
    QDoubleSpinBox,
    QFileDialog,
    QHBoxLayout,
    QLabel,
    QMessageBox,
    QPushButton,
    QTreeWidget,
    QTreeWidgetItem,
    QVBoxLayout,
    QWidget,
)

from spacemissionplanner.wrappers import native_extension_status
from spacemissionplanner.wrappers.backend import NativeExtensionStatus

ViewerCallback = Callable[[object], None]


class MissionGraphPage(QWidget):
    def __init__(self, on_view_in_3d: Optional[ViewerCallback] = None) -> None:
        super().__init__()
        self._mission = None
        self._graph = None
        self._path: Path | None = None
        self._tdb_by_event: dict[str, float] = {}
        self._on_view_in_3d = on_view_in_3d

        root = QVBoxLayout(self)
        root.setContentsMargins(24, 24, 24, 24)
        root.setSpacing(10)

        title = QLabel("Mission timeline")
        title.setStyleSheet("font-size: 16px; font-weight: bold;")
        root.addWidget(title)

        clock_row = QHBoxLayout()
        clock_row.addWidget(QLabel("Display clock:"))
        self._clock_combo = QComboBox()
        self._clock_combo.currentIndexChanged.connect(self._refresh_timeline)
        clock_row.addWidget(self._clock_combo)
        clock_row.addSpacing(16)
        clock_row.addWidget(QLabel("Scene epoch (TDB s):"))
        self._scene_epoch = QDoubleSpinBox()
        self._scene_epoch.setRange(-1e12, 1e12)
        self._scene_epoch.setDecimals(3)
        self._scene_epoch.setValue(0.0)
        clock_row.addWidget(self._scene_epoch)
        clock_row.addStretch(1)
        root.addLayout(clock_row)

        self._status = QLabel("No mission loaded.")
        self._status.setWordWrap(True)
        self._status.setStyleSheet("color: #3f3f46;")
        root.addWidget(self._status)

        self._timeline = QTreeWidget()
        self._timeline.setHeaderLabels(["Event", "Time"])
        self._timeline.setMinimumHeight(160)
        self._timeline.setAlternatingRowColors(True)
        root.addWidget(self._timeline)

        row = QHBoxLayout()
        self._btn_new = QPushButton("New LEO mission")
        self._btn_new.clicked.connect(self._on_new_leo)
        self._btn_two = QPushButton("New 2-phase")
        self._btn_two.clicked.connect(self._on_new_two_phase)
        self._btn_open = QPushButton("Open mission…")
        self._btn_open.clicked.connect(self._on_open_mission)
        self._btn_save = QPushButton("Save mission…")
        self._btn_save.clicked.connect(self._on_save_mission)
        self._btn_graph = QPushButton("Open graph debug…")
        self._btn_graph.clicked.connect(self._on_open_graph)
        self._btn_run = QPushButton("Run")
        self._btn_run.clicked.connect(self._on_run)
        self._btn_view = QPushButton("Run && view in 3D")
        self._btn_view.clicked.connect(self._on_run_and_view)
        for b in (
            self._btn_new,
            self._btn_two,
            self._btn_open,
            self._btn_save,
            self._btn_graph,
            self._btn_run,
            self._btn_view,
        ):
            row.addWidget(b)
        row.addStretch(1)
        root.addLayout(row)

        hint = QLabel(
            "Missions use schema v2 (waypoints + coasts). Inertial frames only. "
            "Graph JSON is available under Open graph debug."
        )
        hint.setWordWrap(True)
        hint.setStyleSheet("color: #71717a; font-style: italic;")
        root.addWidget(hint)
        root.addStretch(1)

        self._update_native_hint()

    def scene_epoch_tdb(self) -> float:
        return float(self._scene_epoch.value())

    def display_clock_id(self) -> str:
        return self._clock_combo.currentData() or "tdb"

    def _update_native_hint(self) -> None:
        info = native_extension_status()
        buttons = (
            self._btn_new,
            self._btn_two,
            self._btn_open,
            self._btn_save,
            self._btn_graph,
            self._btn_run,
            self._btn_view,
        )
        if info.status != NativeExtensionStatus.LOADED:
            self._status.setText("C++ extension not loaded — build native bindings (`make`).")
            for btn in buttons:
                btn.setEnabled(False)

    def _set_mission(self, mission, path: Path | None = None) -> None:
        self._mission = mission
        self._path = path
        self._graph = None
        self._clock_combo.clear()
        for c in mission.clocks:
            self._clock_combo.addItem(f"{c.id} ({c.kind})", c.id)
        try:
            from spacemissionplanner.mission.clocks import resolve_all_event_times

            self._tdb_by_event = resolve_all_event_times(mission)
        except Exception as exc:
            self._tdb_by_event = {}
            QMessageBox.warning(self, "Time resolution failed", str(exc))
        if self._tdb_by_event:
            launch_t = self._tdb_by_event.get("launch", 0.0)
            self._scene_epoch.setValue(launch_t)
        self._refresh_timeline()

    def _on_new_leo(self) -> None:
        try:
            from spacemissionplanner.mission.templates import default_leo_mission

            self._set_mission(default_leo_mission())
        except Exception as exc:
            QMessageBox.warning(self, "Could not create mission", str(exc))

    def _on_new_two_phase(self) -> None:
        try:
            from spacemissionplanner.mission.templates import two_phase_leo_mission

            self._set_mission(two_phase_leo_mission())
        except Exception as exc:
            QMessageBox.warning(self, "Could not create mission", str(exc))

    def _on_open_mission(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open mission JSON",
            "",
            "Mission JSON (*.json);;All files (*)",
        )
        if not path:
            return
        try:
            from spacemissionplanner.mission.io import load_mission_json

            self._set_mission(load_mission_json(Path(path)), Path(path))
        except Exception as exc:
            QMessageBox.warning(self, "Could not load mission", str(exc))

    def _on_save_mission(self) -> None:
        if self._mission is None:
            return
        path = self._path
        if path is None:
            chosen, _ = QFileDialog.getSaveFileName(
                self,
                "Save mission JSON",
                "mission.json",
                "Mission JSON (*.json);;All files (*)",
            )
            if not chosen:
                return
            path = Path(chosen)
        try:
            from spacemissionplanner.mission.io import save_mission_json

            save_mission_json(path, self._mission)
            self._path = path
            self._refresh_timeline()
        except Exception as exc:
            QMessageBox.warning(self, "Could not save mission", str(exc))

    def _on_open_graph(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open mission graph JSON (debug)",
            "",
            "Graph JSON (*.json);;All files (*)",
        )
        if not path:
            return
        try:
            from spacemissionplanner.mission_graph.serialization import load_graph_json

            self._graph = load_graph_json(Path(path))
            self._mission = None
            self._path = Path(path)
            self._refresh_graph_debug()
        except Exception as exc:
            QMessageBox.warning(self, "Could not load graph", str(exc))

    def _refresh_graph_debug(self) -> None:
        self._timeline.clear()
        if self._graph is None:
            return
        self._status.setText(f"<b>Debug graph</b>: {self._path.name if self._path else '?'}")
        root = QTreeWidgetItem(["Graph (debug)", ""])
        self._timeline.addTopLevelItem(root)
        for node in self._graph.get_nodes():
            QTreeWidgetItem(root, [node.name(), type(node).__name__])
        for edge in self._graph.get_edges():
            QTreeWidgetItem(
                root,
                [
                    f"{edge.source().name()} → {edge.target().name()}",
                    f"{edge.source_output()} → {edge.target_input()}",
                ],
            )
        root.setExpanded(True)

    def _format_event_time(self, event_id: str) -> str:
        from spacemissionplanner.mission.clocks import format_time_in_clock

        if self._mission is None or event_id not in self._tdb_by_event:
            return "?"
        clock_id = self.display_clock_id()
        return format_time_in_clock(
            self._mission, clock_id, self._tdb_by_event[event_id], self._tdb_by_event
        )

    def _refresh_timeline(self) -> None:
        self._timeline.clear()
        if self._mission is None:
            self._status.setText("No mission loaded.")
            return

        src = self._path.name if self._path else "(unsaved)"
        n = len(self._mission.events)
        self._status.setText(f"<b>Mission</b>: {self._mission.name} &nbsp; <b>File</b>: {src} &nbsp; <b>Events</b>: {n}")

        vehicle = QTreeWidgetItem([f"Vehicle: {self._mission.vehicle.name}", ""])
        self._timeline.addTopLevelItem(vehicle)
        for stage in self._mission.vehicle.stages:
            QTreeWidgetItem(vehicle, [f"Stage {stage.id}", f"Isp={stage.Isp_s}s"])

        seq = QTreeWidgetItem(["Sequence", ""])
        self._timeline.addTopLevelItem(seq)
        for ev in self._mission.events:
            detail = ""
            if ev.type == "waypoint" and ev.representation:
                detail = ev.representation
            elif ev.type == "coast" and ev.duration_s is not None:
                detail = f"{ev.duration_s:.0f}s coast"
            QTreeWidgetItem(seq, [f"{ev.id} ({ev.type})", self._format_event_time(ev.id)])
            if detail:
                QTreeWidgetItem(seq, ["", detail])

        vehicle.setExpanded(True)
        seq.setExpanded(True)

    def _compile(self):
        from spacemissionplanner.mission.compile import compile_mission

        if self._mission is None:
            raise RuntimeError("No mission loaded")
        self._graph = compile_mission(self._mission)
        return self._graph

    def _on_run(self) -> None:
        try:
            graph = self._graph if self._graph is not None else self._compile()
            from spacemissionplanner.mission_graph.execution import run_graph

            ran = run_graph(graph)
            QMessageBox.information(self, "Run", f"Computed {len(ran)} propagator node(s).")
        except Exception as exc:
            QMessageBox.warning(self, "Run failed", str(exc))

    def _on_run_and_view(self) -> None:
        try:
            if self._mission is not None:
                from spacemissionplanner.visualization.mission_graph_io import episode_from_mission

                ep = episode_from_mission(self._mission)
            elif self._graph is not None:
                from spacemissionplanner.mission_graph.execution import run_graph
                from spacemissionplanner.visualization.mission_graph_io import episode_from_graph

                run_graph(self._graph)
                ep = episode_from_graph(self._graph, run=False)
            else:
                return
            if self._on_view_in_3d:
                self._on_view_in_3d(ep)
        except Exception as exc:
            QMessageBox.warning(self, "Run && view failed", str(exc))

    def mission(self):
        return self._mission

    def graph(self):
        return self._graph
