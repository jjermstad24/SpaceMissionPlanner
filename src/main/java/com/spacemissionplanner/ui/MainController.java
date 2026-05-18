package com.spacemissionplanner.ui;

import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    @FXML private ListView<String> timelineList;
    @FXML private VBox viewerContainer;
    @FXML private Label statusLabel;

    private OrekitService physicsService;
    private ObservableList<String> timelineItems;
    private Viewer3D viewer3D;

    @FXML
    private void initialize() {
        physicsService = new OrekitService();
        timelineItems = FXCollections.observableArrayList(
            "Waypoint 1: LEO (500 km, 45°)"
        );
        timelineList.setItems(timelineItems);
        setDefaults();

        // Create 3D viewer
        viewer3D = new Viewer3D();
        viewerContainer.getChildren().add(0, viewer3D);
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
    private void onRunMission() {
        try {
            statusLabel.setText("Computing...");

            double a = Double.parseDouble(tfSemiMajorAxis.getText()) * 1000;
            double e = Double.parseDouble(tfEccentricity.getText());
            double i = Double.parseDouble(tfInclination.getText());
            double raan = Double.parseDouble(tfRaan.getText());
            double argPe = Double.parseDouble(tfArgPeriapsis.getText());
            double ta = Double.parseDouble(tfTrueAnomaly.getText());

            Orbit orbit = physicsService.createOrbit(a, e, i, raan, argPe, ta);

            List<TrajectoryPoint> trajectory = physicsService.propagate(orbit, 5400, 100);

            viewer3D.setTrajectory(trajectory);

            statusLabel.setText("Mission complete - " + trajectory.size() + " points");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onAddWaypoint() {
        int num = timelineItems.size() + 1;
        timelineItems.add("Waypoint " + num + ": Coast");
    }

    @FXML
    private void onResetView() {
        viewer3D.resetView();
    }

    @FXML
    private void onRotateX() {
        viewer3D.rotateX(15);
    }

    @FXML
    private void onRotateXNeg() {
        viewer3D.rotateX(-15);
    }

    @FXML
    private void onRotateY() {
        viewer3D.rotateY(15);
    }

    @FXML
    private void onRotateYNeg() {
        viewer3D.rotateY(-15);
    }
}