"""Execute mission graphs in topological order with edge wiring."""

from __future__ import annotations

from collections import deque
from typing import Any

try:
    from spacemissionplanner.spacemissionplanner_native import Graph, PropagatorNode
except ImportError:
    Graph = Any  # type: ignore[misc, assignment]
    PropagatorNode = Any  # type: ignore[misc, assignment]


class GraphExecutionError(RuntimeError):
    pass


def topological_order(graph: Graph) -> list[str]:
    """Return node names in an order where dependencies run before dependents."""
    nodes = {n.name(): n for n in graph.get_nodes()}
    if not nodes:
        return []

    indegree: dict[str, int] = {name: 0 for name in nodes}
    for edge in graph.get_edges():
        indegree[edge.target().name()] += 1

    queue = deque(sorted(name for name, deg in indegree.items() if deg == 0))
    order: list[str] = []
    while queue:
        name = queue.popleft()
        order.append(name)
        for edge in graph.get_edges_from(name):
            target = edge.target().name()
            indegree[target] -= 1
            if indegree[target] == 0:
                queue.append(target)

    if len(order) != len(nodes):
        raise GraphExecutionError("Graph has a cycle or disconnected dependency")
    return order


def _wire_propagator_inputs(graph: Graph, node: PropagatorNode) -> None:
    for edge in graph.get_edges_to(node.name()):
        source = edge.source()
        if not isinstance(source, PropagatorNode):
            continue
        if not source.compute():
            raise GraphExecutionError(f"Upstream node {source.name()!r} failed to compute")
        out_id = edge.source_output()
        in_id = edge.target_input()
        if in_id == "initial_state" and out_id == "states":
            states = source.get_states()
            if not states:
                raise GraphExecutionError(f"Node {source.name()!r} produced no states")
            node.set_initial_state(states[-1])
        elif in_id == "initial_state" and out_id == "positions":
            positions = source.get_positions()
            states = source.get_states()
            if not positions or not states:
                raise GraphExecutionError(f"Node {source.name()!r} has no trajectory output")
            last_state = states[-1]
            node.set_initial_state(last_state)
        elif in_id == "step_size":
            node.set_step_size(source.step_size())
        elif in_id == "num_steps":
            node.set_num_steps(source.num_steps())


def run_graph(graph: Graph) -> list[str]:
    """Execute all nodes in dependency order. Returns names of nodes that ran."""
    if graph.has_cycle():
        raise GraphExecutionError("Cannot run graph with a cycle")

    executed: list[str] = []
    for name in topological_order(graph):
        node = graph.get_node(name)
        if node is None:
            continue
        if isinstance(node, PropagatorNode):
            _wire_propagator_inputs(graph, node)
            if not node.has_initial_state():
                raise GraphExecutionError(f"Propagator {name!r} has no initial state")
            if not node.compute():
                raise GraphExecutionError(f"Propagator {name!r} failed to compute")
            executed.append(name)
    return executed
