#include <gtest/gtest.h>

#include "propagator.hh"
#include "core.hh"

#include <Eigen/Geometry>

const double MU_EARTH = 3.986004418e14;
const double EARTH_RADIUS = 6371.0e3;

TEST(PropagatorTest, CircularOrbitPropagate) {
    double r = EARTH_RADIUS + 400.0e3;
    double v = std::sqrt(MU_EARTH / r);
    double period = 2.0 * M_PI * std::sqrt(std::pow(r, 3) / MU_EARTH);

    smp::core::StateVector initial(
        Eigen::Vector3d(r, 0.0, 0.0),
        Eigen::Vector3d(0.0, v, 0.0),
        smp::core::Epoch::J2000(),
        smp::core::Frame::J2000(),
        smp::core::CentralBodyId::earth
    );

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, period / 4.0);

    EXPECT_NEAR(result.position().norm(), r, 1.0);
}

TEST(PropagatorTest, EllipticalOrbitApogee) {
    double a = 10000.0e3;
    double e = 0.5;
    double rp = a * (1.0 - e);
    double ra = a * (1.0 + e);

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.true_anomaly = 0.0;
    elements.epoch = smp::core::Epoch::J2000();
    elements.mu = MU_EARTH;

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    double period = elements.period();
    auto final = prop.propagate(initial, period / 2.0);

    EXPECT_NEAR(final.position().norm(), ra, 100.0);
}

TEST(PropagatorTest, EnergyConservation) {
    double a = 8000.0e3;
    double e = 0.2;

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.true_anomaly = M_PI / 3.0;
    elements.epoch = smp::core::Epoch::J2000();
    elements.mu = MU_EARTH;

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    double initial_energy = initial.kinetic_energy() - MU_EARTH / initial.position().norm();

    auto final = prop.propagate(initial, 3600.0);

    double final_energy = final.kinetic_energy() - MU_EARTH / final.position().norm();

    EXPECT_NEAR(initial_energy, final_energy, 1.0);
}

TEST(PropagatorTest, AngularMomentumConservation) {
    double a = 8000.0e3;
    double e = 0.1;
    double i = 0.0;

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.inclination = i;
    elements.true_anomaly = 0.0;
    elements.epoch = smp::core::Epoch::J2000();
    elements.mu = MU_EARTH;

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto initial = prop.state_from_elements(elements);

    Eigen::Vector3d h_initial = initial.position().cross(initial.velocity());

    auto final = prop.propagate(initial, 1800.0);

    Eigen::Vector3d h_final = final.position().cross(final.velocity());

    double relative_error = std::abs(h_initial.norm() - h_final.norm()) / h_initial.norm();
    EXPECT_LT(relative_error, 1e-10);
}

TEST(PropagatorTest, ZeroTimeOfFlight) {
    double r = 7000.0e3;
    double v_circular = std::sqrt(MU_EARTH / r);

    smp::core::StateVector initial(
        Eigen::Vector3d(r, 0.0, 0.0),
        Eigen::Vector3d(0.0, v_circular, 0.0),
        smp::core::Epoch::J2000(),
        smp::core::Frame::J2000(),
        smp::core::CentralBodyId::earth
    );

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, 0.0);

    EXPECT_NEAR(result.position().x(), initial.position().x(), 1.0);
    EXPECT_NEAR(result.position().y(), initial.position().y(), 1.0);
    EXPECT_NEAR(result.velocity().x(), initial.velocity().x(), 1.0);
    EXPECT_NEAR(result.velocity().y(), initial.velocity().y(), 1.0);
}

TEST(PropagatorTest, ToEpoch) {
    smp::core::StateVector initial(
        Eigen::Vector3d(7000.0e3, 0.0, 0.0),
        Eigen::Vector3d(0.0, 7.5e3, 0.0),
        smp::core::Epoch::J2000(),
        smp::core::Frame::J2000(),
        smp::core::CentralBodyId::earth
    );

    smp::core::Epoch target(3600.0);

    smp::astro::two_body::Propagator prop(MU_EARTH);
    auto result = prop.propagate(initial, target);

    EXPECT_EQ(result.epoch(), target);
}