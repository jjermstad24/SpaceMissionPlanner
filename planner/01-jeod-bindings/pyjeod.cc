/**
 * pyjeod.cc
 * NASA JEOD Python bindings via pybind11.
 */

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/eigen.h>
#include <iostream>
#include <string>
#include <vector>
#include <cstdarg>

// ── JEOD Core Headers ────────────────────────────────────────────────────────
#include "utils/ref_frames/include/ref_frame_interface.hh"
#include "utils/ref_frames/include/ref_frame.hh"
#include "utils/message/include/message_handler.hh"
#include "utils/sim_interface/include/simulation_interface.hh"
#include "utils/sim_interface/include/memory_interface.hh"
#include "utils/memory/include/memory_manager.hh"
#include "environment/time/include/time_manager.hh"
#include "environment/time/include/time_manager_init.hh"
#include "environment/time/include/time_utc.hh"
#include "environment/time/include/time_tdb.hh"
#include "environment/time/include/time_tai.hh"
#include "environment/time/include/time_tt.hh"
#include "environment/time/include/time_converter_tai_utc.hh"
#include "environment/time/include/time_converter_tai_tdb.hh"
#include "environment/time/include/time_converter_tai_tt.hh"
#include "environment/ephemerides/ephem_manager/include/ephem_manager.hh"
#include "environment/ephemerides/ephem_interface/include/ephem_interface.hh"
#include "environment/ephemerides/ephem_interface/include/ephem_ref_frame.hh"
#include "environment/ephemerides/ephem_item/include/ephem_item.hh"
#include "environment/ephemerides/ephem_item/include/ephem_point.hh"
#include "environment/spice/include/spice_ephem.hh"
#include "environment/gravity/include/gravity_manager.hh"
#include "environment/gravity/include/gravity_source.hh"
#include "environment/gravity/include/gravity_interaction.hh"
#include "dynamics/dyn_manager/include/dyn_manager.hh"
#include "dynamics/dyn_manager/include/dyn_manager_init.hh"
#include "dynamics/dyn_body/include/dyn_body.hh"
#include "utils/orbital_elements/include/orbital_elements.hh"

namespace jeod {

class StandaloneMessageHandler : public MessageHandler {
public:
    StandaloneMessageHandler() : MessageHandler() {}
    void process_message(int severity, const char *prefix, const char *file, unsigned int line,
                         const char *msg_code, const char *format, va_list args) const override {
        std::fprintf(stderr, "[JEOD %s] ", prefix);
        std::vfprintf(stderr, format, args);
        std::fprintf(stderr, " (code: %s) at %s:%u\n", msg_code, file, line);
        if (severity <= -1) std::exit(1);
    }
};

class StandaloneMemoryInterface : public JeodMemoryInterface {
public:
    const JEOD_ATTRIBUTES_POINTER_TYPE find_attributes(const std::string &) const override { return nullptr; }
    const JEOD_ATTRIBUTES_POINTER_TYPE find_attributes(const std::type_info &) const override { return nullptr; }
    JEOD_ATTRIBUTES_TYPE primitive_attributes(const std::type_info &) const override { return 0; }
    JEOD_ATTRIBUTES_TYPE pointer_attributes(const JEOD_ATTRIBUTES_TYPE &) const override { return 0; }
    JEOD_ATTRIBUTES_TYPE void_pointer_attributes() const override { return 0; }
    JEOD_ATTRIBUTES_TYPE structure_attributes(const JEOD_ATTRIBUTES_POINTER_TYPE, std::size_t) const override { return 0; }
    bool register_allocation(const void *, const JeodMemoryItem &, const JeodMemoryTypeDescriptor &, const char *, unsigned int) override { return true; }
    void deregister_allocation(const void *, const JeodMemoryItem &, const JeodMemoryTypeDescriptor &, const char *, unsigned int) override {}
    void register_container(const void *, const JeodMemoryTypeDescriptor &, const std::string &, JeodCheckpointable &) override {}
    void deregister_container(const void *, const JeodMemoryTypeDescriptor &, const std::string &, JeodCheckpointable &) override {}
    bool is_checkpoint_restart_supported() const override { return false; }
    const std::string get_name_at_address(const void *, const JeodMemoryTypeDescriptor *) const override { return ""; }
    void * get_address_at_name(const std::string &) const override { return nullptr; }
};

class StandaloneSimulationInterface : public JeodSimulationInterface {
    StandaloneMessageHandler msg_handler;
    StandaloneMemoryInterface mem_interface;
    JeodMemoryManager memory_manager;
public:
    StandaloneSimulationInterface() 
        : JeodSimulationInterface(), 
          memory_manager(static_cast<JeodMemoryInterface &>(mem_interface)) {}

