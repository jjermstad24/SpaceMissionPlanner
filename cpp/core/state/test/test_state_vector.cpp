#include <gtest/gtest.h>

#include "state_vector.hh"

using namespace smp::core;

TEST(StateVectorTest, DefaultConstruct) {
    StateVector sv;
    EXPECT_TRUE(sv.position().isZero());
    EXPECT_TRUE(sv.velocity().isZero());
}

TEST(StateVectorTest, ConstructFromVectors) {
    Eigen::Vector3d r(1000.0, 2000.0, 3000.0);
    Eigen::Vector3d v(1.0, 2.0, 3.0);
    Epoch e(0.0);
    Frame f = Frame::J2000();

    StateVector sv(r, v, e, f, CentralBodyId::earth);

    EXPECT_EQ(sv.position(), r);
    EXPECT_EQ(sv.velocity(), v);
    EXPECT_EQ(sv.central_body(), CentralBodyId::earth);
}

TEST(StateVectorTest, StateAccessor) {
    Eigen::Vector3d r(1000.0, 0.0, 0.0);
    Eigen::Vector3d v(0.0, 7.0, 0.0);
    Epoch e(0.0);

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);

    auto state = sv.state();
    EXPECT_EQ(state.head<3>(), r);
    EXPECT_EQ(state.tail<3>(), v);
}

TEST(StateVectorTest, Speed) {
    Eigen::Vector3d r(1000.0, 0.0, 0.0);
    Eigen::Vector3d v(3.0, 4.0, 0.0);
    Epoch e(0.0);

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);
    EXPECT_DOUBLE_EQ(sv.speed(), 5.0);
}

TEST(StateVectorTest, Arithmetic) {
    Eigen::Vector3d r1(1000.0, 0.0, 0.0);
    Eigen::Vector3d v1(1.0, 0.0, 0.0);
    Eigen::Vector3d r2(500.0, 0.0, 0.0);
    Eigen::Vector3d v2(0.5, 0.0, 0.0);
    Epoch e(0.0);
    Frame f = Frame::J2000();

    StateVector sv1(r1, v1, e, f, CentralBodyId::earth);
    StateVector sv2(r2, v2, e, f, CentralBodyId::earth);

    auto sum = sv1 + sv2;
    EXPECT_EQ(sum.position()[0], 1500.0);
    EXPECT_EQ(sum.velocity()[0], 1.5);

    auto diff = sv1 - sv2;
    EXPECT_EQ(diff.position()[0], 500.0);
    EXPECT_EQ(diff.velocity()[0], 0.5);

    auto scaled = sv1 * 2.0;
    EXPECT_EQ(scaled.position()[0], 2000.0);
    EXPECT_EQ(scaled.velocity()[0], 2.0);

    auto scaled2 = 0.5 * sv1;
    EXPECT_EQ(scaled2.position()[0], 500.0);
}

TEST(StateVectorTest, Altitude) {
    Eigen::Vector3d r(6578.0e3, 0.0, 0.0);
    Eigen::Vector3d v(0.0, 7.0e3, 0.0);
    Epoch e(0.0);
    const double earth_radius = 6371.0e3;

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);
    EXPECT_DOUBLE_EQ(sv.altitude(earth_radius), 207.0e3);
}