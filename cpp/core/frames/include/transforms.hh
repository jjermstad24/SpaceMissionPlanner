#pragma once

#include "state_vector.hh"

#include <Eigen/Core>

namespace smp::core {

class Transform {
public:
    using Matrix3 = Eigen::Matrix3d;
    using Matrix6 = Eigen::Matrix<double, 6, 6>;

    Transform() = default;

    Transform(Matrix3 rotation, Eigen::Vector3d translation,
              Matrix3 rate_rotation = Matrix3::Identity())
        : rotation_(std::move(rotation))
        , translation_(std::move(translation))
        , rate_rotation_(std::move(rate_rotation)) {}

    [[nodiscard]] const Matrix3& rotation() const { return rotation_; }
    [[nodiscard]] const Eigen::Vector3d& translation() const { return translation_; }
    [[nodiscard]] const Matrix3& rate_rotation() const { return rate_rotation_; }

    [[nodiscard]] Matrix6 matrix6() const {
        Matrix6 result = Matrix6::Zero();
        result.topLeftCorner<3,3>() = rotation_;
        result.bottomRightCorner<3,3>() = rotation_;
        result.topRightCorner<3,3>() = skew_symmetric(translation_) * rotation_;
        return result;
    }

    StateVector apply(const StateVector& state) const {
        StateVector result;
        result.position() = rotation_ * state.position() + translation_;
        result.velocity() = rotation_ * state.velocity();
        result.epoch() = state.epoch();
        result.frame() = Frame::Undefined();
        result.set_central_body(state.central_body());
        return result;
    }

    Transform inverse() const {
        return Transform(rotation_.transpose(),
                        -rotation_.transpose() * translation_,
                        rate_rotation_.transpose());
    }

    Transform operator*(const Transform& other) const {
        return Transform(rotation_ * other.rotation_,
                        rotation_ * other.translation_ + translation_,
                        rate_rotation_ * other.rate_rotation_);
    }

    static Eigen::Matrix3d skew_symmetric(const Eigen::Vector3d& v) {
        Eigen::Matrix3d result;
        result << 0.0, -v.z(),  v.y(),
                 v.z(), 0.0,  -v.x(),
                -v.y(),  v.x(), 0.0;
        return result;
    }

    static Transform Identity() {
        return Transform(Matrix3::Identity(), Eigen::Vector3d::Zero());
    }

    static Transform IdentityToInertial(CentralBodyId body) {
        return Identity();
    }

private:
    Eigen::Matrix3d rotation_{Eigen::Matrix3d::Identity()};
    Eigen::Vector3d translation_{Eigen::Vector3d::Zero()};
    Eigen::Matrix3d rate_rotation_{Eigen::Matrix3d::Identity()};
};

namespace transforms {

Transform rotation_only(const Eigen::Matrix3d& rotation);

Transform translation_only(const Eigen::Vector3d& translation);

Transform from_eulerAngles(double raan, double inclination, double arg_of_perigee);

Eigen::Matrix3d rotation_matrix(double raan, double inclination, double arg_of_perigee);

Eigen::Matrix3d rotation_matrix_x(double angle);
Eigen::Matrix3d rotation_matrix_y(double angle);
Eigen::Matrix3d rotation_matrix_z(double angle);

Transform j2000_to_earth_fixed(const Epoch& epoch);
Transform earth_fixed_to_j2000(const Epoch& epoch);

} // namespace transforms
} // namespace smp::core