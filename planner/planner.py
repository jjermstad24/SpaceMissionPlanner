#!/usr/bin/env python3
"""
Space Mission Planner - Simple Python Interface

Usage:
    python planner.py                    # Interactive mode
    python planner.py earth mars 180       # Quick transfer estimate
    python planner.py -o earth mars    # Optimize for payload
"""

import sys
import json
import requests
from datetime import datetime, timedelta

API_BASE = "http://localhost:8000"

AVAILABLE_BODIES = ["Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"]


def list_bodies():
    """List available bodies."""
    r = requests.get(f"{API_BASE}/bodies")
    bodies = r.json()
    print("\nAvailable Bodies:")
    print("-" * 40)
    for b in bodies:
        if b.get("distance_au"):
            print(f"  {b['name']:12} ({b['body_type']:10}) {b['distance_au']:.2f} AU")
        else:
            print(f"  {b['name']:12} ({b['body_type']})")
    print()


def transfer(origin, destination, tof_days, departure=None):
    """Calculate transfer."""
    if not departure:
        departure = datetime.now().isoformat()
    
    payload = {
        "origin": origin,
        "destination": destination,
        "departure_time": departure,
        "time_of_flight": tof_days
    }
    
    r = requests.post(f"{API_BASE}/transfer", json=payload)
    result = r.json()
    
    print(f"\nTransfer: {origin} → {destination}")
    print("=" * 50)
    print(f"  Departure:     {result['departure_time'][:10]}")
    print(f"  Arrival:       {result['arrival_time'][:10]}")
    print(f"  Time of Flight: {result['time_of_flight']} days")
    print(f"  Delta-V (dep):  {result['delta_v_departure']} km/s")
    print(f"  Delta-V (arr): {result['delta_v_arrival']} km/s")
    print(f"  Total ΔV:      {result['total_delta_v']} km/s")
    
    return result


def optimize(origin, destination, fuel_mass, dry_mass, Isp=3000, max_tof=365):
    """Optimize transfer for payload."""
    departure = datetime.now().isoformat()
    
    payload = {
        "origin": origin,
        "destination": destination,
        "departure_time": departure,
        "fuel_mass": fuel_mass,
        "dry_mass": dry_mass,
        "Isp": Isp,
        "max_tof_days": max_tof
    }
    
    r = requests.post(f"{API_BASE}/optimize", json=payload)
    result = r.json()
    
    print(f"\nOptimization: {origin} → {destination}")
    print("=" * 50)
    print(f"  Fuel Mass:     {fuel_mass} kg")
    print(f"  Dry Mass:      {dry_mass} kg")
    print(f"  Engine Isp:    {Isp} s")
    print()
    print(f"  Optimal TOF:   {result['optimal_tof_days']} days")
    print(f"  Total ΔV:      {result['total_delta_v']} km/s")
    print(f"  Fuel Used:     {result['fuel_required']:.1f} km/s equiv")
    print(f"  Payload Mass: {result['achievable_payload']:.0f} kg")
    print()
    
    if result.get("results"):
        print("TOF Trade Study:")
        print("-" * 60)
        print(f"  {'TOF':>6}  {'ΔV dep':>8}  {'ΔV arr':>8}  {'Total':>8}  {'Payload':>10}")
        print("-" * 60)
        for row in result["results"][:15]:
            print(f"  {row['tof_days']:>6}  {row['dv_departure']:>8}  {row['dv_arrival']:>8}  {row['total_delta_v']:>8}  {row['payload_mass']:>10.0f}")
    
    return result


def tof_scan(origin, destination, tof_range=(30, 365, 30)):
    """Scan different TOF values."""
    print(f"\nTOF Scan: {origin} → {destination}")
    print("=" * 60)
    print(f"  {'TOF':>6}  {'ΔV dep':>8}  {'ΔV arr':>8}  {'Total':>8}")
    print("-" * 60)
    
    for tof in range(tof_range[0], tof_range[1]+1, tof_range[2]):
        result = transfer(origin, destination, tof)
        print(f"  {tof:>6}  {result['delta_v_departure']:>8}  {result['delta_v_arrival']:>8}  {result['total_delta_v']:>8}")


