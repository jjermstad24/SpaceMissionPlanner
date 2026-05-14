#include <gtest/gtest.h>

#include "graph.hh"
#include "node.hh"

#include <string>
#include <any>

namespace smp::mission_graph {
namespace {

class SimpleNode : public Node {
public:
    explicit SimpleNode(std::string name)
        : Node(std::move(name)) {}

    std::unordered_set<OutputId> outputs() const override {
        return {"value"};
    }

    std::unordered_set<InputId> inputs() const override {
        return {};
    }

    bool compute() override {
        output_cache_["value"] = 42;
        status_ = NodeStatus::Valid;
        return true;
    }

    void invalidate_output(const OutputId& output) override {
        output_cache_.erase(output);
        status_ = NodeStatus::Invalid;
    }

    std::optional<std::any> get_output(const OutputId& output_id) const override {
        auto it = output_cache_.find(output_id);
        if (it != output_cache_.end()) {
            return it->second;
        }
        return std::nullopt;
    }

    void set_input(const InputId& input_id, std::any value) override {
    }

    bool dirty() const override {
        return status_ == NodeStatus::Invalid;
    }

    void clear_cache() override {
        output_cache_.clear();
        status_ = NodeStatus::Pending;
    }

private:
    std::unordered_map<OutputId, int> output_cache_;
};

class AddNode : public Node {
public:
    explicit AddNode(std::string name)
        : Node(std::move(name)) {}

    std::unordered_set<OutputId> outputs() const override {
        return {"sum"};
    }

    std::unordered_set<InputId> inputs() const override {
        return {"a", "b"};
    }

    bool compute() override {
        auto it_a = input_values_.find("a");
        auto it_b = input_values_.find("b");

        if (it_a == input_values_.end() || it_b == input_values_.end()) {
            return false;
        }

        int a = std::any_cast<int>(it_a->second);
        int b = std::any_cast<int>(it_b->second);

        output_cache_["sum"] = a + b;
        status_ = NodeStatus::Valid;
        return true;
    }

    void invalidate_output(const OutputId& output) override {
        output_cache_.erase(output);
        status_ = NodeStatus::Invalid;
    }

    std::optional<std::any> get_output(const OutputId& output_id) const override {
        auto it = output_cache_.find(output_id);
        if (it != output_cache_.end()) {
            return it->second;
        }
        return std::nullopt;
    }

    void set_input(const InputId& input_id, std::any value) override {
        input_values_[input_id] = std::move(value);
    }

    bool dirty() const override {
        return status_ == NodeStatus::Invalid;
    }

    void clear_cache() override {
        output_cache_.clear();
        status_ = NodeStatus::Pending;
    }

private:
    std::unordered_map<OutputId, int> output_cache_;
    std::unordered_map<InputId, std::any> input_values_;
};

} // anonymous namespace
} // namespace smp::mission_graph

TEST(GraphTest, AddNode) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto add_node = std::make_shared<smp::mission_graph::AddNode>("add");
    graph->add_node(add_node);

    add_node->set_input("a", 10);
    add_node->set_input("b", 32);

    auto result = graph->evaluate("add", "sum");
    ASSERT_TRUE(result.has_value());

    auto sum = std::any_cast<int>(result.value());
    EXPECT_EQ(sum, 42);
}

TEST(GraphTest, GetNodes) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto node1 = std::make_shared<smp::mission_graph::SimpleNode>("node1");
    auto node2 = std::make_shared<smp::mission_graph::SimpleNode>("node2");

    graph->add_node(node1);
    graph->add_node(node2);

    auto nodes = graph->get_nodes();
    EXPECT_EQ(nodes.size(), 2);

    auto retrieved = graph->get_node("node1");
    EXPECT_EQ(retrieved->name(), "node1");

    auto missing = graph->get_node("nonexistent");
    EXPECT_EQ(missing, nullptr);
}

TEST(GraphTest, RemoveNode) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto node = std::make_shared<smp::mission_graph::SimpleNode>("node1");
    graph->add_node(node);

    EXPECT_EQ(graph->get_nodes().size(), 1);

    graph->remove_node("node1");
    EXPECT_EQ(graph->get_nodes().size(), 0);
}

TEST(GraphTest, Invalidation) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto node = std::make_shared<smp::mission_graph::SimpleNode>("node1");
    graph->add_node(node);

    graph->evaluate("node1", "value");
    EXPECT_EQ(node->status(), smp::mission_graph::NodeStatus::Valid);

    graph->invalidate_node("node1");
    EXPECT_EQ(node->status(), smp::mission_graph::NodeStatus::Invalid);
}

TEST(GraphTest, DownstreamNodes) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto node1 = std::make_shared<smp::mission_graph::SimpleNode>("node1");
    auto node2 = std::make_shared<smp::mission_graph::SimpleNode>("node2");
    auto node3 = std::make_shared<smp::mission_graph::SimpleNode>("node3");

    graph->add_node(node1);
    graph->add_node(node2);
    graph->add_node(node3);

    auto downstream = graph->get_downstream_nodes("node1");
    EXPECT_EQ(downstream.size(), 0);

    downstream = graph->get_downstream_nodes("nonexistent");
    EXPECT_EQ(downstream.size(), 0);
}

TEST(GraphTest, CycleDetection) {
    auto graph = std::make_shared<smp::mission_graph::Graph>();

    auto node1 = std::make_shared<smp::mission_graph::SimpleNode>("node1");
    auto node2 = std::make_shared<smp::mission_graph::SimpleNode>("node2");

    graph->add_node(node1);
    graph->add_node(node2);

    EXPECT_FALSE(graph->has_cycle());
}