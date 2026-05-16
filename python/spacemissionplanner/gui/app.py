"""Qt application entrypoint."""

from __future__ import annotations

import os
import sys


def _configure_qt_platform_for_wsl_vtk() -> bool:
    """Optional xcb for WSLg when VTK embedding needs X11 (set SMP_GUI_XCB=1)."""
    if sys.platform != "linux":
        return False
    if os.environ.get("SMP_GUI_XCB", "").strip() not in ("1", "true", "yes"):
        return False
    if os.environ.get("QT_QPA_PLATFORM", "").strip():
        return False
    if not os.environ.get("DISPLAY", "").strip():
        return False
    os.environ["QT_QPA_PLATFORM"] = "xcb"
    return True


def main() -> int:
    forced_xcb = _configure_qt_platform_for_wsl_vtk()

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

    if forced_xcb:
        print(
            "SMP_GUI_XCB=1: using QT_QPA_PLATFORM=xcb for VTK (needs libxcb-cursor0 on Qt 6.5+).\n",
            file=sys.stderr,
            flush=True,
        )
    elif os.environ.get("QT_QPA_PLATFORM", "").strip().lower() == "xcb":
        print(
            "Note: QT_QPA_PLATFORM=xcb on Qt 6.5+ needs the system library libxcb-cursor0 "
            "(e.g. sudo apt install libxcb-cursor0). If startup fails, run: unset QT_QPA_PLATFORM\n",
            file=sys.stderr,
            flush=True,
        )

    # VTK + Qt embedded GL: required on many platforms when using QtInteractor.
    QCoreApplication.setAttribute(Qt.ApplicationAttribute.AA_ShareOpenGLContexts, True)

    from spacemissionplanner.gui.vtk_platform import resolve_vtk_display_mode

    vtk_mode = resolve_vtk_display_mode()
    if vtk_mode == "offscreen":
        print(
            "3D viewer: off-screen VTK mode (WSL-safe). Set SMP_GUI_VTK_MODE=embedded to force Qt GL embedding.\n",
            file=sys.stderr,
            flush=True,
        )

    from spacemissionplanner.gui.main_window import MainWindow

    app = QApplication(sys.argv)
    win = MainWindow()
    win.show()
    return app.exec()
