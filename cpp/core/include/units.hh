#pragma once

#include <cmath>

namespace smp::core {

struct Units {
    static constexpr double meter = 1.0;
    static constexpr double second = 1.0;
    static constexpr double kilogram = 1.0;
    static constexpr double radian = 1.0;

    static constexpr double kilometer = 1000.0 * meter;
    static constexpr double megameter = 1e6 * meter;
    static constexpr double gigameter = 1e9 * meter;

    static constexpr double millisecond = 1e-3 * second;
    static constexpr double minute = 60.0 * second;
    static constexpr double hour = 3600.0 * second;
    static constexpr double day = 86400.0 * second;
    static constexpr double year = 365.25 * day;

    static constexpr double degree = M_PI / 180.0;
    static constexpr double arcminute = degree / 60.0;
    static constexpr double arcsecond = degree / 3600.0;

    static constexpr double kilogram_per_meter3 = 1.0;
    static constexpr double newton = kilogram * meter / (second * second);
    static constexpr double pascal = newton / (meter * meter);

    static constexpr double mu_earth = 3.986004418e14 * meter * meter * meter / (second * second);
    static constexpr double mu_sun = 1.32712440018e20 * meter * meter * meter / (second * second);
};

} // namespace smp::core