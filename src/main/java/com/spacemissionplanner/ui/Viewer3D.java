package com.spacemissionplanner.ui;

import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

public class Viewer3D extends VBox {

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double VISUAL_SCALE = 1.0 / EARTH_RADIUS_M;

    private SubScene subScene;
    private Group sceneRoot;
    private Group earthGroup;
    private Group orbitGroup;
    private Group spacecraftGroup;

    private Sphere earthSphere;
    private MeshView spacecraftMesh;
    private Cylinder spacecraftBody;
    private Cylinder spacecraftNose;

    private PerspectiveCamera camera;
    private double rotX = 20, rotY = 0;
    private double camDist = 5;
    private double mouseX, mouseY;
    private boolean dragging = false;
    private String target = "earth";

    private Group cameraPivot;
    private Point3D targetPosition = new Point3D(0, 0, 0);
    private Point3D currentTargetPos = new Point3D(0, 0, 0);

    private List<TrajectoryPoint> trajectory;
    private int currentIndex = 0;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 50;

    public Viewer3D() {
        setSpacing(5);
        setFillWidth(true);
        VBox.setVgrow(this, Priority.ALWAYS);
        setMinHeight(400);
        setPrefHeight(450);
        setStyle("-fx-background-color: #001122;");

        init3DScene();
        startAnimationTimer();
    }

    private void updateCamera() {
        cameraPivot.getTransforms().clear();

        Rotate rx = new Rotate(rotX, Rotate.X_AXIS);
        Rotate ry = new Rotate(rotY, Rotate.Y_AXIS);
        cameraPivot.getTransforms().addAll(rx, ry);

        double lerpFactor = 0.15;
        currentTargetPos = new Point3D(
            currentTargetPos.getX() + (targetPosition.getX() - currentTargetPos.getX()) * lerpFactor,
            currentTargetPos.getY() + (targetPosition.getY() - currentTargetPos.getY()) * lerpFactor,
            currentTargetPos.getZ() + (targetPosition.getZ() - currentTargetPos.getZ()) * lerpFactor
        );
        cameraPivot.setTranslateX(currentTargetPos.getX());
        cameraPivot.setTranslateY(currentTargetPos.getY());
        cameraPivot.setTranslateZ(currentTargetPos.getZ());
    }

    public void setTarget(String target) {
        this.target = target;
        camDist = 5;
        updateTargetPosition();
        updateCamera();
    }

    private void updateTargetPosition() {
        switch (target) {
            case "spacecraft":
                if (trajectory != null && !trajectory.isEmpty()) {
                    double maxR = getMaxRadius();
                    double scale = 1.0 / maxR;
                    TrajectoryPoint p = trajectory.get(currentIndex);
                    targetPosition = new Point3D(p.x * scale, p.z * scale, p.y * scale);
                }
                break;
            case "earth":
            default:
                targetPosition = new Point3D(0, 0, 0);
                break;
        }
    }

    private double getMaxRadius() {
        double maxR = EARTH_RADIUS_M;
        if (trajectory != null) {
            for (TrajectoryPoint t : trajectory) {
                double r = Math.sqrt(t.x * t.x + t.y * t.y + t.z * t.z);
                if (r > maxR) maxR = r;
            }
            if (maxR < EARTH_RADIUS_M) maxR = EARTH_RADIUS_M * 1.5;
        }
        return maxR;
    }

