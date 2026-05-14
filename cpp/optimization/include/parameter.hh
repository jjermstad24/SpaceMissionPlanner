#pragma once

#include <string>
#include <functional>
#include <optional>
#include <vector>
#include <memory>

#ifdef max
#undef max
#endif

#ifdef min
#undef min
#endif

namespace smp::optimization {

class Parameter {
public:
    using ComputeFn = std::function<double()>;
    using GradientFn = std::function<std::vector<double>()>;

    explicit Parameter(std::string name)
        : name_(std::move(name)), value_(0.0), fixed_(false) {}

    explicit Parameter(std::string name, double value)
        : name_(std::move(name)), value_(value), fixed_(false) {}

    const std::string& name() const { return name_; }

    double value() const { return value_; }
    void set_value(double v) { value_ = v; }

    bool fixed() const { return fixed_; }
    void set_fixed(bool f) { fixed_ = f; }

    std::optional<double> lower_bound() const { return lower_bound_; }
    void set_lower_bound(double b) { lower_bound_ = b; }

    std::optional<double> upper_bound() const { return upper_bound_; }
    void set_upper_bound(double b) { upper_bound_ = b; }

    std::optional<double> gradient() const { return gradient_; }
    void set_gradient(double g) { gradient_ = g; }

    void clamp() {
        if (lower_bound_ && value_ < *lower_bound_) {
            value_ = *lower_bound_;
        }
        if (upper_bound_ && value_ > *upper_bound_) {
            value_ = *upper_bound_;
        }
    }

private:
    std::string name_;
    double value_;
    bool fixed_;
    std::optional<double> lower_bound_;
    std::optional<double> upper_bound_;
    std::optional<double> gradient_;
};

using ParameterPtr = std::shared_ptr<Parameter>;

} // namespace smp::optimization