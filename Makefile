.PHONY: all build clean help
.PHONY: frames two_body spice optimization mission_graph native
.PHONY: test-python test-cpp kernels

BUILD_DIR := build

# pybind11 cmake directory (relative to venv)
PYBIND11_DIR := $(CURDIR)/.venv/lib/python3.12/site-packages/pybind11/share/cmake/pybind11

all: build

# Build everything
build:
	@mkdir -p $(BUILD_DIR)
	@cd $(BUILD_DIR) && cmake .. -Dpybind11_DIR=$(PYBIND11_DIR) || (cd $(BUILD_DIR) && cmake .. -Dpybind11_DIR=$(PYBIND11_DIR))
	@cd $(BUILD_DIR) && $(MAKE)

# Build individual components (runs cmake first if needed)
_build:
	@mkdir -p $(BUILD_DIR)
	@(cd $(BUILD_DIR) && cmake .. -Dpybind11_DIR=$(PYBIND11_DIR) 2>/dev/null) || (cd $(BUILD_DIR) && cmake .. -Dpybind11_DIR=$(PYBIND11_DIR))

build-frames: _build
	$(MAKE) -C $(BUILD_DIR) frames

build-two_body: _build
	$(MAKE) -C $(BUILD_DIR) two_body

build-spice: _build
	$(MAKE) -C $(BUILD_DIR) spice

build-optimization: _build
	$(MAKE) -C $(BUILD_DIR) optimization

build-mission_graph: _build
	$(MAKE) -C $(BUILD_DIR) mission_graph

build-native: build-spice build-optimization build-mission_graph build-two_body
	$(MAKE) -C $(BUILD_DIR) spacemissionplanner_native
	@echo "Built: python/spacemissionplanner/spacemissionplanner_native.so"

build-bindings: _build
	$(MAKE) -C $(BUILD_DIR) spacemissionplanner_native

frames: build-frames
two_body: build-two_body
spice: build-spice
optimization: build-optimization
mission_graph: build-mission_graph
native: build-native

clean:
	rm -rf $(BUILD_DIR)
	rm -f python/spacemissionplanner/*.so

kernels:
	@python -c "from spacemissionplanner.visualization.ephemeris import get_or_download_lsk, get_or_download_de440; print('Downloading kernels...'); get_or_download_lsk(); get_or_download_de440(); print('Kernels ready')"

test-python:
	@python -m pytest python/tests/ -v

test: test-python

help:
	@echo "Available targets:"
	@echo "  make all         - Build everything (default)"
	@echo "  make clean       - Remove build directory"
	@echo ""
	@echo "  make frames      - Build core/frames library"
	@echo "  make two_body   - Build two-body propagator"
	@echo "  make spice       - Build SPICE ephemeris library"
	@echo "  make optimization - Build optimization library"
	@echo "  make mission_graph - Build mission graph library"
	@echo "  make native      - Build Python native bindings"
	@echo "  make build-bindings - Rebuild only Python bindings"
	@echo "  make kernels     - Download SPICE kernels"
	@echo "  make test        - Run Python tests"
	@echo ""
	@echo "Examples:"
	@echo "  make clean && make        # Full rebuild"
	@echo "  make native              # Just bindings (needs C++ built)"
	@echo "  make spice && make native  # Rebuild spice + bindings"