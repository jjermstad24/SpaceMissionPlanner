#pragma once

#include "orbital_elements.hh"

#include <functional>

namespace smp::astro::two_body {

class Propagator {
public:
    explicit Propagator(double mu) : mu_(mu) {}

    core::StateVector propagate(
        const core::StateVector& initial_state,
        const core::Epoch& target_epoch) const;

    core::StateVector propagate(
        const core::StateVector& initial_state,
        double time_of_flight) const;

    OrbitalElements compute_orbital_elements(const core::StateVector& state) const;

    core::StateVector state_from_elements(const OrbitalElements& elements) const;

    void set_mu(double mu) { mu_ = mu; }
    [[nodiscard]] double mu() const { return mu_; }

private:
    double mu_;
};

core::StateVector propagate_keplerian(
    const core::StateVector& initial_state,
    double time_of_flight,
    double mu);

} // namespace smp::astro::two_body