    private void init3DScene() {
        sceneRoot = new Group();

        createStarfield();
        createEarth();
        createSpacecraft();
        createOrbitPath();

        AmbientLight ambientLight = new AmbientLight(Color.rgb(60, 60, 80));
        sceneRoot.getChildren().add(ambientLight);

        PointLight sunLight = new PointLight(Color.WHITE);
        sunLight.setTranslateX(10);
        sunLight.setTranslateY(5);
        sunLight.setTranslateZ(10);
        sceneRoot.getChildren().add(sunLight);

        PointLight fillLight = new PointLight(Color.rgb(100, 150, 255));
        fillLight.setTranslateX(-8);
        fillLight.setTranslateY(-3);
        fillLight.setTranslateZ(5);
        sceneRoot.getChildren().add(fillLight);

        cameraPivot = new Group();
        sceneRoot.getChildren().add(cameraPivot);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        camera.setTranslateZ(-camDist);
        cameraPivot.getChildren().add(camera);

        subScene = new SubScene(sceneRoot, 600, 500, true, null);
        subScene.setCamera(camera);
        subScene.setFill(Color.rgb(10, 10, 20));

        widthProperty().addListener((obs, oldVal, newVal) -> 
            subScene.setWidth(newVal.doubleValue()));
        heightProperty().addListener((obs, oldVal, newVal) -> 
            subScene.setHeight(newVal.doubleValue()));

        subScene.setOnMousePressed(e -> {
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            dragging = true;
        });

        subScene.setOnMouseDragged(e -> {
            if (dragging) {
                rotY += (e.getSceneX() - mouseX) * 0.5;
                rotX += (e.getSceneY() - mouseY) * 0.5;
                mouseX = e.getSceneX();
                mouseY = e.getSceneY();
                updateCamera();
            }
        });

        subScene.setOnMouseReleased(e -> dragging = false);

        subScene.setOnScroll(e -> {
            camDist += e.getDeltaY() * 0.01;
            camDist = Math.max(1.5, Math.min(50, camDist));
            camera.setTranslateZ(-camDist);
            e.consume();
        });

        getChildren().add(subScene);
        updateCamera();
    }

    private void createStarfield() {
        for (int i = 0; i < 200; i++) {
            Sphere star = new Sphere(0.02);
            PhongMaterial starMat = new PhongMaterial();
            starMat.setDiffuseColor(Color.WHITE);
            double dist = 20 + Math.random() * 30;
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            star.setTranslateX(dist * Math.sin(phi) * Math.cos(theta));
            star.setTranslateY(dist * Math.sin(phi) * Math.sin(theta));
            star.setTranslateZ(dist * Math.cos(phi));
            sceneRoot.getChildren().add(star);
        }
    }

    private void createEarth() {
        earthGroup = new Group();

        PhongMaterial earthMaterial = new PhongMaterial();
        earthMaterial.setDiffuseColor(Color.rgb(30, 80, 150));
        earthMaterial.setSpecularColor(Color.rgb(60, 60, 80));
        earthMaterial.setSpecularPower(32);

        earthSphere = new Sphere(1.0);
        earthSphere.setMaterial(earthMaterial);
        earthGroup.getChildren().add(earthSphere);

        createLatLonGrid();

        sceneRoot.getChildren().add(earthGroup);
    }

    private void createLatLonGrid() {
        PhongMaterial gridMaterial = new PhongMaterial();
        gridMaterial.setDiffuseColor(Color.rgb(80, 140, 220));
        gridMaterial.setDiffuseColor(Color.rgb(100, 160, 220));
        gridMaterial.setSpecularColor(Color.TRANSPARENT);

        int numLatLines = 12;
        int numLonLines = 18;

        for (int i = 0; i < numLatLines; i++) {
            double lat = -80 + (160.0 / (numLatLines - 1)) * i;
            createLatitudeCircle(lat, gridMaterial);
        }

        for (int i = 0; i < numLonLines; i++) {
            double lon = -180 + (360.0 / numLonLines) * i;
            createLongitudeArc(lon, gridMaterial);
        }
    }

