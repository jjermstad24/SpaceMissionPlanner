#include <gtest/gtest.h>

#include "epoch.hh"

using namespace smp::core;

TEST(EpochTest, DefaultConstruct) {
    Epoch e;
    EXPECT_DOUBLE_EQ(e.seconds_since_j2000(), 0.0);
}

TEST(EpochTest, J2000Construct) {
    Epoch e = Epoch::J2000();
    EXPECT_DOUBLE_EQ(e.seconds_since_j2000(), 0.0);
    EXPECT_DOUBLE_EQ(e.jd(), 2451545.0);
}

TEST(EpochTest, ExplicitSeconds) {
    Epoch e(86400.0);
    EXPECT_DOUBLE_EQ(e.seconds_since_j2000(), 86400.0);
    EXPECT_DOUBLE_EQ(e.days_since_j2000(), 1.0);
}

TEST(EpochTest, Comparison) {
    Epoch e1(100.0);
    Epoch e2(200.0);
    Epoch e3(100.0);

    EXPECT_TRUE(e1 < e2);
    EXPECT_TRUE(e2 > e1);
    EXPECT_TRUE(e1 == e3);
    EXPECT_FALSE(e1 == e2);
}

TEST(EpochTest, Arithmetic) {
    Epoch e1(100.0);
    Epoch e2 = e1 + 50.0;
    EXPECT_DOUBLE_EQ(e2.seconds_since_j2000(), 150.0);

    Epoch e3 = e2 - 50.0;
    EXPECT_DOUBLE_EQ(e3.seconds_since_j2000(), 100.0);

    double diff = e2 - e1;
    EXPECT_DOUBLE_EQ(diff, 50.0);
}