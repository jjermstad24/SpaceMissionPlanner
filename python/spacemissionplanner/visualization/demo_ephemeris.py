"""Toy coplanar ephemeris + trajectory for exercising the viewer (not physical navigation).

Real missions will replace this with SPICE / C++ samples per ``agent/VIEWER_PLAN.md``.
"""

from __future__ import annotations

import numpy as np

from spacemissionplanner.visualization.viewer_data import ViewerEpisode

try:
    from spacemissionplanner.spacemissionplanner_native import (
        StateVector, Epoch, Frame, CentralBodyId, propagate_keplerian
    )
    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False

# Rough semi-major axes (m) and orbital periods (Earth years) for coplanar circular toy model.
_ORBITS: tuple[tuple[str, float, float], ...] = (
    ("Mercury", 57.9e9, 0.241),
    ("Venus", 108.2e9, 0.615),
    ("Earth", 149.6e9, 1.000),
    ("Mars", 228.0e9, 1.881),
    ("Jupiter", 778.5e9, 11.86),
    ("Saturn", 1432.0e9, 29.46),
    ("Uranus", 2867.0e9, 84.01),
    ("Neptune", 4515.0e9, 164.8),
)

_BODY_COLORS: dict[str, str] = {
    "Sun": "#fbbf24",
    "Mercury": "#a8a29e",
    "Venus": "#fcd34d",
    "Earth": "#60a5fa",
    "Mars": "#f87171",
    "Jupiter": "#d6b88b",
    "Saturn": "#fde68a",
    "Uranus": "#67e8f9",
    "Neptune": "#818cf8",
    "Moon": "#d4d4d8",
}


def body_color(body_id: str) -> str:
    return _BODY_COLORS.get(body_id, "#e4e4e7")


def build_demo_viewer_episode(*, n_steps: int = 192) -> ViewerEpisode:
    """Return a ViewerEpisode with Sun, planets, Moon, and a simple cruise-style trajectory."""
    if n_steps < 8:
        raise ValueError("n_steps must be at least 8")

    n_steps = int(n_steps)
    # Fictitious uniform timeline (seconds); only relative spacing matters for the demo UI.
    duration_s = 86400.0 * 365.25 * 0.8
    times = np.linspace(0.0, duration_s, n_steps, dtype=np.float64)
    frac = np.linspace(0.0, 1.0, n_steps, dtype=np.float64)

    body_ids: tuple[str, ...] = ("Sun",) + tuple(name for name, _, _ in _ORBITS) + ("Moon",)
    a = {name: a_m for name, a_m, _ in _ORBITS}
    a_earth = a["Earth"]

    positions: dict[str, np.ndarray] = {}
    radii: dict[str, float] = {}

    # Exaggerated radii so spheres read at orbit-mode scale (not true scale).
    radii["Sun"] = max(0.11 * a_earth, 6.0e9)
    for name, a_m, _ in _ORBITS:
        radii[name] = max(0.022 * min(a_m, a["Neptune"]), 0.45e9)

    sun_pos = np.zeros((n_steps, 3), dtype=np.float64)
    positions["Sun"] = sun_pos

    earth_xy = np.empty((n_steps, 2), dtype=np.float64)
    mars_xy = np.empty((n_steps, 2), dtype=np.float64)

    for name, a_m, p_y in _ORBITS:
        ang = 2.0 * np.pi * frac * (1.0 / max(p_y, 0.05)) * 0.35
        x = a_m * np.cos(ang)
        y = a_m * np.sin(ang)
        z = np.zeros_like(x)
        positions[name] = np.column_stack([x, y, z])
        if name == "Earth":
            earth_xy[:, 0] = x
            earth_xy[:, 1] = y
        if name == "Mars":
            mars_xy[:, 0] = x
            mars_xy[:, 1] = y

    moon_orbit = 0.00257 * a_earth
    moon_speed = 13.0
    mx = earth_xy[:, 0] + moon_orbit * np.cos(moon_speed * 2.0 * np.pi * frac)
    my = earth_xy[:, 1] + moon_orbit * np.sin(moon_speed * 2.0 * np.pi * frac)
    positions["Moon"] = np.column_stack([mx, my, np.zeros(n_steps, dtype=np.float64)])
    radii["Moon"] = max(0.12 * moon_orbit, 0.12e9)

    ex, ey, _ = positions["Earth"].T
    mxp, myp, _ = positions["Mars"].T
    blend = np.clip(frac * 1.15, 0.0, 1.0)
    ox = (1.0 - blend) * ex + blend * mxp
    oy = (1.0 - blend) * ey + blend * myp
    bend = 0.08 * a_earth * np.sin(np.pi * frac)
    oz = bend * np.sin(2.0 * np.pi * frac)
    trajectory = np.column_stack([ox, oy, oz])

    return ViewerEpisode(
        frame_name="DEMO_HELIOSTATIC_XY",
        origin_description="Toy heliostatic XY plane; not a SPICE frame export.",
        time_scale_note="Fictitious uniform second timeline for UI scrubbing only.",
        times=times,
        body_ids=body_ids,
        body_positions_m=positions,
        body_display_radius_m=radii,
        trajectory_positions_m=trajectory,
        trajectory_render_mode="grow",
    )


EARTH_GM = 3.986004418e14
EARTH_RADIUS = 6371e3


def build_keplerian_episode(
    altitude_km: float = 500.0,
    n_orbits: float = 2.0,
    n_steps: int = 200,
) -> ViewerEpisode:
    """Generate a real Keplerian trajectory around Earth using C++ propagator."""
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available")

    altitude_m = altitude_km * 1000.0
    radius = EARTH_RADIUS + altitude_m
    velocity = np.sqrt(EARTH_GM / radius)

    initial_pos = np.array([radius, 0.0, 0.0], dtype=np.float64)
    initial_vel = np.array([0.0, velocity, 0.0], dtype=np.float64)

    period = 2 * np.pi * np.sqrt(radius**3 / EARTH_GM)
    duration = n_orbits * period

    initial_epoch = Epoch(0.0)
    frame = Frame.J2000()
    body = CentralBodyId.earth

    initial_state = StateVector(initial_pos, initial_vel, initial_epoch, frame, body)

    times = np.linspace(0.0, duration, n_steps, dtype=np.float64)
    positions = []

    for dt in times:
        state = propagate_keplerian(initial_state, dt, EARTH_GM)
        pos = state.position()
        positions.append([pos[0], pos[1], pos[2]])

    trajectory = np.array(positions, dtype=np.float64)

    tlen = times.shape[0]
    sun_pos = np.zeros((tlen, 3), dtype=np.float64)
    earth_pos = np.zeros((tlen, 3), dtype=np.float64)

    return ViewerEpisode(
        frame_name="J2000",
        origin_description=f"Keplerian orbit: {altitude_km} km altitude, {n_orbits} orbits",
        time_scale_note="Seconds since J2000 (TDB)",
        times=times,
        body_ids=("Sun", "Earth"),
        body_positions_m={"Sun": sun_pos, "Earth": earth_pos},
        body_display_radius_m={"Sun": 696340e3, "Earth": EARTH_RADIUS},
        trajectory_positions_m=trajectory,
        trajectory_render_mode="full_path",
    )
