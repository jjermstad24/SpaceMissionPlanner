#pragma once

#include "parameter.hh"

#include <cmath>
#include <functional>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

namespace smp::optimization {

enum class ConstraintType {
    Equality,
    Inequality
};

class Constraint {
public:
    using EvalFn = std::function<double(const std::vector<ParameterPtr>&)>;
    using GradientFn = std::function<std::vector<double>(const std::vector<ParameterPtr>&)>;

    explicit Constraint(std::string name, ConstraintType type)
        : name_(std::move(name)), type_(type), tolerance_(1e-8) {}

    const std::string& name() const { return name_; }

    ConstraintType type() const { return type_; }

    void set_evaluation_function(EvalFn fn) { eval_fn_ = std::move(fn); }

    void set_gradient_function(GradientFn fn) { gradient_fn_ = std::move(fn); }

    double evaluate(const std::vector<ParameterPtr>& params) const {
        if (!eval_fn_) {
            throw std::runtime_error("Constraint '" + name_ + "' has no evaluation function");
        }
        return eval_fn_(params);
    }

    std::vector<double> gradient(const std::vector<ParameterPtr>& params) const {
        if (!gradient_fn_) {
            return std::vector<double>(params.size(), 0.0);
        }
        return gradient_fn_(params);
    }

    bool satisfied(const std::vector<ParameterPtr>& params) const {
        double value = evaluate(params);
        if (type_ == ConstraintType::Equality) {
            return std::abs(value) < tolerance_;
        } else {
            return value >= -tolerance_;
        }
    }

    double violation(const std::vector<ParameterPtr>& params) const {
        double value = evaluate(params);
        if (type_ == ConstraintType::Equality) {
            return std::abs(value);
        } else {
            return value < 0 ? -value : 0.0;
        }
    }

    void set_tolerance(double t) { tolerance_ = t; }

private:
    std::string name_;
    ConstraintType type_;
    double tolerance_;
    EvalFn eval_fn_;
    GradientFn gradient_fn_;
};

using ConstraintPtr = std::shared_ptr<Constraint>;

} // namespace smp::optimization