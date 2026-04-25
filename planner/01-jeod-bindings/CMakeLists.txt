cmake_minimum_required(VERSION 3.18)
project(pyjeod VERSION 1.0 LANGUAGES CXX)

#==============================================================================
# Options
#==============================================================================
option(JEOD_BUILD_TESTS "Build JEOD tests" OFF)
option(JEOD_ENABLE_TRICK "Enable Trick integration" OFF)

#==============================================================================
# Find dependencies
#==============================================================================
# JEOD directory
set(JEOD_DIR "${CMAKE_SOURCE_DIR}/../../jeod" CACHE PATH "Path to JEOD")

# ER7 Utils (from Trick)
if(NOT DEFINED ER7_UTILS_HOME)
    set(ER7_UTILS_HOME $ENV{ER7_UTILS_HOME} CACHE PATH "Path to ER7 Utils source")
endif()

# CSPICE - required for SPICE ephemerides
if(NOT DEFINED JEOD_SPICE_DIR)
    set(JEOD_SPICE_DIR $ENV{JEOD_SPICE_DIR} CACHE PATH "Path to CSPICE installation")
endif()

find_library(SPICE_LIBRARY NAMES cspice PATHS "${JEOD_SPICE_DIR}/lib" NO_DEFAULT_PATH)

# Python
find_package(Python3 COMPONENTS Interpreter Development REQUIRED)
find_package(pybind11 CONFIG REQUIRED)

# Eigen3 (for matrix support)
if(NOT EIGEN3_INCLUDE_DIR)
    find_package(Eigen3 CONFIG)
    if(NOT Eigen3_FOUND)
        # Fallback to standard include paths
        find_path(EIGEN3_INCLUDE_DIR NAMES Eigen/Core
            PATHS /usr/local/include/eigen3 /usr/include/eigen3
        )
    endif()
endif()

if(EIGEN3_INCLUDE_DIR)
    message(STATUS "Found Eigen3 headers: ${EIGEN3_INCLUDE_DIR}")
    include_directories(${EIGEN3_INCLUDE_DIR})
    # Create an alias if find_package didn't create the target
    if(NOT TARGET Eigen3::Eigen)
        add_library(Eigen3::Eigen INTERFACE IMPORTED)
        set_target_properties(Eigen3::Eigen PROPERTIES
            INTERFACE_INCLUDE_DIRECTORIES "${EIGEN3_INCLUDE_DIR}"
        )
    endif()
else()
    find_package(Eigen3 REQUIRED)
endif()

#==============================================================================
# JEOD Include Paths
#==============================================================================
set(JEOD_INCLUDE_DIRS
    "${JEOD_DIR}/models"
    "${JEOD_DIR}/models/dynamics/dyn_body/include"
    "${JEOD_DIR}/models/dynamics/dyn_manager/include"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_interface/include"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_item/include"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_manager/include"
    "${JEOD_DIR}/models/environment/ephemerides/de4xx_ephem/include"
    "${JEOD_DIR}/models/environment/spice/include"
    "${JEOD_DIR}/models/environment/gravity/include"
    "${JEOD_DIR}/models/environment/time/include"
    "${JEOD_DIR}/models/utils/ref_frames/include"
    "${JEOD_DIR}/models/utils/orbital_elements/include"
    "${JEOD_DIR}/models/utils/integration/include"
    "${JEOD_DIR}/models/utils/integration/lsode/include"
    "${JEOD_DIR}/models/utils/sim_interface/include"
    "${JEOD_DIR}/trickified"
    "${ER7_UTILS_HOME}"
)

# Add CSPICE includes if available
if(SPICE_LIBRARY)
    set(JEOD_INCLUDE_DIRS "${JEOD_INCLUDE_DIRS}" "${JEOD_SPICE_DIR}/include")
endif()

include_directories(${JEOD_INCLUDE_DIRS})

#==============================================================================
# Compiler settings
#==============================================================================
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Definitions for JEOD standalone build
add_definitions(-DJEOD_CONFIG_HEADER="config_test_harness.hh")
add_definitions(-DUSE_ER7_UTILS_INTEGRATORS)

#==============================================================================
# Find JEOD source files
#==============================================================================
# Collect JEOD model source files needed for propagation
# Collect JEOD model source files needed for propagation
file(GLOB_RECURSE JEOD_SOURCES
    "${JEOD_DIR}/models/dynamics/dyn_body/src/*.cc"
    "${JEOD_DIR}/models/dynamics/dyn_manager/src/*.cc"
    "${JEOD_DIR}/models/dynamics/mass/src/*.cc"
    "${JEOD_DIR}/models/environment/gravity/src/*.cc"
    "${JEOD_DIR}/models/environment/time/src/*.cc"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_manager/src/*.cc"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_interface/src/*.cc"
    "${JEOD_DIR}/models/environment/ephemerides/ephem_item/src/*.cc"
    "${JEOD_DIR}/models/utils/ref_frames/src/*.cc"
    "${JEOD_DIR}/models/utils/orbital_elements/src/*.cc"
    "${JEOD_DIR}/models/utils/integration/src/*.cc"
    "${JEOD_DIR}/models/utils/integration/gauss_jackson/src/*.cc"
    "${ER7_UTILS_HOME}/integration/core/src/*.cc"
    "${ER7_UTILS_HOME}/math/src/*.cc"
)

# Add SPICE model if CSPICE is available
if(SPICE_LIBRARY)
    file(GLOB_RECURSE SPICE_SOURCES "${JEOD_DIR}/models/environment/spice/src/*.cc")
    list(APPEND JEOD_SOURCES ${SPICE_SOURCES})
endif()

#==============================================================================
# Build pyjeod shared library
#==============================================================================
pybind11_add_module(pyjeod NO_EXTRAS 
    pyjeod.cc
    ${JEOD_SOURCES}
)

set_target_properties(pyjeod PROPERTIES CXX_VISIBILITY_PRESET default)

target_link_libraries(pyjeod PRIVATE
    pybind11::module
    Eigen3::Eigen
)

# Link CSPICE if available
if(SPICE_LIBRARY)
    target_link_libraries(pyjeod PRIVATE "${SPICE_LIBRARY}")
endif()

# Set output directory
set_target_properties(pyjeod PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

#==============================================================================
# Installation
#==============================================================================
install(TARGETS pyjeod
    EXPORT pyjeodExports
    LIBRARY DESTINATION "${Python3_SITEARCH}"
    RUNTIME DESTINATION "${Python3_SITEARCH}"
)

# Create a package config for easy importing
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/pyjeodConfig.cmake.in"
    "${CMAKE_BINARY_DIR}/pyjeodConfig.cmake"
    @ONLY
)

install(EXPORT pyjeodExports
    FILE pyjeodTargets.cmake
    NAMESPACE pyjeod::
    DESTINATION "lib/cmake/pyjeod"
)