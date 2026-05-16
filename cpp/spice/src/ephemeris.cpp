#include "ephemeris.hh"

#include <cstring>
#include <iostream>

extern "C" {
extern int erract_(char *op, char *action, int op_len, int action_len);
}

namespace smp::spice {

namespace {

constexpr int MAX_MSG_LEN = 1000;

std::string to_string_trimmed(char* c_str) {
    if (!c_str) return "";
    char* p = c_str;
    while (*p) p++;
    return std::string(c_str, p - c_str);
}

}

Ephemeris::Ephemeris() {
    SpiceInt init_flags = 0;
}

Ephemeris::~Ephemeris() {
    unload_all();
}

void Ephemeris::load_kernel(const std::string& path) {
    furnsh_c(path.c_str());
    check_error();
}

void Ephemeris::unload_kernel(const std::string& path) {
    unload_c(path.c_str());
    check_error();
}

void Ephemeris::unload_all() {
    kclear_c();
}

void Ephemeris::clear_errors() {
    failed_c();
    reset_c();
}

bool Ephemeris::has_error() const {
    return failed_c();
}

std::string Ephemeris::get_error() const {
    if (!has_error()) return "";
    
    char msg[MAX_MSG_LEN];
    getmsg_c("LONG", MAX_MSG_LEN, msg);
    return to_string_trimmed(msg);
}

void Ephemeris::check_error() const {
    if (has_error()) {
        throw SpiceError(get_error());
    }
}

std::string Ephemeris::frame_to_inertial(const std::string& frame) const {
    if (frame == "J2000" || frame == "ICRF") return frame;
    if (frame == "TOD") return "TOD";
    return frame;
}

std::optional<core::StateVector> Ephemeris::get_state(
    const std::string& target,
    const std::string& observer,
    const core::Epoch& epoch,
    const std::string& frame,
    const std::string& abcorr) const {

    SpiceDouble state[6];
    SpiceDouble lt;

    SpiceDouble et = epoch.seconds_since_j2000();

    spkezr_c(target.c_str(), et, frame.c_str(), abcorr.c_str(), 
             observer.c_str(), state, &lt);

    if (failed_c()) {
        return std::nullopt;
    }

    core::StateVector::Vector3 position(state[0], state[1], state[2]);
    core::StateVector::Vector3 velocity(state[3], state[4], state[5]);

    return core::StateVector(
        position, velocity, epoch,
        core::Frame::J2000(),
        core::CentralBodyId::undefined
    );
}

std::vector<core::StateVector> Ephemeris::get_states(
    const std::string& target,
    const std::string& observer,
    const std::vector<core::Epoch>& epochs,
    const std::string& frame,
    const std::string& abcorr) const {

    std::vector<core::StateVector> result;
    result.reserve(epochs.size());

    for (const auto& epoch : epochs) {
        auto state = get_state(target, observer, epoch, frame, abcorr);
        if (state) {
            result.push_back(*state);
        }
    }

    return result;
}

std::array<double, 3> Ephemeris::get_position(
    const std::string& target,
    const std::string& observer,
    const core::Epoch& epoch,
    const std::string& frame,
    const std::string& abcorr) const {

    SpiceDouble position[3];
    SpiceDouble lt;

    SpiceDouble et = epoch.seconds_since_j2000();

    spkpos_c(target.c_str(), et, frame.c_str(), abcorr.c_str(),
             observer.c_str(), position, &lt);

    check_error();

    return {position[0], position[1], position[2]};
}

double Ephemeris::get_light_time(
    const std::string& target,
    const std::string& observer,
    const core::Epoch& epoch,
    const std::string& abcorr) const {

    SpiceDouble lt;
    SpiceDouble et = epoch.seconds_since_j2000();

    spkezr_c(target.c_str(), et, "J2000", abcorr.c_str(),
             observer.c_str(), NULL, &lt);

    check_error();

    return lt;
}

int Ephemeris::get_body_id(const std::string& name) const {
    SpiceInt id;
    SpiceBoolean found;
    bodn2c_c(name.c_str(), &id, &found);
    if (!found) {
        return 0;
    }
    return static_cast<int>(id);
}

std::string Ephemeris::get_body_name(int id) const {
    SpiceChar name[32];
    SpiceInt id_in = static_cast<SpiceInt>(id);
    SpiceBoolean found;
    bodc2n_c(id_in, 32, name, &found);
    if (!found) {
        return "";
    }
    return to_string_trimmed(name);
}

std::vector<std::string> Ephemeris::solar_system_bodies() {
    return {"Sun", "Mercury", "Venus", "Earth", "Moon", 
            "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"};
}

std::vector<int> Ephemeris::solar_system_naif_ids() {
    return {10, 199, 299, 399, 301, 499, 599, 699, 799, 899, 999};
}

} // namespace smp::spice