#pragma once

#include "epoch.hh"
#include "frame.hh"

#include <Eigen/Core>

namespace smp::core {

enum class CentralBodyId : int {
    undefined = 0,
    sun = 10,
    mercury = 199,
    venus = 299,
    earth = 399,
    moon = 301,
    mars = 499,
    jupiter = 599,
    saturn = 699,
    uranus = 799,
    neptune = 899,
    pluto = 999
};

class StateVector {
public:
    using Vector6 = Eigen::Matrix<double, 6, 1>;
    using Vector3 = Eigen::Vector3d;

    StateVector() = default;

    StateVector(Vector3 position, Vector3 velocity, Epoch epoch,
                Frame frame, CentralBodyId central_body)
        : position_(std::move(position))
        , velocity_(std::move(velocity))
        , epoch_(epoch)
        , frame_(frame)
        , central_body_(central_body) {}

    explicit StateVector(Vector6 state, Epoch epoch, Frame frame, CentralBodyId central_body)
        : position_(state.head<3>())
        , velocity_(state.tail<3>())
        , epoch_(epoch)
        , frame_(frame)
        , central_body_(central_body) {}

    [[nodiscard]] const Vector3& position() const { return position_; }
    [[nodiscard]] const Vector3& velocity() const { return velocity_; }
    [[nodiscard]] Vector3& position() { return position_; }
    [[nodiscard]] Vector3& velocity() { return velocity_; }

    [[nodiscard]] Vector6 state() const {
        Vector6 result;
        result.head<3>() = position_;
        result.tail<3>() = velocity_;
        return result;
    }

    [[nodiscard]] const Epoch& epoch() const { return epoch_; }
    [[nodiscard]] Epoch& epoch() { return epoch_; }

    [[nodiscard]] const Frame& frame() const { return frame_; }
    [[nodiscard]] Frame& frame() { return frame_; }

    [[nodiscard]] CentralBodyId central_body() const { return central_body_; }
    void set_central_body(CentralBodyId body) { central_body_ = body; }

    [[nodiscard]] double kinetic_energy() const {
        return 0.5 * velocity_.dot(velocity_);
    }

    [[nodiscard]] double speed() const {
        return velocity_.norm();
    }

    [[nodiscard]] double altitude(double radius) const {
        return position_.norm() - radius;
    }

    StateVector operator+(const StateVector& other) const {
        return StateVector(position_ + other.position_,
                          velocity_ + other.velocity_,
                          epoch_, frame_, central_body_);
    }

    StateVector operator-(const StateVector& other) const {
        return StateVector(position_ - other.position_,
                          velocity_ - other.velocity_,
                          epoch_, frame_, central_body_);
    }

    StateVector operator*(double scalar) const {
        return StateVector(position_ * scalar,
                          velocity_ * scalar,
                          epoch_, frame_, central_body_);
    }

private:
    Vector3 position_{Vector3::Zero()};
    Vector3 velocity_{Vector3::Zero()};
    Epoch epoch_;
    Frame frame_;
    CentralBodyId central_body_ = CentralBodyId::undefined;
};

inline StateVector operator*(double scalar, const StateVector& sv) {
    return sv * scalar;
}

} // namespace smp::core