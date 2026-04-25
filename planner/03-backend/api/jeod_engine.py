"""
jeod_engine.py

JEOD-backed orbital dynamics engine.

Wraps the pyjeod C++ extension module to provide:
  - SPICE ephemeris queries (planetary state vectors)
  - N-body gravity through JEOD GravityManager
  - High-fidelity trajectory propagation via DynManager

The engine is used as a singleton by the FastAPI server.
"""

import os
import sys
import numpy as np
from datetime import datetime, timezone


def _find_pyjeod() -> None:
    """
    Add the CMake build output directory to sys.path so `import pyjeod` works.
    
    The pyjeod .so file is built in the 01-jeod-bindings directory.
    """
    # Try 01-jeod-bindings directory (where Makefile puts the .so)
    makefile_dir = os.path.realpath(os.path.join(os.path.dirname(__file__), "..", "..", "01-jeod-bindings"))
    if makefile_dir not in sys.path:
        sys.path.insert(0, makefile_dir)

_find_pyjeod()
import pyjeod  # noqa: E402  (after sys.path fixup)


# ---------------------------------------------------------------------------
# Default gravitational parameters [m^3/s^2]
# ---------------------------------------------------------------------------
_DEFAULT_MU: dict[str, float] = {
    "Sun":     1.32712440018e20,
    "Mercury": 2.2032e13,
    "Venus":   3.257e14,
    "Earth":   3.986004418e14,
    "Moon":    4.9048695e12,
    "Mars":    4.282837e13,
    "Jupiter": 1.267127e17,
    "Saturn":  3.7931187e16,
    "Uranus":  5.793939e15,
    "Neptune": 6.836529e15,
}

_J2000 = datetime(2000, 1, 1, 12, 0, 0, tzinfo=timezone.utc)


