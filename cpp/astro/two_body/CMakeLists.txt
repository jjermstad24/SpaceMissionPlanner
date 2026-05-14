add_library(two_body STATIC
    src/orbital_elements.cpp
    src/propagator.cpp
)

target_include_directories(two_body PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/cpp
)

target_link_libraries(two_body PUBLIC
    core
)

if(BUILD_TESTS)
    find_package(GTest REQUIRED)

    add_executable(test_two_body
        test/test_orbital_elements.cpp
        test/test_propagator.cpp
    )

    target_include_directories(test_two_body PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/cpp/core/include
        ${CMAKE_SOURCE_DIR}/cpp/astro/two_body/include
    )

    target_link_libraries(test_two_body
        PRIVATE
        two_body
        GTest::gtest
        GTest::gtest_main
    )

    include(GoogleTest)
    gtest_discover_tests(test_two_body)
endif()