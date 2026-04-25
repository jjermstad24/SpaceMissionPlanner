"""
Space Mission Planner Backend API

FastAPI server providing:
- Planetary ephemeris queries (via JEOD)
- Trajectory propagation
- Lambert transfer calculation
- Fuel-constrained optimization
"""

import os
import sys
from datetime import datetime, timedelta, timezone
from typing import Optional, List, Dict, Any
from dataclasses import dataclass

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import numpy as np

app = FastAPI(
    title="Space Mission Planner API",
    description="Planetary ephemerides and trajectory planning built on NASA JEOD",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==============================================================================
# Configuration
# ==============================================================================

# Standard gravitational parameters (m^3/s^2) - used by fallback model
_GRAVITATIONAL_PARAMS = {
    "Sun": 1.32712440018e20,
    "Mercury": 2.2032e13,
    "Venus": 3.257e14,
    "Earth": 3.986004418e14,
    "Moon": 4.9048695e12,
    "Mars": 4.282837e13,
    "Phobos": 7.6e8,
    "Deimos": 1.2e9,
    "Jupiter": 1.267127e17,
    "Saturn": 3.7931187e16,
    "Uranus": 5.793939e15,
    "Neptune": 6.836529e15,
    "Pluto": 8.706e11,
}

# Average distances from Sun (AU to m) - used by fallback model
AU = 1.496e11
_PLANETARY_DISTANCES = {
    "Mercury": 0.387 * AU,
    "Venus": 0.723 * AU,
    "Earth": 1.0 * AU,
    "Mars": 1.524 * AU,
    "Jupiter": 5.203 * AU,
    "Saturn": 9.537 * AU,
    "Uranus": 19.191 * AU,
    "Neptune": 30.069 * AU,
    "Pluto": 39.482 * AU,
}

# Available planetary bodies
AVAILABLE_BODIES = [
    "Sun", "Mercury", "Venus", "Earth", "Moon",
    "Mars", "Phobos", "Deimos",
    "Jupiter", "Io", "Europa", "Ganymede", "Callisto",
    "Saturn", "Titan", "Enceladus",
    "Uranus", "Neptune", "Triton",
    "Pluto", "Charon"
]

# Constants for rocket equation
G0 = 9.81  # m/s^2

# ==============================================================================
# Lambert Solver (simplified but functional)
# ==============================================================================

def lambert_solver(r1, r2, tof_days, mu, prograde=True):
    """
    Simple Lambert solver returning delta-V values.
    
    Uses simplified two-body approximation for interplanetary transfers.
    """
    tof = tof_days * 86400  # convert to seconds
    
    r1 = np.array(r1, dtype=float)
    r2 = np.array(r2, dtype=float)
    
    # Distance between start and end positions
    delta_r = r2 - r1
    chord = np.linalg.norm(delta_r)
    
    if chord < 1e6:  # Very small - just use circular approximation
        return 0, 0
    
    # Mean distance
    r1_mag = np.linalg.norm(r1)
    r2_mag = np.linalg.norm(r2)
    r_mean = (r1_mag + r2_mag) / 2
    
    # Semi-major axis from Lambert (simplified: assume 180-degree transfer)
    # a = chord / (2 * sin(theta/2)) where theta ~ pi for fast transfers
    a = chord / 2.0
    
    # Specific mechanical energy
    if a > 0:
        # Vis-viva: v^2 = mu * (2/r - 1/a)
        v_dep_sq = mu * (2/r1_mag - 1/a)
        v_arr_sq = mu * (2/r2_mag - 1/a)
        
        # Velocity at departure (relative to central body)
        v_dep = np.sqrt(max(0, v_dep_sq)) / 1000  # convert to km/s
        v_arr = np.sqrt(max(0, v_arr_sq)) / 1000
        
        return v_dep, v_arr
    
    return 0, 0


def calculate_orbit_insertion_delta_v(vehicle_dv: float, target_mu: float, target_radius: float, 
                               insert_altitude: float = 300e3) -> float:
    """
    Calculate delta-V for orbit insertion at destination.
    
    - Circular orbit insertion: need to slow down to orbital velocity
    v_circ = sqrt(mu / r)
    """
    r = target_radius + insert_altitude
    v_orbit = np.sqrt(target_mu / r) / 1000  # km/s
    return v_orbit


def estimate_transfer_delta_v(origin: str, destination: str, tof_days: int) -> tuple:
    """
    Estimate departure and arrival delta-V for a transfer.
    
    Delta-V varies with TOF - shorter transfers need more energy.
    """
    
    # Scale based on TOF - faster = more delta-V needed
    # Standard is ~180 days (Hohmann for Earth-Mars)
    tof_factor = 180.0 / tof_days
    
    # Scale factor: shorter TOF = higher delta-V
    if tof_factor > 1:
        tof_factor = min(2.0, tof_factor)  # Cap at 2x
    else:
        tof_factor = max(0.5, tof_factor)  # Min at 0.5x
    
    # Known transfers at ~180 day baseline
    KNOWN_TRANSFERS = {
        ("Earth", "Mars"): (3.6, 2.1),
        ("Earth", "Venus"): (2.5, 2.8),
        ("Earth", "Jupiter"): (5.9, 5.7),
        ("Earth", "Saturn"): (9.2, 5.4),
        ("Venus", "Mars"): (2.1, 1.8),
    }
    
    key = (origin, destination)
    reverse_key = (destination, origin)
    
    if key in KNOWN_TRANSFERS:
        dv_dep, dv_arr = KNOWN_TRANSFERS[key]
    elif reverse_key in KNOWN_TRANSFERS:
        dv_dep, dv_arr = KNOWN_TRANSFERS[reverse_key]
        dv_dep, dv_arr = dv_arr, dv_dep  # Swap
    else:
        # Rough scaling
        r1 = _PLANETARY_DISTANCES.get(origin, 1.0 * AU)
        r2 = _PLANETARY_DISTANCES.get(destination, 1.5 * AU)
        au_delta = abs(r2 - r1) / AU
        dv_base = 2.0 + au_delta * 1.5
        dv_dep, dv_arr = dv_base, dv_base * 0.7
    
    # Apply TOF scaling
    dv_dep = dv_dep * tof_factor
    if dv_arr > 0:
        dv_arr = dv_arr * tof_factor
    
    return round(dv_dep, 2), round(dv_arr, 2)


# ==============================================================================
# Pydantic Models
# ==============================================================================

class Vector3(BaseModel):
    x: float = 0.0
    y: float = 0.0
    z: float = 0.0

class StateVector(BaseModel):
    position: Vector3
    velocity: Vector3

class TransferRequest(BaseModel):
    origin: str
    destination: str
    departure_time: str  # ISO8601 string
    time_of_flight: float  # days
    transfer_type: str = "prograde"
    accuracy: str = "fast" # "fast" or "high"
    fuel_mass: Optional[float] = None  # kg - for optimization
    dry_mass: Optional[float] = None  # kg - spacecraft dry mass
    Isp: Optional[float] = None  # seconds - engine specific impulse

class OptimizeRequest(BaseModel):
    origin: str
    destination: str
    departure_time: str
    fuel_mass: float  # kg available
    dry_mass: float  # kg spacecraft dry mass
    Isp: float = 3000  # seconds - default high-Isp engine
    max_tof_days: float = 365  # max time of flight constraint
    optimize_for: str = "minimize_dv"  # or "max_payload"

class OptimizeResponse(BaseModel):
    origin: str
    destination: str
    optimal_tof_days: float
    delta_v_departure: float
    delta_v_arrival: float
    total_delta_v: float
    fuel_required: float
    achievable_payload: float
    results: List[Dict[str, Any]]

class TransferResponse(BaseModel):
    origin: str
    destination: str
    departure_time: str
    arrival_time: str
    time_of_flight: float
    delta_v_departure: float
    delta_v_arrival: Optional[float] = None
    total_delta_v: float
    trajectory: List[Dict[str, Any]]

class BodyInfo(BaseModel):
    name: str
    mu: float
    body_type: str
    distance_au: Optional[float] = None

_engine = None
_corrector = None

def _get_engine():
    global _engine, _corrector
    if _engine is None:
        try:
            from .jeod_engine import get_jeod_engine
            from trajectory.shooting import DifferentialCorrector
            _engine = get_jeod_engine()
            _corrector = DifferentialCorrector()
        except Exception as e:
            print(f"Warning: JEOD Engine initialization failed: {e}")
            _engine = None
            _corrector = None
    return _engine, _corrector


@app.get("/bodies", response_model=List[BodyInfo])
async def list_bodies():
    """List all available planetary bodies."""
    bodies = []
        
    for name in AVAILABLE_BODIES:
        mu = _GRAVITATIONAL_PARAMS.get(name, 0.0)
        distance = _PLANETARY_DISTANCES.get(name, 0.0) / AU if name != "Sun" else 0.0
        
        if name == "Sun":
            body_type = "star"
        elif name in ["Mercury", "Venus", "Earth", "Mars", "Jupiter", 
                     "Saturn", "Uranus", "Neptune"]:
            body_type = "planet"
        elif name in ["Pluto"]:
            body_type = "dwarf_planet"
        else:
            body_type = "moon"
            
        bodies.append(BodyInfo(
            name=name, 
            mu=mu, 
            body_type=body_type,
            distance_au=distance
        ))
    return bodies


@app.post("/transfer", response_model=TransferResponse)
async def calculate_transfer(request: TransferRequest) -> TransferResponse:
    """
    Calculate a high-fidelity transfer trajectory.
    """
    if request.origin not in AVAILABLE_BODIES:
        raise HTTPException(status_code=404, detail=f"Origin '{request.origin}' not found")
    if request.destination not in AVAILABLE_BODIES:
        raise HTTPException(status_code=404, detail=f"Destination '{request.destination}' not found")
    
    # Parse departure time
    try:
        departure_dt = datetime.fromisoformat(request.departure_time.replace('Z', '+00:00'))
    except:
        departure_dt = datetime.now(timezone.utc)
    
    arrival_dt = departure_dt + timedelta(days=request.time_of_flight)
    
    engine, corrector = _get_engine()
    
    if engine:
        try:
            # High-fidelity positions from JEOD/SPICE
            s1 = engine.get_body_state(request.origin, departure_dt)
            s2 = engine.get_body_state(request.destination, arrival_dt)
            
            r1, v1_planet = s1[:3], s1[3:]
            r2, v2_planet = s2[:3], s2[3:]
            
            if request.accuracy == "high":
                # Solve with refinement
                v1_req, v2_req, trajectory = corrector.refine_transfer(r1, r2, request.time_of_flight)
            else:
                # Fast Lambert only
                tof_sec = request.time_of_flight * 86400.0
                v1_req, v2_req = corrector.lambert.solve(r1, r2, tof_sec)
                trajectory = corrector.lambert.compute_transfer(r1, r2, tof_sec)
            
            # Delta-V is difference from planet velocity
            dv_dep = np.linalg.norm(v1_req - v1_planet) / 1000.0 # km/s
            dv_arr = np.linalg.norm(v2_req - v2_planet) / 1000.0 # km/s
            
            total_dv = dv_dep + dv_arr
            
            return TransferResponse(
                origin=request.origin,
                destination=request.destination,
                departure_time=departure_dt.isoformat(),
                arrival_time=arrival_dt.isoformat(),
                time_of_flight=request.time_of_flight,
                delta_v_departure=round(dv_dep, 2),
                delta_v_arrival=round(dv_arr, 2),
                total_delta_v=round(total_dv, 2),
                trajectory=trajectory
            )
        except Exception as e:
            print(f"Error in JEOD calculation: {e}")
            # Fallback to simplified model below...

    # --- Fallback to simplified model ---
    tof_seconds = request.time_of_flight * 86400.0
    dv_dep, dv_arr = estimate_transfer_delta_v(
        request.origin, 
        request.destination, 
        int(request.time_of_flight)
    )
    
    total_dv = dv_dep + (dv_arr or 0)
    
    # Generate trajectory points for visualization
    trajectory = []
    n_points = max(2, int(request.time_of_flight / 10))
    for i in range(n_points + 1):
        t = (i / n_points) * tof_seconds
        r1 = _PLANETARY_DISTANCES.get(request.origin, AU)
        r2 = _PLANETARY_DISTANCES.get(request.destination, 1.5 * AU)
        
        # Simple linear interpolation for visualization
        progress = i / n_points
        r = r1 + (r2 - r1) * progress
        
        trajectory.append({
            "time": t,
            "position": {
                "x": r * np.cos(progress * np.pi), 
                "y": 0, 
                "z": r * np.sin(progress * np.pi)
            }
        })
    
    return TransferResponse(
        origin=request.origin,
        destination=request.destination,
        departure_time=departure_dt.isoformat(),
        arrival_time=arrival_dt.isoformat(),
        time_of_flight=request.time_of_flight,
        delta_v_departure=round(dv_dep, 2),
        delta_v_arrival=round(dv_arr, 2) if dv_arr else None,
        total_delta_v=round(total_dv, 2),
        trajectory=trajectory
    )


@app.post("/optimize", response_model=OptimizeResponse)
async def optimize_transfer(request: OptimizeRequest) -> OptimizeResponse:
    """
    Optimize transfer for fuel-constrained mission.
    
    Given a fixed fuel mass, find optimal time of flight that maximizes payload.
    """
    if request.origin not in AVAILABLE_BODIES:
        raise HTTPException(status_code=404, detail=f"Origin '{request.origin}' not found")
    if request.destination not in AVAILABLE_BODIES:
        raise HTTPException(status_code=404, detail=f"Destination '{request.destination}' not found")
    
    # Tsiolkovsky rocket equation: delta_v = Isp * g0 * ln(m0 / m1)
    # where m0 = dry_mass + fuel_mass, m1 = dry_mass
    
    # Total delta-V available from fuel
    available_dv = request.Isp * G0 * np.log((request.dry_mass + request.fuel_mass) / request.dry_mass)
    available_dv = available_dv / 1000  # convert to km/s
    
    # Search over TOF range
    results = []
    best_result = None
    best_score = -float('inf')
    
    for tof in range(30, int(request.max_tof_days) + 1, 5):
        dv_dep, dv_arr = estimate_transfer_delta_v(request.origin, request.destination, tof)
        total_dv = dv_dep + (dv_arr or 0)
        
        if total_dv <= available_dv:
            # Remaining delta-V for payload
            remaining = available_dv - total_dv
            
            # Payload mass possible from remaining delta-V
            # m1 = m0 / exp(remaining * 1000 / (Isp * g0))
            if remaining > 0:
                payload_ratio = np.exp(remaining * 1000 / (request.Isp * G0))
                payload_mass = request.dry_mass * (payload_ratio - 1)
            else:
                payload_mass = 0
            
            # Score: maximize payload
            score = payload_mass
            
            results.append({
                "tof_days": tof,
                "dv_departure": round(dv_dep, 2),
                "dv_arrival": round(dv_arr, 2),
                "total_delta_v": round(total_dv, 2),
                "fuel_used": round(available_dv - remaining, 2),
                "payload_mass": round(max(0, payload_mass), 2)
            })
            
            if score > best_score:
                best_score = score
                best_result = results[-1]
    
    if not best_result:
        # No viable transfer found
        return OptimizeResponse(
            origin=request.origin,
            destination=request.destination,
            optimal_tof_days=request.max_tof_days,
            delta_v_departure=0,
            delta_v_arrival=0,
            total_delta_v=0,
            fuel_required=0,
            achievable_payload=0,
            results=[]
        )
    
    return OptimizeResponse(
        origin=request.origin,
        destination=request.destination,
        optimal_tof_days=best_result["tof_days"],
        delta_v_departure=best_result["dv_departure"],
        delta_v_arrival=best_result["dv_arrival"],
        total_delta_v=best_result["total_delta_v"],
        fuel_required=best_result["fuel_used"],
        achievable_payload=best_result["payload_mass"],
        results=results
    )


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy"}


@app.get("/map")
async def get_solar_system_map(departure_time: str = "2025-01-01T00:00:00Z"):
    """
    Get solar system positions for 2D map visualization.
    
    Returns planet positions (x, y in AU) at given time for drawing a top-down view.
    """
    try:
        dt = datetime.fromisoformat(departure_time.replace('Z', '+00:00'))
    except:
        dt = datetime.now(timezone.utc)
    
    engine, _ = _get_engine()
    
    map_data = {
        "time": dt.isoformat(),
        "bodies": []
    }
    
    planets_to_show = ["Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"]
    
    if engine:
        try:
            for name in planets_to_show:
                state = engine.get_body_state(name, dt)
                x_au = state[0] / AU
                y_au = state[1] / AU
                vx = state[3] / 1000  # km/s
                vy = state[4] / 1000
                map_data["bodies"].append({
                    "name": name,
                    "x": round(x_au, 4),
                    "y": round(y_au, 4),
                    "vx": round(vx, 2),
                    "vy": round(vy, 2)
                })
        except Exception as e:
            print(f"Map JEOD error: {e}")
    
    if not map_data["bodies"]:
        for i, name in enumerate(planets_to_show):
            au_dist = _PLANETARY_DISTANCES.get(name, AU) / AU
            angle = (i * 45 + 20) * np.pi / 180  # crude approx
            map_data["bodies"].append({
                "name": name,
                "x": round(au_dist * np.cos(angle), 4),
                "y": round(au_dist * np.sin(angle), 4),
                "vx": 0,
                "vy": 0
            })
    
    return map_data


# ==============================================================================
# Main
# ==============================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)