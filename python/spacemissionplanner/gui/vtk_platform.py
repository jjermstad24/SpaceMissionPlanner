"""VTK / Qt embedding mode for the 3D viewer (WSL and headless-safe)."""

from __future__ import annotations

import os
import sys
from typing import Literal

VtkDisplayMode = Literal["embedded", "offscreen"]


def _running_under_wsl() -> bool:
    if sys.platform != "linux":
        return False
    try:
        with open("/proc/version", encoding="utf-8", errors="replace") as f:
            text = f.read().lower()
    except OSError:
        return False
    return "microsoft" in text or "wsl" in text


def resolve_vtk_display_mode() -> VtkDisplayMode:
    """Choose embedded Qt VTK or off-screen rendering into a QLabel.

    ``SMP_GUI_VTK_MODE``:
      - ``auto`` (default): off-screen on WSL, embedded elsewhere
      - ``embedded``: force QtInteractor (may crash on WSL/X11)
      - ``offscreen``: always render off-screen and show frames in Qt
    """
    raw = os.environ.get("SMP_GUI_VTK_MODE", "auto").strip().lower()
    if raw in ("embedded", "qt", "interactive"):
        return "embedded"
    if raw in ("offscreen", "off-screen", "osmesa", "image"):
        return "offscreen"
    if raw in ("auto", ""):
        return "offscreen" if _running_under_wsl() else "embedded"
    return "embedded"
