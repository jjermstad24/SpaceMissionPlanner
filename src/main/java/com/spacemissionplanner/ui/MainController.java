package com.spacemissionplanner.ui;

import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
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
    @FXML private Canvas orbitCanvas;
    @FXML private Label statusLabel;

    private OrekitService physicsService;
    private ObservableList<String> timelineItems;

    @FXML
    private void initialize() {
        physicsService = new OrekitService();
        timelineItems = FXCollections.observableArrayList(
            "Waypoint 1: LEO (500 km, 45°)"
        );
        timelineList.setItems(timelineItems);
        setDefaults();
    }

    private void setDefaults() {
        // Default: 500 km altitude circular orbit
        tfSemiMajorAxis.setText("6871");  // 6371 + 500 km in km
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

            double a = Double.parseDouble(tfSemiMajorAxis.getText()) * 1000; // km to m
            double e = Double.parseDouble(tfEccentricity.getText());
            double i = Double.parseDouble(tfInclination.getText());
            double raan = Double.parseDouble(tfRaan.getText());
            double argPe = Double.parseDouble(tfArgPeriapsis.getText());
            double ta = Double.parseDouble(tfTrueAnomaly.getText());

            Orbit orbit = physicsService.createOrbit(a, e, i, raan, argPe, ta);

            // Propagate for one orbit (~90 min)
            List<TrajectoryPoint> trajectory = physicsService.propagate(orbit, 5400, 50);

            // Draw orbit
            drawOrbit(trajectory);

            statusLabel.setText("Mission complete - " + trajectory.size() + " points");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void drawOrbit(List<TrajectoryPoint> trajectory) {
        GraphicsContext gc = orbitCanvas.getGraphicsContext2D();
        double width = orbitCanvas.getWidth();
        double height = orbitCanvas.getHeight();

        // Clear
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        // Find bounds
        double maxR = 0;
        for (TrajectoryPoint p : trajectory) {
            double r = Math.sqrt(p.x*p.x + p.y*p.y + p.z*p.z);
            if (r > maxR) maxR = r;
        }

        // Scale factor
        double scale = (Math.min(width, height) / 2 - 20) / maxR;
        double cx = width / 2;
        double cy = height / 2;

        // Draw orbit (projected onto XY plane)
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);

        double[] xs = new double[trajectory.size()];
        double[] ys = new double[trajectory.size()];

        for (int i = 0; i < trajectory.size(); i++) {
            TrajectoryPoint p = trajectory.get(i);
            xs[i] = cx + (p.x / maxR) * (width / 2 - 20);
            ys[i] = cy - (p.y / maxR) * (height / 2 - 20);
        }

        for (int i = 0; i < xs.length - 1; i++) {
            gc.strokeLine(xs[i], ys[i], xs[i+1], ys[i+1]);
        }

        // Draw Earth
        gc.setFill(Color.BLUE);
        gc.fillOval(cx - 15, cy - 15, 30, 30);

        // Draw spacecraft at last position
        if (trajectory.size() > 0) {
            TrajectoryPoint last = trajectory.get(trajectory.size() - 1);
            double lastX = cx + (last.x / maxR) * (width / 2 - 20);
            double lastY = cy - (last.y / maxR) * (height / 2 - 20);
            gc.setFill(Color.WHITE);
            gc.fillOval(lastX - 5, lastY - 5, 10, 10);
        }
    }

    @FXML
    private void onAddWaypoint() {
        timelineItems.add("Waypoint " + (timelineItems.size() + 1) + ": Coast");
    }
}