#include "gradient_descent.hh"

#include <cmath>

namespace smp::optimization {

void GradientDescent::initialize() {
    if (initialized_) {
        return;
    }

    velocities_.resize(parameters_.size(), 0.0);
    initialized_ = true;
}

std::vector<double> GradientDescent::compute_gradient() {
    std::vector<double> gradient(parameters_.size(), 0.0);

    if (objective_) {
        auto obj_grad = objective_->gradient(parameters_);
        for (size_t i = 0; i < gradient.size(); ++i) {
            gradient[i] += obj_grad[i];
        }
    }

    // Squared-penalty terms match compute_penalty(): equality uses c^2, inequality uses
    // max(0, -g)^2 which equals g^2 when g < 0. In both active cases the coefficient on ∇c is 2*c.
    for (const auto& constraint : constraints_) {
        const double c = constraint->evaluate(parameters_);
        double coef = 0.0;
        if (constraint->type() == ConstraintType::Equality) {
            coef = 2.0 * c;
        } else if (c < 0.0) {
            coef = 2.0 * c;
        }
        if (coef != 0.0) {
            auto constraint_grad = constraint->gradient(parameters_);
            for (size_t i = 0; i < gradient.size(); ++i) {
                gradient[i] += coef * constraint_grad[i];
            }
        }
    }

    for (size_t i = 0; i < parameters_.size(); ++i) {
        if (parameters_[i]->fixed()) {
            gradient[i] = 0.0;
        }
    }

    return gradient;
}

double GradientDescent::compute_penalty() {
    double penalty = 0.0;
    for (const auto& constraint : constraints_) {
        penalty += std::pow(constraint->violation(parameters_), 2);
    }
    return penalty;
}

OptimizationResult GradientDescent::optimize() {
    initialize();

    if (!objective_) {
        return {{}, 0.0, 0, false, "No objective function set"};
    }

    if (parameters_.empty()) {
        return {{}, 0.0, 0, false, "No parameters set"};
    }

    std::vector<double> best_params(parameters_.size());
    for (size_t i = 0; i < parameters_.size(); ++i) {
        best_params[i] = parameters_[i]->value();
    }

    double best_value = objective_->evaluate(parameters_) + compute_penalty();
    double prev_value = best_value + 1.0;

    for (int iter = 0; iter < max_iterations_; ++iter) {
        auto gradient = compute_gradient();

        double grad_norm = 0.0;
        for (double g : gradient) {
            grad_norm += g * g;
        }
        grad_norm = std::sqrt(grad_norm);

        if (grad_norm < tolerance_) {
            for (size_t i = 0; i < parameters_.size(); ++i) {
                parameters_[i]->set_value(best_params[i]);
            }

            return {best_params, best_value, iter + 1, true, "Converged"};
        }

        for (size_t i = 0; i < parameters_.size(); ++i) {
            velocities_[i] = momentum_ * velocities_[i] - learning_rate_ * gradient[i];
            double new_value = parameters_[i]->value() + velocities_[i];
            parameters_[i]->set_value(new_value);
            parameters_[i]->clamp();
        }

        double current_value = objective_->evaluate(parameters_) + compute_penalty();

        if (current_value < best_value) {
            best_value = current_value;
            for (size_t i = 0; i < parameters_.size(); ++i) {
                best_params[i] = parameters_[i]->value();
            }
        }

        if (std::abs(prev_value - current_value) < tolerance_) {
            for (size_t i = 0; i < parameters_.size(); ++i) {
                parameters_[i]->set_value(best_params[i]);
            }

            return {best_params, best_value, iter + 1, true, "Converged"};
        }

        prev_value = current_value;
    }

    for (size_t i = 0; i < parameters_.size(); ++i) {
        parameters_[i]->set_value(best_params[i]);
    }

    return {best_params, best_value, max_iterations_, false, "Max iterations reached"};
}

} // namespace smp::optimization