def interactive():
    """Interactive mode."""
    print("\n" + "="*50)
    print("  SPACE MISSION PLANNER")
    print("  Type 'help' for commands, 'quit' to exit")
    print("="*50 + "\n")
    
    list_bodies()
    
    # Cache origin/destination
    origin = "Earth"
    destination = "Mars"
    tof = 180
    
    history = []
    
    while True:
        try:
            cmd = input(f"[{origin}→{destination} TOF:{tof}d] ").strip()
        except EOFError:
            break
        
        if not cmd:
            continue
        
        parts = cmd.split()
        cmd_upper = parts[0].upper() if parts else ""
        
        # Handle commands
        if cmd_upper in ("Q", "QUIT", "EXIT"):
            break
        elif cmd_upper in ("?", "H", "HELP"):
            print("""
Commands:
  origin <body>     - Set origin (e.g., origin Mars)
  dest <body>       - Set destination (e.g., dest Mars)
  tof <days>       - Set time of flight (e.g., tof 180)
  calc             - Calculate transfer
  scan             - Scan different TOF values
  opt <fuel> <dry>  - Optimize (e.g., opt 5000 1000)
  bodies           - List bodies
  quit             - Exit
            """)
        elif cmd_upper == "BODIES":
            list_bodies()
        elif cmd_upper in ("ORIGIN", "O"):
            if len(parts) > 1:
                origin = parts[1].capitalize()
            print(f"Origin: {origin}")
        elif cmd_upper in ("DEST", "D", "DESTINATION"):
            if len(parts) > 1:
                destination = parts[1].capitalize()
            print(f"Destination: {destination}")
        elif cmd_upper in ("TOF", "T"):
            if len(parts) > 1:
                tof = int(parts[1])
            print(f"TOF: {tof} days")
        elif cmd_upper in ("CALC", "C", "CALCULATE"):
            transfer(origin, destination, tof)
        elif cmd_upper in ("SCAN", "S"):
            tof_scan(origin, destination)
        elif cmd_upper in ("OPT", "OPTIMIZE", "O?"):
            if len(parts) >= 3:
                fuel = float(parts[1])
                dry = float(parts[2])
                optimize(origin, destination, fuel, dry)
            else:
                print("Usage: opt <fuel_mass> <dry_mass>")
        else:
            # Try to interpret
            try:
                tof = int(cmd)
                result = transfer(origin, destination, tof)
            except ValueError:
                print(f"Unknown command: {cmd}")
                print("Type 'help' for commands")


def main():
    # Check API
    try:
        r = requests.get(f"{API_BASE}/health", timeout=2)
        if r.json().get("status") != "healthy":
            print("ERROR: Backend not healthy")
            sys.exit(1)
    except:
        print("ERROR: Cannot connect to backend at", API_BASE)
        print("Start with: cd planner/03-backend && source venv/bin/activate && uvicorn api.server:app")
        sys.exit(1)
    
    args = sys.argv[1:]
    
    if not args:
        interactive()
    elif len(args) == 1 and args[0].upper() == "BODIES":
        list_bodies()
    elif len(args) >= 2:
        origin = args[0].capitalize()
        destination = args[1].capitalize()
        
        if len(args) >= 3:
            try:
                tof = int(args[2])
                transfer(origin, destination, tof)
            except ValueError:
                # Assume it's optimize
                fuel = float(args[2]) if len(args) >= 3 else 5000
                dry = float(args[3]) if len(args) >= 4 else 1000
                optimize(origin, destination, fuel, dry)
        else:
            # Default 180 day transfer
            transfer(origin, destination, 180)


if __name__ == "__main__":
    main()