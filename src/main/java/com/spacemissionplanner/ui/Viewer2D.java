package com.spacemissionplanner.ui;

import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

public class Viewer2D extends VBox {

    private Canvas canvas;
    private GraphicsContext gc;

    private List<TrajectoryPoint> trajectory;
    private int currentIndex = 0;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 50;

    private double centerX, centerY;
    private double scale;

    public Viewer2D() {
        setStyle("-fx-background-color: #0a0a1a;");
        VBox.setVgrow(this, Priority.ALWAYS);

        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            centerX = newVal.doubleValue() / 2;
            if (trajectory == null) scale = computeEarthScale();
            draw();
        });

        heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            centerY = newVal.doubleValue() / 2;
            if (trajectory == null) scale = computeEarthScale();
            draw();
        });

        startAnimationTimer();
    }

    private double computeEarthScale() {
        double canvasSize = Math.min(canvas.getWidth(), canvas.getHeight());
        if (canvasSize <= 0) canvasSize = 400;
        double targetEarthRadius = canvasSize * 0.35;
        return targetEarthRadius * 1000000 / 6371000;
    }

    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        this.trajectory = trajectory;
        this.currentIndex = 0;

        if (trajectory != null && !trajectory.isEmpty()) {
            double maxR = 0;
            for (TrajectoryPoint p : trajectory) {
                double r = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
                if (r > maxR) maxR = r;
            }
            if (maxR < 6371000) maxR = 6371000 * 1.5;
            double canvasSize = Math.min(canvas.getWidth(), canvas.getHeight()) * 0.8;
            double orbitScale = canvasSize / maxR * 1000000;
            scale = Math.min(orbitScale, computeEarthScale());
        } else {
            scale = computeEarthScale();
        }

        draw();
    }

    private void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.rgb(10, 20, 40));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawEarth();
        drawOrbit();
        drawSpacecraft();
    }

    private void drawEarth() {
        double earthRadius = 6371000 * scale / 1000000;

        gc.setFill(Color.rgb(30, 80, 150));
        gc.fillOval(centerX - earthRadius, centerY - earthRadius,
                    earthRadius * 2, earthRadius * 2);

        gc.setStroke(Color.rgb(60, 120, 200));
        gc.setLineWidth(0.5);

        for (int i = -80; i <= 80; i += 20) {
            double latRad = Math.toRadians(i);
            double r = earthRadius * Math.cos(latRad);
            double h = earthRadius * Math.sin(latRad);
            double height = r;
            if (Math.abs(i) == 90) {
                gc.strokeLine(centerX, centerY - earthRadius, centerX, centerY + earthRadius);
            } else {
                gc.strokeOval(centerX - r, centerY - h, r * 2, 2 * h * 0.01);
            }
        }

        for (int i = -180; i < 180; i += 30) {
            double lonRad = Math.toRadians(i);
            double cosLon = Math.cos(lonRad);
            double sinLon = Math.sin(lonRad);

            gc.strokeLine(centerX, centerY,
                         centerX + earthRadius * cosLon,
                         centerY + earthRadius * sinLon);
        }

        gc.setStroke(Color.rgb(80, 140, 220));
        gc.setLineWidth(1);
        gc.strokeOval(centerX - earthRadius, centerY - earthRadius,
                      earthRadius * 2, earthRadius * 2);
    }

    private void drawOrbit() {
        if (trajectory == null || trajectory.isEmpty()) return;

        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);

        boolean first = true;
        double prevX = 0, prevY = 0;

        for (TrajectoryPoint p : trajectory) {
            double x = centerX + p.x * scale / 1000000;
            double y = centerY - p.z * scale / 1000000;

            if (!first) {
                gc.strokeLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
            first = false;
        }
    }

    private void drawSpacecraft() {
        if (trajectory == null || trajectory.isEmpty() || currentIndex >= trajectory.size()) return;

        TrajectoryPoint p = trajectory.get(currentIndex);
        double x = centerX + p.x * scale / 1000000;
        double y = centerY - p.z * scale / 1000000;

        gc.setFill(Color.ORANGE);
        gc.fillOval(x - 4, y - 4, 8, 8);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeOval(x - 6, y - 6, 12, 12);
    }

    private void startAnimationTimer() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (trajectory != null && trajectory.size() > 0) {
                    if (now - lastUpdateTime > UPDATE_INTERVAL_MS * 1_000_000) {
                        draw();
                        currentIndex = (currentIndex + 1) % trajectory.size();
                        lastUpdateTime = now;
                    }
                }
            }
        };
        timer.start();
    }
}