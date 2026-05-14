#pragma once

#include <string>
#include <string_view>

namespace smp::core {

enum class FrameType {
    inertial,
    fixed,
    body_fixed
};

enum class FrameId : int {
    undefined = 0,
    inertial = 1,
    j2000 = 2,
    tod = 3,
    pseudo_earth_fixed = 4,
    earth_fixed = 5,
    moon_fixed = 6,
    sun_fixed = 7
};

class Frame {
public:
    Frame() = default;
    explicit Frame(FrameId id, FrameType type = FrameType::inertial,
                  int body = 0, std::string_view name = "")
        : id_(id), type_(type), central_body_(body), name_(name) {}

    [[nodiscard]] FrameId id() const { return id_; }
    [[nodiscard]] FrameType type() const { return type_; }
    [[nodiscard]] int central_body() const { return central_body_; }
    [[nodiscard]] std::string_view name() const { return name_; }

    [[nodiscard]] bool is_inertial() const { return type_ == FrameType::inertial; }
    [[nodiscard]] bool is_fixed() const { return type_ == FrameType::fixed; }
    [[nodiscard]] bool is_body_fixed() const { return type_ == FrameType::body_fixed; }

    [[nodiscard]] bool operator==(const Frame& other) const {
        return id_ == other.id_ && type_ == other.type_ && central_body_ == other.central_body_;
    }

    [[nodiscard]] bool operator!=(const Frame& other) const { return !(*this == other); }

    static Frame Undefined() { return Frame(FrameId::undefined); }
    static Frame J2000() { return Frame(FrameId::j2000, FrameType::inertial, 0, "J2000"); }
    static Frame TOD() { return Frame(FrameId::tod, FrameType::inertial, 0, "TOD"); }
    static Frame EarthFixed() { return Frame(FrameId::earth_fixed, FrameType::fixed, 399, "EarthFixed"); }
    static Frame MoonFixed() { return Frame(FrameId::moon_fixed, FrameType::body_fixed, 301, "MoonFixed"); }

private:
    FrameId id_ = FrameId::undefined;
    FrameType type_ = FrameType::inertial;
    int central_body_ = 0;
    std::string name_;
};

} // namespace smp::core