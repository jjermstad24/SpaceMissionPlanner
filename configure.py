#!/usr/bin/env python3
"""Bootstrap: create a virtualenv, install requirements.txt, and build the C++ tree with CMake."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


def _repo_root() -> Path:
    return Path(__file__).resolve().parent


def _venv_python(venv: Path) -> Path:
    if sys.platform == "win32":
        return venv / "Scripts" / "python.exe"
    return venv / "bin" / "python"


def _venv_pip(venv: Path) -> Path:
    if sys.platform == "win32":
        return venv / "Scripts" / "pip.exe"
    return venv / "bin" / "pip"


def _run(cmd: list[str], *, cwd: Path, env: dict[str, str] | None = None) -> None:
    printable = " ".join(cmd)
    print(f"+ {printable}", flush=True)
    subprocess.run(cmd, cwd=str(cwd), env=env, check=True)


def _ensure_venv(venv: Path, *, fresh: bool, base_python: str) -> Path:
    if fresh and venv.exists():
        shutil.rmtree(venv)
    if not venv.exists():
        _run([base_python, "-m", "venv", str(venv)], cwd=_repo_root())
    py = _venv_python(venv)
    if not py.is_file():
        raise FileNotFoundError(f"venv python missing after create: {py}")
    return py


def _pip_install(py: Path, args: list[str], *, cwd: Path) -> None:
    _run([str(py), "-m", "pip", *args], cwd=cwd)


def _cmake_configure(
    root: Path,
    build_dir: Path,
    *,
    build_type: str,
    build_tests: bool,
    build_python_bindings: bool,
    cmake_args: list[str],
) -> None:
    build_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        "cmake",
        "-S",
        str(root),
        "-B",
        str(build_dir),
        f"-DCMAKE_BUILD_TYPE={build_type}",
        f"-DBUILD_TESTS={'ON' if build_tests else 'OFF'}",
        f"-DBUILD_PYTHON_BINDINGS={'ON' if build_python_bindings else 'OFF'}",
        *cmake_args,
    ]
    _run(cmd, cwd=root)


def _cmake_build(build_dir: Path, *, jobs: int | None) -> None:
    cmd = ["cmake", "--build", str(build_dir)]
    if jobs is not None and jobs > 0:
        cmd.extend(["--parallel", str(jobs)])
    _run(cmd, cwd=build_dir.parent)


def main() -> int:
    root = _repo_root()
    default_venv = root / ".venv"
    default_build = root / "build"

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--venv", type=Path, default=default_venv, help="virtual environment directory")
    parser.add_argument(
        "--fresh-venv",
        action="store_true",
        help="remove the venv directory if it exists before creating it",
    )
    parser.add_argument(
        "--base-python",
        default=sys.executable,
        help="interpreter used to create the venv (default: current python)",
    )
    parser.add_argument("--build-dir", type=Path, default=default_build, help="CMake build directory")
    parser.add_argument(
        "--build-type",
        default="Release",
        choices=["Release", "Debug", "RelWithDebInfo", "MinSizeRel"],
        help="CMAKE_BUILD_TYPE",
    )
    parser.add_argument("--no-tests", action="store_true", help="pass -DBUILD_TESTS=OFF")
    parser.add_argument("--no-python-bindings", action="store_true", help="pass -DBUILD_PYTHON_BINDINGS=OFF")
    parser.add_argument(
        "-j",
        "--parallel",
        type=int,
        default=max(1, (os.cpu_count() or 1)),
        help="parallel build jobs (cmake --build --parallel)",
    )
    parser.add_argument(
        "--skip-pip",
        action="store_true",
        help="skip venv creation and pip install (only run CMake)",
    )
    parser.add_argument(
        "--skip-cmake",
        action="store_true",
        help="only set up Python venv and requirements",
    )
    args, cmake_extra = parser.parse_known_args()

    requirements = root / "requirements.txt"
    if not requirements.is_file():
        print(f"error: missing {requirements}", file=sys.stderr)
        return 1

    venv_path: Path = args.venv
    if not args.skip_pip:
        py = _ensure_venv(venv_path, fresh=args.fresh_venv, base_python=args.base_python)
        _pip_install(py, ["install", "--upgrade", "pip", "setuptools", "wheel"], cwd=root)
        _pip_install(py, ["install", "-r", str(requirements)], cwd=root)
        print(f"\nvenv ready: {venv_path}\n  python: {py}\n", flush=True)
    else:
        py = _venv_python(venv_path)
        if not py.is_file():
            print(f"error: --skip-pip but venv python not found: {py}", file=sys.stderr)
            return 1

    if args.skip_cmake:
        return 0

    try:
        _cmake_configure(
            root,
            args.build_dir,
            build_type=args.build_type,
            build_tests=not args.no_tests,
            build_python_bindings=not args.no_python_bindings,
            cmake_args=cmake_extra,
        )
        _cmake_build(args.build_dir, jobs=args.parallel)
    except FileNotFoundError:
        print("error: 'cmake' not found on PATH", file=sys.stderr)
        print("hint: install CMake and a C++ toolchain, then re-run configure.py", file=sys.stderr)
        return 1
    except subprocess.CalledProcessError as exc:
        return exc.returncode or 1

    print(f"\nbuild complete: {args.build_dir.resolve()}\n", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
