"""GUI platform selection for WSLg / VTK compatibility."""

from __future__ import annotations

import os
import sys

from spacemissionplanner.gui.app import _configure_qt_platform_for_wsl_vtk
from spacemissionplanner.gui.vtk_platform import resolve_vtk_display_mode


def test_smp_gui_xcb_opt_in(monkeypatch):
    monkeypatch.setenv("SMP_GUI_XCB", "1")
    monkeypatch.delenv("QT_QPA_PLATFORM", raising=False)
    monkeypatch.setenv("DISPLAY", ":0")
    assert _configure_qt_platform_for_wsl_vtk() is True
    assert os.environ["QT_QPA_PLATFORM"] == "xcb"


def test_smp_gui_xcb_off_by_default(monkeypatch):
    monkeypatch.delenv("SMP_GUI_XCB", raising=False)
    monkeypatch.delenv("QT_QPA_PLATFORM", raising=False)
    monkeypatch.setenv("DISPLAY", ":0")
    monkeypatch.setenv("WAYLAND_DISPLAY", "wayland-0")
    assert _configure_qt_platform_for_wsl_vtk() is False
    assert "QT_QPA_PLATFORM" not in os.environ


def test_respects_existing_qt_platform(monkeypatch):
    monkeypatch.setenv("SMP_GUI_XCB", "1")
    monkeypatch.setenv("QT_QPA_PLATFORM", "wayland")
    monkeypatch.setenv("DISPLAY", ":0")
    assert _configure_qt_platform_for_wsl_vtk() is False
    assert os.environ["QT_QPA_PLATFORM"] == "wayland"


def test_skipped_on_non_linux(monkeypatch):
    monkeypatch.setattr(sys, "platform", "darwin")
    monkeypatch.setenv("SMP_GUI_XCB", "1")
    monkeypatch.delenv("QT_QPA_PLATFORM", raising=False)
    assert _configure_qt_platform_for_wsl_vtk() is False


def test_vtk_mode_override(monkeypatch):
    monkeypatch.setenv("SMP_GUI_VTK_MODE", "offscreen")
    assert resolve_vtk_display_mode() == "offscreen"
    monkeypatch.setenv("SMP_GUI_VTK_MODE", "embedded")
    assert resolve_vtk_display_mode() == "embedded"


def test_vtk_mode_auto_on_wsl(monkeypatch):
    monkeypatch.delenv("SMP_GUI_VTK_MODE", raising=False)
    monkeypatch.setattr(
        "spacemissionplanner.gui.vtk_platform._running_under_wsl",
        lambda: True,
    )
    assert resolve_vtk_display_mode() == "offscreen"
