add_library(frames STATIC
    src/transforms.cpp
)

target_include_directories(frames PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_include_directories(frames SYSTEM PUBLIC
    ${CMAKE_SOURCE_DIR}/eigen
)

target_link_libraries(frames PUBLIC
    time
    state
)

if(BUILD_TESTS)
    find_package(GTest REQUIRED)

    add_executable(test_frames
        test/test_frame.cpp
        test/test_transforms.cpp
    )

    target_include_directories(test_frames PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/eigen
    )

    target_link_libraries(test_frames
        PRIVATE
        frames
        GTest::gtest
        GTest::gtest_main
    )

    include(GoogleTest)
    gtest_discover_tests(test_frames)
endif()