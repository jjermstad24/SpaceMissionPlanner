#pragma once

#include "node.hh"
#include "edge.hh"

#include <memory>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <queue>
#include <optional>

namespace smp::mission_graph {

class Graph {
public:
    Graph() = default;

    void add_node(NodePtr node);

    void add_edge(EdgePtr edge);

    bool remove_node(const std::string& node_name);

    bool remove_edge(const Node::OutputId& from_output, const Node::InputId& to_input);

    NodePtr get_node(const std::string& name) const;

    std::vector<NodePtr> get_nodes() const;

    std::vector<EdgePtr> get_edges() const;

    std::vector<EdgePtr> get_edges_from(const std::string& node_name) const;

    std::vector<EdgePtr> get_edges_to(const std::string& node_name) const;

    std::optional<std::any> evaluate(const std::string& node_name, const Node::OutputId& output_id);

    void invalidate_node(const std::string& node_name);

    void invalidate_all();

    std::vector<std::string> get_evaluation_order(const std::string& target_node) const;

    bool has_cycle() const;

    std::unordered_set<std::string> get_downstream_nodes(const std::string& node_name) const;

    std::unordered_set<std::string> get_upstream_nodes(const std::string& node_name) const;

private:
    void topological_sort_visit(
        const std::string& node_name,
        std::unordered_set<std::string>& visited,
        std::unordered_set<std::string>& in_progress,
        std::vector<std::string>& result) const;

    void invalidate_downstream(const std::string& node_name);

    std::unordered_map<std::string, NodePtr> nodes_;
    std::vector<EdgePtr> edges_;
};

using GraphPtr = std::shared_ptr<Graph>;

} // namespace smp::mission_graph