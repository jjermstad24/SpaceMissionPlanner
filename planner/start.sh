#!/bin/bash
# Start script for Space Mission Planner

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Starting Space Mission Planner ==="

# Find directories
KERNELS_DIR="$SCRIPT_DIR/02-kernels"
BACKEND_DIR="$SCRIPT_DIR/03-backend"
FRONTEND_DIR="$SCRIPT_DIR/04-frontend"

# Check kernels exist
if [ ! -f "$KERNELS_DIR/de440.bsp" ]; then
    echo "SPICE kernels not found. Run ./build.sh first."
    exit 1
fi

# Set environment
export KERNELS_PATH="$KERNELS_DIR"

# Quiet JEOD debug noise
export JEOD_DEBUG="fatal"

# Setup backend
cd "$BACKEND_DIR"
if [ ! -d "venv" ]; then
    echo "Creating venv..."
    python3 -m venv venv
fi
source venv/bin/activate
pip install -q -r requirements.txt

# Start backend (redirect stderr to suppress JEOD debug noise)
echo "Starting backend on port 8000..."
uvicorn api.server:app --host 0.0.0.0 --port 8000 2>/dev/null &
BACKEND_PID=$!

sleep 3

# Check backend started
if ! curl -s http://localhost:8000/health > /dev/null 2>&1; then
    echo "Backend failed to start"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi
echo "Backend running at http://localhost:8000"

# Start frontend
if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install --silent
    fi
    
    echo "Starting frontend on port 3000..."
    npm start &
    FRONTEND_PID=$!
fi

echo ""
echo "=== READY ==="
echo "Frontend: http://localhost:3000"
echo "Backend:  http://localhost:8000"
echo ""

# Cleanup on exit
cleanup() {
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
}
trap cleanup EXIT

wait