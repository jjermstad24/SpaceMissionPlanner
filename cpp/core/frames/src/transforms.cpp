#include "transforms.hh"

#include <cmath>

namespace smp::core::transforms {

Transform rotation_only(const Eigen::Matrix3d& rotation) {
    return Transform(rotation, Eigen::Vector3d::Zero());
}

Transform translation_only(const Eigen::Vector3d& translation) {
    return Transform(Eigen::Matrix3d::Identity(), translation);
}

Transform from_eulerAngles(double raan, double inclination, double arg_of_perigee) {
    return Transform(rotation_matrix(raan, inclination, arg_of_perigee),
                    Eigen::Vector3d::Zero());
}

Eigen::Matrix3d rotation_matrix(double raan, double inclination, double arg_of_perigee) {
    return rotation_matrix_z(raan) * rotation_matrix_x(inclination) * rotation_matrix_z(arg_of_perigee);
}

Eigen::Matrix3d rotation_matrix_x(double angle) {
    double c = std::cos(angle);
    double s = std::sin(angle);
    Eigen::Matrix3d result;
    result << 1.0, 0.0,  0.0,
              0.0,  c,   -s,
              0.0,  s,    c;
    return result;
}

Eigen::Matrix3d rotation_matrix_y(double angle) {
    double c = std::cos(angle);
    double s = std::sin(angle);
    Eigen::Matrix3d result;
    result <<  c, 0.0, s,
              0.0, 1.0, 0.0,
              -s, 0.0, c;
    return result;
}

Eigen::Matrix3d rotation_matrix_z(double angle) {
    double c = std::cos(angle);
    double s = std::sin(angle);
    Eigen::Matrix3d result;
    result <<  c, -s, 0.0,
               s,  c, 0.0,
             0.0, 0.0, 1.0;
    return result;
}

Transform j2000_to_earth_fixed(const Epoch& epoch) {
    double gast = 0.0;
    double days = epoch.days_since_j2000();
    double t = days / 36525.0;
    gast = 1.7533685592338185 + 1.0027379093507951 * days
           + 0.000085 * t * t;
    gast = std::fmod(gast, 1.0);
    if (gast < 0) gast += 1.0;
    double theta = gast * 2.0 * M_PI;
    return Transform(rotation_matrix_z(-theta), Eigen::Vector3d::Zero());
}

Transform earth_fixed_to_j2000(const Epoch& epoch) {
    return j2000_to_earth_fixed(epoch).inverse();
}

} // namespace smp::core::transforms