    JeodIntegratorInterface * create_integrator_internal() override { return nullptr; }
    double get_job_cycle_internal() override { return 0.0; }
    JeodMemoryInterface & get_memory_interface_internal() override { return mem_interface; }
    SectionedInputStream get_checkpoint_reader_internal(const std::string &) override { return {}; }
    SectionedOutputStream get_checkpoint_writer_internal(const std::string &) override { return {}; }
};

} // namespace jeod

// Ensure JEOD is initialized immediately when the shared library is loaded.
extern "C" void __attribute__((constructor)) init_pyjeod_standalone() {
    static jeod::StandaloneSimulationInterface * global_sim = new jeod::StandaloneSimulationInterface();
    (void)global_sim;
}

namespace py = pybind11;

PYBIND11_MODULE(pyjeod, m) {
    m.doc() = "NASA JEOD Python Bindings";

    // 1. Register base interfaces
    py::class_<jeod::RefFrameOwner>(m, "RefFrameOwner");
    py::class_<jeod::EphemerisInterface>(m, "EphemerisInterface");

    // 2. Register core utilities
    py::class_<jeod::RefFrameState>(m, "RefFrameState")
        .def(py::init<>())
        .def("get_pos", [](const jeod::RefFrameState & s) {
            return std::vector<double>{s.trans.position[0], s.trans.position[1], s.trans.position[2]};
        })
        .def("get_vel", [](const jeod::RefFrameState & s) {
            return std::vector<double>{s.trans.velocity[0], s.trans.velocity[1], s.trans.velocity[2]};
        });

    py::class_<jeod::RefFrame>(m, "RefFrame")
        .def("get_name", &jeod::RefFrame::get_name)
        .def("get_position", [](const jeod::RefFrame & self) {
            return std::vector<double>{self.state.trans.position[0], self.state.trans.position[1], self.state.trans.position[2]};
        })
        .def("get_velocity", [](const jeod::RefFrame & self) {
            return std::vector<double>{self.state.trans.velocity[0], self.state.trans.velocity[1], self.state.trans.velocity[2]};
        });

    py::class_<jeod::BodyRefFrame, jeod::RefFrame>(m, "BodyRefFrame");

    // 3. Register Time
    py::class_<jeod::TimeManagerInit>(m, "TimeManagerInit").def(py::init<>());
    py::class_<jeod::TimeManager>(m, "TimeManager")
        .def(py::init<>())
        .def("initialize", [](jeod::TimeManager & self, jeod::TimeManagerInit & init) { self.initialize(&init); })
        .def("register_time_named", &jeod::TimeManager::register_time_named)
        .def("register_converter", &jeod::TimeManager::register_converter, py::arg("converter_ref"), py::arg("name_a") = "", py::arg("name_b") = "")
        .def_readonly("simtime", &jeod::TimeManager::simtime);

    // Helper for standard time setup (avoids needing to expose individual TimeScale classes unless requested)
    m.def("setup_default_time", [](jeod::TimeManager & tm) {
        static jeod::TimeUTC utc;
        static jeod::TimeTDB tdb;
        static jeod::TimeTAI tai;
        static jeod::TimeTT tt;
        
        static jeod::TimeConverter_TAI_UTC tai2utc;
        static jeod::TimeConverter_TAI_TDB tai2tdb;
        static jeod::TimeConverter_TAI_TT tai2tt;

        tm.register_time_named(utc, "UTC");
        tm.register_time_named(tdb, "TDB");
        tm.register_time_named(tai, "TAI");
        tm.register_time_named(tt, "TT");
        
        tm.register_converter(tai2utc, "TAI", "UTC");
        tm.register_converter(tai2tdb, "TAI", "TDB");
        tm.register_converter(tai2tt, "TAI", "TT");
    });

    // 4. Register Ephemerides
    py::class_<jeod::EphemerisItem, jeod::RefFrameOwner>(m, "EphemerisItem")
        .def("get_name", &jeod::EphemerisItem::get_name)
        .def("get_target_frame", &jeod::EphemerisItem::get_target_frame, py::return_value_policy::reference_internal);

    py::class_<jeod::EphemerisPoint, jeod::EphemerisItem>(m, "EphemerisPoint");
    py::class_<jeod::EphemerisRefFrame, jeod::RefFrame>(m, "EphemerisRefFrame");

    py::class_<jeod::EphemeridesManager>(m, "EphemeridesManager")
        .def(py::init<>())
        .def("initialize_ephemerides", &jeod::EphemeridesManager::initialize_ephemerides)
        .def("activate_ephemerides", &jeod::EphemeridesManager::activate_ephemerides)
        .def("update_ephemerides", &jeod::EphemeridesManager::update_ephemerides)
        .def("find_integ_frame", &jeod::EphemeridesManager::find_integ_frame, py::return_value_policy::reference_internal)
        .def("find_ephem_point", &jeod::EphemeridesManager::find_ephem_point, py::return_value_policy::reference_internal);

    py::class_<jeod::SpiceEphemeris, jeod::EphemerisInterface>(m, "SpiceEphemeris")
        .def(py::init<>())
        .def_readwrite("metakernel_filename", &jeod::SpiceEphemeris::metakernel_filename)
        .def("initialize_model", &jeod::SpiceEphemeris::initialize_model)
        .def("add_planet_name", &jeod::SpiceEphemeris::add_planet_name)
        .def("add_orientation", &jeod::SpiceEphemeris::add_orientation);

    // 5. Register Gravity
    py::class_<jeod::GravitySource>(m, "GravitySource")
        .def(py::init<>())
        .def_readwrite("name", &jeod::GravitySource::name)
        .def_readwrite("mu", &jeod::GravitySource::mu);

    py::class_<jeod::GravityInteraction>(m, "GravityInteraction")
        .def(py::init<>())
        .def("get_grav_accel", [](const jeod::GravityInteraction & self) {
            return std::vector<double>{self.grav_accel[0], self.grav_accel[1], self.grav_accel[2]};
        });

    py::class_<jeod::GravityManager>(m, "GravityManager")
        .def(py::init<>())
        .def("add_grav_source", [](jeod::GravityManager & self, jeod::GravitySource & src) { self.add_grav_source(src); })
        .def("initialize_model", [](jeod::GravityManager & self, jeod::DynManager & mgr) { self.initialize_model(mgr); })
        .def("gravitation", [](jeod::GravityManager & self, const jeod::RefFrame & point, jeod::GravityInteraction & grav) { self.gravitation(point, grav); });

    // 6. Register Dynamics
    py::class_<jeod::DynManagerInit>(m, "DynManagerInit")
        .def(py::init<>())
        .def_readwrite("mode", &jeod::DynManagerInit::mode);

    py::enum_<jeod::DynManagerInit::EphemerisMode>(m, "EphemerisMode")
        .value("EmptySpace", jeod::DynManagerInit::EphemerisMode_EmptySpace)
        .value("SinglePlanet", jeod::DynManagerInit::EphemerisMode_SinglePlanet)
        .value("Ephemerides", jeod::DynManagerInit::EphemerisMode_Ephemerides)
        .export_values();

    py::class_<jeod::DynManager>(m, "DynManager")
        .def(py::init<>())
        .def("initialize_model", [](jeod::DynManager & self, jeod::DynManagerInit & init, jeod::TimeManager & tm) { self.initialize_model(init, tm); })
        .def("initialize_simulation", &jeod::DynManager::initialize_simulation)
        .def("add_dyn_body", [](jeod::DynManager & self, jeod::DynBody & body) { self.add_dyn_body(body); })
        .def("find_dyn_body", &jeod::DynManager::find_dyn_body, py::return_value_policy::reference_internal)
        .def("compute_derivatives", &jeod::DynManager::compute_derivatives)
        .def("get_timestamp", &jeod::DynManager::timestamp)
        .def("integrate", &jeod::DynManager::integrate);

    py::class_<jeod::DynBody>(m, "DynBody")
        .def(py::init<>())
        .def("set_name", &jeod::DynBody::set_name)
        .def("get_name", [](const jeod::DynBody & self) { return self.name.get_name(); })
        .def("set_mass", [](jeod::DynBody & self, double m) { self.mass.core_properties.mass = m; self.mass.composite_properties.mass = m; })
        .def("set_position", [](jeod::DynBody & self, const std::vector<double> & p, jeod::BodyRefFrame & f) { self.set_position(p.data(), f); })
        .def("set_velocity", [](jeod::DynBody & self, const std::vector<double> & v, jeod::BodyRefFrame & f) { self.set_velocity(v.data(), f); })
        .def_readonly("structure", &jeod::DynBody::structure)
        .def("get_position", [](jeod::DynBody & self) -> std::vector<double> {
            const auto * f = self.get_integ_frame();
            return f ? std::vector<double>{f->state.trans.position[0], f->state.trans.position[1], f->state.trans.position[2]} : std::vector<double>{0,0,0};
        })
        .def("get_velocity", [](jeod::DynBody & self) -> std::vector<double> {
            const auto * f = self.get_integ_frame();
            return f ? std::vector<double>{f->state.trans.velocity[0], f->state.trans.velocity[1], f->state.trans.velocity[2]} : std::vector<double>{0,0,0};
        });

    py::class_<jeod::OrbitalElements>(m, "OrbitalElements")
        .def(py::init<>())
        .def("from_cartesian", [](jeod::OrbitalElements & self, double mu, const std::vector<double> & p, const std::vector<double> & v) { return self.from_cartesian(mu, p.data(), v.data()); })
        .def_readwrite("semi_major_axis", &jeod::OrbitalElements::semi_major_axis);

    m.def("get_earth_mu", []() { return 3.986004418e14; });
}