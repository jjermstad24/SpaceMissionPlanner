#pragma once

#include <chrono>
#include <compare>

namespace smp::core {

class Epoch {
public:
    static constexpr double J2000_TAI = 0.0;
    static constexpr double J2000_TDB = 0.0;
    static constexpr double SECONDS_PER_DAY = 86400.0;
    static constexpr double JD_J2000 = 2451545.0;

    explicit Epoch() = default;
    explicit Epoch(double seconds_since_j2000) : seconds_since_j2000_(seconds_since_j2000) {}

    static Epoch J2000() { return Epoch(0.0); }

    static Epoch Now() {
        auto now = std::chrono::system_clock::now();
        auto epoch_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
            now.time_since_epoch()).count();
        return Epoch(epoch_ms / 1000.0 - 946684800.0);
    }

    [[nodiscard]] double seconds_since_j2000() const { return seconds_since_j2000_; }
    [[nodiscard]] double days_since_j2000() const { return seconds_since_j2000_ / SECONDS_PER_DAY; }
    [[nodiscard]] double jd() const { return JD_J2000 + days_since_j2000(); }

    [[nodiscard]] std::partial_ordering operator<=>(const Epoch& other) const {
        return seconds_since_j2000_ <=> other.seconds_since_j2000_;
    }

    [[nodiscard]] bool operator==(const Epoch& other) const {
        return seconds_since_j2000_ == other.seconds_since_j2000_;
    }

    Epoch operator+(double dt) const { return Epoch(seconds_since_j2000_ + dt); }
    Epoch operator-(double dt) const { return Epoch(seconds_since_j2000_ - dt); }
    double operator-(const Epoch& other) const {
        return seconds_since_j2000_ - other.seconds_since_j2000_;
    }

private:
    double seconds_since_j2000_ = 0.0;
};

} // namespace smp::core