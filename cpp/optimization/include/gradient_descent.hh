#pragma once

#include "objective.hh"
#include "constraint.hh"
#include "parameter.hh"

#include <vector>
#include <memory>
#include <optional>

namespace smp::optimization {

struct OptimizationResult {
    std::vector<double> parameters;
    double objective_value;
    int iterations;
    bool converged;
    std::string message;
};

class GradientDescent {
public:
    explicit GradientDescent()
        : learning_rate_(0.01)
        , max_iterations_(1000)
        , tolerance_(1e-8)
        , momentum_(0.0) {}

    GradientDescent& set_learning_rate(double lr) {
        learning_rate_ = lr;
        return *this;
    }

    GradientDescent& set_max_iterations(int max_iter) {
        max_iterations_ = max_iter;
        return *this;
    }

    GradientDescent& set_tolerance(double tol) {
        tolerance_ = tol;
        return *this;
    }

    GradientDescent& set_momentum(double m) {
        momentum_ = m;
        return *this;
    }

    GradientDescent& set_objective(ObjectivePtr obj) {
        objective_ = std::move(obj);
        return *this;
    }

    GradientDescent& set_parameters(std::vector<ParameterPtr> params) {
        parameters_ = std::move(params);
        return *this;
    }

    GradientDescent& set_constraints(std::vector<ConstraintPtr> constraints) {
        constraints_ = std::move(constraints);
        return *this;
    }

    OptimizationResult optimize();

private:
    double learning_rate_;
    int max_iterations_;
    double tolerance_;
    double momentum_;

    ObjectivePtr objective_;
    std::vector<ParameterPtr> parameters_;
    std::vector<ConstraintPtr> constraints_;

    std::vector<double> velocities_;
    bool initialized_ = false;

    void initialize();
    std::vector<double> compute_gradient();
    double compute_penalty();
};

using OptimizerPtr = std::shared_ptr<GradientDescent>;

} // namespace smp::optimization