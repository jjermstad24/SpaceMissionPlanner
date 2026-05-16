"""Abstraction for ephemeris sampling (SPICE or analytical)."""

from __future__ import annotations

from typing import Protocol, runtime_checkable, Optional
import os

import numpy as np

try:
    import spiceypy as spice
    HAS_SPICY = True
except ImportError:
    HAS_SPICY = False

try:
    from spacemissionplanner.spacemissionplanner_native import Ephemeris, Epoch
    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False


@runtime_checkable
class EphemerisProvider(Protocol):
    """Interface for sampling celestial body positions over time."""

    def sample_bodies(
        self,
        body_ids: list[str],
        times: np.ndarray,
        *,
        frame: str = "J2000",
        origin: str = "SSB",
    ) -> dict[str, np.ndarray]:
        """Return a mapping of body_id -> (N, 3) positions in meters."""
        ...


class SpiceEphemerisProvider:
    """SPICE-based ephemeris provider using C++ native bindings."""

    def __init__(self, kernel_paths: Optional[list[str]] = None):
        if not HAS_NATIVE:
            raise RuntimeError("Native bindings not available")

        self._eph = Ephemeris()

        if HAS_SPICY:
            spice.erract("SET", "RETURN")

        default_lsk = os.environ.get("SPICE_LSK")
        default_spk = os.environ.get("SPICE_SPK")

        if kernel_paths:
            for path in kernel_paths:
                if os.path.exists(path):
                    self._eph.load_kernel(path)
                else:
                    raise FileNotFoundError(f"SPICE kernel not found: {path}")

        if default_lsk and os.path.exists(default_lsk):
            self._eph.load_kernel(default_lsk)
        if default_spk and os.path.exists(default_spk):
            self._eph.load_kernel(default_spk)

    def sample_bodies(
        self,
        body_ids: list[str],
        times: np.ndarray,
        *,
        frame: str = "J2000",
        origin: str = "SSB",
    ) -> dict[str, np.ndarray]:
        """Sample body positions using SPICE."""
        times = np.asarray(times, dtype=np.float64)
        n_steps = times.shape[0]

        epochs = [Epoch(t) for t in times]

        result: dict[str, np.ndarray] = {}

        for body_id in body_ids:
            positions = []
            for epoch in epochs:
                pos = self._eph.get_position(body_id, origin, epoch, frame, "NONE")
                positions.append(pos)

            result[body_id] = np.array(positions, dtype=np.float64)

        return result

    def get_ephemeris(self) -> Ephemeris:
        """Return the underlying Ephemeris object for direct access."""
        return self._eph


class ToyEphemerisProvider:
    """Fallback provider using the circular coplanar model from demo_ephemeris."""

    def sample_bodies(
        self,
        body_ids: list[str],
        times: np.ndarray,
        *,
        frame: str = "J2000",
        origin: str = "SSB",
    ) -> dict[str, np.ndarray]:
        from spacemissionplanner.visualization import demo_ephemeris

        episode = demo_ephemeris.build_demo_viewer_episode(n_steps=times.shape[0])
        result = {}
        for body_id in body_ids:
            if body_id in episode.body_positions_m:
                result[body_id] = episode.body_positions_m[body_id]
        return result


def build_spice_viewer_episode(
    body_ids: list[str],
    times: np.ndarray,
    trajectory: Optional[np.ndarray] = None,
    *,
    frame: str = "J2000",
    origin: str = "SSB",
    body_radii: Optional[dict[str, float]] = None,
    kernel_paths: Optional[list[str]] = None,
) -> "ViewerEpisode":
    """Build a ViewerEpisode from SPICE ephemeris data.

    Args:
        body_ids: List of body names (e.g., ["Sun", "Earth", "Mars"])
        times: Array of times in seconds since J2000
        trajectory: Optional (N, 3) trajectory positions in meters
        frame: SPICE frame name (e.g., "J2000", "ICRF")
        origin: SPICE observer for position queries (e.g., "SSB", "Sun")
        body_radii: Optional dict of body display radii in meters
        kernel_paths: Optional list of SPICE kernel file paths

    Returns:
        ViewerEpisode suitable for the viewer widget
    """
    from spacemissionplanner.visualization.viewer_data import ViewerEpisode

    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available for SPICE")

    provider = SpiceEphemerisProvider(kernel_paths=kernel_paths)

    body_positions = provider.sample_bodies(body_ids, times, frame=frame, origin=origin)

    default_radii = {
        "Sun": 696340e3,
        "Mercury": 2439.7e3,
        "Venus": 6051.8e3,
        "Earth": 6371e3,
        "Moon": 1737.4e3,
        "Mars": 3389.5e3,
        "Jupiter": 69911e3,
        "Saturn": 58232e3,
        "Uranus": 25362e3,
        "Neptune": 24622e3,
        "Pluto": 1188.3e3,
    }

    radii = body_radii if body_radii else default_radii

    return ViewerEpisode(
        frame_name=frame,
        origin_description=f"SPICE: {origin} (use SPICE_LSK/SPICE_SPK env vars or kernel_paths)",
        time_scale_note="Seconds since J2000 (TDB)",
        times=times,
        body_ids=tuple(body_ids),
        body_positions_m=body_positions,
        body_display_radius_m={bid: radii.get(bid, 1e6) for bid in body_ids},
        trajectory_positions_m=trajectory if trajectory is not None else np.zeros((times.shape[0], 3)),
        trajectory_render_mode="full_path",
    )


