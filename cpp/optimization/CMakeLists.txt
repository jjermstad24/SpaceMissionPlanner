add_library(optimization STATIC
    src/gradient_descent.cpp
)

target_include_directories(optimization PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/cpp
)

target_link_libraries(optimization PUBLIC
    core
)

if(BUILD_TESTS)
    find_package(GTest REQUIRED)

    add_executable(test_optimization
        test/test_optimizer.cpp
    )

    target_include_directories(test_optimization PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/cpp
    )

    target_link_libraries(test_optimization
        PRIVATE
        optimization
        GTest::gtest
        GTest::gtest_main
    )

    include(GoogleTest)
    gtest_discover_tests(test_optimization)
endif()