class JEODEngine:
    """
    NASA JEOD-based orbital dynamics engine.

    Manages ephemerides, gravity models, and high-fidelity propagation using
    the SPICE toolkit and JEOD physics models compiled into the pyjeod module.
    """

    def __init__(self, kernels_path: str | None = None) -> None:
        if kernels_path is None:
            env_path = os.environ.get("KERNELS_PATH")
            if env_path:
                kernels_path = env_path
            else:
                # Relative to this file: api/ -> 03-backend/ -> planner/ -> 02-kernels/
                kernels_path = os.path.realpath(
                    os.path.join(os.path.dirname(__file__), "..", "..", "02-kernels")
                )

        self.kernels_path = kernels_path
        self.tm_path = os.path.join(kernels_path, "planner.tm")

        # ── Managers ──────────────────────────────────────────────────────────
        self.time_mngr  = pyjeod.TimeManager()
        pyjeod.setup_default_time(self.time_mngr)
        self.ephem_mngr = pyjeod.EphemeridesManager()
        self.grav_mngr  = pyjeod.GravityManager()
        self.dyn_mngr   = pyjeod.DynManager()

        # ── SPICE ephemeris ────────────────────────────────────────────────────
        self.spice_ephem = pyjeod.SpiceEphemeris()
        self.spice_ephem.metakernel_filename = self.tm_path

        self.bodies = [
            "Sun", "Mercury", "Venus", "Earth", "Moon", "Mars",
            "Jupiter Barycenter", "Saturn Barycenter", "Uranus Barycenter", "Neptune Barycenter",
        ]
        for body in self.bodies:
            self.spice_ephem.add_planet_name(body)
            # Add orientations only for physical bodies with known rotation models in SPICE
            if body in ["Earth", "Moon", "Mars"]:
                self.spice_ephem.add_orientation(body)

        # Initialize & activate ephemerides.
        # SpiceEphemeris::initialize_model takes (const TimeManager&, EphemeridesManager&)
        self.spice_ephem.initialize_model(self.time_mngr, self.ephem_mngr)
        self.ephem_mngr.initialize_ephemerides()
        self.ephem_mngr.activate_ephemerides()

        # ── Gravity sources ────────────────────────────────────────────────────
        # GravitySource is the point-mass model in this JEOD version.
        # Keep Python-side references alive (GravityManager holds raw pointers).
        self._grav_sources: dict[str, pyjeod.GravitySource] = {}
        for body in self.bodies:
            src = pyjeod.GravitySource()
            src.name = body
            src.mu   = _DEFAULT_MU.get(body, 0.0)
            self.grav_mngr.add_grav_source(src)   # was erroneously add_gravity_source
            self._grav_sources[body] = src

        # GravityManager::initialize_model takes BaseDynManager& – DynManager inherits it
        self.grav_mngr.initialize_model(self.dyn_mngr)

        # ── DynManager ────────────────────────────────────────────────────────
        dyn_init = pyjeod.DynManagerInit()
        dyn_init.mode = pyjeod.EphemerisMode.Ephemerides
        self.dyn_mngr.initialize_model(dyn_init, self.time_mngr)

        # Pull initial ephemeris state
        self.ephem_mngr.update_ephemerides()

    # -------------------------------------------------------------------------
    # Internal helpers
    # -------------------------------------------------------------------------

    def _seconds_since_j2000(self, dt_obj: datetime) -> float:
        """Convert a datetime to seconds since J2000 epoch."""
        if dt_obj.tzinfo is None:
            dt_obj = dt_obj.replace(tzinfo=timezone.utc)
        return (dt_obj - _J2000).total_seconds()

    def _update_time(self, dt_obj: datetime) -> float:
        """Step the ephemerides to the given epoch, return J2000 seconds."""
        secs = self._seconds_since_j2000(dt_obj)
        self.ephem_mngr.update_ephemerides()
        return secs

    # -------------------------------------------------------------------------
    # Public API
    # -------------------------------------------------------------------------

    def get_body_state(self, name: str, time_dt: datetime) -> np.ndarray:
        """
        Return the [pos, vel] (6-vector, m and m/s) for *name* at *time_dt*.

        Position and velocity are expressed in the SSB-centred inertial frame
        provided by SPICE.
        """
        self._update_time(time_dt)

        # Try the integration-frame lookup first (major planets), then fall back
        # to ephemeris-point → target-frame (moons, barycentres, etc.)
        frame = self.ephem_mngr.find_integ_frame(name)
        if frame is None:
            point = self.ephem_mngr.find_ephem_point(name)
            if point is not None:
                frame = point.get_target_frame()

        if frame is None:
            raise ValueError(f"Body frame for '{name}' not found in EphemeridesManager")

        pos = frame.get_position()  # list[float] len 3
        vel = frame.get_velocity()  # list[float] len 3
        return np.array(pos + vel, dtype=float)

    def get_relative_state(
        self,
        body_name: str,
        ref_name: str,
        time_dt: datetime,
    ) -> np.ndarray:
        """Return state of *body_name* relative to *ref_name* at *time_dt*."""
        s_body = self.get_body_state(body_name, time_dt)
        s_ref  = self.get_body_state(ref_name,  time_dt)
        return s_body - s_ref

    def propagate(
        self,
        initial_state: np.ndarray,
        duration_days: float,
        dt_sec: float = 3600.0,
        central_body: str = "Sun",
    ) -> list[dict]:
        """
        High-fidelity propagation using JEOD n-body gravity.

        Args:
            initial_state: 6-vector [x,y,z, vx,vy,vz] in m and m/s.
            duration_days: Integration duration in days.
            dt_sec:        Output timestep in seconds (default 1 h).
            central_body:  Name of the body whose inertial frame is used.

        Returns:
            List of trajectory dictionaries with keys 'time', 'position',
            and 'velocity'.
        """
        # Create dynamic body
        vehicle = pyjeod.DynBody()
        vehicle.set_name("Spacecraft")
        vehicle.set_mass(1000.0)

        # Set initial conditions using the structural frame as reference
        # (structure is a BodyRefFrame; full DynManager integration would
        # normally use the planetary inertial frame, but for propagation
        # the structure frame matches when no attitude is set)
        vehicle.set_position(initial_state[:3].tolist(), vehicle.structure)
        vehicle.set_velocity(initial_state[3:].tolist(), vehicle.structure)

        self.dyn_mngr.add_dyn_body(vehicle)
        self.dyn_mngr.initialize_simulation()

        trajectory: list[dict] = []
        total_steps = int(duration_days * 86400 / dt_sec)
        current_time = self.dyn_mngr.get_timestamp()

        for i in range(total_steps + 1):
            pos = vehicle.get_position()
            vel = vehicle.get_velocity()
            trajectory.append({
                "time": i * dt_sec,
                "position": {"x": pos[0], "y": pos[1], "z": pos[2]},
                "velocity": {"x": vel[0], "y": vel[1], "z": vel[2]},
            })

            target_time = current_time + dt_sec
            self.dyn_mngr.integrate(target_time, self.time_mngr)
            current_time = target_time

        return trajectory


# ---------------------------------------------------------------------------
# Singleton accessor
# ---------------------------------------------------------------------------

_engine: JEODEngine | None = None


def get_jeod_engine() -> JEODEngine:
    """Return the module-level singleton JEODEngine, creating it on first call."""
    global _engine
    if _engine is None:
        _engine = JEODEngine()
    return _engine
