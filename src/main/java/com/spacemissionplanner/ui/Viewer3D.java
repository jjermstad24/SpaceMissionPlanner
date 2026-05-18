package com.spacemissionplanner.ui;

import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

public class Viewer3D extends VBox {

    private static final double EARTH_RADIUS = 6371e3;
    private static final double SCALE = 1e-7;

    private Canvas canvas;
    private GraphicsContext gc;

    private List<TrajectoryPoint> trajectory;
    private int currentIndex = 0;

    private double rotX = 20;
    private double rotY = 0;
    private double cameraZ = 4.0;

    private Point3D[] projectedOrbit;
    private Point3D projectedSpacecraft;

    public Viewer3D() {
        setSpacing(5);
        setFillWidth(true);
        VBox.setVgrow(this, Priority.ALWAYS);

        canvas = new Canvas(600, 500);
        gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        // Animation thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50);
                    if (trajectory != null && trajectory.size() > 0) {
                        updatePosition();
                        draw();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        this.trajectory = trajectory;
        this.currentIndex = 0;

        if (trajectory == null || trajectory.isEmpty()) {
            return;
        }

        // Calculate scale
        double maxR = 0;
        for (TrajectoryPoint p : trajectory) {
            double r = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
            if (r > maxR) maxR = r;
        }
        double scale = (EARTH_RADIUS * SCALE * 3) / maxR;

        // Project orbit points
        projectedOrbit = new Point3D[trajectory.size()];
        for (int i = 0; i < trajectory.size(); i++) {
            TrajectoryPoint p = trajectory.get(i);
            projectedOrbit[i] = project(p.x * scale, p.z * scale, p.y * scale);
        }

        updatePosition();
        draw();
    }

    private void updatePosition() {
        if (trajectory == null || trajectory.size() == 0 || projectedOrbit == null) return;

        TrajectoryPoint p = trajectory.get(currentIndex);
        double maxR = 0;
        for (TrajectoryPoint t : trajectory) {
            double r = Math.sqrt(t.x * t.x + t.y * t.y + t.z * t.z);
            if (r > maxR) maxR = r;
        }
        double scale = (EARTH_RADIUS * SCALE * 3) / maxR;

        projectedSpacecraft = project(p.x * scale, p.z * scale, p.y * scale);

        currentIndex = (currentIndex + 1) % trajectory.size();
    }

    private Point3D project(double x, double y, double z) {
        // Rotate around X axis
        double radX = Math.toRadians(rotX);
        double y1 = y * Math.cos(radX) - z * Math.sin(radX);
        double z1 = y * Math.sin(radX) + z * Math.cos(radX);

        // Rotate around Y axis
        double radY = Math.toRadians(rotY);
        double x2 = x * Math.cos(radY) + z1 * Math.sin(radY);
        double z2 = -x * Math.sin(radY) + z1 * Math.cos(radY);

        // Perspective projection
        double fov = 300;
        double depth = cameraZ - z2;
        if (depth <= 0) depth = 0.01;

        double scale = fov / depth;
        double px = x2 * scale + canvas.getWidth() / 2;
        double py = -y1 * scale + canvas.getHeight() / 2;

        return new Point3D(px, py, depth);
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Clear with dark background
        gc.setFill(Color.rgb(13, 13, 26));
        gc.fillRect(0, 0, w, h);

        // Draw Earth (wireframe circle)
        Point3D earthCenter = project(0, 0, 0);
        double earthRadius = (float)(EARTH_RADIUS * SCALE) * 200;  // visual size

        gc.setStroke(Color.rgb(50, 100, 200));
        gc.setLineWidth(1);
        gc.strokeOval(earthCenter.getX() - earthRadius/2, earthCenter.getY() - earthRadius/2, earthRadius, earthRadius);

        // Draw orbit
        if (projectedOrbit != null && projectedOrbit.length > 0) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.beginPath();

            boolean first = true;
            for (Point3D p : projectedOrbit) {
                if (first) {
                    gc.moveTo(p.getX(), p.getY());
                    first = false;
                } else {
                    gc.lineTo(p.getX(), p.getY());
                }
            }
            gc.stroke();
        }

        // Draw spacecraft
        if (projectedSpacecraft != null) {
            gc.setFill(Color.WHITE);
            double size = 6 + 100 / projectedSpacecraft.getZ();
            gc.fillOval(projectedSpacecraft.getX() - size/2, projectedSpacecraft.getY() - size/2, size, size);
        }
    }

    public void rotateX(float delta) {
        rotX += delta;
    }

    public void rotateY(float delta) {
        rotY += delta;
    }

    public void resetView() {
        rotX = 20;
        rotY = 0;
        draw();
    }
}