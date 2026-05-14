add_library(mission_graph STATIC
    src/edge.cpp
    src/graph.cpp
)

target_include_directories(mission_graph PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/cpp
)

target_link_libraries(mission_graph PUBLIC
    core
    two_body
)

if(BUILD_TESTS)
    find_package(GTest REQUIRED)

    add_executable(test_mission_graph
        test/test_graph.cpp
    )

    target_include_directories(test_mission_graph PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/cpp
    )

    target_link_libraries(test_mission_graph
        PRIVATE
        mission_graph
        GTest::gtest
        GTest::gtest_main
    )

    include(GoogleTest)
    gtest_discover_tests(test_mission_graph)
endif()