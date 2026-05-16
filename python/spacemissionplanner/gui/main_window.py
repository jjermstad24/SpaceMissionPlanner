"""Primary application window (shell until mission graph and panels are wired)."""

from __future__ import annotations

from PySide6.QtCore import Qt, QSize
from PySide6.QtGui import QAction, QFont
from PySide6.QtWidgets import (
    QLabel,
    QListWidget,
    QListWidgetItem,
    QMainWindow,
    QMessageBox,
    QStackedWidget,
    QStatusBar,
    QToolBar,
    QVBoxLayout,
    QWidget,
    QSplitter,
)

import spacemissionplanner
from spacemissionplanner.gui.mission_graph_page import MissionGraphPage
from spacemissionplanner.gui.solar_viewer_page import SolarViewerPage
from spacemissionplanner.wrappers import native_extension_status
from spacemissionplanner.wrappers.backend import NativeExtensionStatus


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle("Space Mission Planner")
        self.resize(1100, 720)
        self.setMinimumSize(840, 520)

        self._nav: QListWidget
        self._stack: QStackedWidget

        self._apply_shell_style()
        self._build_menu()
        self._build_toolbar()
        self._build_central()
        self._build_status()

    def _apply_shell_style(self) -> None:
        self.setStyleSheet(
            """
            QMainWindow { background-color: #f4f4f5; }
            QListWidget {
                background-color: #ffffff;
                border: 1px solid #e4e4e7;
                border-radius: 8px;
                padding: 6px;
                outline: none;
            }
            QListWidget::item {
                padding: 10px 12px;
                border-radius: 6px;
            }
            QListWidget::item:selected {
                background-color: #e0e7ff;
                color: #1e1b4b;
            }
            QListWidget::item:hover:!selected {
                background-color: #f4f4f5;
            }
            QFrame#card {
                background-color: #ffffff;
                border: 1px solid #e4e4e7;
                border-radius: 10px;
            }
            """
        )

    def _build_menu(self) -> None:
        menu_file = self.menuBar().addMenu("&File")
        act_quit = QAction("&Quit", self)
        act_quit.setShortcut("Ctrl+Q")
        act_quit.triggered.connect(self.close)
        menu_file.addAction(act_quit)

        menu_help = self.menuBar().addMenu("&Help")
        act_about = QAction("&About", self)
        act_about.triggered.connect(self._show_about)
        menu_help.addAction(act_about)

    def _build_toolbar(self) -> None:
        bar = QToolBar("Main")
        bar.setMovable(False)
        bar.setIconSize(QSize(20, 20))
        self.addToolBar(bar)

        act_new = QAction("New mission (soon)", self)
        act_new.setEnabled(False)
        bar.addAction(act_new)

        bar.addSeparator()

        act_run = QAction("Run graph (soon)", self)
        act_run.setEnabled(False)
        bar.addAction(act_run)

    def _build_central(self) -> None:
        splitter = QSplitter(Qt.Horizontal)

        self._nav = QListWidget()
        self._nav.setMinimumWidth(200)
        self._nav.setMaximumWidth(280)
        self._nav.setSpacing(2)
        for label in ("Overview", "3D viewer", "Mission timeline", "Propagation", "Optimization"):
            item = QListWidgetItem(label)
            item.setSizeHint(QSize(0, 40))
            self._nav.addItem(item)
        self._nav.setCurrentRow(0)
        self._nav.currentRowChanged.connect(self._on_nav_changed)

        self._stack = QStackedWidget()
        self._viewer_page = SolarViewerPage()
        self._mission_page = MissionGraphPage(on_view_in_3d=self._show_episode_in_viewer)
        self._stack.addWidget(self._page_overview())
        self._stack.addWidget(self._viewer_page)
        self._stack.addWidget(self._mission_page)
        self._stack.addWidget(
            self._page_placeholder(
                "Propagation",
                "Choose propagators, epochs, and frames; results will come from the backend, not from GUI math.",
            )
        )
        self._stack.addWidget(
            self._page_placeholder(
                "Optimization",
                "Objectives, constraints, and solver runs will be orchestrated here against the C++ optimizer APIs.",
            )
        )

        splitter.addWidget(self._nav)
        splitter.addWidget(self._stack)
        splitter.setStretchFactor(0, 0)
        splitter.setStretchFactor(1, 1)
        splitter.setSizes([240, 860])

        self.setCentralWidget(splitter)

    def _on_nav_changed(self, row: int) -> None:
        if 0 <= row < self._stack.count():
            self._stack.setCurrentIndex(row)

    def _show_episode_in_viewer(self, episode: object) -> None:
        self._viewer_page.load_episode(episode, source="mission_graph")
        self._nav.setCurrentRow(1)

    def _page_overview(self) -> QWidget:
        outer = QWidget()
        layout = QVBoxLayout(outer)
        layout.setContentsMargins(24, 24, 24, 24)

        card = QWidget()
        card.setObjectName("card")
        card_layout = QVBoxLayout(card)
        card_layout.setContentsMargins(32, 32, 32, 32)
        card_layout.setSpacing(16)

        title = QLabel("Space Mission Planner")
        tf = QFont(title.font())
        tf.setPointSize(22)
        tf.setBold(True)
        title.setFont(tf)

        subtitle = QLabel(
            "Desktop shell for the backend: graph execution, propagation, and optimization stay in C++."
        )
        subtitle.setWordWrap(True)
        sf = QFont(subtitle.font())
        sf.setPointSize(11)
        subtitle.setFont(sf)
        subtitle.setStyleSheet("color: #52525b;")

        bullets = QLabel(
            "<ul style='margin-top:8px; line-height:1.5;'>"
            "<li>Open <b>3D viewer</b> for a toy solar-system scene and trajectory (SPICE-backed data later).</li>"
            "<li>Use <b>notebooks</b> and Python APIs for reproducible workflows.</li>"
            "<li>Use <b>Mission graph</b> to build, run, and send trajectories to the 3D viewer.</li>"
            "<li>Native acceleration will appear once pybind11 modules are built and exposed under <code>spacemissionplanner</code>.</li>"
            "</ul>"
        )
        bullets.setWordWrap(True)
        bullets.setTextFormat(Qt.RichText)

        card_layout.addWidget(title)
        card_layout.addWidget(subtitle)
        card_layout.addWidget(bullets)
        card_layout.addStretch(1)

        layout.addWidget(card)
        layout.addStretch(0)
        return outer

    def _page_placeholder(self, heading: str, body: str) -> QWidget:
        outer = QWidget()
        layout = QVBoxLayout(outer)
        layout.setContentsMargins(24, 24, 24, 24)

        card = QWidget()
        card.setObjectName("card")
        inner = QVBoxLayout(card)
        inner.setContentsMargins(28, 28, 28, 28)
        inner.setSpacing(12)

        h = QLabel(heading)
        hf = QFont(h.font())
        hf.setPointSize(16)
        hf.setBold(True)
        h.setFont(hf)

        text = QLabel(body)
        text.setWordWrap(True)
        text.setStyleSheet("color: #3f3f46;")
        tf = QFont(text.font())
        tf.setPointSize(11)
        text.setFont(tf)

        note = QLabel("This panel is intentionally empty until those systems are wired in.")
        note.setWordWrap(True)
        note.setStyleSheet("color: #71717a; font-style: italic;")
        note.setFont(tf)

        inner.addWidget(h)
        inner.addWidget(text)
        inner.addWidget(note)
        inner.addStretch(1)

        layout.addWidget(card)
        layout.addStretch(0)
        return outer

    def _build_status(self) -> None:
        status = QStatusBar()
        status.showMessage("Ready")

        info = native_extension_status()
        if info.status == NativeExtensionStatus.LOADED:
            ext_msg = f"C++ extension: loaded ({info.module_name})"
        elif info.status == NativeExtensionStatus.MISSING:
            ext_msg = "C++ extension: not built — run `make` in the repo root"
        else:
            ext_msg = "C++ extension: error"

        perm = QLabel(f"{ext_msg}  |  UI {spacemissionplanner.__version__}")
        perm.setStyleSheet("color: #71717a; padding-right: 8px;")
        status.addPermanentWidget(perm)
        self.setStatusBar(status)

    def _show_about(self) -> None:
        QMessageBox.about(
            self,
            "About Space Mission Planner",
            f"<h3>Space Mission Planner</h3>"
            f"<p>Version {spacemissionplanner.__version__}</p>"
            f"<p>Qt-based frontend for mission design. Physics and numerics run in the C++ core.</p>",
        )
