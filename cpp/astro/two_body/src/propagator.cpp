#include "propagator.hh"

namespace smp::astro::two_body {

core::StateVector Propagator::propagate(
    const core::StateVector& initial_state,
    const core::Epoch& target_epoch) const {
    double dt = target_epoch - initial_state.epoch();
    return propagate(initial_state, dt);
}

core::StateVector Propagator::propagate(
    const core::StateVector& initial_state,
    double time_of_flight) const {
    return propagate_keplerian(initial_state, time_of_flight, mu_);
}

OrbitalElements Propagator::compute_orbital_elements(const core::StateVector& state) const {
    return state_to_orbital_elements(state, mu_);
}

core::StateVector Propagator::state_from_elements(const OrbitalElements& elements) const {
    OrbitalElements el = elements;
    el.mu = mu_;
    return orbital_elements_to_state(el);
}

core::StateVector propagate_keplerian(
    const core::StateVector& initial_state,
    double time_of_flight,
    double mu) {
    auto elements = state_to_orbital_elements(initial_state, mu);

    propagate_orbital_elements(elements, time_of_flight);

    elements.mu = mu;

    return orbital_elements_to_state(elements);
}

} // namespace smp::astro::two_body