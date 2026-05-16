#include <pybind11/pybind11.h>
#include <pybind11/eigen.h>
#include <pybind11/stl.h>
#include "core/state/include/state_vector.hh"
#include "core/time/include/epoch.hh"
#include "core/frames/include/frame.hh"
#include "astro/two_body/include/propagator.hh"
#include "mission_graph/include/node.hh"
#include "mission_graph/include/edge.hh"
#include "mission_graph/include/graph.hh"
#include "mission_graph/include/propagator_node.hh"
#include "optimization/include/parameter.hh"
#include "optimization/include/objective.hh"
#include "optimization/include/constraint.hh"
#include "optimization/include/gradient_descent.hh"
#include "spice/include/ephemeris.hh"

#include <memory>

namespace py = pybind11;

namespace smp::mission_graph {

class PythonNodeWrapper : public Node {
public:
    PythonNodeWrapper(std::string name, py::object py_node)
        : Node(std::move(name)), py_node_(std::move(py_node)) {}

    std::unordered_set<OutputId> outputs() const override {
        py::gil_scoped_acquire gil;
        auto result = py_node_.attr("outputs")();
        if (result && !result.is_none()) {
            return py::cast<std::unordered_set<OutputId>>(result);
        }
        return {};
    }

    std::unordered_set<InputId> inputs() const override {
        py::gil_scoped_acquire gil;
        auto result = py_node_.attr("inputs")();
        if (result && !result.is_none()) {
            return py::cast<std::unordered_set<InputId>>(result);
        }
        return {};
    }

    bool compute() override {
        py::gil_scoped_acquire gil;
        return py::cast<bool>(py_node_.attr("compute")());
    }

    void invalidate_output(const OutputId& output) override {
        py::gil_scoped_acquire gil;
        py_node_.attr("invalidate_output")(output);
    }

    std::optional<std::any> get_output(const OutputId& output_id) const override {
        py::gil_scoped_acquire gil;
        auto result = py_node_.attr("get_output")(output_id);
        if (!result || result.is_none()) {
            return std::nullopt;
        }
        return std::any(result.release().ptr());
    }

    void set_input(const InputId& input_id, std::any value) override {
        py::gil_scoped_acquire gil;
        if (value.has_value()) {
            py_node_.attr("set_input")(input_id, py::cast(value));
        } else {
            py_node_.attr("set_input")(input_id, py::none());
        }
    }

    bool dirty() const override {
        py::gil_scoped_acquire gil;
        return py::cast<bool>(py_node_.attr("dirty")());
    }

    void clear_cache() override {
        py::gil_scoped_acquire gil;
        py_node_.attr("clear_cache")();
    }

private:
    py::object py_node_;
};

NodePtr create_node(std::string name, py::object py_node) {
    return std::make_shared<PythonNodeWrapper>(std::move(name), std::move(py_node));
}

EdgePtr create_edge(NodePtr source, Node::OutputId source_output,
                    NodePtr target, Node::InputId target_input) {
    return std::make_shared<Edge>(source, std::move(source_output),
                                   target, std::move(target_input));
}

}