    private void createLatitudeCircle(double lat, PhongMaterial material) {
        int segments = 48;
        double latRad = Math.toRadians(lat);
        double r = Math.cos(latRad);
        double y = Math.sin(latRad);

        float[] points = new float[segments * 6];
        int idx = 0;

        for (int i = 0; i < segments; i++) {
            double lon1 = (2 * Math.PI / segments) * i;
            double lon2 = (2 * Math.PI / segments) * (i + 1);

            points[idx++] = (float)(r * Math.cos(lon1));
            points[idx++] = (float)y;
            points[idx++] = (float)(r * Math.sin(lon1));
            points[idx++] = (float)(r * Math.cos(lon2));
            points[idx++] = (float)y;
            points[idx++] = (float)(r * Math.sin(lon2));
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        for (int i = 0; i < segments; i++) {
            mesh.getTexCoords().addAll(0, 0);
        }
        for (int i = 0; i < segments; i++) {
            mesh.getFaces().addAll(i, 0, (i + 1) % segments, 0, (i + 2) % segments, 0);
        }

        MeshView line = new MeshView(mesh);
        line.setMaterial(material);
        line.setCullFace(null);
        earthGroup.getChildren().add(line);
    }

    private void createLongitudeArc(double lon, PhongMaterial material) {
        int segments = 24;
        double lonRad = Math.toRadians(lon);

        float[] points = new float[(segments + 1) * 3];
        for (int i = 0; i <= segments; i++) {
            double lat = -Math.PI / 2 + (Math.PI / segments) * i;
            points[i * 3] = (float)(Math.cos(lat) * Math.cos(lonRad));
            points[i * 3 + 1] = (float)Math.sin(lat);
            points[i * 3 + 2] = (float)(Math.cos(lat) * Math.sin(lonRad));
        }

        float[] vertices = new float[segments * 6];
        int idx = 0;
        for (int i = 0; i < segments; i++) {
            vertices[idx++] = points[i * 3];
            vertices[idx++] = points[i * 3 + 1];
            vertices[idx++] = points[i * 3 + 2];
            vertices[idx++] = points[(i + 1) * 3];
            vertices[idx++] = points[(i + 1) * 3 + 1];
            vertices[idx++] = points[(i + 1) * 3 + 2];
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(vertices);
        for (int i = 0; i < segments; i++) {
            mesh.getTexCoords().addAll(0, 0);
        }
        for (int i = 0; i < segments; i++) {
            mesh.getFaces().addAll(i, 0, i + 1, 0, (i + 2) % (segments + 1), 0);
        }

        MeshView line = new MeshView(mesh);
        line.setMaterial(material);
        line.setCullFace(null);
        earthGroup.getChildren().add(line);
    }

    private void createSpacecraft() {
        spacecraftGroup = new Group();

        PhongMaterial bodyMat = new PhongMaterial();
        bodyMat.setDiffuseColor(Color.rgb(200, 200, 210));
        bodyMat.setSpecularColor(Color.WHITE);
        bodyMat.setSpecularPower(64);

        PhongMaterial noseMat = new PhongMaterial();
        noseMat.setDiffuseColor(Color.rgb(255, 100, 50));
        noseMat.setSpecularColor(Color.WHITE);
        noseMat.setSpecularPower(32);

        spacecraftBody = new Cylinder(0.05, 0.15);
        spacecraftBody.setMaterial(bodyMat);
        spacecraftBody.setRotationAxis(Rotate.X_AXIS);
        spacecraftBody.setRotate(90);

        spacecraftNose = new Cylinder(0.0, 0.08, 12);
        spacecraftNose.setMaterial(noseMat);
        spacecraftNose.setTranslateY(-0.10);
        spacecraftNose.setRotationAxis(Rotate.X_AXIS);
        spacecraftNose.setRotate(90);

        spacecraftGroup.getChildren().addAll(spacecraftBody, spacecraftNose);
        spacecraftGroup.setVisible(false);

        sceneRoot.getChildren().add(spacecraftGroup);
    }

    private void createOrbitPath() {
        orbitGroup = new Group();
        sceneRoot.getChildren().add(orbitGroup);
    }

    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        this.trajectory = trajectory;
        this.currentIndex = 0;

        if (trajectory == null || trajectory.isEmpty()) {
            spacecraftGroup.setVisible(false);
            orbitGroup.getChildren().clear();
            return;
        }

        double maxR = 0;
        for (TrajectoryPoint p : trajectory) {
            double r = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
            if (r > maxR) maxR = r;
        }

        if (maxR < EARTH_RADIUS_M) {
            maxR = EARTH_RADIUS_M * 1.5;
        }

        double scale = 1.0 / maxR;

        orbitGroup.getChildren().clear();
        PhongMaterial orbitMaterial = new PhongMaterial();
        orbitMaterial.setDiffuseColor(Color.CYAN);
        orbitMaterial.setSpecularColor(Color.TRANSPARENT);

        int numSegments = Math.min(trajectory.size() - 1, 360);
        int step = Math.max(1, trajectory.size() / numSegments);

        List<double[]> lineSegments = new ArrayList<>();

        for (int i = 0; i < trajectory.size() - 1; i += step) {
            TrajectoryPoint p1 = trajectory.get(i);
            TrajectoryPoint p2 = trajectory.get(Math.min(i + step, trajectory.size() - 1));

            double x1 = p1.x * scale;
            double y1 = p1.z * scale;
            double z1 = p1.y * scale;

            double x2 = p2.x * scale;
            double y2 = p2.z * scale;
            double z2 = p2.y * scale;

            lineSegments.add(new double[]{x1, y1, z1, x2, y2, z2});
        }

        for (double[] seg : lineSegments) {
            createLineSegment(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5], orbitMaterial);
        }

        spacecraftGroup.setVisible(true);
        updateSpacecraftPosition();
    }

