#include "mission_graph/include/propagator_node.hh"

#include <cmath>

namespace smp::mission_graph {

PropagatorNode::PropagatorNode(std::string name, double mu)
    : Node(std::move(name))
    , propagator_(mu) {}

std::unordered_set<Node::OutputId> PropagatorNode::outputs() const {
    return {"states", "epochs", "positions"};
}

std::unordered_set<Node::InputId> PropagatorNode::inputs() const {
    return {"initial_state", "step_size", "num_steps"};
}

bool PropagatorNode::compute() {
    if (!initial_state_.has_value()) {
        error_message_ = "Missing required input: initial_state";
        status_ = NodeStatus::Error;
        return false;
    }

    const core::StateVector& initial = initial_state_.value();

    states_.clear();
    states_.reserve(num_steps_ + 1);

    core::StateVector current = initial;
    for (int i = 0; i <= num_steps_; ++i) {
        states_.push_back(current);
        current = propagator_.propagate(current, step_size_);
    }

    epochs_.clear();
    epochs_.reserve(states_.size());
    for (const auto& s : states_) {
        epochs_.push_back(s.epoch().seconds_since_j2000());
    }

    positions_.clear();
    positions_.reserve(states_.size());
    for (const auto& s : states_) {
        auto pos = s.position();
        positions_.push_back({pos[0], pos[1], pos[2]});
    }

    computed_ = true;
    status_ = NodeStatus::Valid;
    return true;
}

void PropagatorNode::invalidate_output(const OutputId& output) {
    computed_ = false;
    status_ = NodeStatus::Invalid;
}

std::optional<std::any> PropagatorNode::get_output(const OutputId& output_id) const {
    return std::nullopt;
}

void PropagatorNode::set_input(const InputId& input_id, std::any value) {
    try {
        if (input_id == "initial_state") {
            initial_state_ = std::any_cast<core::StateVector>(value);
        } else if (input_id == "step_size") {
            step_size_ = std::any_cast<double>(value);
        } else if (input_id == "num_steps") {
            num_steps_ = std::any_cast<int>(value);
        }
    } catch (const std::bad_any_cast&) {
        error_message_ = "Invalid input type for '" + input_id + "'";
        status_ = NodeStatus::Error;
        return;
    }
    input_values_[input_id] = std::move(value);
    status_ = NodeStatus::Invalid;
}

bool PropagatorNode::dirty() const {
    return status_ == NodeStatus::Invalid;
}

void PropagatorNode::clear_cache() {
    input_values_.clear();
    initial_state_ = std::nullopt;
    states_.clear();
    epochs_.clear();
    positions_.clear();
    computed_ = false;
    status_ = NodeStatus::Pending;
}

void PropagatorNode::set_step_size(double dt) {
    step_size_ = dt;
    status_ = NodeStatus::Invalid;
}

void PropagatorNode::set_num_steps(int n) {
    num_steps_ = n;
    status_ = NodeStatus::Invalid;
}

void PropagatorNode::set_initial_state(const core::StateVector& state) {
    initial_state_ = state;
    status_ = NodeStatus::Invalid;
}

std::vector<core::StateVector> PropagatorNode::get_states() const {
    return states_;
}

std::vector<double> PropagatorNode::get_epochs() const {
    return epochs_;
}

std::vector<std::array<double, 3>> PropagatorNode::get_positions() const {
    return positions_;
}

} // namespace smp::mission_graph