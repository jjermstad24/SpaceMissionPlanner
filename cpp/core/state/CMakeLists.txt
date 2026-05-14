add_library(state INTERFACE)

target_include_directories(state INTERFACE
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
)

target_link_libraries(state INTERFACE
    time
)