    private void createLineSegment(double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   PhongMaterial material) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length < 0.0001) return;

        Cylinder line = new Cylinder(0.01, length, 8);
        line.setMaterial(material);

        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;
        double mz = (z1 + z2) / 2;

        line.setTranslateX(mx);
        line.setTranslateY(my);
        line.setTranslateZ(mz);

        double rx = Math.toDegrees(Math.acos(dy / length));
        double ry = Math.toDegrees(Math.atan2(dx, dz));

        line.setRotationAxis(Rotate.Y_AXIS);
        line.setRotate(ry);
        line.setRotationAxis(Rotate.X_AXIS);
        line.setRotate(-rx);

        orbitGroup.getChildren().add(line);
    }

    private void updateSpacecraftPosition() {
        if (trajectory == null || trajectory.isEmpty()) return;

        double maxR = 0;
        for (TrajectoryPoint t : trajectory) {
            double r = Math.sqrt(t.x * t.x + t.y * t.y + t.z * t.z);
            if (r > maxR) maxR = r;
        }
        if (maxR < EARTH_RADIUS_M) maxR = EARTH_RADIUS_M * 1.5;
        double scale = 1.0 / maxR;

        TrajectoryPoint p = trajectory.get(currentIndex);
        spacecraftGroup.setTranslateX(p.x * scale);
        spacecraftGroup.setTranslateY(p.z * scale);
        spacecraftGroup.setTranslateZ(p.y * scale);

        if (currentIndex < trajectory.size() - 1) {
            TrajectoryPoint next = trajectory.get(currentIndex + 1);
            double dx = next.x - p.x;
            double dy = next.y - p.y;
            double dz = next.z - p.z;

            double yaw = Math.toDegrees(Math.atan2(dx, dz));
            double pitch = Math.toDegrees(Math.atan2(Math.sqrt(dx * dx + dz * dz), -dy)) - 90;

            spacecraftGroup.setRotationAxis(Rotate.Y_AXIS);
            spacecraftGroup.setRotate(yaw);
            spacecraftGroup.setRotationAxis(Rotate.X_AXIS);
            spacecraftGroup.setRotate(pitch);
        }
    }

    private void startAnimationTimer() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (trajectory != null && trajectory.size() > 0) {
                    if (now - lastUpdateTime > UPDATE_INTERVAL_MS * 1_000_000) {
                        updateSpacecraftPosition();
                        currentIndex = (currentIndex + 1) % trajectory.size();
                        lastUpdateTime = now;
                    }
                }

                if (target.equals("spacecraft")) {
                    updateTargetPosition();
                }

                updateCamera();
            }
        };
        timer.start();
    }
}