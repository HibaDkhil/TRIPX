package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.concurrent.Worker;
import javafx.application.Platform;
import netscape.javascript.JSObject;
import tn.esprit.entities.Bookingtrans;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.Transport;
import tn.esprit.services.BookingtransService;
import tn.esprit.services.TransportOptimalRouteService;
import tn.esprit.services.ScheduleService;
import tn.esprit.services.TransportService;
import tn.esprit.utils.MyDB;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransportUserInterfaceController {

    @FXML private StackPane contentArea;
    @FXML private Label     welcomeLabel;
    @FXML private Button    btnSchedules;
    @FXML private Button    btnTransport;
    @FXML private Button    btnMyBookings;

    private final TransportService    transportService = new TransportService();
    private final ScheduleService     scheduleService  = new ScheduleService();
    private final BookingtransService bookingService   = new BookingtransService();
    private final TransportOptimalRouteService transportRouteService = new TransportOptimalRouteService();

    private int currentUserId = 1;
    private Map<Long, String> destinationNames = null;

    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"(.*?)\"");
    private static final String TRANSPORT_MAP_PICK_EVENT_PREFIX = "TRIPX_TRANSPORT_PICK:";
    private static final String TRANSPORT_VEHICLE_PICKER_HTML =
            "<!doctype html><html><head><meta charset='utf-8'/>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1'/>"
                    + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css'/>"
                    + "<style>"
                    + "html,body{height:100%;margin:0;background:#f0f0f0;overflow:hidden;}"
                    + "#map{position:absolute;inset:0;}"
                    + "#hint{position:absolute;z-index:9999;top:10px;left:10px;background:#1f294c;color:#fff;"
                    + "padding:8px 10px;border-radius:6px;font:12px sans-serif;max-width:340px;}"
                    + "</style>"
                    + "</head><body>"
                    + "<div id='hint'>Single click: select point | Double click: zoom + select | Mouse wheel: zoom | Drag: pan</div>"
                    + "<div id='map'></div>"
                    + "<script>window.L_DISABLE_3D=true;</script>"
                    + "<script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'></script>"
                    + "<script>"
                    + "L.Map.mergeOptions({zoomAnimation:false,fadeAnimation:false,markerZoomAnimation:false});"
                    + "var map=L.map('map',{center:[36.8065,10.1815],zoom:12,zoomControl:true,doubleClickZoom:true,"
                    + "scrollWheelZoom:true,dragging:true,boxZoom:true,keyboard:true,preferCanvas:true});"
                    + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',"
                    + "{maxZoom:19,minZoom:2,updateWhenIdle:true,keepBuffer:3,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                    + "var marker=null;"
                    + "function notifyJava(lat,lng){"
                    + "alert('" + TRANSPORT_MAP_PICK_EVENT_PREFIX + "'+lat+','+lng);"
                    + "if(window.javaBridge&&window.javaBridge.onPointSelected){"
                    + "window.javaBridge.onPointSelected(String(lat),String(lng));}}"
                    + "function selectPoint(latlng){"
                    + "if(marker){marker.setLatLng(latlng);}else{marker=L.marker(latlng,{keyboard:false}).addTo(map);}notifyJava(latlng.lat,latlng.lng);}"
                    + "map.on('click',function(e){selectPoint(e.latlng);});"
                    + "map.on('dblclick',function(e){selectPoint(e.latlng); map.zoomIn();});"
                    + "setTimeout(function(){map.invalidateSize(true);},180);"
                    + "window.addEventListener('resize',function(){map.invalidateSize(false);});"
                    + "window.map=map;"
                    + "</script>"
                    + "</body></html>";

    /* ═══════════ STYLES ═══════════ */
    private static final String TAB_ACTIVE =
            "-fx-background-color: #1F294C; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-font-size: 13px; -fx-padding: 12 32 12 32; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: white; -fx-text-fill: #1F294C; -fx-font-size: 13px; " +
                    "-fx-padding: 12 32 12 32; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String BTN_TEAL =
            "-fx-background-color: #4FB3B5; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 9 22 9 22; -fx-cursor: hand; -fx-background-radius: 5;";
    private static final String BTN_DARK =
            "-fx-background-color: #1F294C; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 9 22 9 22; -fx-cursor: hand; -fx-background-radius: 5;";
    private static final String BTN_BOOK =
            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-font-size: 13px; -fx-padding: 10 0 10 0; -fx-cursor: hand; -fx-background-radius: 0 0 8 8;";
    private static final String BTN_BOOKED =
            "-fx-background-color: #aaa; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-font-size: 12px; -fx-padding: 10 0 10 0; -fx-background-radius: 0 0 8 8;";
    private static final String BTN_DETAIL =
            "-fx-background-color: #F1EAE7; -fx-text-fill: #1F294C; -fx-font-weight: bold; " +
                    "-fx-font-size: 12px; -fx-padding: 8 0 8 0; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String FORM_FIELD =
            "-fx-padding: 7 11 7 11; -fx-background-radius: 5; " +
                    "-fx-border-color: #D0C8C3; -fx-border-radius: 5; -fx-font-size: 12px;";
    private static final String TOGGLE_ON =
            "-fx-background-color: #1F294C; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 9 26 9 26; -fx-cursor: hand; -fx-font-size: 13px;";
    private static final String TOGGLE_OFF =
            "-fx-background-color: #E8E3DF; -fx-text-fill: #555; " +
                    "-fx-padding: 9 26 9 26; -fx-cursor: hand; -fx-font-size: 13px;";

    /* ═══════════ INIT ═══════════ */

    @FXML public void initialize() { showSchedulesTab(); }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        if (welcomeLabel != null) welcomeLabel.setText("Welcome, User #" + userId);
    }

    private void setActiveTab(Button active) {
        btnSchedules .setStyle(TAB_INACTIVE);
        btnTransport .setStyle(TAB_INACTIVE);
        btnMyBookings.setStyle(TAB_INACTIVE);
        active.setStyle(TAB_ACTIVE);
    }

    @FXML
    public void handleLogout() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setHeaderText(null); a.setContentText("Are you sure you want to logout?");
        a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) ((Stage) contentArea.getScene().getWindow()).close();
        });
    }

    /* ═══════════════════════════════════════════════════════
       DESTINATION NAMES
       ⚠  Adjust table/column names to match  teammate's
          actual destination entity once their code is ready.
          Current query:  SELECT id, name FROM destination
          Change "destination" → your table name
          Change "id"          → your primary key column name
          Change "name"        → your destination name column
       ═══════════════════════════════════════════════════════ */
    private Map<Long, String> getDestinationName() {
        if (destinationNames != null) return destinationNames;
        destinationNames = new LinkedHashMap<>();
        try {
            Connection con = MyDB.getInstance().getConx();
            // ↓↓↓  UPDATE THESE 3 IDENTIFIERS WHEN YOUR TEAMMATE'S TABLE IS READY  ↓↓↓
            String sql = "SELECT id, name FROM destinations ORDER BY name";
            // Example after update:  "SELECT destination_id, destination_name FROM destinations ORDER BY destination_name"
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next())
                    destinationNames.put(rs.getLong("id"), rs.getString("name"));
            }
        } catch (Exception e) {
            // Table not yet available — all destinations will show as "Destination #<id>"
            System.out.println("Destination names unavailable (showing IDs): " + e.getMessage());
        }
        return destinationNames;
    }

    private String destName(long id) {
        String n = getDestinationName().get(id);
        return n != null ? n : "Destination #" + id;
    }

    /* ═══════════ AVAILABILITY HELPERS ═══════════ */

    private int getBookedSeatsForSchedule(int scheduleId) {
        return bookingService.getAllBookings().stream()
                .filter(b -> b.getScheduleId() == scheduleId
                        && !"CANCELLED".equals(b.getBookingStatus()))
                .mapToInt(Bookingtrans::getTotalSeats).sum();
    }

    /**
     * FIX: A VEHICLE schedule becomes unavailable as soon as ANY non-cancelled
     * booking exists for it (not just CONFIRMED). This prevents double-booking
     * even while the first booking is still PENDING admin confirmation.
     */
    private boolean isVehicleScheduleUnavailable(int scheduleId) {
        return bookingService.getAllBookings().stream()
                .anyMatch(b -> b.getScheduleId() == scheduleId
                        && !"CANCELLED".equals(b.getBookingStatus()));
    }

    private int remainingSeatsForFlight(Schedule s, Map<Integer, Transport> tMap) {
        Transport t = tMap.get(s.getTransportId());
        if (t == null) return 0;
        return t.getCapacity() - getBookedSeatsForSchedule(s.getScheduleId());
    }

    /* ═══════════════════════════════════════════════════════
       SCHEDULES TAB
       ═══════════════════════════════════════════════════════ */

    @FXML public void showSchedulesTab() {
        setActiveTab(btnSchedules);
        buildSchedulesView();
    }

    @SuppressWarnings("unchecked")
    private void buildSchedulesView() {
        List<Schedule>  allSched = scheduleService.getAllSchedules();
        List<Transport> allTrans = transportService.getAllTransports();
        Map<Integer, Transport> transMap = new HashMap<>();
        allTrans.forEach(t -> transMap.put(t.getTransportId(), t));
        Map<Long, String> destNames = getDestinationName();

        List<Schedule>[] displayed = new List[]{
                allSched.stream()
                        .filter(s -> !"CANCELLED".equals(s.getStatus()))
                        .collect(Collectors.toList())
        };

        FlowPane cards = new FlowPane();
        cards.setHgap(18); cards.setVgap(18);
        cards.setPadding(new Insets(6, 0, 6, 0));

        Runnable render = () -> {
            cards.getChildren().clear();
            if (displayed[0].isEmpty()) {
                Label empty = new Label("No schedules match your search.");
                empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
                cards.getChildren().add(empty);
            } else {
                for (Schedule s : displayed[0]) {
                    Transport t = transMap.get(s.getTransportId());
                    if (t != null) cards.getChildren().add(
                            buildScheduleCard(s, t, transMap, destNames));
                }
            }
        };
        render.run();

        ToggleGroup tg = new ToggleGroup();
        ToggleButton allBtn  = tog("All",      tg, true);
        ToggleButton fltBtn  = tog("Flights",   tg, false);
        ToggleButton vehBtn  = tog("Vehicles",  tg, false);

        StackPane searchSwap = new StackPane();
        searchSwap.setVisible(false); searchSwap.setManaged(false);

        VBox flightPanel  = buildFlightSearchPanel (allSched, transMap, destNames, displayed, render);
        VBox vehiclePanel = buildVehicleSearchPanel(allSched, transMap, destNames, displayed, render);

        tg.selectedToggleProperty().addListener((obs, old, nv) -> {
            if (nv == null) { old.setSelected(true); return; }
            allBtn.setStyle(nv == allBtn ? TOGGLE_ON : TOGGLE_OFF);
            fltBtn.setStyle(nv == fltBtn ? TOGGLE_ON : TOGGLE_OFF);
            vehBtn.setStyle(nv == vehBtn ? TOGGLE_ON : TOGGLE_OFF);
            if (nv == allBtn) {
                searchSwap.setVisible(false); searchSwap.setManaged(false);
                displayed[0] = allSched.stream().filter(s -> !"CANCELLED".equals(s.getStatus()))
                        .collect(Collectors.toList());
            } else if (nv == fltBtn) {
                searchSwap.getChildren().setAll(flightPanel);
                searchSwap.setVisible(true); searchSwap.setManaged(true);
                displayed[0] = allSched.stream()
                        .filter(s -> !"CANCELLED".equals(s.getStatus()))
                        .filter(s -> isFlight(s, transMap)).collect(Collectors.toList());
            } else {
                searchSwap.getChildren().setAll(vehiclePanel);
                searchSwap.setVisible(true); searchSwap.setManaged(true);
                displayed[0] = allSched.stream()
                        .filter(s -> !"CANCELLED".equals(s.getStatus()))
                        .filter(s -> isVehicle(s, transMap)).collect(Collectors.toList());
            }
            render.run();
        });

        Label title = sectionTitle("Schedules");
        HBox typeRow = new HBox(allBtn, fltBtn, vehBtn);
        HBox topBar  = new HBox(title, new Region(), typeRow);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox layout = new VBox(14, topBar, searchSwap, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    private ToggleButton tog(String text, ToggleGroup g, boolean selected) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g); b.setSelected(selected);
        b.setStyle(selected ? TOGGLE_ON : TOGGLE_OFF);
        return b;
    }

    private boolean isFlight(Schedule s, Map<Integer, Transport> tm) {
        Transport t = tm.get(s.getTransportId());
        return t != null && "FLIGHT".equals(t.getTransportType());
    }

    private boolean isVehicle(Schedule s, Map<Integer, Transport> tm) {
        Transport t = tm.get(s.getTransportId());
        return t != null && "VEHICLE".equals(t.getTransportType());
    }

    /* ── FIX: Flight search uses only DEPARTURE date (arrival date removed) ── */
    @SuppressWarnings("unchecked")
    private VBox buildFlightSearchPanel(List<Schedule> all, Map<Integer, Transport> tMap,
                                        Map<Long, String> dNames,
                                        List<Schedule>[] disp, Runnable render) {
        // "From" = free text (airport / city name)
        TextField fromField = new TextField();
        fromField.setPromptText("Type departure airport or city...");
        fromField.setStyle(FORM_FIELD); fromField.setPrefWidth(210);

        // "To" = dropdown of known destination names
        List<Long> arrIds = all.stream().filter(s -> isFlight(s, tMap))
                .map(Schedule::getArrivalDestinationId).distinct().sorted().collect(Collectors.toList());
        ComboBox<String> toBox = destCombo(arrIds, "Any Arrival", dNames);

        // FIX: only departure date — arrival date removed entirely
        DatePicker depDp = dp("Departure date (optional)");

        ComboBox<String> clsBox = new ComboBox<>(
                FXCollections.observableArrayList("Any Class","ECONOMY","PREMIUM","BUSINESS","FIRST"));
        clsBox.setValue("Any Class"); clsBox.setStyle(FORM_FIELD); clsBox.setMaxWidth(Double.MAX_VALUE);

        Label errLbl = errLabel();

        Button searchBtn = mkBtn("Search Flights", BTN_TEAL);
        Button clearBtn  = mkBtn("Clear",          BTN_DARK);

        searchBtn.setOnAction(e -> {
            errLbl.setText("");
            String from  = fromField.getText().trim().toLowerCase();
            Long   toId  = parseDestId(toBox.getValue(), dNames);
            LocalDate dD = depDp.getValue();
            String cls   = clsBox.getValue();

            boolean aFrom=false, aTo=false, aDep=false, aCls=false;
            List<Schedule> res = new ArrayList<>();

            for (Schedule s : all) {
                if (!"CANCELLED".equals(s.getStatus()) && isFlight(s, tMap)) {
                    boolean ok1 = from.isEmpty()
                            || destName(s.getDepartureDestinationId()).toLowerCase().contains(from);
                    boolean ok2 = toId == null   || s.getArrivalDestinationId() == toId;
                    // FIX: only departure date check — no arrival date
                    boolean ok3 = dD   == null
                            || (s.getDepartureDatetime() != null
                            && s.getDepartureDatetime().toLocalDate().equals(dD));
                    boolean ok4 = "Any Class".equals(cls) || cls.equals(s.getTravelClass());

                    if (ok1) aFrom=true; if (ok2) aTo=true;
                    if (ok3) aDep=true;  if (ok4) aCls=true;
                    if (ok1&&ok2&&ok3&&ok4) res.add(s);
                }
            }

            if (res.isEmpty()) {
                StringBuilder sb = new StringBuilder("No flights found:\n");
                if (!from.isEmpty() && !aFrom)
                    sb.append("- No departure airport/city matching \"").append(from).append("\"\n");
                if (toId != null && !aTo)
                    sb.append("- No flights arriving at ").append(toBox.getValue()).append("\n");
                if (dD != null && !aDep)
                    sb.append("- No flights departing on ").append(dD.format(DATE_FMT)).append("\n");
                if (!"Any Class".equals(cls) && !aCls)
                    sb.append("- No flights offering class: ").append(cls).append("\n");
                errLbl.setText(sb.toString());
            } else { disp[0] = res; render.run(); }
        });

        clearBtn.setOnAction(e -> {
            fromField.clear(); toBox.setValue("Any Arrival");
            depDp.setValue(null); clsBox.setValue("Any Class"); errLbl.setText("");
            disp[0] = all.stream().filter(s -> !"CANCELLED".equals(s.getStatus())
                    && isFlight(s, tMap)).collect(Collectors.toList());
            render.run();
        });

        Label hdr = new Label("Filter Flights");
        hdr.setStyle("-fx-font-weight:bold;-fx-text-fill:#1F294C;-fx-font-size:13px;");

        GridPane g = buildFormGrid(158, 215);
        int r = 0;
        g.addRow(r++, fl("From (airport/city)"), fromField);
        g.addRow(r++, fl("To (arrival city)"),   toBox);
        g.addRow(r++, fl("Departure date"),      depDp);   // only departure date
        g.addRow(r++, fl("Travel class"),        clsBox);

        HBox btnRow = new HBox(10, searchBtn, clearBtn); btnRow.setAlignment(Pos.CENTER_LEFT);
        VBox p = new VBox(10, hdr, g, btnRow, errLbl);
        p.setPadding(new Insets(14));
        p.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.07),6,0,0,2);");
        return p;
    }

    /**
     * FIX: Vehicle search date logic corrected.
     * The user searches for a rental period they WANT.
     * The admin sets the availability window for the vehicle.
     * A vehicle is shown if:   admin.rentalStart <= user.wantedStart
     *                     AND  admin.rentalEnd   >= user.wantedEnd
     * i.e. the vehicle's availability window CONTAINS the user's requested period.
     */
    @SuppressWarnings("unchecked")
    private VBox buildVehicleSearchPanel(List<Schedule> all, Map<Integer, Transport> tMap,
                                         Map<Long, String> dNames,
                                         List<Schedule>[] disp, Runnable render) {
        List<Long> depIds = all.stream().filter(s -> isVehicle(s, tMap))
                .map(Schedule::getDepartureDestinationId).distinct().sorted().collect(Collectors.toList());
        ComboBox<String> placeBox = destCombo(depIds, "Any Location", dNames);

        DatePicker startDp = dp("Rental start (optional)");
        DatePicker endDp   = dp("Rental end (optional)");

        Label errLbl = errLabel();
        Button searchBtn = mkBtn("Search Vehicles", BTN_TEAL);
        Button clearBtn  = mkBtn("Clear",           BTN_DARK);

        searchBtn.setOnAction(e -> {
            errLbl.setText("");
            Long      place = parseDestId(placeBox.getValue(), dNames);
            LocalDate wStart = startDp.getValue();   // what the user WANTS to start
            LocalDate wEnd   = endDp.getValue();     // what the user WANTS to end

            boolean aP=false, aS=false, aE=false;
            List<Schedule> res = new ArrayList<>();

            for (Schedule s : all) {
                if (!"CANCELLED".equals(s.getStatus()) && isVehicle(s, tMap)) {
                    boolean ok1 = place == null
                            || s.getDepartureDestinationId() == place;

                    // FIX: admin's window must START on or before the user's wanted start
                    boolean ok2 = wStart == null || s.getRentalStart() == null
                            || !s.getRentalStart().toLocalDate().isAfter(wStart);

                    // FIX: admin's window must END on or after the user's wanted end
                    boolean ok3 = wEnd == null || s.getRentalEnd() == null
                            || !s.getRentalEnd().toLocalDate().isBefore(wEnd);

                    if (ok1) aP=true; if (ok2) aS=true; if (ok3) aE=true;
                    if (ok1&&ok2&&ok3) res.add(s);
                }
            }

            if (res.isEmpty()) {
                StringBuilder sb = new StringBuilder("No vehicles found:\n");
                if (place  != null && !aP)
                    sb.append("- No vehicles at ").append(placeBox.getValue()).append("\n");
                if (wStart != null && !aS)
                    sb.append("- No vehicles available from ").append(wStart.format(DATE_FMT)).append("\n");
                if (wEnd   != null && !aE)
                    sb.append("- No vehicles available until ").append(wEnd.format(DATE_FMT)).append("\n");
                errLbl.setText(sb.toString());
            } else { disp[0] = res; render.run(); }
        });

        clearBtn.setOnAction(e -> {
            placeBox.setValue("Any Location"); startDp.setValue(null); endDp.setValue(null);
            errLbl.setText("");
            disp[0] = all.stream().filter(s -> !"CANCELLED".equals(s.getStatus())
                    && isVehicle(s, tMap)).collect(Collectors.toList());
            render.run();
        });

        Label hdr = new Label("Filter Vehicles");
        hdr.setStyle("-fx-font-weight:bold;-fx-text-fill:#1F294C;-fx-font-size:13px;");

        GridPane g = buildFormGrid(158, 215);
        int r = 0;
        g.addRow(r++, fl("Your Location"),  placeBox);
        g.addRow(r++, fl("Rental Start"),   startDp);
        g.addRow(r++, fl("Rental End"),     endDp);

        HBox btnRow = new HBox(10, searchBtn, clearBtn); btnRow.setAlignment(Pos.CENTER_LEFT);
        VBox p = new VBox(10, hdr, g, btnRow, errLbl);
        p.setPadding(new Insets(14));
        p.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.07),6,0,0,2);");
        return p;
    }

    /* ── Schedule result card ── */
    private VBox buildScheduleCard(Schedule s, Transport t,
                                   Map<Integer, Transport> tMap, Map<Long, String> dn) {
        boolean flight = "FLIGHT".equals(t.getTransportType());

        boolean unavail; String availText, availColor;
        if (flight) {
            int rem = remainingSeatsForFlight(s, tMap);
            unavail    = rem <= 0;
            availText  = unavail ? "Fully Booked" : rem + " seat" + (rem > 1 ? "s" : "") + " left";
            availColor = unavail ? "#c0392b" : (rem <= 5 ? "#E07020" : "#27ae60");
        } else {
            // FIX: any non-cancelled booking locks the vehicle
            unavail    = isVehicleScheduleUnavailable(s.getScheduleId());
            availText  = unavail ? "Unavailable" : "Available";
            availColor = unavail ? "#c0392b" : "#27ae60";
        }

        String hdrColor = unavail ? "#888" : (flight ? "#1F294C" : "#2C5F6E");

        Label provLbl  = new Label(t.getProviderName());
        provLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:15px;");
        Label modelLbl = new Label(t.getVehicleModel());
        modelLbl.setStyle("-fx-text-fill:rgba(255,255,255,.75);-fx-font-size:12px;");
        VBox hdr = new VBox(3, provLbl, modelLbl);
        hdr.setStyle("-fx-background-color:" + hdrColor +
                ";-fx-padding:14 16 14 16;-fx-background-radius:8 8 0 0;");

        String sColor = switch (s.getStatus()) {
            case "DELAYED"   -> "#E07020";
            case "CANCELLED" -> "#c0392b";
            default          -> "#27ae60";
        };
        HBox badges = new HBox(5,
                badge(flight ? "FLIGHT" : "VEHICLE", "#4FB3B5"),
                badge(s.getStatus().replace("_"," "), sColor),
                badge(availText, availColor),
                badge(s.getTravelClass() != null ? s.getTravelClass() : "N/A", "#7B68EE"));

        Label routeLbl = new Label(destName(s.getDepartureDestinationId())
                + "  ->  " + destName(s.getArrivalDestinationId()));
        routeLbl.setStyle("-fx-text-fill:#444;-fx-font-size:12px;"); routeLbl.setWrapText(true);

        Label timeLbl;
        if (flight) {
            String dep = s.getDepartureDatetime() != null
                    ? s.getDepartureDatetime().format(DT_FMT) : "N/A";
            String arr = s.getArrivalDatetime() != null
                    ? s.getArrivalDatetime().format(DT_FMT) : "N/A";
            timeLbl = new Label(dep + "  ->  " + arr);
        } else {
            String rs = s.getRentalStart() != null ? s.getRentalStart().format(DATE_FMT) : "N/A";
            String re = s.getRentalEnd()   != null ? s.getRentalEnd()  .format(DATE_FMT) : "N/A";
            timeLbl = new Label("Rental: " + rs + "  -  " + re);
        }
        timeLbl.setStyle("-fx-text-fill:#555;-fx-font-size:11px;"); timeLbl.setWrapText(true);

        double price = t.getBasePrice() * s.getPriceMultiplier() * classMultiplier(s.getTravelClass());
        Label priceLbl = new Label(String.format("from  %.2f EUR", price));
        priceLbl.setStyle("-fx-text-fill:#1F294C;-fx-font-weight:bold;-fx-font-size:14px;");
        Label ecoLbl = new Label(String.format("Eco: %.1f / 5", t.getSustainabilityRating()));
        ecoLbl.setStyle("-fx-text-fill:#27ae60;-fx-font-size:11px;");

        VBox body = new VBox(8, badges, routeLbl, timeLbl, priceLbl, ecoLbl);
        body.setPadding(new Insets(12,16,12,16));

        Button bookBtn = new Button(unavail ? "Unavailable" : "Book Now");
        bookBtn.setStyle(unavail ? BTN_BOOKED : BTN_BOOK);
        bookBtn.setMaxWidth(Double.MAX_VALUE); bookBtn.setDisable(unavail);
        if (!unavail) bookBtn.setOnAction(e -> openBookingForm(t, s, null));

        VBox card = new VBox(hdr, body, bookBtn);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.12),10,0,0,3);");
        card.setPrefWidth(285); card.setMaxWidth(285);
        return card;
    }

    /* ═══════════════════════════════════════════════════════
       TRANSPORT TAB
       ═══════════════════════════════════════════════════════ */

    @FXML public void showTransportTab() {
        setActiveTab(btnTransport);
        openTransportTypePopup();
    }

    private void openTransportTypePopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(contentArea.getScene().getWindow());
        popup.setTitle("Choose Transport Type");
        popup.setResizable(false);

        Label hdr = new Label("What are you looking for?");
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox hdrBar = new HBox(hdr);
        hdrBar.setStyle("-fx-background-color:#1F294C;-fx-padding:18 24 18 24;");
        hdrBar.setAlignment(Pos.CENTER_LEFT);

        VBox fc = typeCard("Flight",        "Search & book scheduled\nflights worldwide", "#1F294C");
        VBox vc = typeCard("Vehicle Rental","Rent a car or vehicle\nfor your trip",       "#2C5F6E");
        fc.setOnMouseClicked(e -> { popup.close(); buildProviderList("FLIGHT");  });
        vc.setOnMouseClicked(e -> { popup.close(); buildProviderList("VEHICLE"); });

        HBox body = new HBox(24, fc, vc);
        body.setAlignment(Pos.CENTER); body.setPadding(new Insets(30, 36, 30, 36));
        body.setStyle("-fx-background-color:#F1EAE7;");
        popup.setScene(new Scene(new VBox(hdrBar, body), 520, 260));
        popup.showAndWait();
    }

    private VBox typeCard(String title, String sub, String color) {
        Label tl = new Label(title);
        tl.setStyle("-fx-text-fill:white;-fx-font-size:18px;-fx-font-weight:bold;");
        Label sl = new Label(sub);
        sl.setStyle("-fx-text-fill:rgba(255,255,255,.82);-fx-font-size:12px;");
        sl.setWrapText(true); sl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox card = new VBox(10, tl, sl);
        card.setAlignment(Pos.CENTER); card.setPrefSize(182, 130); card.setPadding(new Insets(18));
        String base = "-fx-background-color:" + color + ";-fx-background-radius:12;-fx-cursor:hand;";
        card.setStyle(base + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.20),12,0,0,4);");
        card.setOnMouseEntered(e -> card.setStyle(base +
                "-fx-scale-x:1.04;-fx-scale-y:1.04;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.32),18,0,0,6);"));
        card.setOnMouseExited(e  -> card.setStyle(base +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.20),12,0,0,4);"));
        return card;
    }

    private void buildProviderList(String type) {
        List<Transport> typed = transportService.getAllTransports().stream()
                .filter(t -> type.equals(t.getTransportType()) && t.isActive())
                .collect(Collectors.toList());

        Map<String, Long> counts = typed.stream()
                .collect(Collectors.groupingBy(Transport::getProviderName, Collectors.counting()));

        Label title   = sectionTitle((type.equals("FLIGHT") ? "Flights" : "Vehicles") + " — Choose a Provider");
        Button backBtn = mkBtn("Back", BTN_DARK);
        backBtn.setOnAction(e -> openTransportTypePopup());

        HBox topBar = hbox(title, new Region(), backBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        FlowPane fp = new FlowPane();
        fp.setHgap(20); fp.setVgap(20); fp.setPadding(new Insets(12, 0, 12, 0));

        String[] palette = {"#1F294C","#2C5F6E","#4FB3B5","#E07020","#7B68EE",
                "#27ae60","#F06E32","#34495e","#8e44ad","#16a085"};
        int[] idx = {0};

        for (Map.Entry<String, Long> en : counts.entrySet()) {
            String prov = en.getKey(); long cnt = en.getValue();
            String col  = palette[idx[0]++ % palette.length];
            Label nl = new Label(prov);
            nl.setStyle("-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;");
            nl.setWrapText(true); nl.setMaxWidth(180);
            Label cl = new Label(cnt + " transport" + (cnt > 1 ? "s" : ""));
            cl.setStyle("-fx-text-fill:rgba(255,255,255,.82);-fx-font-size:12px;");
            VBox pc = new VBox(10, nl, cl);
            pc.setAlignment(Pos.CENTER); pc.setPrefSize(200, 110); pc.setPadding(new Insets(18));
            String base = "-fx-background-color:" + col + ";-fx-background-radius:10;-fx-cursor:hand;";
            pc.setStyle(base + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.16),10,0,0,3);");
            pc.setOnMouseEntered(ev -> pc.setStyle(base +
                    "-fx-scale-x:1.04;-fx-scale-y:1.04;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.28),16,0,0,5);"));
            pc.setOnMouseExited(ev  -> pc.setStyle(base +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.16),10,0,0,3);"));
            pc.setOnMouseClicked(ev -> buildTransportCards(type, prov));
            fp.getChildren().add(pc);
        }

        if (counts.isEmpty()) fp.getChildren().add(new Label("No providers found.") {{
            setStyle("-fx-font-size:14px;-fx-text-fill:#888;"); }});

        ScrollPane sc = scrollPane(fp); VBox.setVgrow(sc, Priority.ALWAYS);
        VBox layout = new VBox(16, topBar, sc); VBox.setVgrow(sc, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    private void buildTransportCards(String type, String provider) {
        List<Transport> list = transportService.getAllTransports().stream()
                .filter(t -> type.equals(t.getTransportType())
                        && provider.equals(t.getProviderName()) && t.isActive())
                .collect(Collectors.toList());

        Label title    = sectionTitle(provider);
        Button backBtn = mkBtn("Back to Providers", BTN_DARK);
        backBtn.setOnAction(e -> buildProviderList(type));

        HBox topBar = hbox(title, new Region(), backBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        FlowPane fp = new FlowPane();
        fp.setHgap(18); fp.setVgap(18); fp.setPadding(new Insets(8, 0, 8, 0));
        list.forEach(t -> fp.getChildren().add(buildTransportCard(t)));

        ScrollPane sc = scrollPane(fp); VBox.setVgrow(sc, Priority.ALWAYS);
        VBox layout = new VBox(14, topBar, sc); VBox.setVgrow(sc, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    private VBox buildTransportCard(Transport t) {
        boolean fl = "FLIGHT".equals(t.getTransportType());
        String hc  = fl ? "#1F294C" : "#2C5F6E";

        Label prov = new Label(t.getProviderName());
        prov.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:15px;");
        Label mdl  = new Label(t.getVehicleModel());
        mdl.setStyle("-fx-text-fill:rgba(255,255,255,.75);-fx-font-size:12px;");
        VBox hdr = new VBox(3, prov, mdl);
        hdr.setStyle("-fx-background-color:" + hc +
                ";-fx-padding:14 16 14 16;-fx-background-radius:8 8 0 0;");

        HBox badges = new HBox(6,
                badge(fl ? "FLIGHT" : "VEHICLE", "#4FB3B5"),
                badge(String.format("Eco: %.1f", t.getSustainabilityRating()), "#27ae60"));

        Label pr = new Label(String.format("from  %.2f EUR", t.getBasePrice()));
        pr.setStyle("-fx-text-fill:#1F294C;-fx-font-weight:bold;-fx-font-size:14px;");
        Label cp = new Label("Capacity: " + t.getCapacity() + "   |   " + t.getAvailableUnits() + " unit(s)");
        cp.setStyle("-fx-text-fill:#555;-fx-font-size:11px;");
        Label am = new Label(t.getAmenities() != null ? t.getAmenities() : "—");
        am.setStyle("-fx-text-fill:#666;-fx-font-size:11px;"); am.setWrapText(true); am.setMaxWidth(240);

        VBox body = new VBox(8, badges, pr, cp, am); body.setPadding(new Insets(12,16,12,16));

        Button det = new Button("View Details");
        det.setStyle(BTN_DETAIL); det.setMaxWidth(Double.MAX_VALUE);
        det.setOnAction(e -> showTransportDetail(t));

        Button bk = new Button("Book Direct");
        bk.setStyle(BTN_BOOK); bk.setMaxWidth(Double.MAX_VALUE);
        bk.setOnAction(e -> openBookingForm(t, null, null));

        HBox btns = new HBox(det, bk);
        HBox.setHgrow(det, Priority.ALWAYS); HBox.setHgrow(bk, Priority.ALWAYS);

        VBox card = new VBox(hdr, body, btns);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.12),10,0,0,3);");
        card.setPrefWidth(285); card.setMaxWidth(285);
        return card;
    }

    /**
     * Transport detail popup.
     * FIX: booking from within this popup now works correctly because:
     * 1) We close this popup first, then call openBookingForm
     * 2) openBookingForm receives the ownerStage so the new popup
     *    is properly owned by the main window (avoids nested-modal issues)
     */
    private void showTransportDetail(Transport t) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(contentArea.getScene().getWindow());
        popup.setTitle(t.getProviderName() + " Details");
        popup.setResizable(false);

        boolean fl = "FLIGHT".equals(t.getTransportType());
        String hc  = fl ? "#1F294C" : "#2C5F6E";

        Map<Integer, Transport> tMap = new HashMap<>();
        transportService.getAllTransports().forEach(tr -> tMap.put(tr.getTransportId(), tr));

        Label pl = new Label(t.getProviderName());
        pl.setStyle("-fx-text-fill:white;-fx-font-size:19px;-fx-font-weight:bold;");
        Label ml = new Label(t.getVehicleModel());
        ml.setStyle("-fx-text-fill:rgba(255,255,255,.8);-fx-font-size:13px;");
        VBox hdrBox = new VBox(3, pl, ml);
        hdrBox.setStyle("-fx-background-color:" + hc + ";-fx-padding:20 24 20 24;");

        GridPane g = buildFormGrid(160, 280);
        int r = 0;
        g.addRow(r++, fl("Provider"),        dv(t.getProviderName()));
        g.addRow(r++, fl("Model"),           dv(t.getVehicleModel()));
        g.addRow(r++, fl("Type"),            dv(t.getTransportType()));
        g.addRow(r++, fl("Base Price"),      dv(String.format("%.2f EUR", t.getBasePrice())));
        g.addRow(r++, fl("Capacity"),        dv(String.valueOf(t.getCapacity())));
        g.addRow(r++, fl("Available Units"), dv(String.valueOf(t.getAvailableUnits())));
        g.addRow(r++, fl("Eco Rating"),      dv(String.format("%.1f / 5.0", t.getSustainabilityRating())));
        g.addRow(r++, fl("Amenities"),       dv(t.getAmenities() != null ? t.getAmenities() : "None"));
        g.addRow(r++, fl("Status"),          dv(t.isActive() ? "Active" : "Inactive"));

        List<Schedule> scheds = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getTransportId() == t.getTransportId()
                        && !"CANCELLED".equals(s.getStatus()))
                .collect(Collectors.toList());

        Label st = new Label("Available Schedules (" + scheds.size() + ")");
        st.setStyle("-fx-font-weight:bold;-fx-text-fill:#1F294C;-fx-font-size:13px;-fx-padding:6 0 4 0;");

        VBox sl = new VBox(6);
        for (Schedule s : scheds) {
            boolean unavail; String avlbl;
            if (fl) {
                int rem = remainingSeatsForFlight(s, tMap);
                unavail = rem <= 0;
                avlbl   = unavail ? "Full" : rem + " seats left";
            } else {
                unavail = isVehicleScheduleUnavailable(s.getScheduleId());
                avlbl   = unavail ? "Booked" : "Available";
            }

            String info = fl
                    ? "#" + s.getScheduleId() + "  "
                    + destName(s.getDepartureDestinationId()) + " -> "
                    + destName(s.getArrivalDestinationId()) + "\n"
                    + (s.getDepartureDatetime()!=null ? s.getDepartureDatetime().format(DT_FMT) : "N/A")
                    + " -> "
                    + (s.getArrivalDatetime()!=null ? s.getArrivalDatetime().format(DT_FMT) : "N/A")
                    + "  |  Class: " + s.getTravelClass() + "  |  " + avlbl
                    : "#" + s.getScheduleId() + "  "
                    + destName(s.getDepartureDestinationId())
                    + "\nRental: "
                    + (s.getRentalStart()!=null ? s.getRentalStart().format(DATE_FMT) : "N/A")
                    + " - "
                    + (s.getRentalEnd()!=null ? s.getRentalEnd().format(DATE_FMT) : "N/A")
                    + "  |  " + avlbl;

            Label il = new Label(info);
            il.setStyle("-fx-text-fill:#333;-fx-font-size:11px;"); il.setWrapText(true);
            HBox.setHgrow(il, Priority.ALWAYS);

            Button bb = mkBtn("Book", unavail
                    ? "-fx-background-color:#aaa;-fx-text-fill:white;-fx-padding:5 12 5 12;-fx-background-radius:5;"
                    : BTN_TEAL.replace("9 22 9 22", "5 12 5 12") + " -fx-font-size:11px;");
            bb.setDisable(unavail);
            Schedule fs = s;
            // FIX: close detail popup first, then open booking form owned by main window
            bb.setOnAction(ev -> { popup.close(); openBookingForm(t, fs, null); });

            HBox row = new HBox(10, il, bb); row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#F1EAE7;-fx-padding:8 12 8 12;-fx-background-radius:5;");
            sl.getChildren().add(row);
        }
        if (scheds.isEmpty()) sl.getChildren().add(new Label("No active schedules available.") {{
            setStyle("-fx-text-fill:#888;-fx-font-size:12px;"); }});

        ScrollPane ss = new ScrollPane(sl);
        ss.setFitToWidth(true); ss.setPrefHeight(170);
        ss.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");

        Button bd = mkBtn("Book Direct (No Schedule)", BTN_TEAL);
        // FIX: close detail popup before opening booking form
        bd.setOnAction(e -> { popup.close(); openBookingForm(t, null, null); });
        Button cb = mkBtn("Close", BTN_DARK); cb.setOnAction(e -> popup.close());

        ScrollPane gs = new ScrollPane(g);
        gs.setFitToWidth(true); gs.setPrefHeight(230);
        gs.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");

        HBox br = hbox(bd, cb); br.setAlignment(Pos.CENTER_RIGHT); br.setPadding(new Insets(10,0,0,0));
        VBox content = new VBox(14, gs, st, ss, br);
        content.setPadding(new Insets(20)); content.setStyle("-fx-background-color:#F1EAE7;");
        popup.setScene(new Scene(new VBox(hdrBox, content), 580, 640));
        popup.showAndWait();
    }

    /* ═══════════════════════════════════════════════════════
       MY BOOKINGS TAB
       ═══════════════════════════════════════════════════════ */

    @FXML public void showMyBookingsTab() {
        setActiveTab(btnMyBookings);
        buildMyBookingsView();
    }

    private void buildMyBookingsView() {
        List<Bookingtrans> bks = bookingService.getBookingsByUserId(currentUserId);
        Map<Integer, Transport> tMap = new HashMap<>();
        transportService.getAllTransports().forEach(t -> tMap.put(t.getTransportId(), t));

        Label title   = sectionTitle("My Bookings");
        Button refreshBtn = mkBtn("Refresh", BTN_DARK);
        refreshBtn.setOnAction(e -> buildMyBookingsView());
        HBox topBar = hbox(title, new Region(), refreshBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        long total = bks.size();
        long conf  = bks.stream().filter(b -> "CONFIRMED".equals(b.getBookingStatus())).count();
        long pend  = bks.stream().filter(b -> "PENDING".equals(b.getBookingStatus())).count();
        double spent = bks.stream().filter(b -> !"CANCELLED".equals(b.getBookingStatus()))
                .mapToDouble(Bookingtrans::getTotalPrice).sum();

        Label sum = new Label(String.format(
                "Total: %d   |   Confirmed: %d   |   Pending: %d   |   Total Spent: %.2f EUR",
                total, conf, pend, spent));
        sum.setStyle("-fx-text-fill:#1F294C;-fx-font-weight:bold;-fx-background-color:white;" +
                "-fx-padding:10 16 10 16;-fx-background-radius:6;-fx-font-size:12px;");

        FlowPane fp = new FlowPane(); fp.setHgap(18); fp.setVgap(18);
        fp.setPadding(new Insets(8,0,8,0));
        if (bks.isEmpty()) {
            fp.getChildren().add(new Label("No bookings yet. Start browsing!") {{
                setStyle("-fx-font-size:14px;-fx-text-fill:#888;"); }});
        } else {
            bks.forEach(b -> fp.getChildren().add(buildBookingCard(b, tMap.get(b.getTransportId()))));
        }

        ScrollPane sc = scrollPane(fp); VBox.setVgrow(sc, Priority.ALWAYS);
        VBox layout = new VBox(14, topBar, sum, sc); VBox.setVgrow(sc, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    /**
     * FIX: Booking card now shows transport TYPE and PROVIDER for each booking.
     */
    private VBox buildBookingCard(Bookingtrans b, Transport t) {
        String sc = switch (b.getBookingStatus()) {
            case "CONFIRMED" -> "#27ae60"; case "CANCELLED" -> "#c0392b"; default -> "#E07020";
        };

        // FIX: show provider name + transport type in the header
        String provText = t != null ? t.getProviderName() : "Transport #" + b.getTransportId();
        String typeText = t != null ? t.getTransportType() : "UNKNOWN";

        Label bid  = new Label("Booking #" + b.getBookingId());
        bid.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;");
        Label provLbl = new Label(provText + " (" + typeText + ")");
        provLbl.setStyle("-fx-text-fill:rgba(255,255,255,.90);-fx-font-size:12px;");
        VBox hdr = new VBox(3, bid, provLbl);
        hdr.setStyle("-fx-background-color:" + sc +
                ";-fx-padding:12 16 12 16;-fx-background-radius:8 8 0 0;");

        String pc = switch (b.getPaymentStatus()) {
            case "PAID" -> "#27ae60"; case "REFUNDED" -> "#3498db"; default -> "#888";
        };

        // FIX: transport type badge visible on card body too
        String typeBadgeColor = "FLIGHT".equals(typeText) ? "#1F294C" : "#2C5F6E";
        HBox badges = new HBox(6,
                badge(b.getBookingStatus(), sc),
                badge(b.getPaymentStatus(), pc),
                badge(typeText, typeBadgeColor));

        Label dl  = new Label("Booked: " + (b.getBookingDate()!=null
                ? b.getBookingDate().format(DT_FMT) : "N/A"));
        dl.setStyle("-fx-text-fill:#555;-fx-font-size:11px;");

        // FIX: show model if available
        if (t != null) {
            Label modelBadge = new Label("Model: " + t.getVehicleModel());
            modelBadge.setStyle("-fx-text-fill:#666;-fx-font-size:11px;");
        }

        Label sl  = new Label(b.getTotalSeats() + " seat(s)  ("
                + b.getAdultsCount() + " adults, " + b.getChildrenCount() + " children)");
        sl.setStyle("-fx-text-fill:#555;-fx-font-size:11px;");

        boolean direct = b.getScheduleId() == 0;
        Label scl = new Label(direct ? "Direct booking (no schedule)" : "Schedule #" + b.getScheduleId());
        scl.setStyle("-fx-text-fill:" + (direct?"#E07020":"#4FB3B5") + ";-fx-font-size:11px;");

        Label mdl = new Label(t != null ? "Model: " + t.getVehicleModel() : "");
        mdl.setStyle("-fx-text-fill:#666;-fx-font-size:11px;");

        Label pl  = new Label(String.format("Total: %.2f EUR", b.getTotalPrice()));
        pl.setStyle("-fx-text-fill:#1F294C;-fx-font-weight:bold;-fx-font-size:13px;");

        Label ins = new Label(b.isInsuranceIncluded() ? "Insurance: Yes" : "Insurance: No");
        ins.setStyle("-fx-text-fill:" + (b.isInsuranceIncluded()?"#27ae60":"#999") + ";-fx-font-size:11px;");

        VBox body = new VBox(6, badges, dl, mdl, sl, scl, pl, ins);
        body.setPadding(new Insets(12,16,12,16));

        Button det = new Button("Details"); det.setStyle(BTN_DETAIL); det.setMaxWidth(Double.MAX_VALUE);
        det.setOnAction(e -> viewBookingDetail(b));

        boolean cancelled = "CANCELLED".equals(b.getBookingStatus());
        Button act;
        if (direct && !cancelled) {
            act = new Button("Add Schedule");
            act.setStyle("-fx-background-color:#4FB3B5;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:11px;-fx-padding:8 0 8 0;-fx-cursor:hand;-fx-background-radius:0 0 8 8;");
            act.setMaxWidth(Double.MAX_VALUE);
            act.setOnAction(e -> openScheduleSelectForBooking(b, t));
        } else if ("PENDING".equals(b.getBookingStatus())) {
            act = new Button("Cancel");
            act.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:11px;-fx-padding:8 0 8 0;-fx-cursor:hand;-fx-background-radius:0 0 8 8;");
            act.setMaxWidth(Double.MAX_VALUE);
            act.setOnAction(e -> {
                if (confirm("Cancel booking #" + b.getBookingId() + "?")) {
                    b.setBookingStatus("CANCELLED"); b.setCancellationReason("Cancelled by user");
                    bookingService.updateBookingtrans(b);
                    showSuccess("Booking #" + b.getBookingId() + " cancelled.");
                    buildMyBookingsView();
                }
            });
        } else {
            act = new Button(cancelled ? "Cancelled" : "Confirmed");
            act.setStyle("-fx-background-color:#ddd;-fx-text-fill:#888;-fx-font-size:11px;" +
                    "-fx-padding:8 0 8 0;-fx-background-radius:0 0 8 8;");
            act.setMaxWidth(Double.MAX_VALUE); act.setDisable(true);
        }

        HBox br = new HBox(det, act);
        HBox.setHgrow(det, Priority.ALWAYS); HBox.setHgrow(act, Priority.ALWAYS);

        VBox card = new VBox(hdr, body, br);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.12),10,0,0,3);");
        card.setPrefWidth(285); card.setMaxWidth(285);
        return card;
    }

    private void viewBookingDetail(Bookingtrans b) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(contentArea.getScene().getWindow());
        popup.setTitle("Booking #" + b.getBookingId()); popup.setResizable(false);

        Label h = new Label("Booking #" + b.getBookingId() + "  Details");
        h.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox hb = new HBox(h); hb.setStyle("-fx-background-color:#1F294C;-fx-padding:16 22 16 22;");

        Map<Integer, Transport> tMap = new HashMap<>();
        transportService.getAllTransports().forEach(t -> tMap.put(t.getTransportId(), t));
        Transport t = tMap.get(b.getTransportId());
        String pname  = t != null ? t.getProviderName() : "Transport #" + b.getTransportId();
        String ttype  = t != null ? t.getTransportType() : "N/A";
        String tmodel = t != null ? t.getVehicleModel()  : "N/A";

        GridPane g = buildFormGrid(160, 270); int r = 0;
        g.addRow(r++, fl("Provider"),       dv(pname));
        g.addRow(r++, fl("Transport Type"), dv(ttype));
        g.addRow(r++, fl("Model"),          dv(tmodel));
        g.addRow(r++, fl("Transport ID"),   dv("#" + b.getTransportId()));
        g.addRow(r++, fl("Schedule"),       dv(b.getScheduleId()>0
                ? "Schedule #" + b.getScheduleId() : "Direct (no schedule)"));
        g.addRow(r++, fl("Booked On"),      dv(b.getBookingDate()!=null
                ? b.getBookingDate().format(DT_FMT) : "N/A"));
        g.addRow(r++, fl("Adults"),         dv(String.valueOf(b.getAdultsCount())));
        g.addRow(r++, fl("Children"),       dv(String.valueOf(b.getChildrenCount())));
        g.addRow(r++, fl("Total Seats"),    dv(String.valueOf(b.getTotalSeats())));
        g.addRow(r++, fl("Total Price"),    dv(String.format("%.2f EUR", b.getTotalPrice())));
        g.addRow(r++, fl("Booking Status"), dv(b.getBookingStatus()));
        g.addRow(r++, fl("Payment"),        dv(b.getPaymentStatus()));
        g.addRow(r++, fl("Insurance"),      dv(b.isInsuranceIncluded() ? "Yes (+25 EUR/seat)" : "No"));
        if ("VEHICLE".equals(ttype)) {
            String pickupCoords = b.getPickupLatitude() != null && b.getPickupLongitude() != null
                    ? String.format("%.6f, %.6f", b.getPickupLatitude(), b.getPickupLongitude())
                    : "-";
            String dropoffCoords = b.getDropoffLatitude() != null && b.getDropoffLongitude() != null
                    ? String.format("%.6f, %.6f", b.getDropoffLatitude(), b.getDropoffLongitude())
                    : "-";
            g.addRow(r++, fl("Pickup Coords"), dv(pickupCoords));
            g.addRow(r++, fl("Pickup Address"), dv(b.getPickupAddress() != null ? b.getPickupAddress() : "-"));
            g.addRow(r++, fl("Drop-off Coords"), dv(dropoffCoords));
            g.addRow(r++, fl("Drop-off Address"), dv(b.getDropoffAddress() != null ? b.getDropoffAddress() : "-"));
        }
        // Cancellation reason and QR code intentionally hidden in booking details popup.

        Button cb = mkBtn("Close", BTN_DARK); cb.setOnAction(e -> popup.close());
        HBox br = new HBox(cb); br.setAlignment(Pos.CENTER_RIGHT); br.setPadding(new Insets(10,0,0,0));
        VBox content = new VBox(14, g, br);
        content.setPadding(new Insets(20)); content.setStyle("-fx-background-color:#F1EAE7;");
        popup.setScene(new Scene(new VBox(hb, content), 480, 550));
        popup.showAndWait();
    }

    private void openScheduleSelectForBooking(Bookingtrans b, Transport t) {
        if (t == null) { showAlert("Transport details unavailable."); return; }
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(contentArea.getScene().getWindow());
        popup.setTitle("Add Schedule to Booking #" + b.getBookingId()); popup.setResizable(false);

        Label h = new Label("Select a Schedule — Booking #" + b.getBookingId());
        h.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox hb = new HBox(h); hb.setStyle("-fx-background-color:#1F294C;-fx-padding:16 22 16 22;");

        Map<Integer, Transport> tMap = new HashMap<>();
        transportService.getAllTransports().forEach(tr -> tMap.put(tr.getTransportId(), tr));
        boolean fl = "FLIGHT".equals(t.getTransportType());

        List<Schedule> eligible = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getTransportId() == t.getTransportId()
                        && !"CANCELLED".equals(s.getStatus()))
                .filter(s -> fl ? remainingSeatsForFlight(s, tMap) >= b.getTotalSeats()
                        : !isVehicleScheduleUnavailable(s.getScheduleId()))
                .collect(Collectors.toList());

        VBox list = new VBox(8);
        if (eligible.isEmpty()) {
            list.getChildren().add(new Label("No eligible schedules available.") {{
                setStyle("-fx-font-size:13px;-fx-text-fill:#888;"); }});
        } else {
            for (Schedule s : eligible) {
                String info = fl
                        ? "#" + s.getScheduleId() + "  "
                        + destName(s.getDepartureDestinationId()) + " -> "
                        + destName(s.getArrivalDestinationId()) + "\n"
                        + (s.getDepartureDatetime()!=null?s.getDepartureDatetime().format(DT_FMT):"N/A")
                        + " -> "
                        + (s.getArrivalDatetime()!=null?s.getArrivalDatetime().format(DT_FMT):"N/A")
                        + "  |  " + s.getTravelClass()
                        + "  |  " + remainingSeatsForFlight(s, tMap) + " seats left"
                        : "#" + s.getScheduleId() + "  "
                        + destName(s.getDepartureDestinationId())
                        + "\nRental: "
                        + (s.getRentalStart()!=null?s.getRentalStart().format(DATE_FMT):"N/A")
                        + " - "
                        + (s.getRentalEnd()!=null?s.getRentalEnd().format(DATE_FMT):"N/A");

                Label il = new Label(info);
                il.setStyle("-fx-text-fill:#333;-fx-font-size:12px;"); il.setWrapText(true);
                HBox.setHgrow(il, Priority.ALWAYS);
                Button sb = mkBtn("Select", BTN_TEAL);
                Schedule fs = s;
                sb.setOnAction(e -> {
                    if (confirm("Link Schedule #" + fs.getScheduleId()
                            + " to Booking #" + b.getBookingId() + "?")) {
                        b.setScheduleId(fs.getScheduleId());
                        bookingService.updateBookingtrans(b);
                        popup.close();
                        showSuccess("Schedule #" + fs.getScheduleId()
                                + " linked to Booking #" + b.getBookingId() + "!");
                        buildMyBookingsView();
                    }
                });
                HBox row = new HBox(12, il, sb); row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10,14,10,14));
                row.setStyle("-fx-background-color:white;-fx-background-radius:6;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.07),4,0,0,1);");
                list.getChildren().add(row);
            }
        }

        ScrollPane sc = new ScrollPane(list); sc.setFitToWidth(true); sc.setPrefHeight(300);
        sc.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        Button cb = mkBtn("Cancel", BTN_DARK); cb.setOnAction(e -> popup.close());
        HBox br = new HBox(cb); br.setAlignment(Pos.CENTER_RIGHT); br.setPadding(new Insets(10,0,0,0));
        VBox content = new VBox(14, sc, br);
        content.setPadding(new Insets(20)); content.setStyle("-fx-background-color:#F1EAE7;");
        popup.setScene(new Scene(new VBox(hb, content), 560, 440));
        popup.showAndWait();
    }

    /* ═══════════════════════════════════════════════════════
       BOOKING FORM
       FIX: Added ownerStage parameter so the popup is always
       properly owned, fixing the "void/static" issue that
       occurred when called from within another modal popup.
       ═══════════════════════════════════════════════════════ */

    private double classMultiplier(String cls) {
        return switch (cls == null ? "ECONOMY" : cls) {
            case "PREMIUM"  -> 1.5; case "BUSINESS" -> 2.2; case "FIRST" -> 3.0; default -> 1.0;
        };
    }

    /**
     * Opens the booking form.
     * @param transport  The transport (may be null if coming from a schedule only)
     * @param schedule   The schedule (null = direct/no-schedule booking)
     * @param ownerStage Pass the calling stage here when calling from inside another popup,
     *                   so JavaFX correctly stacks the modals. Pass null to use main window.
     */
    private void openBookingForm(Transport transport, Schedule schedule, Stage ownerStage) {
        // Always resolve the transport object
        Map<Integer, Transport> tMap = new HashMap<>();
        transportService.getAllTransports().forEach(t -> tMap.put(t.getTransportId(), t));

        final Transport resolvedT;
        if (transport != null) {
            resolvedT = transport;
        } else if (schedule != null) {
            resolvedT = tMap.get(schedule.getTransportId());
        } else {
            resolvedT = null;
        }

        if (resolvedT == null) {
            showAlert("Could not resolve transport details. Please try again.");
            return;
        }

        int preTransId = resolvedT.getTransportId();
        int preSchedId = schedule != null ? schedule.getScheduleId() : 0;
        final double basePrice  = resolvedT.getBasePrice();
        final double schedMult  = schedule != null ? schedule.getPriceMultiplier() : 1.0;
        final boolean fl        = "FLIGHT".equals(resolvedT.getTransportType());
        final String  pName     = resolvedT.getProviderName();

        // FIX: proper owner so the popup is never orphaned / static
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        if (ownerStage != null && ownerStage.isShowing()) {
            popup.initOwner(ownerStage);
        } else {
            popup.initOwner(contentArea.getScene().getWindow());
        }
        popup.setTitle("Book Your Trip"); popup.setResizable(false);

        Label hdrLbl = new Label("Book:  " + pName);
        hdrLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        HBox hb = new HBox(hdrLbl);
        hb.setStyle("-fx-background-color:#1F294C;-fx-padding:16 22 16 22;");

        // Schedule info banner
        VBox banner = new VBox();
        if (schedule != null) {
            String info = fl
                    ? "Flight:  " + destName(schedule.getDepartureDestinationId())
                    + "  ->  " + destName(schedule.getArrivalDestinationId())
                    + "\nDep: " + (schedule.getDepartureDatetime()!=null
                    ? schedule.getDepartureDatetime().format(DT_FMT) : "N/A")
                    + "   Arr: " + (schedule.getArrivalDatetime()!=null
                    ? schedule.getArrivalDatetime().format(DT_FMT) : "N/A")
                    + "   Status: " + schedule.getStatus()
                    : "Vehicle Rental:  " + destName(schedule.getDepartureDestinationId())
                    + "\nRental period: "
                    + (schedule.getRentalStart()!=null ? schedule.getRentalStart().format(DATE_FMT) : "N/A")
                    + "  to  "
                    + (schedule.getRentalEnd()!=null ? schedule.getRentalEnd().format(DATE_FMT) : "N/A");

            Label bl = new Label(info);
            bl.setStyle("-fx-text-fill:#1F294C;-fx-font-size:12px;-fx-background-color:#E3F6F7;" +
                    "-fx-padding:10 14 10 14;-fx-background-radius:5;");
            bl.setWrapText(true); banner.getChildren().add(bl);
            banner.setPadding(new Insets(0,0,4,0));
        }

        // Transport type info row
        Label typeInfoLbl = new Label("Type: " + resolvedT.getTransportType()
                + "   |   Model: " + resolvedT.getVehicleModel()
                + "   |   Base price: " + String.format("%.2f EUR", basePrice));
        typeInfoLbl.setStyle("-fx-text-fill:#555;-fx-font-size:11px;-fx-padding:0 0 4 0;");

        // FIX: create all form controls fresh — no reuse
        ComboBox<String> clsBox = new ComboBox<>(
                FXCollections.observableArrayList("ECONOMY","PREMIUM","BUSINESS","FIRST"));
        clsBox.setValue(schedule!=null && schedule.getTravelClass()!=null
                ? schedule.getTravelClass() : "ECONOMY");
        clsBox.setMaxWidth(Double.MAX_VALUE); clsBox.setStyle(FORM_FIELD);

        Spinner<Integer> adSpin = new Spinner<>(1, 20, 1);
        adSpin.setEditable(true); adSpin.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> chSpin = new Spinner<>(0, 20, 0);
        chSpin.setEditable(true); chSpin.setMaxWidth(Double.MAX_VALUE);

        CheckBox ins = new CheckBox("Add travel insurance  (+25 EUR per seat)");
        ins.setStyle("-fx-text-fill:#1F294C;");

        Label prev = new Label();
        prev.setWrapText(true);
        prev.setStyle("-fx-text-fill:#1F294C;-fx-font-weight:bold;-fx-font-size:13px;" +
                "-fx-background-color:white;-fx-padding:10 14 10 14;-fx-background-radius:5;");

        TextField pickupLatField = new TextField();
        pickupLatField.setPromptText("e.g. 36.8065");
        pickupLatField.setStyle(FORM_FIELD);
        pickupLatField.setMaxWidth(Double.MAX_VALUE);

        TextField pickupLonField = new TextField();
        pickupLonField.setPromptText("e.g. 10.1815");
        pickupLonField.setStyle(FORM_FIELD);
        pickupLonField.setMaxWidth(Double.MAX_VALUE);

        Label pickupAddressLbl = new Label("Address not resolved yet.");
        pickupAddressLbl.setWrapText(true);
        pickupAddressLbl.setStyle("-fx-text-fill:#666;-fx-font-size:11px;");

        Button pickupReverseBtn = mkBtn("Resolve Pickup Address", BTN_DARK);
        pickupReverseBtn.setOnAction(ev -> {
            Double lat = parseTransportCoordinate(pickupLatField.getText(), true);
            Double lon = parseTransportCoordinate(pickupLonField.getText(), false);
            if (lat == null || lon == null) {
                showAlert("Pickup coordinates are invalid.\nLatitude must be between -90 and 90.\nLongitude must be between -180 and 180.");
                return;
            }
            String addr = reverseGeocodeWithTransportOpenStreetMap(lat, lon);
            if (addr == null || addr.isBlank()) {
                showAlert("Could not resolve pickup address from OpenStreetMap.");
                return;
            }
            pickupAddressLbl.setText(addr);
        });
        Button pickupMapBtn = mkBtn("Pick on Map", BTN_TEAL);
        pickupMapBtn.setOnAction(ev -> openTransportMapPicker(pickupLatField, pickupLonField, pickupAddressLbl));

        TextField dropoffLatField = new TextField();
        dropoffLatField.setPromptText("e.g. 36.8999");
        dropoffLatField.setStyle(FORM_FIELD);
        dropoffLatField.setMaxWidth(Double.MAX_VALUE);

        TextField dropoffLonField = new TextField();
        dropoffLonField.setPromptText("e.g. 10.1890");
        dropoffLonField.setStyle(FORM_FIELD);
        dropoffLonField.setMaxWidth(Double.MAX_VALUE);

        Label dropoffAddressLbl = new Label("Address not resolved yet.");
        dropoffAddressLbl.setWrapText(true);
        dropoffAddressLbl.setStyle("-fx-text-fill:#666;-fx-font-size:11px;");

        Button dropoffReverseBtn = mkBtn("Resolve Drop-off Address", BTN_DARK);
        dropoffReverseBtn.setOnAction(ev -> {
            Double lat = parseTransportCoordinate(dropoffLatField.getText(), true);
            Double lon = parseTransportCoordinate(dropoffLonField.getText(), false);
            if (lat == null || lon == null) {
                showAlert("Drop-off coordinates are invalid.\nLatitude must be between -90 and 90.\nLongitude must be between -180 and 180.");
                return;
            }
            String addr = reverseGeocodeWithTransportOpenStreetMap(lat, lon);
            if (addr == null || addr.isBlank()) {
                showAlert("Could not resolve drop-off address from OpenStreetMap.");
                return;
            }
            dropoffAddressLbl.setText(addr);
        });
        Button dropoffMapBtn = mkBtn("Pick on Map", BTN_TEAL);
        dropoffMapBtn.setOnAction(ev -> openTransportMapPicker(dropoffLatField, dropoffLonField, dropoffAddressLbl));
        Button routeAiBtn = mkBtn("Find Optimal Route (AI)", BTN_DARK);
        routeAiBtn.setOnAction(ev -> {
            Double pickupLat = parseTransportCoordinate(pickupLatField.getText(), true);
            Double pickupLon = parseTransportCoordinate(pickupLonField.getText(), false);
            Double dropoffLat = parseTransportCoordinate(dropoffLatField.getText(), true);
            Double dropoffLon = parseTransportCoordinate(dropoffLonField.getText(), false);
            if (pickupLat == null || pickupLon == null || dropoffLat == null || dropoffLon == null) {
                showAlert("Enter valid pickup/drop-off coordinates first.");
                return;
            }
            routeAiBtn.setDisable(true);
            routeAiBtn.setText("Analyzing...");
            try {
                showTransportOptimalRoutePopup(
                        pickupLat, pickupLon, dropoffLat, dropoffLon,
                        pickupAddressLbl.getText(), dropoffAddressLbl.getText(), popup
                );
            } catch (Exception ex) {
                showAlert("Route assistant failed to start:\n" + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            } finally {
                Platform.runLater(() -> {
                    routeAiBtn.setDisable(false);
                    routeAiBtn.setText("Find Optimal Route (AI)");
                });
            }
        });

        // FIX: price updater uses fresh references only — no stale closures
        Runnable upd = () -> {
            int    seats = adSpin.getValue() + chSpin.getValue();
            double cm    = classMultiplier(clsBox.getValue());
            double insV  = ins.isSelected() ? 25.0 * seats : 0;
            double total = basePrice * schedMult * cm * seats + insV;
            prev.setText(String.format(
                    "Estimated Total:  %.2f EUR\n" +
                            "  Base %.2f  x  %.1fx %s class  x  %d seat%s%s",
                    total, basePrice * schedMult,
                    cm, clsBox.getValue(),
                    seats, seats > 1 ? "s" : "",
                    ins.isSelected() ? "  + insurance" : ""));
        };

        // attach listeners AFTER defining upd
        clsBox.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> upd.run());
        adSpin.valueProperty().addListener((o, a, n) -> upd.run());
        chSpin.valueProperty().addListener((o, a, n) -> upd.run());
        ins.selectedProperty().addListener((o, a, n) -> upd.run());
        upd.run();  // initial calculation

        GridPane g = buildFormGrid(165, 270); int r = 0;
        g.addRow(r++, fl("Travel Class"),    clsBox);
        g.addRow(r++, fl("Adults"),          adSpin);
        g.addRow(r++, fl("Children (0-11)"), chSpin);
        g.addRow(r++, fl("Insurance"),       ins);
        if (!fl) {
            Label geoHint = new Label("Vehicle booking: choose pickup/drop-off coordinates, then resolve address with OpenStreetMap.");
            geoHint.setWrapText(true);
            geoHint.setStyle("-fx-text-fill:#666;-fx-font-size:11px;-fx-font-style:italic;");
            g.addRow(r++, fl("Location Capture"), geoHint);
            g.addRow(r++, fl("Pickup Latitude"), pickupLatField);
            g.addRow(r++, fl("Pickup Longitude"), pickupLonField);
            g.addRow(r++, fl("Pickup Address"), new VBox(6, new HBox(8, pickupMapBtn, pickupReverseBtn), pickupAddressLbl));
            g.addRow(r++, fl("Drop-off Latitude"), dropoffLatField);
            g.addRow(r++, fl("Drop-off Longitude"), dropoffLonField);
            g.addRow(r++, fl("Drop-off Address"), new VBox(6, new HBox(8, dropoffMapBtn, dropoffReverseBtn), dropoffAddressLbl));
            g.addRow(r++, fl("Route Assistant"), routeAiBtn);
        }
        g.addRow(r++, fl("Price Estimate"),  prev);

        Button conf = mkBtn("Confirm Booking", BTN_TEAL);
        Button canc = mkBtn("Cancel", BTN_DARK); canc.setOnAction(e -> popup.close());

        conf.setOnAction(e -> {
            try {
                int adults   = adSpin.getValue();
                int children = chSpin.getValue();
                int seats    = adults + children;

                // Re-check availability at moment of confirm
                if (schedule != null) {
                    if (fl) {
                        int rem = remainingSeatsForFlight(schedule, tMap);
                        if (rem < seats) {
                            showAlert("Not enough seats!\nRequested: " + seats
                                    + "\nAvailable: " + rem);
                            return;
                        }
                    } else {
                        if (isVehicleScheduleUnavailable(schedule.getScheduleId())) {
                            showAlert("This vehicle schedule is no longer available.\n" +
                                    "It has already been booked.");
                            return;
                        }
                    }
                }

                double cm    = classMultiplier(clsBox.getValue());
                double insV  = ins.isSelected() ? 25.0 * seats : 0;
                double total = basePrice * schedMult * cm * seats + insV;

                Double pickupLat = null;
                Double pickupLon = null;
                Double dropoffLat = null;
                Double dropoffLon = null;
                String pickupAddress = null;
                String dropoffAddress = null;

                if (!fl) {
                    pickupLat = parseTransportCoordinate(pickupLatField.getText(), true);
                    pickupLon = parseTransportCoordinate(pickupLonField.getText(), false);
                    dropoffLat = parseTransportCoordinate(dropoffLatField.getText(), true);
                    dropoffLon = parseTransportCoordinate(dropoffLonField.getText(), false);

                    if (pickupLat == null || pickupLon == null || dropoffLat == null || dropoffLon == null) {
                        showAlert("Please provide valid pickup and drop-off coordinates for vehicle booking.");
                        return;
                    }

                    pickupAddress = reverseGeocodeWithTransportOpenStreetMap(pickupLat, pickupLon);
                    dropoffAddress = reverseGeocodeWithTransportOpenStreetMap(dropoffLat, dropoffLon);
                    if (pickupAddress == null || dropoffAddress == null) {
                        showAlert("Could not resolve pickup/drop-off addresses from OpenStreetMap. Please retry.");
                        return;
                    }
                }

                Bookingtrans bk = new Bookingtrans();
                bk.setUserId(currentUserId);
                bk.setTransportId(preTransId);
                bk.setScheduleId(preSchedId);
                bk.setAdultsCount(adults);
                bk.setChildrenCount(children);
                bk.setTotalSeats(seats);
                bk.setBookingStatus("PENDING");
                bk.setPaymentStatus("UNPAID");
                bk.setInsuranceIncluded(ins.isSelected());
                bk.setBookingDate(LocalDateTime.now());
                bk.setTotalPrice(total);
                bk.setPickupLatitude(pickupLat);
                bk.setPickupLongitude(pickupLon);
                bk.setPickupAddress(pickupAddress);
                bk.setDropoffLatitude(dropoffLat);
                bk.setDropoffLongitude(dropoffLon);
                bk.setDropoffAddress(dropoffAddress);
                bookingService.addBookingtrans(bk);
                popup.close();

                String successMsg =
                        "Booking Submitted Successfully!\n\n" +
                                "Provider:      " + pName + "\n" +
                                "Type:          " + resolvedT.getTransportType() + "\n" +
                                "Model:         " + resolvedT.getVehicleModel() + "\n" +
                                "Schedule:      " + (preSchedId > 0 ? "#" + preSchedId : "Direct (no schedule)") + "\n" +
                                "Class:         " + clsBox.getValue() + "\n" +
                                "Seats:         " + seats + "  (" + adults + " adults, " + children + " children)\n" +
                                "Insurance:     " + (ins.isSelected() ? "Yes (+25 EUR x " + seats + " seats)" : "No") + "\n" +
                                "Total:         " + String.format("%.2f EUR", total) +
                                (!fl ? "\nPickup:       " + pickupAddress + "\nDrop-off:     " + dropoffAddress : "") +
                                "\n\n" +
                                "Status:  PENDING  -  awaiting admin confirmation";

                showSuccess(successMsg);
                showMyBookingsTab();
            } catch (Exception ex) {
                showAlert("Booking failed: " + ex.getMessage());
            }
        });

        HBox br = new HBox(12, conf, canc);
        br.setAlignment(Pos.CENTER_RIGHT); br.setPadding(new Insets(10,0,0,0));

        ScrollPane formScroll = new ScrollPane(g);
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        formScroll.setPrefViewportHeight(fl ? 220 : 430);
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        VBox content = new VBox(12, banner, typeInfoLbl, formScroll, br);
        content.setPadding(new Insets(20)); content.setStyle("-fx-background-color:#F1EAE7;");
        popup.setScene(new Scene(new VBox(hb, content), 620, fl ? 470 : 730));
        popup.show();   // use show() instead of showAndWait() to avoid nested-modal blocking
    }

    /* ═══════════════════════════════════════════════════════
       SHARED UI HELPERS
       ═══════════════════════════════════════════════════════ */

    private GridPane buildFormGrid(int c1, int c2) {
        GridPane g = new GridPane(); g.setHgap(12); g.setVgap(10);
        g.getColumnConstraints().addAll(new ColumnConstraints(c1), new ColumnConstraints(c2));
        return g;
    }

    private Label fl(String t) {
        Label l = new Label(t + ":");
        l.setStyle("-fx-font-weight:bold;-fx-text-fill:#1F294C;-fx-font-size:12px;");
        l.setAlignment(Pos.CENTER_RIGHT); l.setMaxWidth(Double.MAX_VALUE); return l;
    }

    private Label dv(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
        l.setWrapText(true); return l;
    }

    private Label sectionTitle(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#1F294C;");
        return l;
    }

    private Label badge(String t, String bg) {
        Label l = new Label(t);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-font-size:10px;" +
                "-fx-font-weight:bold;-fx-padding:3 8 3 8;-fx-background-radius:10;");
        return l;
    }

    private Label errLabel() {
        Label l = new Label(); l.setWrapText(true); l.setMaxWidth(430);
        l.setStyle("-fx-text-fill:#c0392b;-fx-font-size:11px;-fx-font-weight:bold;"); return l;
    }

    private Button mkBtn(String t, String s) { Button b = new Button(t); b.setStyle(s); return b; }

    private HBox hbox(javafx.scene.Node... nodes) {
        HBox box = new HBox(12, nodes); box.setAlignment(Pos.CENTER_LEFT); return box;
    }

    private ScrollPane scrollPane(javafx.scene.Node content) {
        ScrollPane sc = new ScrollPane(content); sc.setFitToWidth(true);
        sc.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;"); return sc;
    }

    private ComboBox<String> destCombo(List<Long> ids, String prompt, Map<Long, String> dn) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().add(prompt);
        ids.forEach(id -> box.getItems().add(destName(id)));
        box.setValue(prompt); box.setMaxWidth(Double.MAX_VALUE); box.setStyle(FORM_FIELD);
        return box;
    }

    private DatePicker dp(String prompt) {
        DatePicker p = new DatePicker(); p.setPromptText(prompt);
        p.setMaxWidth(Double.MAX_VALUE); p.setStyle(FORM_FIELD); return p;
    }

    private Long parseDestId(String val, Map<Long, String> dn) {
        if (val == null || val.startsWith("Any")) return null;
        for (Map.Entry<Long, String> e : dn.entrySet())
            if (e.getValue().equals(val)) return e.getKey();
        try { return Long.parseLong(val.replace("Destination #","").trim()); }
        catch (NumberFormatException ex) { return null; }
    }

    private Double parseTransportCoordinate(String text, boolean latitude) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            if (latitude && (value < -90 || value > 90)) return null;
            if (!latitude && (value < -180 || value > 180)) return null;
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String reverseGeocodeWithTransportOpenStreetMap(Double lat, Double lon) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat="
                    + URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8)
                    + "&lon="
                    + URLEncoder.encode(String.valueOf(lon), StandardCharsets.UTF_8)
                    + "&zoom=18&addressdetails=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "tripx-javafx/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            Matcher matcher = DISPLAY_NAME_PATTERN.matcher(response.body());
            if (!matcher.find()) return null;
            return matcher.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception ex) {
            return null;
        }
    }

    private void openTransportMapPicker(TextField latField, TextField lonField, Label addressLabel) {
        Stage picker = new Stage();
        picker.initModality(Modality.APPLICATION_MODAL);
        picker.initOwner(contentArea.getScene().getWindow());
        picker.setTitle("OpenStreetMap Picker");
        picker.setResizable(true);

        WebView webView = new WebView();
        webView.setPrefSize(760, 520);
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setOnAlert(event -> {
            String msg = event.getData();
            if (msg == null || !msg.startsWith(TRANSPORT_MAP_PICK_EVENT_PREFIX)) return;
            String payload = msg.substring(TRANSPORT_MAP_PICK_EVENT_PREFIX.length());
            String[] parts = payload.split(",", 2);
            if (parts.length != 2) return;
            Platform.runLater(() -> {
                latField.setText(parts[0]);
                lonField.setText(parts[1]);
                Double parsedLat = parseTransportCoordinate(parts[0], true);
                Double parsedLon = parseTransportCoordinate(parts[1], false);
                if (parsedLat != null && parsedLon != null) {
                    String addr = reverseGeocodeWithTransportOpenStreetMap(parsedLat, parsedLon);
                    if (addr != null && !addr.isBlank()) addressLabel.setText(addr);
                }
                if (picker.isShowing()) picker.close();
            });
        });

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaBridge", new Object() {
                        public void onPointSelected(String lat, String lon) {
                            Platform.runLater(() -> {
                                latField.setText(lat);
                                lonField.setText(lon);
                                Double parsedLat = parseTransportCoordinate(lat, true);
                                Double parsedLon = parseTransportCoordinate(lon, false);
                                if (parsedLat != null && parsedLon != null) {
                                    String addr = reverseGeocodeWithTransportOpenStreetMap(parsedLat, parsedLon);
                                    if (addr != null && !addr.isBlank()) addressLabel.setText(addr);
                                }
                            });
                        }
                    });
                    engine.executeScript("if(window.map){window.map.invalidateSize(true);}");
                } catch (Exception ex) {
                    showAlert("Map initialization failed. Please reopen map picker.");
                }
            }
        });

        engine.loadContent(TRANSPORT_VEHICLE_PICKER_HTML);

        Button closeBtn = mkBtn("Done", BTN_DARK);
        closeBtn.setOnAction(e -> picker.close());
        HBox closeRow = new HBox(closeBtn);
        closeRow.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(8, webView, closeRow);
        root.setPadding(new Insets(10));
        picker.setScene(new Scene(root, 780, 580));
        picker.showAndWait();
    }

    private void showTransportOptimalRoutePopup(
            double pickupLat, double pickupLon, double dropoffLat, double dropoffLon,
            String pickupAddress, String dropoffAddress, Stage ownerStage
    ) {
        Stage popup = new Stage();
        // Keep this popup independent from nested modal ownership issues.
        popup.initModality(Modality.NONE);
        popup.setTitle("AI Route Assistant");
        popup.setResizable(true);

        Label title = new Label("Optimal Route Recommendation");
        title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1F294C;");

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(42, 42);

        Label status = new Label("Analyzing route with routing engine + Groq AI...");
        status.setStyle("-fx-text-fill:#666;-fx-font-size:12px;");

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setPrefRowCount(14);
        output.setStyle("-fx-font-size:12px;");
        output.setText("Please wait...");

        Button closeBtn = mkBtn("Close", BTN_DARK);
        closeBtn.setOnAction(e -> popup.close());
        HBox closeRow = new HBox(closeBtn);
        closeRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, loading, status, output, closeRow);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#F1EAE7;");

        popup.setScene(new Scene(root, 560, 520));
        popup.show();
        popup.toFront();
        popup.requestFocus();

        Thread worker = new Thread(() -> {
            String report;
            try {
                report = transportRouteService.generateTransportOptimalRouteReport(
                        pickupLat, pickupLon, dropoffLat, dropoffLon, pickupAddress, dropoffAddress
                );
            } catch (Exception ex) {
                report = "AI route analysis failed: " + ex.getMessage();
            }
            String finalReport = report;
            Platform.runLater(() -> {
                loading.setVisible(false);
                loading.setManaged(false);
                status.setText("Recommendation ready.");
                output.setText(finalReport);
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK); a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK); a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setHeaderText(null); a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}