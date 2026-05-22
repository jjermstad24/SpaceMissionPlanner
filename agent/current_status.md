# Space Mission Planner - Current Status

## Project Overview
A JavaFX desktop application for space mission planning with 3D orbit visualization using OreKit physics engine.

## Current Features
- **3D Visualization**: JavaFX 3D SubScene with PerspectiveCamera
  - Earth sphere with lat/lon grid lines (TriangleMesh)
  - Animated spacecraft following orbit trajectory (3D cylinder model)
  - Starfield background (200 random stars)
  - Trackball camera controls (drag to rotate, scroll to zoom)
  - Smooth camera follow — toggle between Earth-centered and spacecraft-centered
- **2D Visualization**: Canvas-based top-down orbit view
  - TabPane to switch between 3D and 2D views
  - Earth as filled circle with grid lines
  - Animated spacecraft marker
- **Orbit Calculation**: OreKit for orbital mechanics
  - Keplerian orbit propagation
  - Coast arc computation between waypoints
- **Mission Model**: Waypoint-based mission planning
  - Add/remove waypoints with orbital parameters
  - Select waypoint to load its parameters into the input form
  - "Run Mission" propagates selected waypoint
  - "Run All Waypoints" executes all waypoints sequentially with coast arcs
- **Tests**: JUnit 5 tests for model and physics (11 tests)

## Technical Stack
- Java 17
- JavaFX 17.0.2
- OreKit 13.1
- JUnit 5.10.0
- Maven build

## Recent Changes
- **Model layer**: Added `Waypoint` POJO with Keplerian orbital elements
- **OrekitService**: Added `computeCoastArc()` for multi-waypoint mission propagation
- **MainController**: Refactored to use `Waypoint` objects; add/remove/select waypoints; "Run All Waypoints"
- **Viewer3D**: Complete rewrite (Canvas 2D → JavaFX 3D SubScene)
- **Viewer2D**: New Canvas-based 2D orbit viewer with animation
- **UI layout**: TabPane for 3D/2D view switching
- **Testing**: Added JUnit 5 tests for Waypoint and OrekitService

## Notes
- Camera rotate buttons in FXML are stubs (trackball camera replaces them)
- 2D viewer scale recalculates on resize
