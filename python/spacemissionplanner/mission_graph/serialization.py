"""Save and load mission graphs as versioned JSON (backend-first, notebook-friendly)."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Mapping, Union

import numpy as np

MISSION_GRAPH_JSON_SCHEMA_VERSION = 1

PathLike = Union[str, Path]

try:
    from spacemissionplanner.spacemissionplanner_native import (
        CentralBodyId,
        Edge,
        Epoch,
        Frame,
        Graph,
        PropagatorNode,
        StateVector,
    )

    HAS_NATIVE = True
except ImportError:
    HAS_NATIVE = False
    Graph = Any  # type: ignore[misc, assignment]
    PropagatorNode = Any  # type: ignore[misc, assignment]


def _require_native() -> None:
    if not HAS_NATIVE:
        raise RuntimeError("Native bindings not available; build the C++ extension first")


def _frame_from_name(name: str) -> Frame:
    key = name.strip()
    factories = {
        "J2000": Frame.J2000,
        "TOD": Frame.TOD,
        "EarthFixed": Frame.EarthFixed,
        "MoonFixed": Frame.MoonFixed,
        "Undefined": Frame.Undefined,
    }
    if key not in factories:
        raise ValueError(f"Unknown frame: {name!r}")
    return factories[key]()


def _central_body_from_name(name: str) -> CentralBodyId:
    key = name.strip()
    try:
        return getattr(CentralBodyId, key)
    except AttributeError as exc:
        raise ValueError(f"Unknown central body: {name!r}") from exc


def state_vector_to_dict(state: StateVector) -> dict[str, Any]:
    pos = np.asarray(state.position(), dtype=np.float64).reshape(3)
    vel = np.asarray(state.velocity(), dtype=np.float64).reshape(3)
    return {
        "position_m": [float(pos[0]), float(pos[1]), float(pos[2])],
        "velocity_m_s": [float(vel[0]), float(vel[1]), float(vel[2])],
        "epoch_s": float(state.epoch().seconds_since_j2000()),
        "frame": state.frame().name(),
        "central_body": state.central_body().name,
    }


def state_vector_from_dict(data: Mapping[str, Any]) -> StateVector:
    _require_native()
    return StateVector(
        np.asarray(data["position_m"], dtype=np.float64).reshape(3),
        np.asarray(data["velocity_m_s"], dtype=np.float64).reshape(3),
        Epoch(float(data["epoch_s"])),
        _frame_from_name(str(data["frame"])),
        _central_body_from_name(str(data["central_body"])),
    )


def _serialize_node(node: Any) -> dict[str, Any]:
    if isinstance(node, PropagatorNode):
        spec: dict[str, Any] = {
            "type": "propagator",
            "name": node.name(),
            "mu": float(node.mu()),
            "step_size": float(node.step_size()),
            "num_steps": int(node.num_steps()),
        }
        if node.has_initial_state():
            state = node.initial_state()
            if state is not None:
                spec["initial_state"] = state_vector_to_dict(state)
        return spec
    raise TypeError(f"Unsupported node type for serialization: {type(node)!r}")


def _deserialize_node(spec: Mapping[str, Any]) -> Any:
    _require_native()
    node_type = str(spec["type"])
    name = str(spec["name"])
    if node_type == "propagator":
        mu = float(spec.get("mu", 3.986004418e14))
        node = PropagatorNode(name, mu)
        node.set_step_size(float(spec["step_size"]))
        node.set_num_steps(int(spec["num_steps"]))
        if "initial_state" in spec:
            node.set_initial_state(state_vector_from_dict(spec["initial_state"]))
        return node
    raise ValueError(f"Unknown node type: {node_type!r}")


def graph_to_dict(graph: Graph) -> dict[str, Any]:
    """Serialize a graph to a portable, deterministic dictionary."""
    _require_native()
    nodes = sorted(graph.get_nodes(), key=lambda n: n.name())
    node_specs = [_serialize_node(n) for n in nodes]

    edges = sorted(
        graph.get_edges(),
        key=lambda e: (
            e.source().name(),
            e.source_output(),
            e.target().name(),
            e.target_input(),
        ),
    )
    edge_specs = [
        {
            "source": e.source().name(),
            "source_output": e.source_output(),
            "target": e.target().name(),
            "target_input": e.target_input(),
        }
        for e in edges
    ]

    return {
        "schema_version": MISSION_GRAPH_JSON_SCHEMA_VERSION,
        "nodes": node_specs,
        "edges": edge_specs,
    }


def graph_from_dict(data: Mapping[str, Any]) -> Graph:
    """Rebuild a graph from a dictionary produced by :func:`graph_to_dict`."""
    _require_native()
    version = int(data.get("schema_version", -1))
    if version != MISSION_GRAPH_JSON_SCHEMA_VERSION:
        raise ValueError(
            f"Unsupported mission graph schema version {version}; "
            f"expected {MISSION_GRAPH_JSON_SCHEMA_VERSION}"
        )

    graph = Graph()
    nodes_by_name: dict[str, Any] = {}
    for spec in data.get("nodes", []):
        node = _deserialize_node(spec)
        if node.name() in nodes_by_name:
            raise ValueError(f"Duplicate node name: {node.name()!r}")
        nodes_by_name[node.name()] = node
        graph.add_node(node)

    for spec in data.get("edges", []):
        source_name = str(spec["source"])
        target_name = str(spec["target"])
        if source_name not in nodes_by_name or target_name not in nodes_by_name:
            raise ValueError(f"Edge references unknown node: {spec!r}")
        edge = Edge(
            nodes_by_name[source_name],
            str(spec["source_output"]),
            nodes_by_name[target_name],
            str(spec["target_input"]),
        )
        graph.add_edge(edge)

    return graph


def save_graph_json(path: PathLike, graph: Graph, *, indent: int = 2) -> None:
    """Write a mission graph to JSON."""
    payload = graph_to_dict(graph)
    text = json.dumps(payload, indent=indent, sort_keys=True)
    text += "\n"
    Path(path).write_text(text, encoding="utf-8")


def load_graph_json(path: PathLike) -> Graph:
    """Load a mission graph from JSON."""
    raw = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("Mission graph JSON root must be an object")
    return graph_from_dict(raw)
