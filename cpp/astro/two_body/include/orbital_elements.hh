#pragma once

#include "core/time/include/epoch.hh"
#include "core/state/include/state_vector.hh"

#include <cmath>

namespace smp::astro::two_body {

struct OrbitalElements {
    double semi_major_axis = 0.0;
    double eccentricity = 0.0;
    double inclination = 0.0;
    double raan = 0.0;
    double arg_of_perigee = 0.0;
    double true_anomaly = 0.0;
    double mean_anomaly = 0.0;
    double eccentric_anomaly = 0.0;
    core::Epoch epoch;
    double mu = 0.0;

    double period() const {
        if (semi_major_axis <= 0.0 || mu <= 0.0) return 0.0;
        return 2.0 * M_PI * std::sqrt(std::pow(semi_major_axis, 3) / mu);
    }

    double mean_motion() const {
        if (semi_major_axis <= 0.0 || mu <= 0.0) return 0.0;
        return std::sqrt(mu / std::pow(semi_major_axis, 3));
    }

    double apogee() const {
        return semi_major_axis * (1.0 + eccentricity);
    }

    double perigee() const {
        return semi_major_axis * (1.0 - eccentricity);
    }

    double semi_latus_rectum() const {
        return semi_major_axis * (1.0 - eccentricity * eccentricity);
    }
};

OrbitalElements state_to_orbital_elements(
    const core::StateVector& state,
    double mu);

core::StateVector orbital_elements_to_state(
    const OrbitalElements& elements);

double solve_kepler_equation(double mean_anomaly, double eccentricity, double tolerance = 1e-12);

void propagate_orbital_elements(OrbitalElements& elements, double dt);

} // namespace smp::astro::two_body