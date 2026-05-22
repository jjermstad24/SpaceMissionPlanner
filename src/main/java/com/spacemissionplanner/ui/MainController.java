package com.spacemissionplanner.ui;

import com.spacemissionplanner.model.Waypoint;
import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.orekit.orbits.Orbit;

import java.util.List;

public class MainController {

    @FXML private TextField tfSemiMajorAxis;
    @FXML private TextField tfEccentricity;
    @FXML private TextField tfInclination;
    @FXML private TextField tfRaan;
    @FXML private TextField tfArgPeriapsis;
    @FXML private TextField tfTrueAnomaly;

    @FXML private ListView<Waypoint> timelineList;
    @FXML private TabPane viewerTabs;
    @FXML private VBox tab3DContent;
    @FXML private VBox tab2DContent;
    @FXML private Label statusLabel;

    private OrekitService physicsService;
    private ObservableList<Waypoint> waypoints;
    private Viewer3D viewer3D;
    private Viewer2D viewer2D;

    @FXML
    private void initialize() {
        physicsService = new OrekitService();
        waypoints = FXCollections.observableArrayList();
        
        Waypoint defaultWaypoint = new Waypoint("Waypoint 1", 6871, 0.0, 45.0, 0.0, 0.0, 0.0);
        waypoints.add(defaultWaypoint);
        
        timelineList.setItems(waypoints);
        timelineList.getSelectionModel().select(0);
        
        loadWaypointToForm(defaultWaypoint);
        
        viewer3D = new Viewer3D();
        tab3DContent.getChildren().add(0, viewer3D);
        
        viewer2D = new Viewer2D();
        tab2DContent.getChildren().add(viewer2D);
    }

    private void loadWaypointToForm(Waypoint wp) {
        tfSemiMajorAxis.setText(String.valueOf(wp.getSemiMajorAxis()));
        tfEccentricity.setText(String.valueOf(wp.getEccentricity()));
        tfInclination.setText(String.valueOf(wp.getInclination()));
        tfRaan.setText(String.valueOf(wp.getRaan()));
        tfArgPeriapsis.setText(String.valueOf(wp.getArgPeriapsis()));
        tfTrueAnomaly.setText(String.valueOf(wp.getTrueAnomaly()));
    }

    private void setDefaults() {
        tfSemiMajorAxis.setText("6871");
        tfEccentricity.setText("0.0");
        tfInclination.setText("45.0");
        tfRaan.setText("0.0");
        tfArgPeriapsis.setText("0.0");
        tfTrueAnomaly.setText("0.0");
    }

    @FXML
    private void onWaypointSelected() {
        Waypoint selected = timelineList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadWaypointToForm(selected);
        }
    }

    @FXML
    private void onRunMission() {
        try {
            Waypoint selectedWaypoint = timelineList.getSelectionModel().getSelectedItem();
            
            if (selectedWaypoint == null) {
                statusLabel.setText("Please select a waypoint first");
                return;
            }
            
            statusLabel.setText("Computing...");
            
            double a = Double.parseDouble(tfSemiMajorAxis.getText()) * 1000;
            double e = Double.parseDouble(tfEccentricity.getText());
            double i = Double.parseDouble(tfInclination.getText());
            double raan = Double.parseDouble(tfRaan.getText());
            double argPe = Double.parseDouble(tfArgPeriapsis.getText());
            double ta = Double.parseDouble(tfTrueAnomaly.getText());
            
            updateWaypointFromForm(selectedWaypoint);
            
            Orbit orbit = physicsService.createOrbit(a, e, i, raan, argPe, ta);

            List<TrajectoryPoint> trajectory = physicsService.propagate(orbit, 5400, 100);

            viewer3D.setTrajectory(trajectory);
            viewer2D.setTrajectory(trajectory);

            statusLabel.setText("Mission complete - " + trajectory.size() + " points");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateWaypointFromForm(Waypoint wp) {
        wp.setSemiMajorAxis(Double.parseDouble(tfSemiMajorAxis.getText()));
        wp.setEccentricity(Double.parseDouble(tfEccentricity.getText()));
        wp.setInclination(Double.parseDouble(tfInclination.getText()));
        wp.setRaan(Double.parseDouble(tfRaan.getText()));
        wp.setArgPeriapsis(Double.parseDouble(tfArgPeriapsis.getText()));
        wp.setTrueAnomaly(Double.parseDouble(tfTrueAnomaly.getText()));
        timelineList.refresh();
    }

    @FXML
    private void onAddWaypoint() {
        int num = waypoints.size() + 1;
        Waypoint newWaypoint = new Waypoint("Waypoint " + num);
        waypoints.add(newWaypoint);
        timelineList.getSelectionModel().select(newWaypoint);
    }

    @FXML
    private void onRemoveWaypoint() {
        Waypoint selected = timelineList.getSelectionModel().getSelectedItem();
        if (selected != null && waypoints.size() > 1) {
            int index = timelineList.getSelectionModel().getSelectedIndex();
            waypoints.remove(selected);
            if (index >= waypoints.size()) {
                timelineList.getSelectionModel().select(waypoints.size() - 1);
            } else {
                timelineList.getSelectionModel().select(index);
            }
        } else {
            statusLabel.setText("Cannot remove the last waypoint");
        }
    }

    @FXML
    private void onRunAllWaypoints() {
        try {
            statusLabel.setText("Running mission with " + waypoints.size() + " waypoints...");
            
            List<TrajectoryPoint> allTrajectory = new java.util.ArrayList<>();
            
            for (int i = 0; i < waypoints.size(); i++) {
                Waypoint wp = waypoints.get(i);
                double a = wp.getSemiMajorAxis() * 1000;
                double e = wp.getEccentricity();
                double inc = wp.getInclination();
                double raan = wp.getRaan();
                double argPe = wp.getArgPeriapsis();
                double ta = wp.getTrueAnomaly();
                
                Orbit orbit = physicsService.createOrbit(a, e, inc, raan, argPe, ta);
                List<TrajectoryPoint> trajectory = physicsService.propagate(orbit, 5400, 100);
                
                allTrajectory.addAll(trajectory);
                
                if (i < waypoints.size() - 1) {
                    Waypoint nextWp = waypoints.get(i + 1);
                    List<TrajectoryPoint> coastArc = physicsService.computeCoastArc(
                        trajectory.get(trajectory.size() - 1),
                        nextWp.getSemiMajorAxis() * 1000,
                        nextWp.getEccentricity(),
                        nextWp.getInclination(),
                        nextWp.getRaan(),
                        nextWp.getArgPeriapsis(),
                        nextWp.getTrueAnomaly()
                    );
                    allTrajectory.addAll(coastArc);
                }
                
                statusLabel.setText("Processed waypoint " + (i + 1) + "/" + waypoints.size());
            }
            
            viewer3D.setTrajectory(allTrajectory);
            viewer2D.setTrajectory(allTrajectory);
            statusLabel.setText("Mission complete - " + allTrajectory.size() + " points");
            
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onResetView() {
    }

    @FXML
    private void onRotateX() {
    }

    @FXML
    private void onRotateXNeg() {
    }

    @FXML
    private void onRotateY() {
    }

    @FXML
    private void onRotateYNeg() {
    }

    @FXML
    private void onTargetEarth() {
        viewer3D.setTarget("earth");
    }

    @FXML
    private void onTargetSpacecraft() {
        viewer3D.setTarget("spacecraft");
    }
}