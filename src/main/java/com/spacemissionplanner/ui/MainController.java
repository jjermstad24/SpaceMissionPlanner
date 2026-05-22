package com.spacemissionplanner.ui;

import com.spacemissionplanner.model.CelestialBody;
import com.spacemissionplanner.model.Coast;
import com.spacemissionplanner.model.Maneuver;
import com.spacemissionplanner.model.MissionEvent;
import com.spacemissionplanner.model.Vehicle;
import com.spacemissionplanner.physics.OrekitService;
import com.spacemissionplanner.physics.OrekitService.TrajectoryPoint;
import com.spacemissionplanner.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.List;
import java.util.ArrayList;

public class MainController {

    @FXML private ComboBox<String> cbVehicleFrame;
    @FXML private ComboBox<String> cbVehicleBody;
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
        ErrorHandler.setStatusConsumer(msg -> statusLabel.setText(msg));
        events = FXCollections.observableArrayList();

        Vehicle defaultVehicle = new Vehicle("Vehicle 1", 6871, 0.0, 45.0, 0.0, 0.0, 0.0);
        defaultVehicle.setColorHex(COLOR_PALETTE[0]);
        events.add(defaultVehicle);

        timelineList.setItems(events);
        timelineList.setCellFactory(new Callback<ListView<MissionEvent>, ListCell<MissionEvent>>() {
            @Override
            public ListCell<MissionEvent> call(ListView<MissionEvent> param) {
                return new ListCell<MissionEvent>() {
                    private static final double RAIL_W = 26;
                    private static final double CELL_H = 36;
                    private static final double LINE_W = 2;
                    private final Rectangle line = new Rectangle(LINE_W, CELL_H, Color.rgb(160, 160, 160));

                    private boolean isChildOfVehicle(int idx, ObservableList<MissionEvent> items) {
                        if (idx <= 0) return false;
                        for (int i = idx - 1; i >= 0; i--) {
                            MissionEvent prev = items.get(i);
                            if (prev instanceof Vehicle) return true;
                            if (prev instanceof Coast || prev instanceof Maneuver) continue;
                            return false;
                        }
                        return false;
                    }

                    @Override
                    protected void updateItem(MissionEvent item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                            setStyle(null);
                            return;
                        }

                        int idx = getIndex();
                        ObservableList<MissionEvent> items = getListView().getItems();
                        boolean isChild = !(item instanceof Vehicle)
                            && idx >= 0 && idx < items.size()
                            && isChildOfVehicle(idx, items);

                        setStyle("-fx-background-color: transparent; -fx-padding: 0;");

                        Color c = ErrorHandler.parseSafe(
                            () -> Color.web(item.getColorHex()), Color.CYAN, "Parse event color");

                        if (item instanceof Vehicle) {
                            Circle circle = new Circle(5, c);
                            circle.setStroke(Color.BLACK);
                            circle.setStrokeWidth(1.5);
                            StackPane rail = new StackPane(line, circle);
                            rail.setPrefWidth(RAIL_W);
                            rail.setMinWidth(RAIL_W);
                            rail.setMaxWidth(RAIL_W);

                            Label nameLabel = new Label(item.toString());
                            nameLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");

                            Label typeLabel = new Label("WAYPOINT");
                            typeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");

                            VBox textBox = new VBox(-2, typeLabel, nameLabel);
                            textBox.setAlignment(Pos.CENTER_LEFT);

                            HBox cell = new HBox(4, rail, textBox);
                            cell.setAlignment(Pos.CENTER_LEFT);
                            setGraphic(cell);

                        } else {
                            Node marker;
                            String typeText;
                            if (item instanceof Maneuver) {
                                Polygon triangle = new Polygon(2.0, -4.0, 2.0, 4.0, 8.0, 0.0);
                                triangle.setFill(c);
                                triangle.setStroke(Color.BLACK);
                                triangle.setStrokeWidth(1);
                                marker = triangle;
                                typeText = "MANEUVER";
                            } else {
                                Rectangle bar = new Rectangle(10, 3, c);
                                bar.setArcWidth(2);
                                bar.setArcHeight(2);
                                marker = bar;
                                typeText = "COAST";
                            }

                            if (isChild) {
                                Rectangle connector = new Rectangle(10, 2, Color.rgb(160, 160, 160));
                                HBox markerBox = new HBox(2, connector, marker);
                                markerBox.setAlignment(Pos.CENTER_LEFT);

                                StackPane rail = new StackPane(line, markerBox);
                                rail.setPrefWidth(RAIL_W + 10);
                                rail.setMinWidth(RAIL_W + 10);
                                rail.setMaxWidth(RAIL_W + 10);
                                StackPane.setAlignment(markerBox, Pos.CENTER_LEFT);

                                Label nameLabel = new Label(item.toString());
                                nameLabel.setStyle("-fx-font-size: 11;");

                                Label typeLabel = new Label(typeText);
                                typeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");

                                VBox textBox = new VBox(-2, typeLabel, nameLabel);
                                textBox.setAlignment(Pos.CENTER_LEFT);
                                textBox.setPadding(new Insets(0, 0, 0, 8));

                                HBox cell = new HBox(4, rail, textBox);
                                cell.setAlignment(Pos.CENTER_LEFT);
                                setGraphic(cell);

                            } else {
                                StackPane rail = new StackPane(line, marker);
                                rail.setPrefWidth(RAIL_W);
                                rail.setMinWidth(RAIL_W);
                                rail.setMaxWidth(RAIL_W);

                                Label nameLabel = new Label(item.toString());
                                nameLabel.setStyle("-fx-font-size: 11;");

                                Label typeLabel = new Label(typeText);
                                typeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #888;");

                                VBox textBox = new VBox(-2, typeLabel, nameLabel);
                                textBox.setAlignment(Pos.CENTER_LEFT);

                                HBox cell = new HBox(4, rail, textBox);
                                cell.setAlignment(Pos.CENTER_LEFT);
                                setGraphic(cell);
                            }
                        }
                    }
                };
            }
        });

        timelineList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            ErrorHandler.runSafe(() -> {
                if (oldVal != null) {
                    if (oldVal instanceof Vehicle) {
                        saveCurrentVehicleForm((Vehicle) oldVal);
                    } else if (oldVal instanceof Maneuver) {
                        updateManeuverFromForm((Maneuver) oldVal);
                    } else if (oldVal instanceof Coast) {
                        updateCoastFromForm((Coast) oldVal);
                    }
                }
                if (newVal != null) {
                    onEventSelected(newVal);
                }
            }, "Save/load event form");
        });

        timelineList.setFixedCellSize(36);
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
                double cellSize = timelineList.getFixedCellSize();
                int toIdx = (int)(event.getY() / cellSize + 0.5);
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
                selected.setColorHex(toHex(c));
                timelineList.refresh();
            }
        });

        cbVehicleFrame.getItems().addAll("Orbital Elements", "Inertial (EME2000)", "ECEF (ITRF)", "Geodetic (LLA)");
        cbVehicleFrame.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateOrbitLabels(newVal);
            }
        });

        cbVehicleBody.getItems().addAll("Earth", "Moon");
        cbVehicleBody.getSelectionModel().select(0);

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
    private boolean missionHasRun = false;

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

            RadioButton inertialRb = new RadioButton("Coord");
            inertialRb.setToggleGroup(inertialGroup);
            inertialRb.setStyle("-fx-font-size: 10;");
            if (body == CelestialBody.EARTH) {
                inertialRb.setSelected(true);
                inertialBody = CelestialBody.EARTH;
                viewer3D.setCoordOrigin(CelestialBody.EARTH);
            }
            int bodyIdx = body.ordinal();
            inertialRb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    inertialBody = CelestialBody.values()[bodyIdx];
                    viewer3D.setCoordOrigin(inertialBody);
                    statusLabel.setText("Coordinate origin: " + inertialBody.getDisplayName());
                    if (missionHasRun) {
                        onRunAllVehicles();
                    }
                }
            });

            row.getChildren().addAll(visCb, nameLabel, trailCb, trailLabel, gravityCb, gravityLabel, inertialRb);
            tabBodiesContent.getChildren().add(row);
        }
    }

    private AbsoluteDate parseEpoch() {
        String format = cbEpochFormat.getSelectionModel().getSelectedItem();
        String val = tfEpochValue.getText();
        return ErrorHandler.parseSafe(() -> {
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
        }, AbsoluteDate.J2000_EPOCH, "Parse epoch");
    }

    private void saveCurrentVehicleForm(Vehicle wp) {
        String frame = wp.getReferenceFrame();
        ErrorHandler.runSafe(() -> {
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
        }, "Save vehicle form");
    }

    private void saveCurrentEvent() {
        MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (selected instanceof Vehicle) {
            updateVehicleFromForm((Vehicle) selected);
        } else if (selected instanceof Maneuver) {
            updateManeuverFromForm((Maneuver) selected);
        } else if (selected instanceof Coast) {
            updateCoastFromForm((Coast) selected);
        }
    }

    private void onEventSelected(MissionEvent event) {
        eventColorPicker.setValue(Color.web(event.getColorHex()));
        if (event instanceof Vehicle) {
            showOrbitInput((Vehicle) event);
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

    private void showOrbitInput(Vehicle wp) {
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
        cbVehicleFrame.getSelectionModel().select(comboVal);
        updateOrbitLabels(comboVal);

        cbVehicleBody.getSelectionModel().select(wp.getCelestialBody() == CelestialBody.MOON ? "Moon" : "Earth");

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
        ErrorHandler.runSafe(() -> {
            saveCurrentEvent();
            MissionEvent selected = timelineList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                ErrorHandler.warn("No event selected", null);
                return;
            }

            if (selected instanceof Vehicle) {
                runVehicle((Vehicle) selected);
            } else if (selected instanceof Maneuver) {
                runManeuver((Maneuver) selected);
            } else if (selected instanceof Coast) {
                runCoast((Coast) selected);
            }
        }, "Run mission");
    }

    private void runVehicle(Vehicle wp) {
        updateVehicleFromForm(wp);
        statusLabel.setText("Vehicle set: " + wp.toString());
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

    private String selectedVehicleFrame() {
        String sel = cbVehicleFrame.getSelectionModel().getSelectedItem();
        if (sel == null) return "ORBITAL_ELEMENTS";
        if (sel.startsWith("Inertial")) return "INERTIAL";
        if (sel.startsWith("ECEF")) return "ECEF";
        if (sel.startsWith("Geodetic")) return "LLA";
        return "ORBITAL_ELEMENTS";
    }

    private void updateVehicleFromForm(Vehicle wp) {
        String frame = selectedVehicleFrame();
        wp.setReferenceFrame(frame);
        String bodySel = cbVehicleBody.getSelectionModel().getSelectedItem();
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
    private void onAddVehicle() {
        int num = 1;
        for (MissionEvent e : events) {
            if (e instanceof Vehicle) num++;
        }
        Vehicle newWp = new Vehicle("Vehicle " + num);
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
        Coast c = new Coast("Coast " + num, 3600, 60);
        assignEventColor(c);
        events.add(c);
        timelineList.getSelectionModel().select(c);
    }

    private static String toHex(Color c) {
        int r = (int)(c.getRed() * 255);
        int g = (int)(c.getGreen() * 255);
        int b = (int)(c.getBlue() * 255);
        return "#" + Integer.toHexString((r << 16) | (g << 8) | b);
    }

    @FXML
    private void onRemoveVehicle() {
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
    private void onOptimizeMeet() {
        List<Vehicle> vehicles = new ArrayList<>();
        for (MissionEvent evt : events) {
            if (evt instanceof Vehicle) vehicles.add((Vehicle) evt);
        }
        if (vehicles.size() < 2) {
            ErrorHandler.info("Need at least 2 vehicles for optimization");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Optimize Transfer");

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15");

        Label lblStart = new Label("Start Vehicle:");
        ComboBox<Vehicle> cbStart = new ComboBox<>();
        cbStart.setItems(FXCollections.observableArrayList(vehicles));
        cbStart.getSelectionModel().selectFirst();
        cbStart.setCellFactory(lv -> new ListCell<Vehicle>() {
            @Override protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : "Vehicle " + (vehicles.indexOf(item) + 1));
            }
        });
        cbStart.setButtonCell(new ListCell<Vehicle>() {
            @Override protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : "Vehicle " + (vehicles.indexOf(item) + 1));
            }
        });

        Label lblEnd = new Label("End Vehicle (target orbit):");
        ComboBox<Vehicle> cbEnd = new ComboBox<>();
        cbEnd.setItems(FXCollections.observableArrayList(vehicles));
        cbEnd.getSelectionModel().select(Math.min(1, vehicles.size() - 1));
        cbEnd.setCellFactory(lv -> new ListCell<Vehicle>() {
            @Override protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : "Vehicle " + (vehicles.indexOf(item) + 1));
            }
        });
        cbEnd.setButtonCell(new ListCell<Vehicle>() {
            @Override protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : "Vehicle " + (vehicles.indexOf(item) + 1));
            }
        });

        String[] paramNames = {"SMA (km)", "Eccentricity", "Inclination", "RAAN", "Arg Perigee", "True Anomaly"};
        CheckBox[] paramChecks = new CheckBox[6];
        TextField[][] paramMin = new TextField[6][];
        TextField[][] paramMax = new TextField[6][];
        double[][] defaultRanges = {
            {6800, 7200}, {0.001, 0.05}, {0.3, 1.0}, {0, 6.28}, {0, 6.28}, {0, 6.28}
        };
        for (int i = 0; i < 6; i++) {
            paramMin[i] = new TextField[1];
            paramMax[i] = new TextField[1];
        }

        TitledPane paramsPane = new TitledPane();
        paramsPane.setText("Parameters to Vary");
        GridPane paramsGrid = new GridPane();
        paramsGrid.setHgap(5);
        paramsGrid.setVgap(3);
        for (int i = 0; i < 6; i++) {
            paramChecks[i] = new CheckBox(paramNames[i]);
            paramMin[i][0] = new TextField(String.valueOf(defaultRanges[i][0]));
            paramMin[i][0].setPrefWidth(80);
            paramMax[i][0] = new TextField(String.valueOf(defaultRanges[i][1]));
            paramMax[i][0].setPrefWidth(80);
            paramsGrid.add(paramChecks[i], 0, i);
            paramsGrid.add(new Label("Min:"), 1, i);
            paramsGrid.add(paramMin[i][0], 2, i);
            paramsGrid.add(new Label("Max:"), 3, i);
            paramsGrid.add(paramMax[i][0], 4, i);
        }
        paramsPane.setContent(paramsGrid);
        paramsPane.setExpanded(true);

        HBox coastRow = new HBox(5);
        coastRow.getChildren().addAll(
            new Label("Coast duration (s):"),
            new TextField("3600") {{ setPrefWidth(100); }}
        );
        TextField tfCoast = (TextField) coastRow.getChildren().get(1);

        HBox stepsRow = new HBox(5);
        stepsRow.getChildren().addAll(
            new Label("Steps per param:"),
            new TextField("10") {{ setPrefWidth(60); }}
        );
        TextField tfSteps = (TextField) stepsRow.getChildren().get(1);

        TextArea taResults = new TextArea();
        taResults.setPrefHeight(150);
        taResults.setEditable(false);

        HBox btnRow = new HBox(10);
        Button btnRun = new Button("Run Optimization");
        Button btnApply = new Button("Apply Best Result");
        btnApply.setDisable(true);
        Button btnClose = new Button("Close");
        btnRow.getChildren().addAll(btnRun, btnApply, btnClose);

        root.getChildren().addAll(lblStart, cbStart, lblEnd, cbEnd, paramsPane, coastRow, stepsRow, taResults, btnRow);

        Scene scene = new Scene(root, 520, 600);
        dialog.setScene(scene);
        dialog.show();

        final OrekitService.TransferOptimizerResult[] bestHolder = new OrekitService.TransferOptimizerResult[1];

        btnRun.setOnAction(e -> {
            ErrorHandler.runSafe(() -> {
                Vehicle start = cbStart.getValue();
                Vehicle end = cbEnd.getValue();
                if (start == null || end == null) return;
                if (start == end) { taResults.setText("Start and end must be different vehicles"); return; }

                double coastDuration = ErrorHandler.parseSafe(() -> Double.parseDouble(tfCoast.getText()), 3600.0, "parse coast duration");
                int stepsPerParam = (int) (double) ErrorHandler.parseSafe(() -> Double.parseDouble(tfSteps.getText()), 10.0, "parse steps per param");

                AbsoluteDate epoch = parseEpoch();
                TrajectoryPoint startPoint = physicsService.createTrajectoryPoint(
                    start.getSemiMajorAxis() * 1000, start.getEccentricity(), start.getInclination(),
                    start.getRaan(), start.getArgPeriapsis(), start.getTrueAnomaly(), epoch);

                TrajectoryPoint endPoint = physicsService.createTrajectoryPoint(
                    end.getSemiMajorAxis() * 1000, end.getEccentricity(), end.getInclination(),
                    end.getRaan(), end.getArgPeriapsis(), end.getTrueAnomaly(), epoch);

                if (!"Orbital Elements".equals(end.getReferenceFrame())) {
                    taResults.setText("Warning: end vehicle not in Orbital Elements frame.\nUsing current Keplerian values.");
                }

                String[] varyParams = new String[6];
                double[][] bounds = new double[6][2];
                int count = 0;
                for (int i = 0; i < 6; i++) {
                    if (paramChecks[i].isSelected()) {
                        varyParams[count] = paramNames[i];
                        TextField minField = paramMin[i][0];
                        TextField maxField = paramMax[i][0];
                        double dMin = defaultRanges[i][0];
                        double dMax = defaultRanges[i][1];
                        String pName = paramNames[i];
                        double min = ErrorHandler.parseSafe(() -> Double.parseDouble(minField.getText()), dMin, "parse min " + pName);
                        double max = ErrorHandler.parseSafe(() -> Double.parseDouble(maxField.getText()), dMax, "parse max " + pName);
                        bounds[count][0] = Math.min(min, max);
                        bounds[count][1] = Math.max(min, max);
                        count++;
                    }
                }

                if (count == 0) {
                    taResults.setText("Select at least one parameter to vary.");
                    return;
                }

                String[] activeNames = new String[count];
                double[][] activeBounds = new double[count][2];
                System.arraycopy(varyParams, 0, activeNames, 0, count);
                System.arraycopy(bounds, 0, activeBounds, 0, count);

                taResults.setText("Optimizing " + count + " parameter(s)...\n");

                AbsoluteDate targetDate = epoch.shiftedBy(coastDuration);
                OrekitService.TransferOptimizerResult result = physicsService.optimizeTransfer(
                    startPoint, targetDate,
                    endPoint, targetDate,
                    activeNames, activeBounds, stepsPerParam);

                bestHolder[0] = result;

                StringBuilder sb = new StringBuilder();
                sb.append("Best total ΔV: ").append(String.format("%.1f", result.bestTotalDeltaV));
                sb.append(" m/s\n");
                sb.append("  Departure ΔV: ").append(String.format("%.1f", result.departureDeltaV)).append(" m/s\n");
                sb.append("  Arrival ΔV:   ").append(String.format("%.1f", result.arrivalDeltaV)).append(" m/s\n");
                sb.append("  Transfer duration: ").append(String.format("%.1f", result.transferDuration)).append(" s\n");
                sb.append("\nBest parameters:\n");
                String[] resultNames = {"SMA (km)", "Ecc", "Inc", "RAAN", "ArgP", "TA"};
                int idx = 0;
                for (int i = 0; i < 6; i++) {
                    if (paramChecks[i].isSelected()) {
                        sb.append(resultNames[i]).append(" = ").append(String.format("%.6f", result.bestParams[idx])).append("\n");
                        idx++;
                    }
                }

                taResults.setText(sb.toString());
                btnApply.setDisable(false);
            }, "optimize transfer");
        });

        btnApply.setOnAction(e -> {
            if (bestHolder[0] == null) return;
            Vehicle end = cbEnd.getValue();
            int idx = 0;
            for (int i = 0; i < 6; i++) {
                if (paramChecks[i].isSelected()) {
                    double val = bestHolder[0].bestParams[idx];
                    switch (i) {
                        case 0: end.setSemiMajorAxis(val); break;       // SMA in km
                        case 1: end.setEccentricity(val); break;
                        case 2: end.setInclination(val); break;
                        case 3: end.setRaan(val); break;
                        case 4: end.setArgPeriapsis(val); break;
                        case 5: end.setTrueAnomaly(val); break;
                    }
                    idx++;
                }
            }
            saveCurrentEvent();
            dialog.close();
            statusLabel.setText("Applied optimized parameters to Vehicle " + (vehicles.indexOf(end) + 1));
        });

        btnClose.setOnAction(e -> dialog.close());
    }

    @FXML
    private void onComputeTransfer() {
        List<Vehicle> vehicles = new ArrayList<>();
        for (MissionEvent evt : events) {
            if (evt instanceof Vehicle) vehicles.add((Vehicle) evt);
        }
        if (vehicles.size() < 2) {
            ErrorHandler.info("Need at least 2 vehicles");
            return;
        }

        AbsoluteDate epoch = parseEpoch();
        errorCheck: {
            // Build trajectory points for first and last vehicle
            Vehicle firstWp = vehicles.get(0);
            Vehicle lastWp = vehicles.get(vehicles.size() - 1);

            // Re-run mission to get all trajectory segments
            saveCurrentEvent();
            List<List<TrajectoryPoint>> segments = new ArrayList<>();
            List<CelestialBody> segmentBodies = new ArrayList<>();
            TrajectoryPoint lastPoint = null;
            CelestialBody currentBody = CelestialBody.EARTH;

            try {
                for (int i = 0; i < events.size(); i++) {
                    MissionEvent event = events.get(i);
                    if (event instanceof Vehicle) {
                        Vehicle wp = (Vehicle) event;
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
                    } else if (event instanceof Coast) {
                        Coast c = (Coast) event;
                        if (lastPoint != null) {
                            int steps = Math.max(1, (int) (c.getDuration() / c.getStepSize()));
                            List<TrajectoryPoint> coastTraj = physicsService.propagateWithCoast(lastPoint, c.getDuration(), steps);
                            segments.add(coastTraj);
                            segmentBodies.add(currentBody);
                            lastPoint = coastTraj.get(coastTraj.size() - 1);
                        }
                    }
                }
            } catch (Exception ex) {
                ErrorHandler.error("Compute transfer: mission run failed", ex);
                break errorCheck;
            }

            if (lastPoint == null) { ErrorHandler.info("No trajectory to compute transfer on"); break errorCheck; }

            // Use first vehicle point and last propagated point
            TrajectoryPoint from = physicsService.createTrajectoryPoint(
                firstWp.getSemiMajorAxis() * 1000, firstWp.getEccentricity(), firstWp.getInclination(),
                firstWp.getRaan(), firstWp.getArgPeriapsis(), firstWp.getTrueAnomaly(), epoch);
            TrajectoryPoint to = lastPoint;

            // Translate to inertial body centered for display
            TrajectoryPoint fromCentered = from;
            TrajectoryPoint toCentered = to;
            CelestialBody firstBody = firstWp.getCelestialBody();
            if (firstBody != inertialBody) {
                List<TrajectoryPoint> translated = physicsService.translateToBodyCentered(
                    List.of(from), firstBody, inertialBody);
                fromCentered = translated.get(0);
            }
            if (segmentBodies.size() > 0) {
                CelestialBody lastBody = segmentBodies.get(segmentBodies.size() - 1);
                if (lastBody != inertialBody) {
                    List<TrajectoryPoint> translated = physicsService.translateToBodyCentered(
                        List.of(to), lastBody, inertialBody);
                    toCentered = translated.get(0);
                }
            }

            OrekitService.TransferBurnResult burn = physicsService.computeTransferBurn(fromCentered, toCentered);

            statusLabel.setText(String.format("Transfer: ΔV dep=%.1f m/s, arr=%.1f m/s, dt=%.1f s",
                burn.dVMagnitude, burn.arrivalDVMagnitude, burn.transferDuration));
        }
    }

    @FXML
    private void onRunAllVehicles() {
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

                if (event instanceof Vehicle) {
                    Vehicle wp = (Vehicle) event;
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

            // Translate all segments to inertial-body-centered coordinates
            for (int i = 0; i < segments.size(); i++) {
                if (segmentBodies.get(i) != inertialBody) {
                    segments.set(i, physicsService.translateToBodyCentered(
                        segments.get(i), segmentBodies.get(i), inertialBody));
                }
            }

            viewer3D.setTrajectoryGroups(segments, segmentColors);
            viewer2D.setTrajectoryGroups(segments, segmentColors);

            ErrorHandler.runSafeSilent(() ->
                viewer3D.setMoonPosition(physicsService.getMoonPosition(epoch))
            );

            // Compute body trails for all bodies except the inertial body
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
                    // Translate trail to inertial body centered
                    trail = physicsService.translateToBodyCentered(trail, body, inertialBody);
                    viewer3D.setBodyTrail(body, trail);
                } else {
                    viewer3D.setBodyTrail(body, null);
                }
            }

            int totalPoints = 0;
            for (List<TrajectoryPoint> seg : segments) totalPoints += seg.size();
            missionHasRun = true;
            statusLabel.setText("Mission complete - " + totalPoints + " points");

        } catch (Exception ex) {
            ErrorHandler.error("Run all events failed", ex);
        }
    }
}