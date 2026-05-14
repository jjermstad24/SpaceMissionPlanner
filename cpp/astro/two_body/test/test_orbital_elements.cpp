#include <gtest/gtest.h>

#include "orbital_elements.hh"
#include "core.hh"

#include <Eigen/Geometry>

const double MU_EARTH = 3.986004418e14;

TEST(OrbitalElementsTest, CircularOrbit) {
    double r = 7000.0e3;
    double v = std::sqrt(MU_EARTH / r);

    smp::core::StateVector state(
        Eigen::Vector3d(r, 0.0, 0.0),
        Eigen::Vector3d(0.0, v, 0.0),
        smp::core::Epoch::J2000(),
        smp::core::Frame::J2000(),
        smp::core::CentralBodyId::earth
    );

    auto elements = smp::astro::two_body::state_to_orbital_elements(state, MU_EARTH);

    EXPECT_NEAR(elements.semi_major_axis, r, 1.0);
    EXPECT_NEAR(elements.eccentricity, 0.0, 1e-10);
    EXPECT_NEAR(elements.inclination, 0.0, 1e-10);
}

TEST(OrbitalElementsTest, EllipticalOrbit) {
    double a = 10000.0e3;
    double e = 0.3;
    double i = M_PI / 4.0;
    double Omega = M_PI / 6.0;
    double w = M_PI / 3.0;
    double nu = M_PI / 2.0;

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.inclination = i;
    elements.raan = Omega;
    elements.arg_of_perigee = w;
    elements.true_anomaly = nu;
    elements.epoch = smp::core::Epoch::J2000();
    elements.mu = MU_EARTH;

    auto state = smp::astro::two_body::orbital_elements_to_state(elements);

    auto elements_back = smp::astro::two_body::state_to_orbital_elements(state, MU_EARTH);

    EXPECT_NEAR(elements_back.semi_major_axis, a, 1.0);
    EXPECT_NEAR(elements_back.eccentricity, e, 1e-8);
    EXPECT_NEAR(elements_back.inclination, i, 1e-10);
    EXPECT_NEAR(elements_back.raan, Omega, 1e-10);
    EXPECT_NEAR(elements_back.arg_of_perigee, w, 1e-10);
}

TEST(OrbitalElementsTest, PeriodCalculation) {
    double a = 6778.0e3;

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.mu = MU_EARTH;

    double period = elements.period();

    EXPECT_NEAR(period, 2.0 * M_PI * std::sqrt(std::pow(a, 3) / MU_EARTH), 1.0);
    EXPECT_NEAR(period, 5553.0, 10.0);
}

TEST(OrbitalElementsTest, ApogeePerigee) {
    double a = 10000.0e3;
    double e = 0.5;

    smp::astro::two_body::OrbitalElements elements;
    elements.semi_major_axis = a;
    elements.eccentricity = e;
    elements.mu = MU_EARTH;

    EXPECT_NEAR(elements.apogee(), a * (1.0 + e), 1.0);
    EXPECT_NEAR(elements.perigee(), a * (1.0 - e), 1.0);
}

TEST(KeplerSolverTest, CircularCase) {
    double mean_anomaly = M_PI / 2.0;
    double e = 0.0;

    double e_anom = smp::astro::two_body::solve_kepler_equation(mean_anomaly, e);

    EXPECT_NEAR(e_anom, mean_anomaly, 1e-12);
}

TEST(KeplerSolverTest, EllipticalCase) {
    double mean_anomaly = M_PI / 2.0;
    double e = 0.5;

    double e_anom = smp::astro::two_body::solve_kepler_equation(mean_anomaly, e);

    double check = e_anom - e * std::sin(e_anom);
    EXPECT_NEAR(check, mean_anomaly, 1e-12);
}