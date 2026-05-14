"""Qt application entrypoint."""

from __future__ import annotations

import sys


def main() -> int:
    try:
        from PySide6.QtWidgets import QApplication
    except ImportError as exc:
        print("PySide6 is required for the GUI. Install with: pip install '.[gui]'", file=sys.stderr)
        raise SystemExit(1) from exc

    from spacemissionplanner.gui.main_window import MainWindow

    app = QApplication(sys.argv)
    win = MainWindow()
    win.show()
    return app.exec()
