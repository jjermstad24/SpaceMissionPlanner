#pragma once

#include "node.hh"

#include <vector>
#include <unordered_map>
#include <optional>
#include <array>

#include "core/state/include/state_vector.hh"
#include "astro/two_body/include/propagator.hh"

namespace smp::mission_graph {

class PropagatorNode : public Node {
public:
    PropagatorNode(std::string name, double mu = 3.986004418e14);

    std::unordered_set<OutputId> outputs() const override;
    std::unordered_set<InputId> inputs() const override;

    bool compute() override;

    void invalidate_output(const OutputId& output) override;

    std::optional<std::any> get_output(const OutputId& output_id) const override;

    void set_input(const InputId& input_id, std::any value) override;

    bool dirty() const override;

    void clear_cache() override;

    void set_step_size(double dt);
    void set_num_steps(int n);
    void set_initial_state(const core::StateVector& state);

    [[nodiscard]] double mu() const { return propagator_.mu(); }
    [[nodiscard]] double step_size() const { return step_size_; }
    [[nodiscard]] int num_steps() const { return num_steps_; }
    [[nodiscard]] bool has_initial_state() const { return initial_state_.has_value(); }
    [[nodiscard]] std::optional<core::StateVector> initial_state() const { return initial_state_; }

    std::vector<core::StateVector> get_states() const;
    std::vector<double> get_epochs() const;
    std::vector<std::array<double, 3>> get_positions() const;

private:
    astro::two_body::Propagator propagator_;
    double step_size_ = 60.0;
    int num_steps_ = 100;
    std::optional<core::StateVector> initial_state_;

    std::unordered_map<InputId, std::any> input_values_;
    bool computed_ = false;
    std::vector<core::StateVector> states_;
    std::vector<double> epochs_;
    std::vector<std::array<double, 3>> positions_;
};

} // namespace smp::mission_graph