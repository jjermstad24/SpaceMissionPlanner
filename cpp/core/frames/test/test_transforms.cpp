#include <gtest/gtest.h>

#include "transforms.hh"

using namespace smp::core;

TEST(TransformTest, Identity) {
    auto ident = Transform::Identity();

    Eigen::Vector3d r(100.0, 200.0, 300.0);
    Eigen::Vector3d v(1.0, 2.0, 3.0);
    Epoch e(0.0);

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);
    auto result = ident.apply(sv);

    EXPECT_EQ(result.position(), r);
    EXPECT_EQ(result.velocity(), v);
}

TEST(TransformTest, Translation) {
    Eigen::Vector3d offset(1000.0, 2000.0, 3000.0);
    auto trans = transforms::translation_only(offset);

    Eigen::Vector3d r(100.0, 200.0, 300.0);
    Eigen::Vector3d v(1.0, 2.0, 3.0);
    Epoch e(0.0);

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);
    auto result = trans.apply(sv);

    EXPECT_EQ(result.position(), r + offset);
    EXPECT_EQ(result.velocity(), v);
}

TEST(TransformTest, Rotation) {
    auto rot = transforms::rotation_only(transforms::rotation_matrix_z(M_PI / 2.0));

    Eigen::Vector3d r(100.0, 0.0, 0.0);
    Eigen::Vector3d v(10.0, 0.0, 0.0);
    Epoch e(0.0);

    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);
    auto result = rot.apply(sv);

    EXPECT_NEAR(result.position()[0], 0.0, 1e-10);
    EXPECT_NEAR(result.position()[1], 100.0, 1e-10);
    EXPECT_NEAR(result.velocity()[0], 0.0, 1e-10);
    EXPECT_NEAR(result.velocity()[1], 10.0, 1e-10);
}

TEST(TransformTest, Inverse) {
    Eigen::Vector3d offset(100.0, 200.0, 300.0);
    auto trans = transforms::translation_only(offset);

    auto inv = trans.inverse();

    Eigen::Vector3d r(50.0, 60.0, 70.0);
    Eigen::Vector3d v(1.0, 2.0, 3.0);
    Epoch e(0.0);
    StateVector sv(r, v, e, Frame::J2000(), CentralBodyId::earth);

    auto result = trans.apply(inv.apply(sv));
    EXPECT_EQ(result.position(), sv.position());
    EXPECT_EQ(result.velocity(), sv.velocity());
}

TEST(TransformTest, SkewSymmetric) {
    Eigen::Vector3d v(1.0, 2.0, 3.0);
    auto skew = Transform::skew_symmetric(v);

    Eigen::Matrix3d expected;
    expected << 0.0, -3.0,  2.0,
                3.0,  0.0, -1.0,
               -2.0,  1.0,  0.0;

    EXPECT_EQ(skew, expected);
}

TEST(TransformTest, EulerAngles) {
    double raan = M_PI / 4.0;
    double inc = M_PI / 6.0;
    double aop = M_PI / 3.0;

    auto trans = transforms::from_eulerAngles(raan, inc, aop);

    EXPECT_TRUE(trans.rotation().isUnitary());
}

TEST(TransformTest, EarthFixedJ2000) {
    Epoch e(0.0);
    auto j2000_to_ef = transforms::j2000_to_earth_fixed(e);
    auto ef_to_j2000 = transforms::earth_fixed_to_j2000(e);

    auto combined = j2000_to_ef * ef_to_j2000;
    auto identity = Transform::Identity();

    auto rot_diff = combined.rotation() - identity.rotation();
    EXPECT_TRUE(rot_diff.norm() < 1e-10);
}