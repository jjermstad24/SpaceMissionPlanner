package com.spacemissionplanner.ui;

import com.spacemissionplanner.model.CelestialBody;
import com.spacemissionplanner.model.Coast;
import com.spacemissionplanner.model.Maneuver;
import com.spacemissionplanner.model.MissionEvent;
import com.spacemissionplanner.model.Waypoint;
import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.geometry.Pos;

import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.List;
import java.util.ArrayList;

public class MainController {

    @FXML private ComboBox<String> cbWaypointFrame;
    @FXML private ComboBox<String> cbWaypointBody;
    @FXML private Label lblOrbit1;
    @FXML private Label lblOrbit2;
    @FXML private Label lblOrbit3;
    @FXML private Label lblOrbit4;
    @FXML private Label lblOrbit5;
    @FXML private Label lblOrbit6;
    @FXML private TextField tfOrbit1;
    @FXML private TextField tfOrbit2;
    @FXML private TextField tfOrbit3;
    @FXML private TextField tfOrbit4;
    @FXML private TextField tfOrbit5;
    @FXML private TextField tfOrbit6;

    @FXML private TextField tfDvx;
    @FXML private TextField tfDvy;
    @FXML private TextField tfDvz;
    @FXML private ComboBox<String> cbManeuverFrame;
    @FXML private Label lblDvx;
    @FXML private Label lblDvy;
    @FXML private Label lblDvz;

    @FXML private ListView<MissionEvent> timelineList;
    @FXML private TabPane viewerTabs;
    @FXML private VBox tab3DContent;
    @FXML private VBox tab2DContent;
    @FXML private VBox tabBodiesContent;
    @FXML private Label statusLabel;
    @FXML private Label inputPanelTitle;
    @FXML private VBox orbitInputPanel;
    @FXML private VBox maneuverInputPanel;
    @FXML private VBox coastInputPanel;

    @FXML private TextField tfCoastDuration;
    @FXML private TextField tfCoastStepSize;
    @FXML private ColorPicker eventColorPicker;
    @FXML private ComboBox<String> cbCameraTarget;
    @FXML private ComboBox<String> cbEpochFormat;
    @FXML private TextField tfEpochValue;

    private static final String[] COLOR_PALETTE = {
        "#00FFFF", "#FF4444", "#44FF44", "#FFFF44",
        "#FF44FF", "#FF8844", "#8844FF", "#44FFFF",
        "#FF6688", "#88FF66"
    };

    private OrekitService physicsService;
    private ObservableList<MissionEvent> events;
    private Viewer3D viewer3D;
    private Viewer2D viewer2D;

    @FXML
    private void initialize() {
        physicsService = new OrekitService();
        events = FXCollections.observableArrayList();

        Waypoint defaultWaypoint = new Waypoint("Waypoint 1", 6871, 0.0, 45.0, 0.0, 0.0, 0.0);
        defaultWaypoint.setColorHex(COLOR_PALETTE[0]);
        events.add(defaultWaypoint);

        timelineList.setItems(events);
        timelineList.setCellFactory(new Callback<ListView<MissionEvent>, ListCell<MissionEvent>>() {
            @Override
            public ListCell<MissionEvent> call(ListView<MissionEvent> param) {
                return new ListCell<MissionEvent>() {
                    private final Rectangle colorRect = new Rectangle(12, 12);

                    @Override
                    protected void updateItem(MissionEvent item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            String prefix = item instanceof Waypoint ? "[W] "
                                : item instanceof Maneuver ? "[M] "
                                : item instanceof Coast ? "[C] " : "";
                            colorRect.setFill(Color.web(item.getColorHex()));
                            setGraphic(colorRect);
                            setText(prefix + item.toString());
                        }
                    }
                };
            }
        });

