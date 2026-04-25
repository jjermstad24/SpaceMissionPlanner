# SPICE Kernels

This directory contains SPICE kernel files required for planetary ephemerides.

## Required Files

| File | Description | Source |
|---|---|---|
| `de440.bsp` | Planetary ephemerides (DE440) | NAIF |
| `mar097.bsp` | Mars system (Phobos, Deimos) | NAIF |
| `pck00011.tpc` | Planetary constants | NAIF |
| `earth_000101_240604_240312.bpc` | Earth orientation | NAIF |
| `moon_pa_de440_200625.bpc` | Moon orientation | NAIF |
| `moon_de440_220930.tf` | Moon reference frames | NAIF |

## Download

Run the download script:

```bash
python download.py
```

This will download all required kernels from the NAIF (Navigation and Ancillary Information Facility) at JPL.

## Manual Download

If you prefer to download manually, get files from:

- **SPK files**: https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/
- **PCK files**: https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/

## Kernel Paths

The backend expects kernels in this directory. Update `KERNELED_PATH` in the backend config if using a different location.