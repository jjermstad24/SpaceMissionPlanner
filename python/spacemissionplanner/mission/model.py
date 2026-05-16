"""Mission timeline data model (schema v2)."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Literal, Optional

MISSION_SCHEMA_VERSION = 2

EventType = Literal["launch", "waypoint", "coast", "burn"]
Representation = Literal["eci", "orbital_elements"]
ClockKind = Literal["tdb_since_j2000", "mission_elapsed", "utc"]


@dataclass
class ClockDef:
    id: str
    kind: ClockKind
    zero_event: Optional[str] = None  # for mission_elapsed

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {"id": self.id, "kind": self.kind}
        if self.zero_event is not None:
            d["zero_event"] = self.zero_event
        return d

    @staticmethod
    def from_dict(data: dict[str, Any]) -> ClockDef:
        return ClockDef(
            id=str(data["id"]),
            kind=data["kind"],  # type: ignore[arg-type]
            zero_event=data.get("zero_event"),
        )


@dataclass
class TimeSpec:
    clock: str
    value: Optional[float] = None
    offset_s: Optional[float] = None
    relative_to: Optional[str] = None
    iso: Optional[str] = None

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {"clock": self.clock}
        if self.value is not None:
            d["value"] = self.value
        if self.offset_s is not None:
            d["offset_s"] = self.offset_s
        if self.relative_to is not None:
            d["relative_to"] = self.relative_to
        if self.iso is not None:
            d["iso"] = self.iso
        return d

    @staticmethod
    def from_dict(data: dict[str, Any]) -> TimeSpec:
        return TimeSpec(
            clock=str(data["clock"]),
            value=data.get("value"),
            offset_s=data.get("offset_s"),
            relative_to=data.get("relative_to"),
            iso=data.get("iso"),
        )


@dataclass
class StageDef:
    id: str
    Isp_s: float = 0.0
    propellant_mass_kg: float = 0.0
    dry_mass_kg: float = 0.0

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "Isp_s": self.Isp_s,
            "propellant_mass_kg": self.propellant_mass_kg,
            "dry_mass_kg": self.dry_mass_kg,
        }

    @staticmethod
    def from_dict(data: dict[str, Any]) -> StageDef:
        return StageDef(
            id=str(data["id"]),
            Isp_s=float(data.get("Isp_s", 0.0)),
            propellant_mass_kg=float(data.get("propellant_mass_kg", 0.0)),
            dry_mass_kg=float(data.get("dry_mass_kg", 0.0)),
        )


@dataclass
class VehicleDef:
    id: str = "sc1"
    name: str = "Spacecraft 1"
    stages: list[StageDef] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "stages": [s.to_dict() for s in self.stages],
        }

    @staticmethod
    def from_dict(data: dict[str, Any]) -> VehicleDef:
        return VehicleDef(
            id=str(data.get("id", "sc1")),
            name=str(data.get("name", "Spacecraft 1")),
            stages=[StageDef.from_dict(s) for s in data.get("stages", [])],
        )


@dataclass
class MissionEvent:
    id: str
    type: EventType
    time: TimeSpec
    representation: Optional[Representation] = None
    central_body: Optional[str] = None
    frame: Optional[str] = None
    position_m: Optional[list[float]] = None
    velocity_m_s: Optional[list[float]] = None
    elements: Optional[dict[str, float]] = None
    duration_s: Optional[float] = None
    step_s: Optional[float] = None
    from_event: Optional[str] = None

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {"id": self.id, "type": self.type, "time": self.time.to_dict()}
        if self.representation is not None:
            d["representation"] = self.representation
        if self.central_body is not None:
            d["central_body"] = self.central_body
        if self.frame is not None:
            d["frame"] = self.frame
        if self.position_m is not None:
            d["position_m"] = self.position_m
        if self.velocity_m_s is not None:
            d["velocity_m_s"] = self.velocity_m_s
        if self.elements is not None:
            d["elements"] = self.elements
        if self.duration_s is not None:
            d["duration_s"] = self.duration_s
        if self.step_s is not None:
            d["step_s"] = self.step_s
        if self.from_event is not None:
            d["from_event"] = self.from_event
        return d

    @staticmethod
    def from_dict(data: dict[str, Any]) -> MissionEvent:
        return MissionEvent(
            id=str(data["id"]),
            type=data["type"],  # type: ignore[arg-type]
            time=TimeSpec.from_dict(data["time"]),
            representation=data.get("representation"),
            central_body=data.get("central_body"),
            frame=data.get("frame"),
            position_m=data.get("position_m"),
            velocity_m_s=data.get("velocity_m_s"),
            elements=data.get("elements"),
            duration_s=data.get("duration_s"),
            step_s=data.get("step_s"),
            from_event=data.get("from_event"),
        )


@dataclass
class Mission:
    name: str
    clocks: list[ClockDef]
    events: list[MissionEvent]
    schema_version: int = MISSION_SCHEMA_VERSION
    metadata: dict[str, Any] = field(default_factory=dict)
    vehicle: VehicleDef = field(default_factory=VehicleDef)

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "name": self.name,
            "metadata": self.metadata,
            "clocks": [c.to_dict() for c in self.clocks],
            "vehicle": self.vehicle.to_dict(),
            "events": [e.to_dict() for e in self.events],
        }

    @staticmethod
    def from_dict(data: dict[str, Any]) -> Mission:
        version = int(data.get("schema_version", -1))
        if version != MISSION_SCHEMA_VERSION:
            raise ValueError(f"Unsupported mission schema version {version}")
        return Mission(
            schema_version=version,
            name=str(data.get("name", "mission")),
            metadata=dict(data.get("metadata", {})),
            clocks=[ClockDef.from_dict(c) for c in data.get("clocks", [])],
            vehicle=VehicleDef.from_dict(data.get("vehicle", {})),
            events=[MissionEvent.from_dict(e) for e in data.get("events", [])],
        )

    def event_by_id(self, event_id: str) -> Optional[MissionEvent]:
        for ev in self.events:
            if ev.id == event_id:
                return ev
        return None