PYBIND11_MODULE(spacemissionplanner_native, m) {
    m.doc() = "Native C++ bindings for SpaceMissionPlanner";

    py::class_<smp::core::Epoch>(m, "Epoch")
        .def(py::init<double>())
        .def_static("J2000", &smp::core::Epoch::J2000)
        .def("seconds_since_j2000", &smp::core::Epoch::seconds_since_j2000)
        .def("days_since_j2000", &smp::core::Epoch::days_since_j2000)
        .def("jd", &smp::core::Epoch::jd);

    py::class_<smp::core::Frame>(m, "Frame")
        .def_static("J2000", &smp::core::Frame::J2000)
        .def_static("TOD", &smp::core::Frame::TOD)
        .def_static("EarthFixed", &smp::core::Frame::EarthFixed)
        .def_static("MoonFixed", &smp::core::Frame::MoonFixed)
        .def_static("Undefined", &smp::core::Frame::Undefined)
        .def("name", &smp::core::Frame::name)
        .def("is_inertial", &smp::core::Frame::is_inertial)
        .def("is_fixed", &smp::core::Frame::is_fixed)
        .def("is_body_fixed", &smp::core::Frame::is_body_fixed);

    py::enum_<smp::core::CentralBodyId>(m, "CentralBodyId")
        .value("undefined", smp::core::CentralBodyId::undefined)
        .value("sun", smp::core::CentralBodyId::sun)
        .value("mercury", smp::core::CentralBodyId::mercury)
        .value("venus", smp::core::CentralBodyId::venus)
        .value("earth", smp::core::CentralBodyId::earth)
        .value("moon", smp::core::CentralBodyId::moon)
        .value("mars", smp::core::CentralBodyId::mars)
        .value("jupiter", smp::core::CentralBodyId::jupiter)
        .value("saturn", smp::core::CentralBodyId::saturn)
        .value("uranus", smp::core::CentralBodyId::uranus)
        .value("neptune", smp::core::CentralBodyId::neptune)
        .value("pluto", smp::core::CentralBodyId::pluto)
        .export_values();

    py::class_<smp::core::StateVector>(m, "StateVector")
        .def(py::init<smp::core::StateVector::Vector3, smp::core::StateVector::Vector3, smp::core::Epoch, smp::core::Frame, smp::core::CentralBodyId>())
        .def("position", py::overload_cast<>(&smp::core::StateVector::position))
        .def("velocity", py::overload_cast<>(&smp::core::StateVector::velocity))
        .def("epoch", py::overload_cast<>(&smp::core::StateVector::epoch))
        .def("frame", py::overload_cast<>(&smp::core::StateVector::frame))
        .def("central_body", &smp::core::StateVector::central_body);

    m.def("propagate_keplerian", &smp::astro::two_body::propagate_keplerian,
          "Propagate a state vector using Keplerian two-body motion");

    py::enum_<smp::mission_graph::NodeStatus>(m, "NodeStatus")
        .value("Pending", smp::mission_graph::NodeStatus::Pending)
        .value("Computing", smp::mission_graph::NodeStatus::Computing)
        .value("Valid", smp::mission_graph::NodeStatus::Valid)
        .value("Invalid", smp::mission_graph::NodeStatus::Invalid)
        .value("Error", smp::mission_graph::NodeStatus::Error);

    py::class_<smp::mission_graph::Node, std::shared_ptr<smp::mission_graph::Node>>(m, "Node")
        .def("name", &smp::mission_graph::Node::name)
        .def("status", &smp::mission_graph::Node::status)
        .def("outputs", &smp::mission_graph::Node::outputs)
        .def("inputs", &smp::mission_graph::Node::inputs)
        .def("compute", &smp::mission_graph::Node::compute)
        .def("invalidate", &smp::mission_graph::Node::invalidate)
        .def("invalidate_output", &smp::mission_graph::Node::invalidate_output)
        .def("dirty", &smp::mission_graph::Node::dirty)
        .def("clear_cache", &smp::mission_graph::Node::clear_cache);

    m.def("create_node", &smp::mission_graph::create_node,
          "Create a mission graph node from a Python object with node methods");

    m.def("create_edge", &smp::mission_graph::create_edge,
          "Create an edge from node outputs to inputs");

    py::class_<smp::mission_graph::Edge, std::shared_ptr<smp::mission_graph::Edge>>(m, "Edge")
        .def(py::init<smp::mission_graph::NodePtr, smp::mission_graph::Node::OutputId,
                     smp::mission_graph::NodePtr, smp::mission_graph::Node::InputId>())
        .def("source", &smp::mission_graph::Edge::source)
        .def("source_output", &smp::mission_graph::Edge::source_output)
        .def("target", &smp::mission_graph::Edge::target)
        .def("target_input", &smp::mission_graph::Edge::target_input)
        .def("is_valid", &smp::mission_graph::Edge::is_valid);

    py::class_<smp::mission_graph::Graph, smp::mission_graph::GraphPtr>(m, "Graph")
        .def(py::init<>())
        .def("add_node", [](smp::mission_graph::Graph& self, std::shared_ptr<smp::mission_graph::Node> node) {
            self.add_node(node);
        })
        .def("add_edge", [](smp::mission_graph::Graph& self, std::shared_ptr<smp::mission_graph::Edge> edge) {
            self.add_edge(edge);
        })
        .def("remove_node", &smp::mission_graph::Graph::remove_node)
        .def("remove_edge", &smp::mission_graph::Graph::remove_edge)
        .def("get_node", &smp::mission_graph::Graph::get_node)
        .def("get_nodes", &smp::mission_graph::Graph::get_nodes)
        .def("get_edges", &smp::mission_graph::Graph::get_edges)
        .def("get_edges_from", &smp::mission_graph::Graph::get_edges_from)
        .def("get_edges_to", &smp::mission_graph::Graph::get_edges_to)
        .def("invalidate_node", &smp::mission_graph::Graph::invalidate_node)
        .def("invalidate_all", &smp::mission_graph::Graph::invalidate_all)
        .def("get_evaluation_order", &smp::mission_graph::Graph::get_evaluation_order)
        .def("has_cycle", &smp::mission_graph::Graph::has_cycle)
        .def("get_downstream_nodes", &smp::mission_graph::Graph::get_downstream_nodes)
        .def("get_upstream_nodes", &smp::mission_graph::Graph::get_upstream_nodes);

    py::class_<smp::mission_graph::PropagatorNode, std::shared_ptr<smp::mission_graph::PropagatorNode>,
               smp::mission_graph::Node>(m, "PropagatorNode")
        .def(py::init<std::string>())
        .def(py::init<std::string, double>())
        .def("set_step_size", &smp::mission_graph::PropagatorNode::set_step_size)
        .def("set_num_steps", &smp::mission_graph::PropagatorNode::set_num_steps)
        .def("set_initial_state", &smp::mission_graph::PropagatorNode::set_initial_state)
        .def("has_initial_state", &smp::mission_graph::PropagatorNode::has_initial_state)
        .def("initial_state", &smp::mission_graph::PropagatorNode::initial_state)
        .def("mu", &smp::mission_graph::PropagatorNode::mu)
        .def("step_size", &smp::mission_graph::PropagatorNode::step_size)
        .def("num_steps", &smp::mission_graph::PropagatorNode::num_steps)
        .def("get_states", &smp::mission_graph::PropagatorNode::get_states)
        .def("get_epochs", &smp::mission_graph::PropagatorNode::get_epochs)
        .def("get_positions", &smp::mission_graph::PropagatorNode::get_positions)
        .def("set_input", &smp::mission_graph::Node::set_input)
        .def("get_output", &smp::mission_graph::Node::get_output)
        .def("compute", &smp::mission_graph::Node::compute)
        .def("dirty", &smp::mission_graph::Node::dirty)
        .def("invalidate", &smp::mission_graph::Node::invalidate)
        .def("clear_cache", &smp::mission_graph::Node::clear_cache);

    py::enum_<smp::optimization::ObjectiveType>(m, "ObjectiveType")
        .value("Minimize", smp::optimization::ObjectiveType::Minimize)
        .value("Maximize", smp::optimization::ObjectiveType::Maximize);

    py::class_<smp::optimization::Parameter, std::shared_ptr<smp::optimization::Parameter>>(m, "Parameter")
        .def(py::init<std::string>())
        .def(py::init<std::string, double>())
        .def("name", &smp::optimization::Parameter::name)
        .def("value", &smp::optimization::Parameter::value)
        .def("set_value", &smp::optimization::Parameter::set_value)
        .def("fixed", &smp::optimization::Parameter::fixed)
        .def("set_fixed", &smp::optimization::Parameter::set_fixed)
        .def("lower_bound", &smp::optimization::Parameter::lower_bound)
        .def("set_lower_bound", &smp::optimization::Parameter::set_lower_bound)
        .def("upper_bound", &smp::optimization::Parameter::upper_bound)
        .def("set_upper_bound", &smp::optimization::Parameter::set_upper_bound)
        .def("clamp", &smp::optimization::Parameter::clamp);

    py::class_<smp::optimization::Objective, std::shared_ptr<smp::optimization::Objective>>(m, "Objective")
        .def(py::init<std::string>())
        .def(py::init<std::string, smp::optimization::ObjectiveType>())
        .def("name", &smp::optimization::Objective::name)
        .def("type", &smp::optimization::Objective::type)
        .def("has_gradient", &smp::optimization::Objective::has_gradient)
        .def("set_evaluation_function", [](smp::optimization::Objective& self, py::object func) {
            self.set_evaluation_function([func](const std::vector<smp::optimization::ParameterPtr>& params) {
                py::list py_params;
                for (const auto& p : params) {
                    py_params.append(p->value());
                }
                return py::cast<double>(func(py_params));
            });
        })
        .def("set_gradient_function", [](smp::optimization::Objective& self, py::object func) {
            self.set_gradient_function([func](const std::vector<smp::optimization::ParameterPtr>& params) {
                py::list py_params;
                for (const auto& p : params) {
                    py_params.append(p->value());
                }
                return py::cast<std::vector<double>>(func(py_params));
            });
        });

    py::enum_<smp::optimization::ConstraintType>(m, "ConstraintType")
        .value("Equality", smp::optimization::ConstraintType::Equality)
        .value("Inequality", smp::optimization::ConstraintType::Inequality);

    py::class_<smp::optimization::Constraint, std::shared_ptr<smp::optimization::Constraint>>(m, "Constraint")
        .def(py::init<std::string, smp::optimization::ConstraintType>())
        .def("name", &smp::optimization::Constraint::name)
        .def("type", &smp::optimization::Constraint::type)
        .def("set_evaluation_function", [](smp::optimization::Constraint& self, py::object func) {
            self.set_evaluation_function([func](const std::vector<smp::optimization::ParameterPtr>& params) {
                py::list py_params;
                for (const auto& p : params) {
                    py_params.append(p->value());
                }
                return py::cast<double>(func(py_params));
            });
        })
        .def("set_gradient_function", [](smp::optimization::Constraint& self, py::object func) {
            self.set_gradient_function([func](const std::vector<smp::optimization::ParameterPtr>& params) {
                py::list py_params;
                for (const auto& p : params) {
                    py_params.append(p->value());
                }
                return py::cast<std::vector<double>>(func(py_params));
            });
        })
        .def("satisfied", [](smp::optimization::Constraint& self, const std::vector<smp::optimization::ParameterPtr>& params) {
            return self.satisfied(params);
        })
        .def("violation", [](smp::optimization::Constraint& self, const std::vector<smp::optimization::ParameterPtr>& params) {
            return self.violation(params);
        });

    py::class_<smp::optimization::OptimizationResult>(m, "OptimizationResult")
        .def_readonly("parameters", &smp::optimization::OptimizationResult::parameters)
        .def_readonly("objective_value", &smp::optimization::OptimizationResult::objective_value)
        .def_readonly("iterations", &smp::optimization::OptimizationResult::iterations)
        .def_readonly("converged", &smp::optimization::OptimizationResult::converged)
        .def_readonly("message", &smp::optimization::OptimizationResult::message);

    py::class_<smp::optimization::GradientDescent, std::shared_ptr<smp::optimization::GradientDescent>>(m, "GradientDescent")
        .def(py::init<>())
        .def("set_learning_rate", &smp::optimization::GradientDescent::set_learning_rate)
        .def("set_max_iterations", &smp::optimization::GradientDescent::set_max_iterations)
        .def("set_tolerance", &smp::optimization::GradientDescent::set_tolerance)
        .def("set_momentum", &smp::optimization::GradientDescent::set_momentum)
        .def("set_objective", &smp::optimization::GradientDescent::set_objective)
        .def("set_parameters", &smp::optimization::GradientDescent::set_parameters)
        .def("set_constraints", &smp::optimization::GradientDescent::set_constraints)
        .def("optimize", &smp::optimization::GradientDescent::optimize);

    py::class_<smp::spice::Ephemeris, std::shared_ptr<smp::spice::Ephemeris>>(m, "Ephemeris")
        .def(py::init<>())
        .def("load_kernel", &smp::spice::Ephemeris::load_kernel)
        .def("unload_kernel", &smp::spice::Ephemeris::unload_kernel)
        .def("unload_all", &smp::spice::Ephemeris::unload_all)
        .def("clear_errors", &smp::spice::Ephemeris::clear_errors)
        .def("has_error", &smp::spice::Ephemeris::has_error)
        .def("get_error", &smp::spice::Ephemeris::get_error)
        .def("get_position", [](smp::spice::Ephemeris& self, const std::string& target,
                                const std::string& observer, const smp::core::Epoch& epoch,
                                const std::string& frame, const std::string& abcorr) {
            auto pos = self.get_position(target, observer, epoch, frame, abcorr);
            return py::make_tuple(pos[0], pos[1], pos[2]);
        })
        .def("get_light_time", &smp::spice::Ephemeris::get_light_time)
        .def("get_body_id", &smp::spice::Ephemeris::get_body_id)
        .def("get_body_name", &smp::spice::Ephemeris::get_body_name)
        .def("get_state", [](smp::spice::Ephemeris& self, const std::string& target,
                            const std::string& observer, const smp::core::Epoch& epoch,
                            const std::string& frame, const std::string& abcorr) -> py::object {
            auto state = self.get_state(target, observer, epoch, frame, abcorr);
            if (!state) return py::none();
            auto pos = state->position();
            auto vel = state->velocity();
            return py::make_tuple(
                py::make_tuple(pos[0], pos[1], pos[2]),
                py::make_tuple(vel[0], vel[1], vel[2])
            );
        }, py::arg("target"), py::arg("observer"), py::arg("epoch"),
           py::arg("frame") = "J2000", py::arg("abcorr") = "NONE")
        .def("get_states", [](smp::spice::Ephemeris& self, const std::string& target,
                             const std::string& observer, const std::vector<smp::core::Epoch>& epochs,
                             const std::string& frame, const std::string& abcorr) {
            auto states = self.get_states(target, observer, epochs, frame, abcorr);
            py::list pos_list, vel_list;
            for (const auto& state : states) {
                auto pos = state.position();
                auto vel = state.velocity();
                pos_list.append(py::make_tuple(pos[0], pos[1], pos[2]));
                vel_list.append(py::make_tuple(vel[0], vel[1], vel[2]));
            }
            return py::make_tuple(pos_list, vel_list);
        }, py::arg("target"), py::arg("observer"), py::arg("epochs"),
           py::arg("frame") = "J2000", py::arg("abcorr") = "NONE")
        .def_static("solar_system_bodies", &smp::spice::Ephemeris::solar_system_bodies)
        .def_static("solar_system_naif_ids", &smp::spice::Ephemeris::solar_system_naif_ids);
}
