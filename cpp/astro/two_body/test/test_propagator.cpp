#include <gtest/gtest.h>

#include "propagator.hh"
#include "core.hh"

#include <Eigen/Geometry>

using namespace smp::astro::two_body;

const double MU_EARTH = 3.986004418e14;
const double EARTH_RADIUS = 6371.0e3;

TEST(PropagatorTest, CircularOrbitPropagate) {
    double r = EARTH_RADIUS + 400.0e3;
    double v = std::sqrt(MU_EARTH / r);
    double period = 2.0 * M_PI * std::sqrt(std::pow(r, 3) / MU_EARTH);

    core::StateVector initial(
        Eigen::Vector3d(r, 0.0, 0.0),
        Eigen::Vector3d(0.0, v, 0.0),
        core::Epoch::J2000(),
        core::Frame::J2000(),
        core::CentralBodyId::earth
    );

    Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, period / 4.0);

    EXPECT_NEAR(result.position().norm(), r, 1.0);
}

TEST(PropagatorTest, EllipticalOrbitApogee) {
    double a = 10000.0e3;
    double e = 0.5;
    double rp = a * (1.0 - e);
    double ra = a * (1.0 + e);

    OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.true_anomaly = 0.0;
    elements.epoch = core::Epoch::J2000();
    elements.mu = MU_EARTH;

    Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    double period = elements.period();
    auto final = prop.propagate(initial, period / 2.0);

    EXPECT_NEAR(final.position().norm(), ra, 100.0);
}

TEST(PropagatorTest, EnergyConservation) {
    double a = 8000.0e3;
    double e = 0.2;

    OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.true_anomaly = M_PI / 3.0;
    elements.epoch = core::Epoch::J2000();
    elements.mu = MU_EARTH;

    Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    double initial_energy = initial.kinetic_energy() - MU_EARTH / initial.position().norm();

    auto final = prop.propagate(initial, 3600.0);

    double final_energy = final.kinetic_energy() - MU_EARTH / final.position().norm();

    EXPECT_NEAR(initial_energy, final_energy, 1.0);
}

TEST(PropagatorTest, AngularMomentumConservation) {
    double a = 8000.0e3;
    double e = 0.3;
    double i = M_PI / 6.0;

    OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.inclination = i;
    elements.true_anomaly = M_PI / 4.0;
    elements.epoch = core::Epoch::J2000();
    elements.mu = MU_EARTH;

    Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    Eigen::Vector3d h_initial = initial.position().cross(initial.velocity());

    auto final = prop.propagate(initial, 1800.0);

    Eigen::Vector3d h_final = final.position().cross(final.velocity());

    EXPECT_NEAR(h_initial.norm(), h_final.norm(), 1e-6);
}

TEST(PropagatorTest, ZeroTimeOfFlight) {
    core::StateVector initial(
        Eigen::Vector3d(7000.0e3, 0.0, 0.0),
        Eigen::Vector3d(0.0, 7.5e3, 0.0),
        core::Epoch::J2000(),
        core::Frame::J2000(),
        core::CentralBodyId::earth
    );

    Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, 0.0);

    EXPECT_EQ(result.position(), initial.position());
    EXPECT_EQ(result.velocity(), initial.velocity());
}

TEST(PropagatorTest, ToEpoch) {
    core::StateVector initial(
        Eigen::Vector3d(7000.0e3, 0.0, 0.0),
        Eigen::Vector3d(0.0, 7.5e3, 0.0),
        core::Epoch::J2000(),
        core::Frame::J2000(),
        core::CentralBodyId::earth
    );

    core::Epoch target(3600.0);

    Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, target);

    EXPECT_EQ(result.epoch(), target);
}