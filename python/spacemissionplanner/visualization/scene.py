"""Lightweight trajectory plotting hooks; optional PyVista."""

from __future__ import annotations

from typing import TYPE_CHECKING

import numpy as np

if TYPE_CHECKING:
    import pyvista as pv


def pyvista_available() -> bool:
    try:
        import pyvista  # noqa: F401

        return True
    except ImportError:
        return False


class TrajectoryScene:
    """Builds a simple polyline from Cartesian samples (meters)."""

    def __init__(self, positions_m: np.ndarray) -> None:
        arr = np.asarray(positions_m, dtype=np.float64)
        if arr.ndim != 2 or arr.shape[1] != 3:
            raise ValueError("positions_m must be shape (N, 3)")
        self._positions = arr

    def positions(self) -> np.ndarray:
        return self._positions

    def to_pyvista_polyline(self) -> "pv.PolyData":
        if not pyvista_available():
            raise ImportError("PyVista is required for to_pyvista_polyline(); install the 'viz' extra.")
        import pyvista as pv

        poly = pv.PolyData(self._positions)
        poly.lines = np.hstack([[len(self._positions)], np.arange(len(self._positions), dtype=np.int32)])
        return poly
