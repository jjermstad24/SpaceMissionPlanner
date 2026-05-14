#include <gtest/gtest.h>

#include "gradient_descent.hh"
#include "objective.hh"
#include "constraint.hh"
#include "parameter.hh"

#include <cmath>

TEST(OptimizerTest, MinimizeQuadratic) {
    auto param = std::make_shared<smp::optimization::Parameter>("x", 10.0);

    auto objective = std::make_shared<smp::optimization::Objective>("quadratic");
    objective->set_evaluation_function([](const std::vector<smp::optimization::ParameterPtr>& params) {
        double x = params[0]->value();
        return x * x;
    });
    objective->set_gradient_function([](const std::vector<smp::optimization::ParameterPtr>& params) {
        double x = params[0]->value();
        return std::vector<double>{2.0 * x};
    });

    auto optimizer = std::make_shared<smp::optimization::GradientDescent>();
    optimizer->set_learning_rate(0.1);
    optimizer->set_max_iterations(100);
    optimizer->set_tolerance(1e-6);
    optimizer->set_objective(objective);
    optimizer->set_parameters({param});

    auto result = optimizer->optimize();

    EXPECT_TRUE(result.converged);
    EXPECT_NEAR(result.parameters[0], 0.0, 0.1);
    EXPECT_NEAR(result.objective_value, 0.0, 0.1);
}

TEST(ParameterTest, Clamp) {
    auto param = std::make_shared<smp::optimization::Parameter>("x", 10.0);
    param->set_lower_bound(0.0);
    param->set_upper_bound(5.0);

    param->set_value(15.0);
    param->clamp();
    EXPECT_EQ(param->value(), 5.0);

    param->set_value(-5.0);
    param->clamp();
    EXPECT_EQ(param->value(), 0.0);
}

TEST(ParameterTest, Fixed) {
    auto param = std::make_shared<smp::optimization::Parameter>("x", 5.0);
    param->set_fixed(true);

    EXPECT_TRUE(param->fixed());
    EXPECT_EQ(param->value(), 5.0);
}

TEST(ObjectiveTest, Minimize) {
    auto objective = std::make_shared<smp::optimization::Objective>("test", smp::optimization::ObjectiveType::Minimize);
    objective->set_evaluation_function([](const std::vector<smp::optimization::ParameterPtr>&) {
        return 42.0;
    });

    auto param = std::make_shared<smp::optimization::Parameter>("x", 0.0);
    double result = objective->evaluate({param});

    EXPECT_EQ(result, 42.0);
}

TEST(ObjectiveTest, Maximize) {
    auto objective = std::make_shared<smp::optimization::Objective>("test", smp::optimization::ObjectiveType::Maximize);
    objective->set_evaluation_function([](const std::vector<smp::optimization::ParameterPtr>&) {
        return 42.0;
    });

    auto param = std::make_shared<smp::optimization::Parameter>("x", 0.0);
    double result = objective->evaluate({param});

    EXPECT_EQ(result, -42.0);
}

TEST(ConstraintTest, Equality) {
    auto constraint = std::make_shared<smp::optimization::Constraint>("eq", smp::optimization::ConstraintType::Equality);
    constraint->set_evaluation_function([](const std::vector<smp::optimization::ParameterPtr>&) {
        return 0.0;
    });

    auto param = std::make_shared<smp::optimization::Parameter>("x", 0.0);
    bool satisfied = constraint->satisfied({param});

    EXPECT_TRUE(satisfied);
}

TEST(ConstraintTest, Inequality) {
    auto constraint = std::make_shared<smp::optimization::Constraint>("ineq", smp::optimization::ConstraintType::Inequality);
    constraint->set_evaluation_function([](const std::vector<smp::optimization::ParameterPtr>&) {
        return 1.0;
    });

    auto param = std::make_shared<smp::optimization::Parameter>("x", 0.0);
    bool satisfied = constraint->satisfied({param});

    EXPECT_TRUE(satisfied);
}