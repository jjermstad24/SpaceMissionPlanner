"""Mission graph panel: load/save JSON graphs, run propagation, view in 3D."""

from __future__ import annotations

from collections.abc import Callable
from pathlib import Path
from typing import Optional

from PySide6.QtWidgets import (
    QFileDialog,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QMessageBox,
    QPushButton,
    QVBoxLayout,
    QWidget,
)

from spacemissionplanner.wrappers import native_extension_status
from spacemissionplanner.wrappers.backend import NativeExtensionStatus

ViewerCallback = Callable[[object], None]


class MissionGraphPage(QWidget):
    def __init__(self, on_view_in_3d: Optional[ViewerCallback] = None) -> None:
        super().__init__()
        self._graph = None
        self._path: Path | None = None
        self._on_view_in_3d = on_view_in_3d

        root = QVBoxLayout(self)
        root.setContentsMargins(24, 24, 24, 24)
        root.setSpacing(12)

        title = QLabel("Mission graph")
        title.setStyleSheet("font-size: 16px; font-weight: bold;")
        root.addWidget(title)

        self._status = QLabel("No graph loaded.")
        self._status.setWordWrap(True)
        self._status.setStyleSheet("color: #3f3f46;")
        root.addWidget(self._status)

        self._structure = QListWidget()
        self._structure.setMinimumHeight(120)
        root.addWidget(self._structure)

        row = QHBoxLayout()
        self._btn_new = QPushButton("New Earth orbit")
        self._btn_new.clicked.connect(self._on_new_template)
        self._btn_two = QPushButton("New 2-segment")
        self._btn_two.clicked.connect(self._on_new_two_segment)
        self._btn_open = QPushButton("Open JSON…")
        self._btn_open.clicked.connect(self._on_open)
        self._btn_save = QPushButton("Save JSON…")
        self._btn_save.clicked.connect(self._on_save)
        self._btn_run = QPushButton("Run graph")
        self._btn_run.clicked.connect(self._on_run)
        self._btn_view = QPushButton("Run && view in 3D")
        self._btn_view.clicked.connect(self._on_run_and_view)
        row.addWidget(self._btn_new)
        row.addWidget(self._btn_two)
        row.addWidget(self._btn_open)
        row.addWidget(self._btn_save)
        row.addWidget(self._btn_run)
        row.addWidget(self._btn_view)
        row.addStretch(1)
        root.addLayout(row)

        hint = QLabel(
            "Graph structure is listed above. Chained propagators merge into one trajectory in the 3D viewer."
        )
        hint.setWordWrap(True)
        hint.setStyleSheet("color: #71717a; font-style: italic;")
        root.addWidget(hint)
        root.addStretch(1)

        self._update_native_hint()

    def _update_native_hint(self) -> None:
        info = native_extension_status()
        if info.status != NativeExtensionStatus.LOADED:
            self._status.setText("C++ extension not loaded — build native bindings to use mission graphs.")
            for btn in (
                self._btn_new,
                self._btn_two,
                self._btn_open,
                self._btn_save,
                self._btn_run,
                self._btn_view,
            ):
                btn.setEnabled(False)

    def _on_new_template(self) -> None:
        try:
            from spacemissionplanner.mission_graph.templates import earth_orbit_graph
        except (ImportError, RuntimeError) as exc:
            QMessageBox.warning(self, "Could not create graph", str(exc))
            return
        self._graph = earth_orbit_graph()
        self._path = None
        self._refresh_status()

    def _on_new_two_segment(self) -> None:
        try:
            from spacemissionplanner.mission_graph.templates import two_segment_earth_orbit_graph
        except (ImportError, RuntimeError) as exc:
            QMessageBox.warning(self, "Could not create graph", str(exc))
            return
        self._graph = two_segment_earth_orbit_graph()
        self._path = None
        self._refresh_status()

    def _on_open(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open mission graph JSON",
            "",
            "Mission graph JSON (*.json);;All files (*)",
        )
        if not path:
            return
        try:
            from spacemissionplanner.mission_graph.serialization import load_graph_json

            self._graph = load_graph_json(Path(path))
            self._path = Path(path)
        except (OSError, ValueError, RuntimeError) as exc:
            QMessageBox.warning(self, "Could not load graph", str(exc))
            return
        self._refresh_status()

    def _on_save(self) -> None:
        if self._graph is None:
            return
        path = self._path
        if path is None:
            chosen, _ = QFileDialog.getSaveFileName(
                self,
                "Save mission graph JSON",
                "mission.json",
                "Mission graph JSON (*.json);;All files (*)",
            )
            if not chosen:
                return
            path = Path(chosen)
        try:
            from spacemissionplanner.mission_graph.serialization import save_graph_json

            save_graph_json(path, self._graph)
            self._path = path
        except (OSError, ValueError, RuntimeError) as exc:
            QMessageBox.warning(self, "Could not save graph", str(exc))
            return
        self._refresh_status()

    def _on_run(self) -> None:
        self._run_graph(show_message=True)

    def _on_run_and_view(self) -> None:
        if not self._run_graph(show_message=False):
            return
        if self._on_view_in_3d is None:
            QMessageBox.information(self, "3D viewer", "Viewer hook not configured.")
            return
        try:
            from spacemissionplanner.visualization.mission_graph_io import episode_from_graph

            ep = episode_from_graph(self._graph, run=False)
            self._on_view_in_3d(ep)
        except Exception as exc:
            QMessageBox.warning(self, "Could not build viewer episode", str(exc))

    def _run_graph(self, *, show_message: bool) -> bool:
        if self._graph is None:
            return False
        try:
            from spacemissionplanner.mission_graph.execution import run_graph

            ran = run_graph(self._graph)
            if show_message:
                if not ran:
                    QMessageBox.information(self, "Run graph", "No propagator nodes to execute.")
                else:
                    QMessageBox.information(self, "Run graph", f"Computed {len(ran)} propagator node(s).")
            return bool(ran)
        except Exception as exc:
            QMessageBox.warning(self, "Run graph failed", str(exc))
            return False

    def _refresh_status(self) -> None:
        self._structure.clear()
        if self._graph is None:
            self._status.setText("No graph loaded.")
            return
        n_nodes = len(self._graph.get_nodes())
        n_edges = len(self._graph.get_edges())
        src = self._path.name if self._path else "(unsaved)"
        self._status.setText(f"<b>File</b>: {src}<br><b>Nodes</b>: {n_nodes} &nbsp; <b>Edges</b>: {n_edges}")

        try:
            from spacemissionplanner.mission_graph.execution import topological_order

            order = topological_order(self._graph)
        except Exception:
            order = [n.name() for n in self._graph.get_nodes()]

        for name in order:
            node = self._graph.get_node(name)
            kind = type(node).__name__ if node is not None else "?"
            self._structure.addItem(f"• {name} ({kind})")
        for edge in self._graph.get_edges():
            self._structure.addItem(
                f"  → {edge.source().name()}.{edge.source_output()} "
                f"→ {edge.target().name()}.{edge.target_input()}"
            )

    def graph(self):
        """Return the in-memory graph, or ``None``."""
        return self._graph
