# Space Mission Planner

Modular astrodynamics and mission design framework: C++ numerical core (Eigen, optional SPICE), mission graph, optimization, and a Python frontend (notebooks, Qt GUI, visualization). See `agent/ARCHITECTURE.md` and `agent/AGENTS.md` for design goals.

---

## Prerequisites

| Component | Notes |
|-----------|--------|
| **C++20** compiler | GCC, Clang, or MSVC |
| **CMake** | 3.20 or newer |
| **Python** | 3.10 or newer (for scripts, tests, and GUI) |
| **GoogleTest** | Required when `BUILD_TESTS` is `ON` (default) |

Optional: **Ninja** build tool (`-G Ninja` with CMake). Python packages are listed in `requirements.txt` and `pyproject.toml`.

---

## Quick start (recommended)

From the repository root, run the bootstrap script. It creates a **`.venv`**, installs **`requirements.txt`** (including an editable install of the `spacemissionplanner` package), configures CMake in **`build/`**, and compiles the C++ targets.

```bash
./configure.py
```

On Windows, use `python configure.py` if the script is not marked executable.

### `configure.py` options

| Option | Description |
|--------|-------------|
| `--venv PATH` | Virtual environment directory (default: `.venv`) |
| `--fresh-venv` | Delete the venv if it exists, then recreate it |
| `--base-python PATH` | Interpreter used to create the venv (default: current `python3`) |
| `--build-dir PATH` | CMake build tree (default: `build`) |
| `--build-type TYPE` | `Release`, `Debug`, `RelWithDebInfo`, or `MinSizeRel` (default: `Release`) |
| `--no-tests` | CMake: `-DBUILD_TESTS=OFF` |
| `--no-python-bindings` | CMake: `-DBUILD_PYTHON_BINDINGS=OFF` |
| `--skip-pip` | Only run CMake (venv must already exist if you need Python) |
| `--skip-cmake` | Only create/update the venv and install Python deps |
| `-j N`, `--parallel N` | Parallel compile jobs (default: CPU count) |

Any additional arguments are passed through to **CMake configure**, for example:

```bash
./configure.py -G Ninja -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
```

---

## Manual build (C++ only)

If you prefer not to use `configure.py` for the native build:

```bash
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTS=ON -DBUILD_PYTHON_BINDINGS=ON
cmake --build build --parallel
```

CMake options (see root `CMakeLists.txt`):

- **`BUILD_TESTS`** — build C++ unit test executables (default: `ON`).
- **`BUILD_PYTHON_BINDINGS`** — reserved for future pybind11 targets (default: `ON`; bindings are still placeholders).

---

## Manual Python environment

If you already built C++ with `--skip-pip` or only want Python tooling:

```bash
python3 -m venv .venv
.venv/bin/pip install --upgrade pip setuptools wheel
.venv/bin/pip install -r requirements.txt
```

`requirements.txt` installs runtime and dev dependencies and **`pip install -e .`** for the editable `spacemissionplanner` package defined in `pyproject.toml`.

---

## Running C++ unit tests

CTest may not list individual GoogleTest cases in all configurations; the reliable approach is to run the built binaries from the build tree (paths match a default `build/` directory):

```bash
./build/cpp/core/frames/test_frames
./build/cpp/astro/two_body/test_two_body
./build/cpp/mission_graph/test_mission_graph
./build/cpp/optimization/test_optimization
```

On Windows, use `build\cpp\...\test_*.exe` under your generator's layout. Pass [GoogleTest flags](https://google.github.io/googletest/advanced.html#running-a-subset-of-the-tests) as needed, for example:

```bash
./build/cpp/optimization/test_optimization --gtest_color=yes
```

---

## Running Python tests

Activate the venv created by `configure.py` (or your own), then:

```bash
.venv/bin/python -m pytest python/tests -q
```

---

## Running the Python GUI

Install dependencies first (included in `./configure.py` and in `requirements.txt`). Then either:

```bash
.venv/bin/smp-gui
```

or:

```bash
.venv/bin/python -m spacemissionplanner.gui
```

PySide6 is required; the entry point prints a short message if it is missing.

On **WSLg**, do **not** set `QT_QPA_PLATFORM=xcb` unless you have installed **`libxcb-cursor0`** (Qt 6.5+). Leave the variable **unset** so Qt can use **wayland** or another working plugin.

The **3D viewer** (sidebar: “3D viewer”) uses **PyVista** embedded via **pyvistaqt**; both are listed in `requirements.txt`. It loads a **toy** solar-system demo until SPICE-backed ephemeris is wired (see `agent/VIEWER_PLAN.md`). Use the time slider to scrub the demo trajectory.

---

## Using the Python package in notebooks or scripts

With the venv activated:

```bash
.venv/bin/python -c "import spacemissionplanner; print(spacemissionplanner.__version__)"
```

Example notebooks can live under `python/notebooks/` (see `agent/ARCHITECTURE.md`).

---

## Troubleshooting

- **`cmake` not found** — Install CMake and ensure it is on your `PATH`.
- **`pip install` fails with "externally managed environment"** — Use a virtual environment (as `configure.py` does), not system-wide pip.
- **Missing GTest** — Install your platform's GoogleTest development package, or set `-DBUILD_TESTS=OFF` if you only need libraries without tests.
- **GUI / Qt: “Could not load the Qt platform plugin `xcb`” / `xcb-cursor0` (Qt 6.5+)** — If you set `QT_QPA_PLATFORM=xcb`, you must install **`libxcb-cursor0`** on the system (not via pip), e.g. `sudo apt install libxcb-cursor0` on Debian/Ubuntu/WSL. **Prefer** leaving `QT_QPA_PLATFORM` **unset** on **WSLg** so Qt can pick **wayland** or a working backend automatically. Alternatively try explicitly: `export QT_QPA_PLATFORM=wayland`.
- **GUI / VTK: `BadWindow` (X11)** — The app defers VTK until the **3D viewer** tab is shown and enables shared GL contexts. If issues remain with legacy X11 forwarding, try `export LIBGL_ALWAYS_INDIRECT=1` (remote X only); on WSLg use the default platform, not forced `xcb`, unless you install `libxcb-cursor0`.

---

## Repository layout (short)

| Path | Content |
|------|--------|
| `cpp/` | C++ libraries (`core`, `astro`, `optimization`, `mission_graph`, `bindings`, …) |
| `python/spacemissionplanner/` | Installable Python frontend |
| `configure.py` | Venv + pip + CMake build |
| `agent/` | Architecture and contributor notes |
