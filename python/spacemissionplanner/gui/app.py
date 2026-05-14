"""Qt application entrypoint."""

from __future__ import annotations

import os
import sys


def main() -> int:
    try:
        from PySide6.QtCore import QCoreApplication, QMessageLogContext, Qt, QtMsgType, qInstallMessageHandler
        from PySide6.QtWidgets import QApplication
    except ImportError as exc:
        print("PySide6 is required for the GUI. Install with: pip install '.[gui]'", file=sys.stderr)
        raise SystemExit(1) from exc

    def _qt_msg_handler(msg_type: QtMsgType, ctx: QMessageLogContext, message: str) -> None:
        m = message.lower()
        if "xcb-cursor" in m or "libxcb-cursor" in m:
            print(
                "\nQt could not finish loading the xcb platform plugin (common with Qt 6.5+).\n"
                "  • Install the cursor extension:  sudo apt install libxcb-cursor0\n"
                "  • Or use Wayland on WSLg:        export QT_QPA_PLATFORM=wayland\n"
                "  • Or let Qt choose:              unset QT_QPA_PLATFORM\n",
                file=sys.stderr,
                flush=True,
            )

    qInstallMessageHandler(_qt_msg_handler)

    if os.environ.get("QT_QPA_PLATFORM", "").strip().lower() == "xcb":
        print(
            "Note: QT_QPA_PLATFORM=xcb on Qt 6.5+ needs the system library libxcb-cursor0 "
            "(e.g. sudo apt install libxcb-cursor0). If startup fails, run: unset QT_QPA_PLATFORM\n",
            file=sys.stderr,
            flush=True,
        )

    # VTK + Qt embedded GL: required on many platforms (including WSLg) to avoid X11 BadWindow / GL issues.
    QCoreApplication.setAttribute(Qt.ApplicationAttribute.AA_ShareOpenGLContexts, True)

    from spacemissionplanner.gui.main_window import MainWindow

    app = QApplication(sys.argv)
    win = MainWindow()
    win.show()
    return app.exec()
