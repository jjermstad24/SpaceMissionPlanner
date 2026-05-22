package com.spacemissionplanner.ui;

import com.spacemissionplanner.model.CelestialBody;
import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import com.spacemissionplanner.util.ErrorHandler;
import javafx.animation.AnimationTimer;
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
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

import javafx.scene.shape.Box;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class Viewer3D extends VBox {

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double VISUAL_SCALE = 1.0 / EARTH_RADIUS_M;

    private CelestialBody currentBody = CelestialBody.EARTH;
    private double bodyRadiusM = EARTH_RADIUS_M;
    private double visualScale = VISUAL_SCALE;

    private SubScene subScene;
    private Group sceneRoot;
    private Group earthGroup;
    private Group moonGroup;
    private Group orbitGroup;
    private Group spacecraftGroup;
    private Group bodyTrailGroup;

    private Sphere earthSphere;
    private Sphere moonSphere;
    private PerspectiveCamera camera;
    private Affine cameraOrient = new Affine();
    private double camDist = 5;
    private double mouseX, mouseY;
    private boolean dragging = false;
    private String target = "earth";

    private Group cameraPivot;
    private Point3D targetPosition = new Point3D(0, 0, 0);
    private Point3D currentTargetPos = new Point3D(0, 0, 0);

    private List<TrajectoryPoint> trajectory;
    private List<List<TrajectoryPoint>> groups = new ArrayList<>();
    private List<Color> groupColors = new ArrayList<>();
    private int currentIndex = 0;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 50;
    private OrekitService orekitService;
    private boolean isPlaying = true;
    private double playbackSpeed = 1.0;
    private Slider timelineSlider;
    private boolean scrubbing = false;

    public Viewer3D() {
        setFillWidth(true);
        VBox.setVgrow(this, Priority.ALWAYS);
        setMinHeight(400);
        setPrefHeight(450);
        setStyle("-fx-background-color: #001122;");

        init3DScene();

        BorderPane content = new BorderPane();
        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(subScene);
        content.setCenter(centerWrapper);
        subScene.widthProperty().bind(centerWrapper.widthProperty());
        subScene.heightProperty().bind(centerWrapper.heightProperty());
        HBox controls = initPlaybackControls();
        content.setBottom(controls);
        getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);

        startAnimationTimer();
    }

    private void updateCamera() {
        cameraPivot.getTransforms().setAll(cameraOrient);

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
        updateTargetPosition();
        updateCamera();
    }

    private void updateTargetPosition() {
        switch (target) {
            case "spacecraft":
                if (trajectory != null && !trajectory.isEmpty()) {
                    TrajectoryPoint p = trajectory.get(currentIndex);
                    targetPosition = new Point3D(p.x * visualScale, p.z * visualScale, p.y * visualScale);
                }
                break;
            case "moon":
                if (moonGroup != null) {
                    targetPosition = new Point3D(
                        moonGroup.getTranslateX(),
                        moonGroup.getTranslateY(),
                        moonGroup.getTranslateZ()
                    );
                }
                break;
            case "earth":
            default:
                targetPosition = new Point3D(0, 0, 0);
                break;
        }
    }

    private void init3DScene() {
        sceneRoot = new Group();

        createStarfield();
        createEarth();
        createMoon();
        createSpacecraft();
        createOrbitPath();
        createBodyTrails();

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

        cameraOrient.appendRotation(20, 0, 0, 0, new Point3D(1, 0, 0));

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

        subScene.setOnMousePressed(e -> {
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            dragging = true;
        });

        subScene.setOnMouseDragged(e -> {
            if (dragging) {
                double dx = e.getSceneX() - mouseX;
                double dy = e.getSceneY() - mouseY;
                mouseX = e.getSceneX();
                mouseY = e.getSceneY();

                Point3D up = cameraOrient.transform(0, 1, 0);
                cameraOrient.prependRotation(dx * 0.3, 0, 0, 0, up);

                Point3D right = cameraOrient.transform(1, 0, 0);
                cameraOrient.prependRotation(-dy * 0.3, 0, 0, 0, right);

                updateCamera();
            }
        });

        subScene.setOnMouseReleased(e -> dragging = false);

        subScene.setOnScroll(e -> {
            camDist += e.getDeltaY() * 0.05;
            camDist = Math.max(1.5, Math.min(500, camDist));
            camera.setTranslateZ(-camDist);
            e.consume();
        });

        updateCamera();
    }

    public void setCelestialBody(CelestialBody body) {
        this.currentBody = body;
        this.bodyRadiusM = body.getMeanRadius();
        this.visualScale = 1.0 / bodyRadiusM;
        if (trajectory != null && !trajectory.isEmpty()) {
            setTrajectoryGroups(groups, groupColors);
        }
    }

    public void setOrekitService(OrekitService service) {
        this.orekitService = service;
    }

    public void setEarthVisible(boolean visible) {
        if (earthGroup != null) earthGroup.setVisible(visible);
    }

    public void setMoonVisible(boolean visible) {
        if (moonGroup != null) moonGroup.setVisible(visible);
    }

    private void createMoon() {
        moonGroup = new Group();
        PhongMaterial moonMaterial = new PhongMaterial();
        moonMaterial.setDiffuseColor(Color.rgb(200, 200, 200));
        moonMaterial.setSpecularColor(Color.rgb(80, 80, 80));
        moonMaterial.setSpecularPower(16);
        moonSphere = new Sphere(1737400.0 * visualScale);
        moonSphere.setMaterial(moonMaterial);
        moonGroup.getChildren().add(moonSphere);
        sceneRoot.getChildren().add(moonGroup);
    }

    public void setMoonPosition(org.hipparchus.geometry.euclidean.threed.Vector3D posEME2000) {
        if (posEME2000 != null && moonGroup != null) {
            moonGroup.setTranslateX(posEME2000.getX() * visualScale);
            moonGroup.setTranslateY(posEME2000.getZ() * visualScale);
            moonGroup.setTranslateZ(posEME2000.getY() * visualScale);
        }
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
        if (earthGroup != null) {
            sceneRoot.getChildren().remove(earthGroup);
        }
        earthGroup = new Group();

        Image earthTexture = new Image(
            getClass().getResourceAsStream("/com/spacemissionplanner/ui/earth_texture.jpg")
        );

        PhongMaterial earthMaterial = new PhongMaterial();
        earthMaterial.setDiffuseMap(earthTexture);
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
        gridMaterial.setDiffuseColor(Color.rgb(120, 200, 255, 0.25));
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

        Cylinder spacecraftBody = new Cylinder(0.05, 0.15);
        spacecraftBody.setMaterial(bodyMat);
        spacecraftBody.setRotationAxis(Rotate.X_AXIS);
        spacecraftBody.setRotate(90);

        Cylinder spacecraftNose = new Cylinder(0.0, 0.08, 12);
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

    private void createBodyTrails() {
        bodyTrailGroup = new Group();
        sceneRoot.getChildren().add(bodyTrailGroup);
    }

    public void setBodyTrail(CelestialBody body, List<TrajectoryPoint> trail) {
        // remove old trail for this body
        bodyTrailGroup.getChildren().removeIf(n -> {
            Object b = n.getProperties().get("body");
            return b != null && b.equals(body);
        });
        if (trail == null || trail.isEmpty()) return;

        PhongMaterial mat = new PhongMaterial();
        Color color = body == CelestialBody.MOON ? Color.rgb(200, 200, 200, 0.6) : Color.rgb(60, 120, 255, 0.4);
        mat.setDiffuseColor(color);
        mat.setSpecularColor(Color.TRANSPARENT);

        Group group = new Group();
        group.getProperties().put("body", body);

        for (int i = 0; i < trail.size() - 1; i++) {
            TrajectoryPoint p1 = trail.get(i);
            TrajectoryPoint p2 = trail.get(i + 1);
            double x1 = p1.x * visualScale;
            double y1 = p1.z * visualScale;
            double z1 = p1.y * visualScale;
            double x2 = p2.x * visualScale;
            double y2 = p2.z * visualScale;
            double z2 = p2.y * visualScale;
            double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.0001) continue;

            Box seg = new Box(0.01, 0.01, len);
            seg.setMaterial(mat);
            seg.setTranslateX((x1 + x2) / 2);
            seg.setTranslateY((y1 + y2) / 2);
            seg.setTranslateZ((z1 + z2) / 2);
            Point3D dir = new Point3D(dx / len, dy / len, dz / len);
            Point3D zAxis = new Point3D(0, 0, 1);
            double dot = zAxis.dotProduct(dir);
            if (dot < 0.9999) {
                seg.setRotationAxis(zAxis.crossProduct(dir));
                seg.setRotate(Math.toDegrees(Math.acos(dot)));
            }
            group.getChildren().add(seg);
        }
        bodyTrailGroup.getChildren().add(group);
    }

    public void setBodyTrailVisible(CelestialBody body, boolean visible) {
        for (var n : bodyTrailGroup.getChildren()) {
            if (body.equals(n.getProperties().get("body"))) {
                n.setVisible(visible);
            }
        }
    }

    public void setTrajectory(List<TrajectoryPoint> trajectory) {
        List<List<TrajectoryPoint>> singleGroup = new ArrayList<>();
        singleGroup.add(trajectory);
        List<Color> colors = new ArrayList<>();
        colors.add(Color.CYAN);
        setTrajectoryGroups(singleGroup, colors);
    }

    public void setTrajectoryGroups(List<List<TrajectoryPoint>> groups, List<Color> colors) {
        this.groups = groups;
        this.groupColors = colors;

        List<TrajectoryPoint> flat = new ArrayList<>();
        for (List<TrajectoryPoint> group : groups) {
            flat.addAll(group);
        }
        this.trajectory = flat;
        this.currentIndex = 0;

        if (timelineSlider != null && trajectory != null && !trajectory.isEmpty()) {
            timelineSlider.setMax(trajectory.size() - 1);
            timelineSlider.setValue(0);
        }

        if (trajectory == null || trajectory.isEmpty()) {
            spacecraftGroup.setVisible(false);
            orbitGroup.getChildren().clear();
            return;
        }

        double maxR = bodyRadiusM;
        for (TrajectoryPoint p : trajectory) {
            double r = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
            if (r > maxR) maxR = r;
        }

        // Only auto-adjust camera distance for the first trajectory load
        if (orbitGroup.getChildren().isEmpty()) {
            double initCamDist = Math.max(maxR / bodyRadiusM * 2.5, 4);
            camDist = Math.min(initCamDist, 120);
            camera.setTranslateZ(-camDist);
        }

        orbitGroup.getChildren().clear();

        for (int g = 0; g < groups.size(); g++) {
            List<TrajectoryPoint> group = groups.get(g);
            Color color = colors.get(Math.min(g, colors.size() - 1));

            PhongMaterial orbitMaterial = new PhongMaterial();
            orbitMaterial.setDiffuseColor(color);
            orbitMaterial.setSpecularColor(Color.TRANSPARENT);

            int numSegments = Math.min(group.size() - 1, 360);
            int step = Math.max(1, group.size() / numSegments);

            for (int i = 0; i < group.size() - 1; i += step) {
                TrajectoryPoint p1 = group.get(i);
                TrajectoryPoint p2 = group.get(Math.min(i + step, group.size() - 1));

                double x1 = p1.x * visualScale;
                double y1 = p1.z * visualScale;
                double z1 = p1.y * visualScale;

                double x2 = p2.x * visualScale;
                double y2 = p2.z * visualScale;
                double z2 = p2.y * visualScale;

                createLineSegment(x1, y1, z1, x2, y2, z2, orbitMaterial);
            }
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

        Box line = new Box(0.005, 0.005, length);
        line.setMaterial(material);

        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;
        double mz = (z1 + z2) / 2;

        line.setTranslateX(mx);
        line.setTranslateY(my);
        line.setTranslateZ(mz);

        double dlen = length;
        Point3D dir = new Point3D(dx / dlen, dy / dlen, dz / dlen);
        Point3D zAxis = new Point3D(0, 0, 1);
        double dot = zAxis.dotProduct(dir);
        if (dot < 0.9999) {
            Point3D rotAxis = zAxis.crossProduct(dir);
            double angle = Math.toDegrees(Math.acos(dot));
            line.setRotationAxis(rotAxis);
            line.setRotate(angle);
        }

        orbitGroup.getChildren().add(line);
    }

    private void updateSpacecraftPosition() {
        if (trajectory == null || trajectory.isEmpty()) return;

        TrajectoryPoint p = trajectory.get(currentIndex);
        spacecraftGroup.setTranslateX(p.x * visualScale);
        spacecraftGroup.setTranslateY(p.z * visualScale);
        spacecraftGroup.setTranslateZ(p.y * visualScale);

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
                    double intervalMs = UPDATE_INTERVAL_MS / playbackSpeed;
                    if (isPlaying && now - lastUpdateTime > intervalMs * 1_000_000) {
                        updateSpacecraftPosition();
                        currentIndex = (currentIndex + 1) % trajectory.size();
                        lastUpdateTime = now;

                        if (!scrubbing && timelineSlider != null) {
                            timelineSlider.setValue(currentIndex);
                        }

                        TrajectoryPoint p = trajectory.get(currentIndex);
                        if (orekitService != null && p.date != null) {
                            ErrorHandler.runSafeSilent(() ->
                                setMoonPosition(orekitService.getMoonPosition(p.date))
                            );
                        }
                    }
                }

                if (target.equals("spacecraft") || target.equals("moon")) {
                    updateTargetPosition();
                }

                updateCamera();
            }
        };
        timer.start();
    }

    private HBox initPlaybackControls() {
        HBox controls = new HBox(8);
        controls.setStyle("-fx-padding: 5 8; -fx-background-color: #2a3a4a; -fx-border-color: #0a1a2a; -fx-border-width: 1 0 0 0;");
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button playBtn = new Button("||");
        playBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #334; -fx-text-fill: white;");
        playBtn.setOnAction(e -> {
            isPlaying = !isPlaying;
            playBtn.setText(isPlaying ? "||" : "\u25B6");
            lastUpdateTime = System.nanoTime();
        });

        timelineSlider = new Slider(0, 1, 0);
        timelineSlider.setPrefWidth(300);
        timelineSlider.setStyle("-fx-control-inner-background: #445;");
        timelineSlider.setOnMousePressed(e -> scrubbing = true);
        timelineSlider.setOnMouseReleased(e -> {
            scrubbing = false;
            if (trajectory != null && !trajectory.isEmpty()) {
                currentIndex = Math.max(0, Math.min(trajectory.size() - 1, (int) (timelineSlider.getValue())));
                updateSpacecraftPosition();
                TrajectoryPoint p = trajectory.get(currentIndex);
                if (orekitService != null && p.date != null) {
                    ErrorHandler.runSafeSilent(() ->
                        setMoonPosition(orekitService.getMoonPosition(p.date))
                    );
                }
            }
        });
        timelineSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (scrubbing && trajectory != null && !trajectory.isEmpty()) {
                currentIndex = Math.max(0, Math.min(trajectory.size() - 1, newVal.intValue()));
                updateSpacecraftPosition();
            }
        });

        Slider speedSlider = new Slider(0.1, 5.0, 1.0);
        speedSlider.setPrefWidth(120);
        speedSlider.setStyle("-fx-control-inner-background: #445;");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playbackSpeed = newVal.doubleValue();
        });

        javafx.scene.control.Label speedLabel = new javafx.scene.control.Label("1.0x");
        speedLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            speedLabel.setText(String.format("%.1fx", newVal.doubleValue()));
        });

        controls.getChildren().addAll(playBtn, timelineSlider, speedSlider, speedLabel);
        return controls;
    }
}
