#include <gtest/gtest.h>

#include "frame.hh"

using namespace smp::core;

TEST(FrameTest, DefaultConstruct) {
    Frame f;
    EXPECT_EQ(f.id(), FrameId::undefined);
    EXPECT_EQ(f.type(), FrameType::inertial);
}

TEST(FrameTest, J2000Construct) {
    Frame f = Frame::J2000();
    EXPECT_EQ(f.id(), FrameId::j2000);
    EXPECT_TRUE(f.is_inertial());
    EXPECT_EQ(f.name(), "J2000");
}

TEST(FrameTest, EarthFixed) {
    Frame f = Frame::EarthFixed();
    EXPECT_EQ(f.id(), FrameId::earth_fixed);
    EXPECT_TRUE(f.is_fixed());
    EXPECT_EQ(f.central_body(), 399);
}

TEST(FrameTest, Comparison) {
    Frame f1 = Frame::J2000();
    Frame f2 = Frame::J2000();
    Frame f3 = Frame::EarthFixed();

    EXPECT_TRUE(f1 == f2);
    EXPECT_FALSE(f1 == f3);
    EXPECT_TRUE(f1 != f3);
}

TEST(FrameTest, StaticFactories) {
    auto f1 = Frame::J2000();
    auto f2 = Frame::TOD();
    auto f3 = Frame::MoonFixed();
    auto f4 = Frame::Undefined();

    EXPECT_NE(f1, f2);
    EXPECT_NE(f2, f3);
    EXPECT_EQ(f4.id(), FrameId::undefined);
}