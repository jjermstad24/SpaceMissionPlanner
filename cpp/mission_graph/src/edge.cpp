#include "edge.hh"

#include <optional>
#include <any>

namespace smp::mission_graph {

bool Edge::is_valid() const {
    if (!source_ || !target_) {
        return false;
    }

    auto outputs = source_->outputs();
    if (outputs.find(source_output_) == outputs.end()) {
        return false;
    }

    auto inputs = target_->inputs();
    return inputs.find(target_input_) != inputs.end();
}

} // namespace smp::mission_graph