        timelineList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                if (oldVal instanceof Waypoint) {
                    saveCurrentWaypointForm((Waypoint) oldVal);
                } else if (oldVal instanceof Maneuver) {
                    updateManeuverFromForm((Maneuver) oldVal);
                } else if (oldVal instanceof Coast) {
                    updateCoastFromForm((Coast) oldVal);
                }
            }
            if (newVal != null) {
                onEventSelected(newVal);
            }
        });

        timelineList.setFixedCellSize(30);
        timelineList.setOnDragDetected(event -> {
            MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = timelineList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(events.indexOf(selected)));
                db.setContent(cc);
                event.consume();
            }
        });
        timelineList.setOnDragOver(event -> {
            if (event.getGestureSource() == timelineList && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        timelineList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int fromIdx = Integer.parseInt(db.getString());
                int toIdx = (int)(event.getY() / timelineList.getFixedCellSize() + 0.5);
                toIdx = Math.max(0, Math.min(events.size() - 1, toIdx));
                if (fromIdx != toIdx) {
                    MissionEvent item = events.remove(fromIdx);
                    events.add(toIdx, item);
                    timelineList.getSelectionModel().select(toIdx);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        timelineList.getSelectionModel().select(0);

        eventColorPicker.setOnAction(e -> {
            MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Color c = eventColorPicker.getValue();
                selected.setColorHex(String.format("#%02X%02X%02X",
                    (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255)));
                timelineList.refresh();
            }
        });

        cbWaypointFrame.getItems().addAll("Orbital Elements", "Inertial (EME2000)", "ECEF (ITRF)", "Geodetic (LLA)");
        cbWaypointFrame.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateOrbitLabels(newVal);
            }
        });

        cbWaypointBody.getItems().addAll("Earth", "Moon");
        cbWaypointBody.getSelectionModel().select(0);

        cbEpochFormat.getItems().addAll("UTC", "Julian Date", "MJD");
        cbEpochFormat.getSelectionModel().select(0);
        tfEpochValue.setText("2024-01-15T12:00:00");

        cbCameraTarget.getItems().addAll("Earth", "Moon", "Spacecraft");
        cbCameraTarget.getSelectionModel().select(0);
        cbCameraTarget.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                viewer3D.setTarget(newVal.toLowerCase());
                statusLabel.setText("Camera target: " + newVal);
            }
        });

        cbManeuverFrame.getItems().addAll("EME2000 (Inertial)", "LVLH (RTN)");
        cbManeuverFrame.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateManeuverLabels(newVal);
            }
        });

        viewer3D = new Viewer3D();
        viewer3D.setOrekitService(physicsService);
        tab3DContent.getChildren().add(0, viewer3D);

        viewer2D = new Viewer2D();
        tab2DContent.getChildren().add(viewer2D);

        initBodiesTab();
    }

    private CelestialBody inertialBody = CelestialBody.EARTH;

    private void initBodiesTab() {
        ToggleGroup inertialGroup = new ToggleGroup();
        for (CelestialBody body : CelestialBody.values()) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            CheckBox visCb = new CheckBox();
            visCb.setSelected(true);
            visCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (body == CelestialBody.EARTH) {
                    viewer3D.setEarthVisible(newVal);
                } else if (body == CelestialBody.MOON) {
                    viewer3D.setMoonVisible(newVal);
                }
            });

            Label nameLabel = new Label(body.getDisplayName());
            nameLabel.setPrefWidth(60);

            CheckBox trailCb = new CheckBox();
            trailCb.setSelected(true);
            trailCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                viewer3D.setBodyTrailVisible(body, newVal);
            });

            Label trailLabel = new Label("Trail");
            trailLabel.setStyle("-fx-font-size: 10;");

            CheckBox gravityCb = new CheckBox();
            gravityCb.setSelected(true);
            gravityCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                physicsService.setGravityEnabled(body, newVal);
                statusLabel.setText((newVal ? "Enabled" : "Disabled") + " gravity for " + body.getDisplayName());
            });

            Label gravityLabel = new Label("Grav");
            gravityLabel.setStyle("-fx-font-size: 10;");

            RadioButton inertialRb = new RadioButton("Inertial");
            inertialRb.setToggleGroup(inertialGroup);
            inertialRb.setStyle("-fx-font-size: 10;");
            if (body == CelestialBody.EARTH) {
                inertialRb.setSelected(true);
                inertialBody = CelestialBody.EARTH;
            }
            int bodyIdx = body.ordinal();
            inertialRb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    inertialBody = CelestialBody.values()[bodyIdx];
                    statusLabel.setText("Inertial body: " + inertialBody.getDisplayName());
                }
            });

            row.getChildren().addAll(visCb, nameLabel, trailCb, trailLabel, gravityCb, gravityLabel, inertialRb);
            tabBodiesContent.getChildren().add(row);
        }
    }

    private AbsoluteDate parseEpoch() {
        String format = cbEpochFormat.getSelectionModel().getSelectedItem();
        String val = tfEpochValue.getText();
        try {
            if ("Julian Date".equals(format)) {
                double jd = Double.parseDouble(val);
                double secFromJ2000 = (jd - 2451545.0) * 86400.0;
                return AbsoluteDate.J2000_EPOCH.shiftedBy(secFromJ2000);
            } else if ("MJD".equals(format)) {
                double mjd = Double.parseDouble(val);
                double secFromJ2000 = (mjd - 51544.5) * 86400.0;
                return AbsoluteDate.J2000_EPOCH.shiftedBy(secFromJ2000);
            } else {
                return new AbsoluteDate(val, TimeScalesFactory.getUTC());
            }
        } catch (Exception e) {
            statusLabel.setText("Invalid epoch: " + e.getMessage());
            return AbsoluteDate.J2000_EPOCH;
        }
    }

    private void saveCurrentWaypointForm(Waypoint wp) {
        String frame = wp.getReferenceFrame();
        switch (frame) {
            case "INERTIAL":
                wp.setInertialX(Double.parseDouble(tfOrbit1.getText()));
                wp.setInertialY(Double.parseDouble(tfOrbit2.getText()));
                wp.setInertialZ(Double.parseDouble(tfOrbit3.getText()));
                wp.setInertialVx(Double.parseDouble(tfOrbit4.getText()));
                wp.setInertialVy(Double.parseDouble(tfOrbit5.getText()));
                wp.setInertialVz(Double.parseDouble(tfOrbit6.getText()));
                break;
            case "ECEF":
                wp.setEcefX(Double.parseDouble(tfOrbit1.getText()));
                wp.setEcefY(Double.parseDouble(tfOrbit2.getText()));
                wp.setEcefZ(Double.parseDouble(tfOrbit3.getText()));
                wp.setEcefVx(Double.parseDouble(tfOrbit4.getText()));
                wp.setEcefVy(Double.parseDouble(tfOrbit5.getText()));
                wp.setEcefVz(Double.parseDouble(tfOrbit6.getText()));
                break;
            case "LLA":
                wp.setLatitude(Double.parseDouble(tfOrbit1.getText()));
                wp.setLongitude(Double.parseDouble(tfOrbit2.getText()));
                wp.setAltitude(Double.parseDouble(tfOrbit3.getText()));
                break;
            default:
                wp.setSemiMajorAxis(Double.parseDouble(tfOrbit1.getText()));
                wp.setEccentricity(Double.parseDouble(tfOrbit2.getText()));
                wp.setInclination(Double.parseDouble(tfOrbit3.getText()));
                wp.setRaan(Double.parseDouble(tfOrbit4.getText()));
                wp.setArgPeriapsis(Double.parseDouble(tfOrbit5.getText()));
                wp.setTrueAnomaly(Double.parseDouble(tfOrbit6.getText()));
                break;
        }
        timelineList.refresh();
    }

    private void saveCurrentEvent() {
        MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (selected instanceof Waypoint) {
            updateWaypointFromForm((Waypoint) selected);
        } else if (selected instanceof Maneuver) {
            updateManeuverFromForm((Maneuver) selected);
        } else if (selected instanceof Coast) {
            updateCoastFromForm((Coast) selected);
        }
    }

    private void onEventSelected(MissionEvent event) {
        eventColorPicker.setValue(Color.web(event.getColorHex()));
        if (event instanceof Waypoint) {
            showOrbitInput((Waypoint) event);
        } else if (event instanceof Maneuver) {
            showManeuverInput((Maneuver) event);
        } else if (event instanceof Coast) {
            showCoastInput((Coast) event);
        }
    }

    private void updateOrbitLabels(String frame) {
        GridPane grid = (GridPane) lblOrbit1.getParent();
        switch (frame) {
            case "Inertial (EME2000)":
                lblOrbit1.setText("X (km):");    lblOrbit2.setText("Y (km):");    lblOrbit3.setText("Z (km):");
                lblOrbit4.setText("Vx (km/s):");  lblOrbit5.setText("Vy (km/s):"); lblOrbit6.setText("Vz (km/s):");
                tfOrbit4.setVisible(true); lblOrbit4.setVisible(true);
                tfOrbit5.setVisible(true); lblOrbit5.setVisible(true);
                tfOrbit6.setVisible(true); lblOrbit6.setVisible(true);
                break;
            case "ECEF (ITRF)":
                lblOrbit1.setText("X (km):");    lblOrbit2.setText("Y (km):");    lblOrbit3.setText("Z (km):");
                lblOrbit4.setText("Vx (km/s):");  lblOrbit5.setText("Vy (km/s):"); lblOrbit6.setText("Vz (km/s):");
                tfOrbit4.setVisible(true); lblOrbit4.setVisible(true);
                tfOrbit5.setVisible(true); lblOrbit5.setVisible(true);
                tfOrbit6.setVisible(true); lblOrbit6.setVisible(true);
                break;
            case "Geodetic (LLA)":
                lblOrbit1.setText("Lat (\u00B0):"); lblOrbit2.setText("Lon (\u00B0):"); lblOrbit3.setText("Alt (km):");
                tfOrbit4.setVisible(false); lblOrbit4.setVisible(false);
                tfOrbit5.setVisible(false); lblOrbit5.setVisible(false);
                tfOrbit6.setVisible(false); lblOrbit6.setVisible(false);
                break;
            default:
                lblOrbit1.setText("a (km):");    lblOrbit2.setText("e:");        lblOrbit3.setText("i (\u00B0):");
                lblOrbit4.setText("\u03A9 (\u00B0):"); lblOrbit5.setText("\u03C9 (\u00B0):"); lblOrbit6.setText("\u03BD (\u00B0):");
                tfOrbit4.setVisible(true); lblOrbit4.setVisible(true);
                tfOrbit5.setVisible(true); lblOrbit5.setVisible(true);
                tfOrbit6.setVisible(true); lblOrbit6.setVisible(true);
                break;
        }
    }

    private void showOrbitInput(Waypoint wp) {
        inputPanelTitle.setText("ORBIT INPUT (" + wp.getName() + ")");
        orbitInputPanel.setVisible(true);
        orbitInputPanel.setManaged(true);
        maneuverInputPanel.setVisible(false);
        maneuverInputPanel.setManaged(false);
        coastInputPanel.setVisible(false);
        coastInputPanel.setManaged(false);

        String frame = wp.getReferenceFrame();
        String comboVal = "Orbital Elements";
        if (frame.equals("INERTIAL")) comboVal = "Inertial (EME2000)";
        else if (frame.equals("ECEF")) comboVal = "ECEF (ITRF)";
        else if (frame.equals("LLA")) comboVal = "Geodetic (LLA)";
        cbWaypointFrame.getSelectionModel().select(comboVal);
        updateOrbitLabels(comboVal);

        cbWaypointBody.getSelectionModel().select(wp.getCelestialBody() == CelestialBody.MOON ? "Moon" : "Earth");

        switch (frame) {
            case "INERTIAL":
                tfOrbit1.setText(String.valueOf(wp.getInertialX()));
                tfOrbit2.setText(String.valueOf(wp.getInertialY()));
                tfOrbit3.setText(String.valueOf(wp.getInertialZ()));
                tfOrbit4.setText(String.valueOf(wp.getInertialVx()));
                tfOrbit5.setText(String.valueOf(wp.getInertialVy()));
                tfOrbit6.setText(String.valueOf(wp.getInertialVz()));
                break;
            case "ECEF":
                tfOrbit1.setText(String.valueOf(wp.getEcefX()));
                tfOrbit2.setText(String.valueOf(wp.getEcefY()));
                tfOrbit3.setText(String.valueOf(wp.getEcefZ()));
                tfOrbit4.setText(String.valueOf(wp.getEcefVx()));
                tfOrbit5.setText(String.valueOf(wp.getEcefVy()));
                tfOrbit6.setText(String.valueOf(wp.getEcefVz()));
                break;
            case "LLA":
                tfOrbit1.setText(String.valueOf(wp.getLatitude()));
                tfOrbit2.setText(String.valueOf(wp.getLongitude()));
                tfOrbit3.setText(String.valueOf(wp.getAltitude()));
                break;
            default:
                tfOrbit1.setText(String.valueOf(wp.getSemiMajorAxis()));
                tfOrbit2.setText(String.valueOf(wp.getEccentricity()));
                tfOrbit3.setText(String.valueOf(wp.getInclination()));
                tfOrbit4.setText(String.valueOf(wp.getRaan()));
                tfOrbit5.setText(String.valueOf(wp.getArgPeriapsis()));
                tfOrbit6.setText(String.valueOf(wp.getTrueAnomaly()));
                break;
        }
    }

    private void updateManeuverLabels(String frame) {
        if (frame.startsWith("LVLH")) {
            lblDvx.setText("dV_R (m/s):");
            lblDvy.setText("dV_T (m/s):");
            lblDvz.setText("dV_N (m/s):");
        } else {
            lblDvx.setText("dVx (m/s):");
            lblDvy.setText("dVy (m/s):");
            lblDvz.setText("dVz (m/s):");
        }
    }

    private void showManeuverInput(Maneuver m) {
        inputPanelTitle.setText("MANEUVER INPUT (" + m.getName() + ")");
        orbitInputPanel.setVisible(false);
        orbitInputPanel.setManaged(false);
        maneuverInputPanel.setVisible(true);
        maneuverInputPanel.setManaged(true);
        coastInputPanel.setVisible(false);
        coastInputPanel.setManaged(false);
        tfDvx.setText(String.valueOf(m.getdVx()));
        tfDvy.setText(String.valueOf(m.getdVy()));
        tfDvz.setText(String.valueOf(m.getdVz()));
        String frame = m.getReferenceFrame();
        cbManeuverFrame.getSelectionModel().select(frame.startsWith("LVLH") ? "LVLH (RTN)" : "EME2000 (Inertial)");
        updateManeuverLabels(cbManeuverFrame.getSelectionModel().getSelectedItem());
    }

    private void showCoastInput(Coast c) {
        inputPanelTitle.setText("COAST INPUT (" + c.getName() + ")");
        orbitInputPanel.setVisible(false);
        orbitInputPanel.setManaged(false);
        maneuverInputPanel.setVisible(false);
        maneuverInputPanel.setManaged(false);
        coastInputPanel.setVisible(true);
        coastInputPanel.setManaged(true);
        tfCoastDuration.setText(String.valueOf(c.getDuration()));
        tfCoastStepSize.setText(String.valueOf(c.getStepSize()));
    }

    @FXML
    private void onRunMission() {
        try {
            saveCurrentEvent();
            MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                statusLabel.setText("Please select an event first");
                return;
            }

            if (selected instanceof Waypoint) {
                runWaypoint((Waypoint) selected);
            } else if (selected instanceof Maneuver) {
                runManeuver((Maneuver) selected);
            } else if (selected instanceof Coast) {
                runCoast((Coast) selected);
            }

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void runWaypoint(Waypoint wp) {
        updateWaypointFromForm(wp);
        statusLabel.setText("Waypoint set: " + wp.toString());
    }

    private void runManeuver(Maneuver m) {
        updateManeuverFromForm(m);
        statusLabel.setText("Maneuver dV = " + String.format("%.1f", m.getMagnitude()) + " m/s (instantaneous)");
    }

    private void runCoast(Coast c) {
        statusLabel.setText("Coasting...");
        updateCoastFromForm(c);
        statusLabel.setText("Coast duration: " + String.format("%.0f", c.getDuration()) + " s, step: " + String.format("%.0f", c.getStepSize()) + " s");
    }

    private String selectedWaypointFrame() {
        String sel = cbWaypointFrame.getSelectionModel().getSelectedItem();
        if (sel == null) return "ORBITAL_ELEMENTS";
        if (sel.startsWith("Inertial")) return "INERTIAL";
        if (sel.startsWith("ECEF")) return "ECEF";
        if (sel.startsWith("Geodetic")) return "LLA";
        return "ORBITAL_ELEMENTS";
    }

    private void updateWaypointFromForm(Waypoint wp) {
        String frame = selectedWaypointFrame();
        wp.setReferenceFrame(frame);
        String bodySel = cbWaypointBody.getSelectionModel().getSelectedItem();
        wp.setCelestialBody("Moon".equals(bodySel) ? CelestialBody.MOON : CelestialBody.EARTH);
        switch (frame) {
            case "INERTIAL":
                wp.setInertialX(Double.parseDouble(tfOrbit1.getText()));
                wp.setInertialY(Double.parseDouble(tfOrbit2.getText()));
                wp.setInertialZ(Double.parseDouble(tfOrbit3.getText()));
                wp.setInertialVx(Double.parseDouble(tfOrbit4.getText()));
                wp.setInertialVy(Double.parseDouble(tfOrbit5.getText()));
                wp.setInertialVz(Double.parseDouble(tfOrbit6.getText()));
                break;
            case "ECEF":
                wp.setEcefX(Double.parseDouble(tfOrbit1.getText()));
                wp.setEcefY(Double.parseDouble(tfOrbit2.getText()));
                wp.setEcefZ(Double.parseDouble(tfOrbit3.getText()));
                wp.setEcefVx(Double.parseDouble(tfOrbit4.getText()));
                wp.setEcefVy(Double.parseDouble(tfOrbit5.getText()));
                wp.setEcefVz(Double.parseDouble(tfOrbit6.getText()));
                break;
            case "LLA":
                wp.setLatitude(Double.parseDouble(tfOrbit1.getText()));
                wp.setLongitude(Double.parseDouble(tfOrbit2.getText()));
                wp.setAltitude(Double.parseDouble(tfOrbit3.getText()));
                break;
            default:
                wp.setSemiMajorAxis(Double.parseDouble(tfOrbit1.getText()));
                wp.setEccentricity(Double.parseDouble(tfOrbit2.getText()));
                wp.setInclination(Double.parseDouble(tfOrbit3.getText()));
                wp.setRaan(Double.parseDouble(tfOrbit4.getText()));
                wp.setArgPeriapsis(Double.parseDouble(tfOrbit5.getText()));
                wp.setTrueAnomaly(Double.parseDouble(tfOrbit6.getText()));
                break;
        }
        timelineList.refresh();
    }

    private void updateManeuverFromForm(Maneuver m) {
        m.setdVx(Double.parseDouble(tfDvx.getText()));
        m.setdVy(Double.parseDouble(tfDvy.getText()));
        m.setdVz(Double.parseDouble(tfDvz.getText()));
        String sel = cbManeuverFrame.getSelectionModel().getSelectedItem();
        if (sel != null && sel.startsWith("LVLH")) {
            m.setReferenceFrame("LVLH");
        } else {
            m.setReferenceFrame("EME2000");
        }
        timelineList.refresh();
    }

    private void updateCoastFromForm(Coast c) {
        c.setDuration(Double.parseDouble(tfCoastDuration.getText()));
        c.setStepSize(Double.parseDouble(tfCoastStepSize.getText()));
        timelineList.refresh();
    }

    private void assignEventColor(MissionEvent event) {
        int idx = events.size();
        event.setColorHex(COLOR_PALETTE[idx % COLOR_PALETTE.length]);
    }

    @FXML
    private void onAddWaypoint() {
        int num = 1;
        for (MissionEvent e : events) {
            if (e instanceof Waypoint) num++;
        }
        Waypoint newWp = new Waypoint("Waypoint " + num);
        assignEventColor(newWp);
        events.add(newWp);
        timelineList.getSelectionModel().select(newWp);
    }

    @FXML
    private void onAddManeuver() {
        int num = 1;
        for (MissionEvent e : events) {
            if (e instanceof Maneuver) num++;
        }
        Maneuver newM = new Maneuver("Maneuver " + num);
        assignEventColor(newM);
        events.add(newM);
        timelineList.getSelectionModel().select(newM);
    }

    @FXML
    private void onAddCoast() {
        int num = 1;
        for (MissionEvent e : events) {
            if (e instanceof Coast) num++;
        }
        Coast newC = new Coast("Coast " + num);
        assignEventColor(newC);
        events.add(newC);
        timelineList.getSelectionModel().select(newC);
    }

    @FXML
    private void onRemoveWaypoint() {
        MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();
        if (selected != null && events.size() > 1) {
            int index = timelineList.getSelectionModel().getSelectedIndex();
            events.remove(selected);
            if (index >= events.size()) {
                timelineList.getSelectionModel().select(events.size() - 1);
            } else {
                timelineList.getSelectionModel().select(index);
            }
        } else {
            statusLabel.setText("Cannot remove the last event");
        }
    }

    @FXML
    private void onRunAllWaypoints() {
        try {
            saveCurrentEvent();
            AbsoluteDate epoch = parseEpoch();
            statusLabel.setText("Running mission with " + events.size() + " events...");

            List<List<TrajectoryPoint>> segments = new ArrayList<>();
            List<Color> segmentColors = new ArrayList<>();
            List<CelestialBody> segmentBodies = new ArrayList<>();
            TrajectoryPoint lastPoint = null;
            CelestialBody currentBody = CelestialBody.EARTH;

            for (int i = 0; i < events.size(); i++) {
                MissionEvent event = events.get(i);

                if (event instanceof Waypoint) {
                    Waypoint wp = (Waypoint) event;
                    currentBody = wp.getCelestialBody();
                    physicsService.setCelestialBody(currentBody);
                    TrajectoryPoint point;
                    switch (wp.getReferenceFrame()) {
                        case "INERTIAL":
                            point = physicsService.createTrajectoryPointFromInertial(
                                wp.getInertialX(), wp.getInertialY(), wp.getInertialZ(),
                                wp.getInertialVx(), wp.getInertialVy(), wp.getInertialVz(), epoch);
                            break;
                        case "ECEF":
                            point = physicsService.createTrajectoryPointFromECEF(
                                wp.getEcefX(), wp.getEcefY(), wp.getEcefZ(),
                                wp.getEcefVx(), wp.getEcefVy(), wp.getEcefVz(), epoch);
                            break;
                        case "LLA":
                            point = physicsService.createTrajectoryPointFromLLA(
                                wp.getLatitude(), wp.getLongitude(), wp.getAltitude(), epoch);
                            break;
                        default:
                            point = physicsService.createTrajectoryPoint(
                                wp.getSemiMajorAxis() * 1000, wp.getEccentricity(), wp.getInclination(),
                                wp.getRaan(), wp.getArgPeriapsis(), wp.getTrueAnomaly(), epoch);
                            break;
                    }
                    lastPoint = point;

                } else if (event instanceof Maneuver) {
                    Maneuver m = (Maneuver) event;

                    if (lastPoint != null) {
                        lastPoint = physicsService.applyDeltaV(lastPoint, m.getdVx(), m.getdVy(), m.getdVz(), m.getReferenceFrame());
                    }
                    statusLabel.setText("Applied maneuver " + (i + 1) + "/" + events.size());

                } else if (event instanceof Coast) {
                    Coast c = (Coast) event;

                    if (lastPoint != null) {
                        int steps = Math.max(1, (int) (c.getDuration() / c.getStepSize()));
                        List<TrajectoryPoint> coastTraj = physicsService.propagateWithCoast(lastPoint, c.getDuration(), steps);
                        segments.add(coastTraj);
                        segmentBodies.add(currentBody);
                        segmentColors.add(Color.web(event.getColorHex()));
                        lastPoint = coastTraj.get(coastTraj.size() - 1);
                    }
                    statusLabel.setText("Coast " + (i + 1) + "/" + events.size());
                }

                statusLabel.setText("Processed event " + (i + 1) + "/" + events.size());
            }

            // Translate Moon-relative segments to Earth-centered for rendering
            for (int i = 0; i < segments.size(); i++) {
                if (segmentBodies.get(i) != CelestialBody.EARTH) {
                    segments.set(i, physicsService.translateToEarthCentered(segments.get(i), segmentBodies.get(i)));
                }
            }

            viewer3D.setTrajectoryGroups(segments, segmentColors);
            viewer2D.setTrajectoryGroups(segments, segmentColors);

            try {
                viewer3D.setMoonPosition(physicsService.getMoonPosition(epoch));
            } catch (Exception e) {
                // ephemeris unavailable, Moon not shown
            }

            // Compute body trails for non-inertial celestial bodies
            double totalDuration = 0;
            for (List<TrajectoryPoint> seg : segments) {
                if (seg.size() >= 2) {
                    double start = seg.get(0).date.durationFrom(epoch);
                    double end = seg.get(seg.size() - 1).date.durationFrom(epoch);
                    totalDuration = Math.max(totalDuration, end - start);
                }
            }
            if (totalDuration < 60) totalDuration = 86400 * 28;

            for (CelestialBody body : CelestialBody.values()) {
                if (body != inertialBody) {
                    List<TrajectoryPoint> trail = physicsService.getCelestialBodyTrajectory(body, epoch, totalDuration, 360);
                    viewer3D.setBodyTrail(body, trail);
                } else {
                    viewer3D.setBodyTrail(body, null);
                }
            }

            int totalPoints = 0;
            for (List<TrajectoryPoint> seg : segments) totalPoints += seg.size();
            statusLabel.setText("Mission complete - " + totalPoints + " points");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}