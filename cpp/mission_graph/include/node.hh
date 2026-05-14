#pragma once

#include <string>
#include <unordered_map>
#include <unordered_set>
#include <memory>
#include <optional>
#include <any>

namespace smp::mission_graph {

enum class NodeStatus {
    Pending,
    Computing,
    Valid,
    Invalid,
    Error
};

class Node {
public:
    using OutputId = std::string;
    using InputId = std::string;

    virtual ~Node() = default;

    explicit Node(std::string name)
        : name_(std::move(name)) {}

    const std::string& name() const { return name_; }
    NodeStatus status() const { return status_; }

    virtual std::unordered_set<OutputId> outputs() const = 0;
    virtual std::unordered_set<InputId> inputs() const = 0;

    virtual bool compute() = 0;

    virtual void invalidate() { status_ = NodeStatus::Invalid; }

    virtual void invalidate_output(const OutputId& output) = 0;

    virtual std::optional<std::any> get_output(const OutputId& output_id) const = 0;

    virtual void set_input(const InputId& input_id, std::any value) = 0;

    virtual bool dirty() const = 0;

    virtual void clear_cache() = 0;

protected:
    std::string name_;
    NodeStatus status_ = NodeStatus::Pending;
    std::string error_message_;
};

using NodePtr = std::shared_ptr<Node>;
using NodeConstPtr = std::shared_ptr<const Node>;

} // namespace smp::mission_graph