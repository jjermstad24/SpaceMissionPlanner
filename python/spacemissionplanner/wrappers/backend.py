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


def native_extension_status(module_name: str = "spacemissionplanner._native") -> NativeExtensionInfo:
    """Try to load the pybind11 module produced by the C++ build (name TBD)."""
    try:
        mod = import_module(module_name)
    except ImportError as exc:
        return NativeExtensionInfo(NativeExtensionStatus.MISSING, None, str(exc))
    except Exception as exc:  # pragma: no cover - defensive
        return NativeExtensionInfo(NativeExtensionStatus.ERROR, None, str(exc))
    return NativeExtensionInfo(NativeExtensionStatus.LOADED, mod, None)
