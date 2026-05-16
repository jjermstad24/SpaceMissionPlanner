#pragma once

#include <string>
#include <vector>
#include <optional>
#include <array>
#include <memory>
#include <stdexcept>

#include "core/time/include/epoch.hh"
#include "core/frames/include/frame.hh"
#include "core/state/include/state_vector.hh"

extern "C" {
#include "SpiceZfc.h"
#include "SpiceCel.h"
}

namespace smp::spice {

class SpiceError : public std::runtime_error {
public:
    explicit SpiceError(const std::string& msg) : std::runtime_error(msg) {}
};

struct KernelSet {
    std::vector<std::string> lsk_files;
    std::vector<std::string> spk_files;
    std::vector<std::string> pck_files;
    std::vector<std::string> text_kernels;
};

class Ephemeris {
public:
    Ephemeris();
    ~Ephemeris();

    void load_kernel(const std::string& path);
    void unload_kernel(const std::string& path);
    void unload_all();
    void clear_errors();
    void set_error_action(const std::string& action);

    bool has_error() const;
    std::string get_error() const;

    std::optional<core::StateVector> get_state(
        const std::string& target,
        const std::string& observer,
        const core::Epoch& epoch,
        const std::string& frame = "J2000",
        const std::string& abcorr = "NONE"
    ) const;

    std::vector<core::StateVector> get_states(
        const std::string& target,
        const std::string& observer,
        const std::vector<core::Epoch>& epochs,
        const std::string& frame = "J2000",
        const std::string& abcorr = "NONE"
    ) const;

    std::array<double, 3> get_position(
        const std::string& target,
        const std::string& observer,
        const core::Epoch& epoch,
        const std::string& frame = "J2000",
        const std::string& abcorr = "NONE"
    ) const;

    double get_light_time(
        const std::string& target,
        const std::string& observer,
        const core::Epoch& epoch,
        const std::string& abcorr = "NONE"
    ) const;

    int get_body_id(const std::string& name) const;
    std::string get_body_name(int id) const;

    static std::vector<std::string> solar_system_bodies();
    static std::vector<int> solar_system_naif_ids();

private:
    void check_error() const;
    std::string frame_to_inertial(const std::string& frame) const;
};

using EphemerisPtr = std::shared_ptr<Ephemeris>;

} // namespace smp::spice