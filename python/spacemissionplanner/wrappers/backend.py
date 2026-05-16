"""Import boundary for compiled extensions; keeps physics out of GUI code."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from importlib import import_module
from types import ModuleType
from typing import Optional


class NativeExtensionStatus(Enum):
    MISSING = "missing"
    LOADED = "loaded"
    ERROR = "error"


@dataclass(frozen=True)
class NativeExtensionInfo:
    status: NativeExtensionStatus
    module: Optional[ModuleType] = None
    error: Optional[str] = None
    module_name: Optional[str] = None


# CMake/pybind11 output module (see cpp/bindings/python/CMakeLists.txt).
_NATIVE_MODULE_CANDIDATES: tuple[str, ...] = (
    "spacemissionplanner.spacemissionplanner_native",
    "spacemissionplanner._native",
)


def native_extension_status(module_name: str | None = None) -> NativeExtensionInfo:
    """Try to load the pybind11 module produced by the C++ build."""
    candidates = (module_name,) if module_name else _NATIVE_MODULE_CANDIDATES
    last_error: str | None = None
    for name in candidates:
        try:
            mod = import_module(name)
        except ImportError as exc:
            last_error = str(exc)
            continue
        except Exception as exc:  # pragma: no cover - defensive
            return NativeExtensionInfo(NativeExtensionStatus.ERROR, None, str(exc), name)
        return NativeExtensionInfo(NativeExtensionStatus.LOADED, mod, None, name)
    return NativeExtensionInfo(NativeExtensionStatus.MISSING, None, last_error, None)
