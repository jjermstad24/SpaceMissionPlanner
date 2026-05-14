find_package(pybind11 REQUIRED)

pybind11_add_module(spacemissionplanner_native
    bindings.cpp
)

target_include_directories(spacemissionplanner_native PRIVATE 
    "${CMAKE_SOURCE_DIR}/eigen"
    "${CMAKE_SOURCE_DIR}/cpp"
    "${CMAKE_SOURCE_DIR}/cpp/core/time/include"
    "${CMAKE_SOURCE_DIR}/cpp/core/frames/include"
    "${CMAKE_SOURCE_DIR}/cpp/core/state/include"
)

# Set the output directory to match the python package expected location
set_target_properties(spacemissionplanner_native PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/python/spacemissionplanner")