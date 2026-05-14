#pragma once

#include "node.hh"

#include <memory>

namespace smp::mission_graph {

class Edge {
public:
    Edge(NodePtr source, Node::OutputId output_id,
         NodePtr target, Node::InputId input_id)
        : source_(std::move(source))
        , source_output_(std::move(output_id))
        , target_(std::move(target))
        , target_input_(std::move(input_id)) {}

    const NodePtr& source() const { return source_; }
    const Node::OutputId& source_output() const { return source_output_; }

    const NodePtr& target() const { return target_; }
    const Node::InputId& target_input() const { return target_input_; }

    bool is_valid() const;

private:
    NodePtr source_;
    Node::OutputId source_output_;
    NodePtr target_;
    Node::InputId target_input_;
};

using EdgePtr = std::shared_ptr<Edge>;

} // namespace smp::mission_graph