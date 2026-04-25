# Makefile for pyjeod module

# Paths
JEOD_DIR ?= /home/jjermsta/SpaceMissionPlanner/jeod
TRICK_HOME ?= /home/jjermsta/SimulationFramework/trick
ER7_UTILS_HOME ?= $(TRICK_HOME)/trick_source
EIGEN_DIR ?= /home/jjermsta/SpaceMissionPlanner/eigen
CSPICE_DIR ?= /home/jjermsta/SpaceMissionPlanner/cspice/cspice
BACKEND_VENV ?= /home/jjermsta/SpaceMissionPlanner/planner/03-backend/venv

# Compiler and Flags
CXX = g++
# -w suppresses the heavy volume of warnings from JEOD headers
CXXFLAGS = -O2 -Wall -std=c++17 -fPIC -fvisibility=default -DJEOD_UNIT_TEST -w
LDFLAGS = -shared

# Python and pybind11 flags
PYTHON_CONFIG = /usr/bin/python3-config
PYTHON_INCLUDES = $(shell $(PYTHON_CONFIG) --includes)
PYBIND11_INCLUDES = $(shell $(BACKEND_VENV)/bin/python3 -m pybind11 --includes)
EXTENSION_SUFFIX = $(shell $(PYTHON_CONFIG) --extension-suffix)

# Include directories
INCLUDES = $(PYTHON_INCLUDES) $(PYBIND11_INCLUDES) \
           -I$(JEOD_DIR)/models \
           -I$(EIGEN_DIR) \
           -I$(CSPICE_DIR)/include \
           -I$(TRICK_HOME)/trick_source

# JEOD Source Directories
# JEOD Source Directories - specific set needed for SMP core
JEOD_SRC_DIRS = \
    $(JEOD_DIR)/models/dynamics/dyn_body/src \
    $(JEOD_DIR)/models/dynamics/dyn_manager/src \
    $(JEOD_DIR)/models/dynamics/body_action/src \
    $(JEOD_DIR)/models/dynamics/derived_state/src \
    $(JEOD_DIR)/models/dynamics/rel_kin/src \
    $(JEOD_DIR)/models/dynamics/mass/src \
    $(JEOD_DIR)/models/experimental/constraints/src \
    $(JEOD_DIR)/models/environment/gravity/src \
    $(JEOD_DIR)/models/environment/time/src \
    $(JEOD_DIR)/models/environment/planet/src \
    $(JEOD_DIR)/models/environment/ephemerides/ephem_manager/src \
    $(JEOD_DIR)/models/environment/ephemerides/ephem_interface/src \
    $(JEOD_DIR)/models/environment/ephemerides/ephem_item/src \
    $(JEOD_DIR)/models/environment/spice/src \
    $(JEOD_DIR)/models/utils/ref_frames/src \
    $(JEOD_DIR)/models/utils/orientation/src \
    $(JEOD_DIR)/models/utils/planet_fixed/north_east_down/src \
    $(JEOD_DIR)/models/utils/planet_fixed/planet_fixed_posn/src \
    $(JEOD_DIR)/models/utils/orbital_elements/src \
    $(JEOD_DIR)/models/utils/integration/src \
    $(JEOD_DIR)/models/utils/integration/gauss_jackson/src \
    $(JEOD_DIR)/models/utils/named_item/src \
    $(JEOD_DIR)/models/utils/memory/src \
    $(JEOD_DIR)/models/utils/sim_interface/src \
    $(JEOD_DIR)/models/utils/message/src \
    $(JEOD_DIR)/models/utils/math/src \
    $(JEOD_DIR)/models/utils/quaternion/src \
    $(JEOD_DIR)/models/utils/lvlh_frame/src \
    $(JEOD_DIR)/models/utils/container/src

# ER7 Utils Source Directories
ER7_SRC_DIRS = \
    $(wildcard $(ER7_UTILS_HOME)/er7_utils/*/src) \
    $(ER7_UTILS_HOME)/er7_utils/integration/*/src

# Collect all sources
SOURCES = pyjeod.cc
SOURCES += $(foreach dir,$(JEOD_SRC_DIRS),$(wildcard $(dir)/*.cc))
SOURCES += $(foreach dir,$(ER7_SRC_DIRS),$(wildcard $(dir)/*.cc))

# Objects
OBJS = $(SOURCES:.cc=.o)

# Library linkage
LIBS = -L$(CSPICE_DIR)/lib -lcspice

# Target
TARGET = pyjeod$(EXTENSION_SUFFIX)

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) $(OBJS) -o $(TARGET) $(LDFLAGS) $(LIBS)

%.o: %.cc
	$(CXX) $(CXXFLAGS) $(INCLUDES) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

.PHONY: all clean
