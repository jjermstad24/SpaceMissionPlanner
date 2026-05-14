#pragma once

#include "parameter.hh"

#include <functional>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

namespace smp::optimization {

enum class ObjectiveType {
    Minimize,
    Maximize
};

class Objective {
public:
    using EvalFn = std::function<double(const std::vector<ParameterPtr>&)>;
    using GradientFn = std::function<std::vector<double>(const std::vector<ParameterPtr>&)>;

    explicit Objective(std::string name, ObjectiveType type = ObjectiveType::Minimize)
        : name_(std::move(name)), type_(type) {}

    const std::string& name() const { return name_; }

    ObjectiveType type() const { return type_; }

    void set_evaluation_function(EvalFn fn) { eval_fn_ = std::move(fn); }

    void set_gradient_function(GradientFn fn) { gradient_fn_ = std::move(fn); }

    double evaluate(const std::vector<ParameterPtr>& params) const {
        if (!eval_fn_) {
            throw std::runtime_error("Objective '" + name_ + "' has no evaluation function");
        }
        double result = eval_fn_(params);
        return type_ == ObjectiveType::Maximize ? -result : result;
    }

    std::vector<double> gradient(const std::vector<ParameterPtr>& params) const {
        if (!gradient_fn_) {
            throw std::runtime_error("Objective '" + name_ + "' has no gradient function");
        }
        auto grad = gradient_fn_(params);
        return type_ == ObjectiveType::Maximize ? negate_vector(grad) : grad;
    }

    bool has_gradient() const { return gradient_fn_ != nullptr; }

private:
    static std::vector<double> negate_vector(const std::vector<double>& v) {
        std::vector<double> result(v.size());
        for (size_t i = 0; i < v.size(); ++i) {
            result[i] = -v[i];
        }
        return result;
    }

    std::string name_;
    ObjectiveType type_;
    EvalFn eval_fn_;
    GradientFn gradient_fn_;
};

using ObjectivePtr = std::shared_ptr<Objective>;

} // namespace smp::optimization