def download_naif_kernel(url: str, dest_path: str) -> str:
    """Download a SPICE kernel from NAIF and return the local path."""
    import urllib.request

    os.makedirs(os.path.dirname(dest_path), exist_ok=True)

    print(f"Downloading {url} ...")
    urllib.request.urlretrieve(url, dest_path)
    print(f"Saved to {dest_path}")

    return dest_path


def get_or_download_de440(cache_dir: Optional[str] = None) -> str:
    """Get or download the DE440 planetary ephemeris kernel.

    Returns the path to de440.bsp. Downloads from NAIF if not cached.
    """
    if cache_dir is None:
        cache_dir = os.path.expanduser("~/.cache/spacemissionplanner")

    os.makedirs(cache_dir, exist_ok=True)
    de440_path = os.path.join(cache_dir, "de440.bsp")

    if os.path.exists(de440_path):
        return de440_path

    naif_url = "https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440.bsp"

    return download_naif_kernel(naif_url, de440_path)


def get_or_download_lsk(cache_dir: Optional[str] = None) -> str:
    """Get or download the leapsecond kernel (LSK).

    Returns the path to naif0012.tls. Downloads from NAIF if not cached.
    """
    if cache_dir is None:
        cache_dir = os.path.expanduser("~/.cache/spacemissionplanner")

    os.makedirs(cache_dir, exist_ok=True)
    lsk_path = os.path.join(cache_dir, "naif0012.tls")

    if os.path.exists(lsk_path):
        return lsk_path

    naif_url = "https://naif.jpl.nasa.gov/pub/naif/generic_kernels/lsk/naif0012.tls"

    return download_naif_kernel(naif_url, lsk_path)


def get_default_body_positions(
    times: np.ndarray,
    frame: str = "J2000",
    origin: str = "SSB",
) -> dict[str, np.ndarray]:
    """Get default solar system body positions for visualization.

    Uses SPICE if available, otherwise returns Sun at origin.

    Args:
        times: Array of times in seconds since J2000
        frame: Coordinate frame (default J2000)
        origin: Origin body (default SSB - solar system barycenter)

    Returns:
        Mapping of body_id -> (N, 3) positions in meters
    """
    if not HAS_NATIVE:
        return {"Sun": np.zeros((len(times), 3), dtype=np.float64)}

    try:
        cache_dir = os.path.expanduser("~/.cache/spacemissionplanner")
        lsk_path = os.path.join(cache_dir, "naif0012.tls")
        spk_path = os.path.join(cache_dir, "de440.bsp")

        if not os.path.exists(lsk_path) or not os.path.exists(spk_path):
            return {"Sun": np.zeros((len(times), 3), dtype=np.float64)}

        eph = Ephemeris()
        eph.load_kernel(lsk_path)
        eph.load_kernel(spk_path)

        body_ids = ["Sun", "Earth", "Moon"]
        result = {}

        for bid in body_ids:
            positions = []
            for t in times:
                epoch = Epoch(t)
                try:
                    pos = eph.get_position(bid, origin, epoch, frame, "NONE")
                    positions.append(pos)
                except Exception:
                    positions.append((0.0, 0.0, 0.0))
            result[bid] = np.array(positions, dtype=np.float64)

        return result
    except Exception:
        return {"Sun": np.zeros((len(times), 3), dtype=np.float64)}
