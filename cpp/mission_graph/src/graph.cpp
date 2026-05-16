#include "graph.hh"

#include <algorithm>
#include <any>
#include <stdexcept>

namespace smp::mission_graph {

void Graph::add_node(NodePtr node) {
    if (!node) {
        throw std::invalid_argument("Cannot add null node to graph");
    }
    if (nodes_.count(node->name()) > 0) {
        throw std::invalid_argument("Node with name '" + node->name() + "' already exists");
    }
    nodes_[node->name()] = std::move(node);
}

void Graph::add_edge(EdgePtr edge) {
    if (!edge) {
        throw std::invalid_argument("Cannot add null edge to graph");
    }
    if (!edge->is_valid()) {
        throw std::invalid_argument("Invalid edge: source or target output/input not found");
    }

    auto source_node = edge->source();
    auto target_node = edge->target();

    auto output = source_node->get_output(edge->source_output());
    if (output.has_value()) {
        target_node->set_input(edge->target_input(), *output);
    }

    edges_.push_back(std::move(edge));
}

bool Graph::remove_node(const std::string& node_name) {
    auto it = nodes_.find(node_name);
    if (it == nodes_.end()) {
        return false;
    }

    edges_.erase(
        std::remove_if(edges_.begin(), edges_.end(),
            [&node_name](const EdgePtr& edge) {
                return edge->source()->name() == node_name ||
                       edge->target()->name() == node_name;
            }),
        edges_.end()
    );

    nodes_.erase(it);
    return true;
}

bool Graph::remove_edge(const Node::OutputId& from_output, const Node::InputId& to_input) {
    auto it = std::find_if(edges_.begin(), edges_.end(),
        [&from_output, &to_input](const EdgePtr& edge) {
            return edge->source_output() == from_output &&
                   edge->target_input() == to_input;
        });

    if (it == edges_.end()) {
        return false;
    }

    edges_.erase(it);
    return true;
}

NodePtr Graph::get_node(const std::string& name) const {
    auto it = nodes_.find(name);
    if (it == nodes_.end()) {
        return nullptr;
    }
    return it->second;
}

std::vector<NodePtr> Graph::get_nodes() const {
    std::vector<NodePtr> result;
    result.reserve(nodes_.size());
    for (const auto& [name, node] : nodes_) {
        result.push_back(node);
    }
    return result;
}

std::vector<EdgePtr> Graph::get_edges() const {
    return edges_;
}

std::vector<EdgePtr> Graph::get_edges_from(const std::string& node_name) const {
    std::vector<EdgePtr> result;
    for (const auto& edge : edges_) {
        if (edge->source()->name() == node_name) {
            result.push_back(edge);
        }
    }
    return result;
}

std::vector<EdgePtr> Graph::get_edges_to(const std::string& node_name) const {
    std::vector<EdgePtr> result;
    for (const auto& edge : edges_) {
        if (edge->target()->name() == node_name) {
            result.push_back(edge);
        }
    }
    return result;
}

std::optional<std::any> Graph::evaluate(const std::string& node_name, const Node::OutputId& output_id) {
    auto node = get_node(node_name);
    if (!node) {
        return std::nullopt;
    }

    if (!node->compute()) {
        return std::nullopt;
    }

    return node->get_output(output_id);
}

void Graph::invalidate_node(const std::string& node_name) {
    auto node = get_node(node_name);
    if (!node) {
        return;
    }

    node->invalidate();
    invalidate_downstream(node_name);
}

void Graph::invalidate_all() {
    for (auto& [name, node] : nodes_) {
        node->invalidate();
    }
}

void Graph::invalidate_downstream(const std::string& node_name) {
    auto downstream = get_downstream_nodes(node_name);
    for (const auto& downstream_name : downstream) {
        auto node = get_node(downstream_name);
        if (node) {
            node->invalidate();
        }
    }
}

std::vector<std::string> Graph::get_evaluation_order(const std::string& target_node) const {
    if (!nodes_.count(target_node)) {
        return {};
    }

    std::unordered_set<std::string> visited;
    std::unordered_set<std::string> in_progress;
    std::vector<std::string> result;

    topological_sort_visit(target_node, visited, in_progress, result);

    std::reverse(result.begin(), result.end());
    return result;
}

void Graph::topological_sort_visit(
    const std::string& node_name,
    std::unordered_set<std::string>& visited,
    std::unordered_set<std::string>& in_progress,
    std::vector<std::string>& result) const {

    if (visited.count(node_name) > 0) {
        return;
    }

    if (in_progress.count(node_name) > 0) {
        throw std::runtime_error("Cycle detected in graph");
    }

    in_progress.insert(node_name);

    for (const auto& edge : get_edges_to(node_name)) {
        topological_sort_visit(edge->source()->name(), visited, in_progress, result);
    }

    in_progress.erase(node_name);
    visited.insert(node_name);
    result.push_back(node_name);
}

bool Graph::has_cycle() const {
    std::unordered_set<std::string> visited;
    std::unordered_set<std::string> in_progress;

    for (const auto& [name, node] : nodes_) {
        try {
            std::vector<std::string> result;
            topological_sort_visit(name, visited, in_progress, result);
        } catch (const std::runtime_error&) {
            return true;
        }
    }
    return false;
}

std::unordered_set<std::string> Graph::get_downstream_nodes(const std::string& node_name) const {
    std::unordered_set<std::string> result;
    std::queue<std::string> queue;
    queue.push(node_name);

    while (!queue.empty()) {
        auto current = queue.front();
        queue.pop();

        for (const auto& edge : get_edges_from(current)) {
            auto target_name = edge->target()->name();
            if (result.count(target_name) == 0) {
                result.insert(target_name);
                queue.push(target_name);
            }
        }
    }

    result.erase(node_name);
    return result;
}

std::unordered_set<std::string> Graph::get_upstream_nodes(const std::string& node_name) const {
    std::unordered_set<std::string> result;
    std::queue<std::string> queue;
    queue.push(node_name);

    while (!queue.empty()) {
        auto current = queue.front();
        queue.pop();

        for (const auto& edge : get_edges_to(current)) {
            auto source_name = edge->source()->name();
            if (result.count(source_name) == 0) {
                result.insert(source_name);
                queue.push(source_name);
            }
        }
    }

    result.erase(node_name);
    return result;
}

} // namespace smp::mission_graph