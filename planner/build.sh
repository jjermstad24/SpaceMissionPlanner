#!/bin/bash
# Build script for Space Mission Planner

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLANNER_DIR="$SCRIPT_DIR"
JEOD_DIR="$PLANNER_DIR/jeod"

echo "=== Building Space Mission Planner ==="

# Check JEOD_SPICE_DIR
if [ -z "$JEOD_SPICE_DIR" ] && [ -d "/usr/local/lib" ]; then
    export JEOD_SPICE_DIR="/usr/local"
fi

# Check TRICK_HOME
if [ -z "$TRICK_HOME" ]; then
    TRICK_HOME=$(trick-config --prefix 2>/dev/null || echo "/home/jjermsta/SimulationFramework/trick")
fi

if [ -d "$TRICK_HOME" ]; then
    export TRICK_HOME
    export ER7_UTILS_HOME="$TRICK_HOME/trick_source"
    echo "Using Trick at: $TRICK_HOME"
fi

# BuildJEOD bindings if needed
if [ ! -f "$SCRIPT_DIR/01-jeod-bindings/pyjeod"*.so ]; then
    echo "Building pyjeod..."
    cd "$SCRIPT_DIR/01-jeod-bindings"
    make clean 2>/dev/null || true
    make -j$(nproc)
    echo "Built pyjeod"
fi

# Setup backend virtualenv
if [ ! -d "$SCRIPT_DIR/03-backend/venv" ]; then
    echo "Creating backend venv..."
    python3 -m venv "$SCRIPT_DIR/03-backend/venv"
fi

echo "Installing backend dependencies..."
cd "$SCRIPT_DIR/03-backend"
source venv/bin/activate
pip install -q -r requirements.txt

echo "=== Build Complete ==="
echo "Run ./start.sh to start the application"