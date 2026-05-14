#include <pybind11/pybind11.h>
#include <pybind11/eigen.h>
#include "core/state/include/state_vector.hh"

namespace py = pybind11;

PYBIND11_MODULE(spacemissionplanner_native, m) {
    m.doc() = "Native C++ bindings for SpaceMissionPlanner";

    py::class_<smp::core::StateVector>(m, "StateVector")
        .def(py::init<smp::core::StateVector::Vector3, smp::core::StateVector::Vector3, smp::core::Epoch, smp::core::Frame, smp::core::CentralBodyId>())
        .def("position", py::overload_cast<>(&smp::core::StateVector::position))
        .def("velocity", py::overload_cast<>(&smp::core::StateVector::velocity))
        .def("epoch", py::overload_cast<>(&smp::core::StateVector::epoch));
}
