#!/usr/bin/env python3
"""
Download SPICE kernels from NAIF (Navigation and Ancillary Information Facility).

This script downloads the required kernel files for planetary ephemerides:
- de440.bsp: Planetary ephemerides (DE440)
- mar099.bsp: Mars system (Phobos, Deimos)
- pck00011.tpc: Planetary constants
- earth_latest_high_prec.bpc: High-precision Earth orientation
- moon_pa_de440_200625.bpc: Moon orientation
- moon_de440_250416.tf: Moon reference frames
"""

import os
import sys
from urllib.request import urlretrieve
from urllib.error import URLError

BASE_URL = "https://naif.jpl.nasa.gov/pub/naif/generic_kernels"

KERNELS = {
    "de440.bsp": f"{BASE_URL}/spk/planets/de440.bsp",
    "mar099.bsp": f"{BASE_URL}/spk/satellites/mar099.bsp",
    "pck00011.tpc": f"{BASE_URL}/pck/pck00011.tpc",
    "earth_latest_high_prec.bpc": f"{BASE_URL}/pck/earth_latest_high_prec.bpc",
    "moon_pa_de440_200625.bpc": f"{BASE_URL}/pck/moon_pa_de440_200625.bpc",
    "moon_de440_250416.tf": f"{BASE_URL}/fk/satellites/moon_de440_250416.tf",
}


def download_file(filename: str, url: str, target_dir: str) -> bool:
    """Download a single file."""
    filepath = os.path.join(target_dir, filename)
    if os.path.exists(filepath):
        print(f"  [SKIP] {filename} already exists")
        return True
    print(f"  [DOWNLOAD] {filename}...")
    try:
        urlretrieve(url, filepath)
        print(f"  [OK] {filename} saved")
        return True
    except URLError as e:
        print(f"  [ERROR] Failed to download {filename}: {e}")
        return False


def main():
    target_dir = os.path.dirname(os.path.abspath(__file__))
    print(f"SPICE Kernel Downloader")
    print(f"Target directory: {target_dir}")
    print(f"Downloading {len(KERNELS)} files...")
    
    failed = []
    for filename, url in KERNELS.items():
        if not download_file(filename, url, target_dir):
            failed.append(filename)
    
    if failed:
        print(f"\n[WARNING] Failed to download: {', '.join(failed)}")
        print("These files may need to be downloaded manually from:")
        print("  https://naif.jpl.nasa.gov/pub/naif/generic_kernels/")
        return 1
    
    print(f"\n[SUCCESS] All {len(KERNELS)} kernels downloaded")
    return 0


if __name__ == "__main__":
    sys.exit(main())