add_library(time INTERFACE)

target_include_directories(time INTERFACE
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
)