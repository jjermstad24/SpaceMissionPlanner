#include "orbital_elements.hh"

#include "epoch.hh"
#include "state_vector.hh"

#include <Eigen/Geometry>

#include <cmath>
#include <vector>

namespace smp::astro::two_body {

OrbitalElements state_to_orbital_elements(
    const core::StateVector& state,
    double mu) {
    OrbitalElements elements;
    elements.epoch = state.epoch();
    elements.mu = mu;

    const Eigen::Vector3d& r = state.position();
    const Eigen::Vector3d& v = state.velocity();

    double r_mag = r.norm();
    double v_mag = v.norm();

    Eigen::Vector3d h = r.cross(v);
    double h_mag = h.norm();

    Eigen::Vector3d n = Eigen::Vector3d::Zero().cross(h);
    double n_mag = n.norm();

    Eigen::Vector3d e_vec = (r * (v_mag * v_mag - mu / r_mag) - v * (r.dot(v))) / mu;
    elements.eccentricity = e_vec.norm();

    double energy = v_mag * v_mag / 2.0 - mu / r_mag;
    if (std::abs(elements.eccentricity - 1.0) > 1e-10) {
        elements.semi_major_axis = -mu / (2.0 * energy);
    }

    if (n_mag > 1e-10) {
        elements.raan = std::atan2(n.y(), n.x());
    }

    if (elements.eccentricity > 1e-10 && n_mag > 1e-10) {
        elements.arg_of_perigee = std::acos(n.dot(e_vec) / (n_mag * elements.eccentricity));
        if (e_vec.z() < 0.0) {
            elements.arg_of_perigee = 2.0 * M_PI - elements.arg_of_perigee;
        }
    }

    if (elements.eccentricity > 1e-10) {
        elements.true_anomaly = std::acos(e_vec.dot(r) / (elements.eccentricity * r_mag));
        if (r.dot(v) < 0.0) {
            elements.true_anomaly = 2.0 * M_PI - elements.true_anomaly;
        }
    }

    double sin_e = (std::sqrt(1.0 - elements.eccentricity * elements.eccentricity) *
                   std::sin(elements.true_anomaly)) /
                  (1.0 + elements.eccentricity * std::cos(elements.true_anomaly));
    double cos_e = (elements.eccentricity + std::cos(elements.true_anomaly)) /
                  (1.0 + elements.eccentricity * std::cos(elements.true_anomaly));
    elements.eccentric_anomaly = std::atan2(sin_e, cos_e);
    if (elements.eccentric_anomaly < 0.0) {
        elements.eccentric_anomaly += 2.0 * M_PI;
    }

    elements.mean_anomaly = elements.eccentric_anomaly -
                           elements.eccentricity * std::sin(elements.eccentric_anomaly);
    if (elements.mean_anomaly < 0.0) {
        elements.mean_anomaly += 2.0 * M_PI;
    }

    elements.inclination = std::acos(h.z() / h_mag);

    return elements;
}

core::StateVector orbital_elements_to_state(const OrbitalElements& elements) {
    double a = elements.semi_major_axis;
    double e = elements.eccentricity;
    double i = elements.inclination;
    double Omega = elements.raan;
    double w = elements.arg_of_perigee;
    double nu = elements.true_anomaly;
    double mu = elements.mu;

    double r = a * (1.0 - e * e) / (1.0 + e * std::cos(nu));

    double x_orb = r * std::cos(nu);
    double y_orb = r * std::sin(nu);

    double v_r = std::sqrt(mu / (a * (1.0 - e * e))) * e * std::sin(nu);
    double v_t = std::sqrt(mu / (a * (1.0 - e * e))) * (1.0 + e * std::cos(nu));

    double cos_o = std::cos(Omega);
    double sin_o = std::sin(Omega);
    double cos_w = std::cos(w);
    double sin_w = std::sin(w);
    double cos_i = std::cos(i);
    double sin_i = std::sin(i);

    double x = (cos_o * cos_w - sin_o * sin_w * cos_i) * x_orb +
               (-cos_o * sin_w - sin_o * cos_w * cos_i) * y_orb;
    double y = (sin_o * cos_w + cos_o * sin_w * cos_i) * x_orb +
               (-sin_o * sin_w + cos_o * cos_w * cos_i) * y_orb;
    double z = (sin_w * sin_i) * x_orb + (cos_w * sin_i) * y_orb;

    double vx = (cos_o * cos_w - sin_o * sin_w * cos_i) * v_t +
               (-cos_o * sin_w - sin_o * cos_w * cos_i) * v_r;
    double vy = (sin_o * cos_w + cos_o * sin_w * cos_i) * v_t +
               (-sin_o * sin_w + cos_o * cos_w * cos_i) * v_r;
    double vz = (sin_w * sin_i) * v_t + (cos_w * sin_i) * v_r;

    core::StateVector result(
        Eigen::Vector3d(x, y, z),
        Eigen::Vector3d(vx, vy, vz),
        elements.epoch,
        core::Frame::J2000(),
        core::CentralBodyId::earth
    );

    return result;
}

double solve_kepler_equation(double mean_anomaly, double eccentricity, double tolerance) {
    double e = mean_anomaly;
    int max_iterations = 100;

    for (int i = 0; i < max_iterations; ++i) {
        double delta = e - eccentricity * std::sin(e) - mean_anomaly;
        double delta_deriv = 1.0 - eccentricity * std::cos(e);

        if (std::abs(delta_deriv) < 1e-15) {
            break;
        }

        double delta_e = delta / delta_deriv;
        e -= delta_e;

        if (std::abs(delta_e) < tolerance) {
            break;
        }
    }

    if (e < 0.0) e += 2.0 * M_PI;
    if (e > 2.0 * M_PI) e -= 2.0 * M_PI;

    return e;
}

void propagate_orbital_elements(OrbitalElements& elements, double dt) {
    double n = elements.mean_motion();

    elements.mean_anomaly += n * dt;
    elements.mean_anomaly = std::fmod(elements.mean_anomaly, 2.0 * M_PI);
    if (elements.mean_anomaly < 0.0) {
        elements.mean_anomaly += 2.0 * M_PI;
    }

    elements.eccentric_anomaly = solve_kepler_equation(
        elements.mean_anomaly,
        elements.eccentricity
    );

    double e = elements.eccentricity;
    double e_anom = elements.eccentric_anomaly;

    elements.true_anomaly = 2.0 * std::atan2(
        std::sqrt(1.0 + e) * std::sin(e_anom / 2.0),
        std::sqrt(1.0 - e) * std::cos(e_anom / 2.0)
    );

    elements.epoch = elements.epoch + dt;
}

} // namespace smp::astro::two_body