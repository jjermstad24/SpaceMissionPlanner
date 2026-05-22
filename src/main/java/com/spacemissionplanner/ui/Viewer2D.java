package com.spacemissionplanner.ui;

import com.spacemissionplanner.model.CelestialBody;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.utils.IERSConventions;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class Viewer2D extends VBox {

    private CelestialBody body = CelestialBody.EARTH;
    private static final Frame EME2000 = FramesFactory.getEME2000();
    private OneAxisEllipsoid earth;

    private OneAxisEllipsoid getEarth() {
        if (earth == null) {
            earth = new OneAxisEllipsoid(body.getEllipsoidA(), body.getFlattening(),
                FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        }
        return earth;
    }

    public void setCelestialBody(CelestialBody body) {
        this.body = body;
        this.earth = null;
        updatePlot();
    }

    private ComboBox<String> xSelector;
    private ComboBox<String> ySelector;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> chart;

    private List<TrajectoryPoint> trajectory;

    private static final String[] VARIABLES = {
        "Time (s)",
        "X (km)", "Y (km)", "Z (km)",
        "Vx (km/s)", "Vy (km/s)", "Vz (km/s)",
        "Radius (km)", "Speed (km/s)",
        "Latitude (deg)", "Longitude (deg)", "Altitude (km)",
        "Semi-major axis (km)", "Eccentricity",
        "Inclination (deg)", "RAAN (deg)",
        "Arg. Periapsis (deg)", "True Anomaly (deg)"
    };

    public Viewer2D() {
        setStyle("-fx-background-color: #0a0a1a;");
        VBox.setVgrow(this, Priority.ALWAYS);
        setSpacing(5);
        setFillWidth(true);

        xSelector = new ComboBox<>();
        ySelector = new ComboBox<>();
        xSelector.getItems().addAll(VARIABLES);
        ySelector.getItems().addAll(VARIABLES);
        xSelector.getSelectionModel().select(0);
        ySelector.getSelectionModel().select(7);

        xSelector.setStyle("-fx-text-fill: white; -fx-background-color: #2a2a3e;");
        ySelector.setStyle("-fx-text-fill: white; -fx-background-color: #2a2a3e;");

        Label xLabel = new Label("X-axis:");
        xLabel.setStyle("-fx-text-fill: #ccc;");
        Label yLabel = new Label("Y-axis:");
        yLabel.setStyle("-fx-text-fill: #ccc;");

        HBox controls = new HBox(10, xLabel, xSelector, yLabel, ySelector);
        controls.setStyle("-fx-padding: 6; -fx-background-color: #1a1a2e;");

        xAxis = new NumberAxis();
        xAxis.setStyle("-fx-text-fill: #aaa;");
        yAxis = new NumberAxis();
        yAxis.setStyle("-fx-text-fill: #aaa;");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: #0a0a1a;");
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setPrefHeight(300);

        getChildren().addAll(controls, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        xSelector.setOnAction(e -> updatePlot());
        ySelector.setOnAction(e -> updatePlot());
    }

    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        this.trajectory = trajectory;
        updatePlot();
    }

    public void setTrajectoryGroups(List<List<TrajectoryPoint>> groups, List<Color> colors) {
        List<TrajectoryPoint> flat = new ArrayList<>();
        if (groups != null) {
            for (List<TrajectoryPoint> g : groups) flat.addAll(g);
        }
        setTrajectory(flat);
    }

    private void updatePlot() {
        chart.getData().clear();
        if (trajectory == null || trajectory.isEmpty()) return;

        int xIdx = xSelector.getSelectionModel().getSelectedIndex();
        int yIdx = ySelector.getSelectionModel().getSelectedIndex();
        if (xIdx < 0 || yIdx < 0) return;

        xAxis.setLabel(VARIABLES[xIdx]);
        yAxis.setLabel(VARIABLES[yIdx]);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        AbsoluteDate startDate = trajectory.get(0).date;

        for (TrajectoryPoint p : trajectory) {
            double xVal = getVariable(p, xIdx, startDate);
            double yVal = getVariable(p, yIdx, startDate);
            series.getData().add(new XYChart.Data<>(xVal, yVal));
        }

        chart.getData().add(series);
    }

    private double getVariable(TrajectoryPoint p, int idx, AbsoluteDate startDate) {
        switch (idx) {
            case 0: return p.date.durationFrom(startDate);
            case 1: return p.x / 1000;
            case 2: return p.y / 1000;
            case 3: return p.z / 1000;
            case 4: return p.vx / 1000;
            case 5: return p.vy / 1000;
            case 6: return p.vz / 1000;
            case 7: return Math.sqrt(p.x*p.x + p.y*p.y + p.z*p.z) / 1000;
            case 8: return Math.sqrt(p.vx*p.vx + p.vy*p.vy + p.vz*p.vz) / 1000;
            case 9: case 10: case 11: return getGeodetic(p, idx);
            default: return getKeplerianElement(p, idx);
        }
    }

    private double getGeodetic(TrajectoryPoint p, int idx) {
        try {
            GeodeticPoint geo = getEarth().transform(new Vector3D(p.x, p.y, p.z), EME2000, p.date);
            switch (idx) {
                case 9:  return Math.toDegrees(geo.getLatitude());
                case 10: return Math.toDegrees(geo.getLongitude());
                case 11: return geo.getAltitude() / 1000;
            }
        } catch (Exception e) {
            return Double.NaN;
        }
        return Double.NaN;
    }

    private double getKeplerianElement(TrajectoryPoint p, int idx) {
        try {
            CartesianOrbit orbit = new CartesianOrbit(
                new PVCoordinates(
                    new Vector3D(p.x, p.y, p.z),
                    new Vector3D(p.vx, p.vy, p.vz)
                ),
                EME2000, p.date, body.getGm()
            );
            KeplerianOrbit kep = new KeplerianOrbit(orbit);
            switch (idx) {
                case 12: return kep.getA() / 1000;
                case 13: return kep.getE();
                case 14: return Math.toDegrees(kep.getI());
                case 15: return Math.toDegrees(kep.getRightAscensionOfAscendingNode());
                case 16: return Math.toDegrees(kep.getPerigeeArgument());
                case 17: return Math.toDegrees(kep.getTrueAnomaly());
            }
        } catch (Exception e) {
            return Double.NaN;
        }
        return Double.NaN;
    }
}
