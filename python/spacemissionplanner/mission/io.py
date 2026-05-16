"""Load and save mission JSON (schema v2)."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Union

from spacemissionplanner.mission.model import Mission

PathLike = Union[str, Path]


def save_mission_json(path: PathLike, mission: Mission, *, indent: int = 2) -> None:
    payload = mission.to_dict()
    text = json.dumps(payload, indent=indent, sort_keys=True)
    Path(path).write_text(text + "\n", encoding="utf-8")


def load_mission_json(path: PathLike) -> Mission:
    raw = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("Mission JSON root must be an object")
    return Mission.from_dict(raw)
