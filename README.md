# TripX Platform - Technical Documentation

## 1) Project Overview

### Purpose
TripX is a university innovation/entrepreneurship project focused on digital accommodation operations.  
This repository currently implements the **accommodation vertical** end-to-end:
- accommodation lifecycle management,
- room lifecycle management,
- room image lifecycle management,
- booking availability checks,
- AI-assisted comparison and nearby recommendations,
- admin and user-facing JavaFX interfaces.

### Target users
- **Platform administrators**: manage properties, rooms, and media from the admin dashboard.
- **End users/travelers**: browse accommodations, filter/search, inspect maps/details, and use AI-assisted decision support.
- **Academic evaluators and developers**: assess architecture quality, engineering practices, and extensibility.

### Main modules in this repository
- **Accommodation Management** (`Accommodation`, `AccommodationService`, admin/user controllers)
- **Room Management** (`Room`, `RoomService`, `RoomDetailsController`)
- **Room Image Management** (`RoomImage`, `RoomImageService`, drag/drop ordering)
- **Booking Core** (`Booking`, `BookingService`)
- **AI Layer**
  - `AccommodationCompareService` (multi-property comparison)
  - `NearbyAiPlannerService` (location-based recommendations)
- **UI Shell and Theming**
  - main JavaFX app, admin dashboard, user listing/details views
  - reusable `admin-shell-kit`

### Innovation aspects
- Hybrid **map + AI** decision journey: location intelligence and LLM-driven insights integrated into UX.
- Rich JavaFX desktop UX with animated transitions, modular FXML views, and context-aware panels.
- Multi-image room media model with ordered galleries and primary-image logic.
- Explicit separation of responsibilities (controller/service/entity/util) while keeping a lightweight JDBC stack.

---

## 2) Architecture Overview

## High-level architecture
The project is a **JavaFX desktop application** (Java 17, Maven) using:
- **UI layer**: FXML + JavaFX controllers
- **application/service layer**: domain services with business logic
- **data layer**: JDBC (`PreparedStatement`) against MySQL
- **integration layer**: HTTP clients for Groq and Nominatim

### Architecture style
- Predominantly **MVC-style desktop architecture**:
  - **Model**: entities (`Accommodation`, `Room`, `Booking`, `RoomImage`)
  - **View**: FXML/CSS/HTML resources
  - **Controller**: JavaFX controllers (admin/user)
  - **Service**: data and integration orchestration

### Backend architecture (desktop data backend)
- No Spring Boot or REST server in current codebase.
- Service classes act as the “backend boundary” for controllers.
- SQL access is performed through a singleton connection provider (`MyDB`).

### Frontend structure
- JavaFX views under `src/main/resources/fxml`.
- CSS themes under `src/main/resources/css`.
- Embedded map views under `src/main/resources/map.html` and `map-picker.html` rendered via `WebView`.

### Database structure (implemented/inferred from SQL usage)
- `accommodation`
- `room`
- `room_images`
- `booking`
- Foreign keys are used (e.g., room -> accommodation, room_images -> room, booking -> room).

### Folder structure
```text
RAGHDD/
├─ src/main/java/tn/esprit/
│  ├─ Main.java                         # JavaFX entry point
│  ├─ controllers/
│  │  ├─ admin/                         # Admin-facing workflows and modals
│  │  └─ user/                          # User-facing listing/details workflows
│  ├─ entities/                         # Domain models (Accommodation, Room, ...)
│  ├─ services/                         # Business logic + SQL + AI integrations
│  └─ utils/                            # DB connection, cards, image utilities
├─ src/main/resources/
│  ├─ fxml/                             # JavaFX views
│  ├─ css/                              # Styling themes
│  ├─ map.html                          # OSM/Leaflet user map
│  └─ map-picker.html                   # OSM/Leaflet admin picker + reverse geocode bridge
├─ admin-shell-kit/                     # Reusable admin shell package
└─ pom.xml                              # Build/dependency management
```

---

## 3) Technologies Used

### Core stack
- **Language**: Java 17
- **Build**: Maven
- **UI Framework**: JavaFX 17 (`controls`, `fxml`, `web`, `media`, `swing`)
- **Database**: MySQL
- **Data access**: JDBC (no ORM in current implementation)

### Libraries and purpose
- `mysql-connector-j`: MySQL driver
- `gson`: JSON serialization/parsing for AI API integration
- `controlsfx`, `jfoenix`, `atlantafx`, `gemsfx`, `formsfx`, `validatorfx`: advanced JavaFX UI controls and form UX
- `ikonli-*`: icon packs
- `slf4j`, `logback`: logging stack (plus java.util.logging usage in controllers)
- `pdfbox`, `poi-ooxml`, `tilesfx`: reporting and advanced UI/chart support

### APIs and integrations
- **Groq Chat Completions API**
  - model: `llama-3.1-8b-instant`
  - used for comparison and nearby suggestions
- **OpenStreetMap + Leaflet**
  - map visualization and interactivity in `WebView`
- **Nominatim Reverse Geocoding**
  - admin map click -> city/postal/country enrichment

---

## 4) Detailed CRUD Explanation (Entity-by-Entity)

This section follows actual flow patterns in code:  
**Controller -> Service -> SQL (PreparedStatement) -> Database**.

## 4.1 Accommodation CRUD

### Create
1. Admin opens add modal (`AccommodationAdminController -> showAddAccommodationModal()`).
2. Form submission calls `AccommodationModalController.handleSave()`.
3. `validateForm()` enforces required fields, format checks (email/url), and numeric constraints.
4. `buildAccommodationFromForm()` maps UI state into `Accommodation`.
5. `AccommodationService.addAccommodation()` executes `INSERT INTO accommodation (...)`.
6. Generated key is returned and applied to entity.
7. Room list is persisted via `RoomService.addRoom()` for each room (if present).

### Read
- `AccommodationService.getAll()` performs `SELECT * FROM accommodation ORDER BY created_at DESC`.
- Per-accommodation rooms are hydrated via `RoomService.getRoomsByAccommodationId()`.
- Used by both admin and user flows (`AccommodationAdminController`, `AccommodationsController`).

### Update
1. Admin opens edit modal.
2. Existing values are loaded with defensive null handling (`loadAccommodation()`).
3. On save, same validation pipeline executes.
4. `AccommodationService.updateAccommodation()` updates all mutable fields.
5. Room add/update/delete operations are coordinated from modal state.

### Delete
- Triggered from admin card actions.
- `AccommodationService.deleteAccommodation(id)` executes `DELETE`.
- Room cleanup relies on relational cascade semantics (and/or explicit room operations).

### Validation logic (implemented)
- Required fields: name, address, city, postal code, type, status, stars, country, phone, email.
- Format constraints: email regex, URL format, phone digits length, coordinates ranges.
- Input filters: digits-only and decimal-only text formatters on selected fields.

### Security checks
- Uses parameterized SQL (`PreparedStatement`) to reduce injection risk.
- No auth gate around CRUD in current repository (admin access is UI-route based, not identity-token based).

### Error handling
- SQL exceptions are caught in services and logged/printed.
- Controller displays modal alerts for user-facing failures.

### Data flow summary
- UI fields -> DTO-like entity object -> SQL bind parameters -> DB rows -> entity hydration -> UI rendering.

## 4.2 Room CRUD

### Create/Update/Delete/Read
- Service class: `RoomService`.
- Table: `room`.
- Operations:
  - `addRoom(Room)`
  - `getAll()`, `getRoomsByAccommodationId(int)`, `getRoomById(int)`
  - `updateRoom(Room)`
  - `deleteRoom(int)`, `deleteRoomsByAccommodationId(int)`
- Admin dialogs provide room editing with rich amenity selection and numeric parsing checks.

### Validation
- Required room fields in dialogs (name/type/capacity/size/price).
- Numeric parsing guards with explicit error alerts.

### Security/Error handling
- `PreparedStatement` usage.
- Error feedback through controller alerts and logs.

## 4.3 RoomImage CRUD

### Create
- `RoomImageService.addImage(roomId, sourceFile)`:
  - validates file presence,
  - generates UUID filename,
  - stores file under `uploads/images/rooms/{roomId}/`,
  - records metadata in `room_images`,
  - auto-sets first image as primary.

### Read
- `getByRoomId(roomId)` ordered by `is_primary DESC`, then `display_order`.
- `getById(id)` for detail operations.

### Update
- `setPrimaryImage(roomId, imageId)` in transaction.
- `moveImage(...)` and `reorderImages(...)` for ordering.

### Delete
- `deleteImage(imageId, roomId)` removes DB row and physical file; reassigns primary when needed.

### Security/Error handling
- Transaction blocks for consistency (`setAutoCommit(false)`, commit/rollback).
- Safe file deletion and MIME/type probing guards.

## 4.4 Booking CRUD (partial in current repo)

### Create
- `BookingService.addBooking(Booking)` checks overlap via `isRoomAvailable(...)` before insert.

### Read
- `BookingService.getAll()`.

### Update/Delete
- Not implemented in current service (can be added similarly with status/date mutation rules).

### Validation and checks
- Conflict detection query prevents overlapping bookings.

---

## 5) API Documentation

## 5.1 Clarification: internal vs HTTP APIs
This repository is a **desktop JavaFX application**, so there is no in-process REST controller layer yet (`@RestController` not present).  
Documentation below includes:
1. **Internal service APIs** (method contracts),
2. **External HTTP APIs** used by integrations.

## 5.2 Internal Service API (selected)

| Service | Operation | Description |
|---|---|---|
| `AccommodationService` | `addAccommodation(Accommodation)` | Create accommodation row |
| `AccommodationService` | `getAll()` / `getById(int)` | Read accommodations (+ rooms) |
| `AccommodationService` | `updateAccommodation(Accommodation)` | Update accommodation |
| `AccommodationService` | `deleteAccommodation(int)` | Delete accommodation |
| `RoomService` | `addRoom(Room)` / `updateRoom(Room)` | Room write operations |
| `RoomService` | `getRoomsByAccommodationId(int)` | Room list per accommodation |
| `RoomImageService` | `addImage`, `deleteImage`, `setPrimaryImage`, `reorderImages` | Room media lifecycle |
| `BookingService` | `addBooking`, `isRoomAvailable` | Booking + conflict check |
| `AccommodationCompareService` | `compareAccommodations(List<Accommodation>)` | AI compare |
| `NearbyAiPlannerService` | `generateNearbyPlan(Accommodation)` | AI nearby plan |

## 5.3 External HTTP APIs

### Groq Chat Completions
- **Method**: `POST`
- **URL**: `https://api.groq.com/openai/v1/chat/completions`
- **Auth**: `Authorization: Bearer ${GROQ_API_KEY}`
- **Content-Type**: `application/json`

Example request body (shape):
```json
{
  "model": "llama-3.1-8b-instant",
  "temperature": 0.2,
  "max_tokens": 900,
  "messages": [
    {"role": "system", "content": "You are a travel assistant. Always output valid JSON only, no markdown."},
    {"role": "user", "content": "Compare these accommodations ..."}
  ],
  "response_format": {"type": "json_object"}
}
```

### Nominatim Reverse Geocoding
- **Method**: `GET`
- **URL pattern**:
  `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat={lat}&lon={lon}&addressdetails=1`
- **Headers**:
  - `User-Agent: TripX-AdminDashboard/1.0`
  - `Accept: application/json`

---

## 6) AI Integration Explanation

## 6.1 Where AI is used
- **Accommodation comparison** in user listing compare bar.
- **Nearby recommendations** in accommodation details page.

## 6.2 Request pipeline
1. UI action triggers async call (`CompletableFuture.supplyAsync`).
2. Service composes domain-rich prompt from accommodation data.
3. Request sent via Java `HttpClient`.
4. Response parsed with Gson and normalized into structured result DTO classes.
5. UI thread update via `Platform.runLater`.

## 6.3 Prompt engineering strategy
- Prompts enforce strict JSON output shape (`response_format: json_object` + explicit schema text).
- Domain context includes city/country/address/stars/status/amenities/coordinates.
- Low-to-moderate temperature for deterministic output.

## 6.4 Response handling
- Defensive parsing:
  - strips markdown fences if returned,
  - JSON extraction fallback,
  - safe defaults on missing sections.
- Returns structured result objects with:
  - `success` flag,
  - `errorMessage`,
  - parsed fields,
  - raw response for diagnostics.

## 6.5 Rate limit / failure behavior
- Timeout limits on HTTP clients.
- Missing API key short-circuits with clear user-facing message.
- Non-200 status and parse failures reported safely without crashing UI.

---



---

## 8) Database Design

Schema details below are inferred from active SQL statements in services.

## 8.1 Core entities

### `accommodation`
- Primary key: `id`
- Main columns: name/type/location/contact/status/coordinates/amenities/timestamps
- Parent of `room`.

### `room`
- Primary key: `id`
- Foreign key: `accommodation_id -> accommodation.id`
- Attributes: room_name, room_type, price_per_night, capacity, size, amenities, is_available

### `room_images`
- Primary key: `id`
- Foreign key: `room_id -> room.id`
- Attributes: file metadata, `is_primary`, `display_order`, timestamps
- Indexed by room and ordering strategy (recommended and previously used in migration guidance).

### `booking`
- Primary key: `id`
- Foreign keys: `room_id` and `user_id` (user table not in current codebase)
- Attributes: check_in, check_out, total_price, status

## 8.2 Relationship model
- `Accommodation` 1..N `Room`
- `Room` 1..N `RoomImage`
- `Room` 1..N `Booking`

## 8.3 Constraints and indexing guidance
- Enforce FK constraints with `ON DELETE CASCADE` where appropriate.
- Add/retain indexes on:
  - `room(accommodation_id)`
  - `room_images(room_id, display_order)`
  - `room_images(room_id, is_primary)`
  - `booking(room_id, check_in, check_out)` for overlap checks

---

## 9) Advanced Features

This section distinguishes **implemented** vs **project-scope target** features.

## Implemented in current repository
- Accommodation search/filter/sort (admin and user views)
- Compare bar with AI compare panel
- OSM map markers and admin map picker
- AI nearby suggestions with structured cards
- Room image management: upload/delete/primary/reorder (including drag-and-drop workflow at controller level)
- Sidebar behavior, dark mode toggle, language selector scaffolding

l

These can be added as independent bounded contexts with dedicated entities/services/controllers.

---

## 10) Code Structure Explanation

## Key runtime files
- `src/main/java/tn/esprit/Main.java`  
  JavaFX launcher, loads main dashboard scene.

- `src/main/java/tn/esprit/utils/MyDB.java`  
  Singleton JDBC connection provider.

## Controllers
- `controllers/admin/AccommodationAdminController.java`  
  Admin dashboard orchestration: filters, cards, modals, charts, sidebar.
- `controllers/admin/AccommodationModalController.java`  
  Add/edit form, validation, room subdialogs, map picker bridge.
- `controllers/admin/AccommodationDetailsController.java`  
  Admin details view, room-level navigation.
- `controllers/admin/RoomDetailsController.java`  
  Room detail editing + room image operations.
- `controllers/user/AccommodationsController.java`  
  User listing page, filtering, compare bar, map markers, AI compare trigger.
- `controllers/user/AccommodationDetailsController.java`  
  User details page, gallery logic, map, AI nearby rendering.
- `controllers/user/AccommodationCardController.java`  
  Card view model binding and compare toggle behavior.

## Services
- `AccommodationService`, `RoomService`, `RoomImageService`, `BookingService`
  - SQL CRUD and domain-specific operations.
- `AccommodationCompareService`, `NearbyAiPlannerService`
  - external AI integrations with prompt/response handling.

## Entities
- `Accommodation`, `Room`, `RoomImage`, `Booking`
  - POJO models with fields aligned to SQL schema.

## Utilities
- `AccommodationCard` for reusable card rendering component.
- `ImagePathFixer` for migration/normalization of historical image paths.
- `ImageUtils` helper for basic image loading.

## Configuration / resources
- `pom.xml`: dependencies and JavaFX plugin entry configuration.
- `src/main/resources/fxml/*`: scene layouts.
- `src/main/resources/css/*`: visual themes.
- `map.html` / `map-picker.html`: embedded Leaflet map apps for WebView.

---

## 11) Security Measures

## Implemented
- **SQL injection mitigation**: SQL calls are parameterized via `PreparedStatement`.
- **Input validation**: strict form validation in accommodation modal (required fields, regex/url/number constraints).
- **Operational guards**:
  - booking overlap check before insert,
  - AI key presence checks,
  - transactional consistency in image primary/order updates.

## Gaps / pending hardening
- No centralized authN/authZ middleware yet.
- No CSRF context (desktop app; would be relevant once HTTP API exists).
- DB credentials currently hard-coded in `MyDB` (must be externalized).
- File upload constraints can be further hardened (type whitelist + size cap + antivirus policy).

---

## 12) Setup Instructions

## Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+

## 12.1 Clone and build
```bash
git clone <your-repo-url>
cd RAGHDD
mvn clean compile
```

## 12.2 Configure database
1. Create database:
```sql
CREATE DATABASE tripx_db;
```
2. Ensure tables required by services exist:
   - `accommodation`
   - `room`
   - `room_images`
   - `booking`
3. Update connection settings in `src/main/java/tn/esprit/utils/MyDB.java` or externalize them (recommended).

## 12.3 Configure AI key
Preferred: create a local `.env` file at project root:
```dotenv
GROQ_API_KEY=your_key_here
```
The app reads `GROQ_API_KEY` from `.env`.

## 12.4 Run application
```bash
mvn javafx:run
```

## 12.5 Seed data (manual)
Current repository does not include automated migration/seed scripts.  
Seed accommodations/rooms/bookings using SQL scripts or through admin UI forms.

---

## 13) Dashboard KPIs, Analytics, and ML Cards (How They Work)

This section documents the dashboard card mechanisms and the exact technical implementation used in code.

### 13.1 Card categories and mechanisms

- **KPI cards (descriptive analytics)**
  - `Total Accommodations`
  - `Active Bookings`
  - `Monthly Revenue`
  - Mechanism: direct SQL aggregates (counts/sums), no predictive model.

- **ANL cards (analytics/ranking)**
  - `Avg Booking Value`
  - `Cancellation Rate`
  - `Top City / Top Type`
  - Mechanism: SQL aggregate + group/rank logic (still deterministic analytics, not external LLM API).

- **ML cards (predictive)**
  - `Forecast Occupancy (Next 30d)`
  - `Suggested Price Action`
  - `ML Confidence`
  - Mechanism: historical feature extraction + trained regression + confidence logic.

### 13.2 Files responsible for dashboard intelligence

- `src/main/java/tn/esprit/services/AccommodationDashboardAnalyticsService.java`
  - Computes KPI + ANL values from SQL over `bookingacc`, `room`, `accommodation`.
- `src/main/java/tn/esprit/services/AccommodationMlInsightsService.java`
  - Builds occupancy forecast/momentum logic and combines ML prediction into a business-ready insight.
- `src/main/java/tn/esprit/services/AccommodationPriceRegressionService.java`
  - Trains and evaluates the true regression model from confirmed booking history.
- `src/main/java/tn/esprit/controllers/admin/AccommodationAdminController.java`
  - Binds computed values to JavaFX labels/charts.
- `src/main/resources/fxml/admin/accommodation-admin-dashboard.fxml`
  - Card layout and labels.
- `src/main/resources/css/admin-style.css`
  - Card visual themes (KPI/ANL/ML), hover effects, pastel chart skin.

### 13.3 Data sources used by the cards

- Main tables:
  - `bookingacc`
  - `room`
  - `accommodation`
- Booking rows used for pricing model training:
  - status = `CONFIRMED`
  - `total_price > 0`
  - `price_per_night > 0`
  - `DATEDIFF(check_out, check_in) > 0`

### 13.4 KPI and ANL technical logic

- **Total Accommodations**
  - `SELECT COUNT(*) FROM accommodation`
- **Active Bookings**
  - `SELECT COUNT(*) FROM bookingacc WHERE status IN ('PENDING','CONFIRMED')`
- **Monthly Revenue**
  - `SELECT COALESCE(SUM(total_price),0) FROM bookingacc WHERE status='CONFIRMED'`
- **Avg Booking Value**
  - `SELECT COALESCE(AVG(total_price),0) FROM bookingacc WHERE status='CONFIRMED'`
- **Cancellation Rate**
  - `cancelled_count / total_count` from `bookingacc`
- **Top City / Top Type**
  - Join `bookingacc -> room -> accommodation`, `GROUP BY`, `ORDER BY COUNT(*) DESC LIMIT 1`

### 13.5 True ML pricing model (technical details)

Implementation class: `AccommodationPriceRegressionService`.

- **Model family**
  - Ridge Linear Regression (L2 regularization), trained in Java (no external Python service required).

- **Feature vector (per training row)**
  - Intercept
  - room base price (`price_per_night`)
  - room capacity
  - accommodation stars
  - accommodation rating
  - weekend flag
  - seasonal encoding (`sin(month)`, `cos(month)`)

- **Target**
  - learned nightly realized price: `total_price / nights`.

- **Training pipeline**
  1. Load confirmed bookings and join room/accommodation metadata.
  2. Build feature matrix `X` and target vector `y`.
  3. Standardize non-intercept features.
  4. Solve ridge normal equation:  
     `w = (X^T X + lambda I)^-1 X^T y`
  5. Evaluate model quality with:
     - `RMSE`
     - `R2`
  6. Predict current global average nightly recommendation over available rooms.
  7. Convert prediction delta to bounded action: `[-15%, +15%]`.

- **Confidence score**
  - Combined from model quality (`R2`) and dataset volume (sample count), then clamped to a safe display range.

### 13.6 ML occupancy + recommendation synthesis

Implementation class: `AccommodationMlInsightsService`.

- Forecast occupancy combines:
  - last 30-day occupancy,
  - previous 30-day occupancy,
  - confirmed-booking momentum.
- Suggested price action uses:
  - true regression output when model is trained,
  - fallback heuristic when data is insufficient.
- Final card text (`mlDecisionSummaryLabel`) includes model context (samples/confidence and mode).

### 13.7 Is there external AI API usage for these cards?

- **No external LLM API is called for KPI/ANL/ML cards** in current implementation.
- These cards are local analytics + local ML computations running in Java over MySQL data.
- Existing external AI API usage (Groq) remains in the dedicated compare/nearby AI modules.

### 13.8 Why fallback exists

- If confirmed-booking history is too small, a pure regression recommendation is statistically weak.
- The code falls back to safer heuristic behavior and reports lower confidence until enough data is available.

---

## 14) Future Improvements

### Scalability
- Replace singleton JDBC with pooled datasource (HikariCP).
- Introduce DAO/repository abstraction and transaction boundaries per use case.
- Add Flyway/Liquibase migrations for reproducible schema management.

### AI improvements
- Prompt versioning and trace storage.
- Response caching and idempotency keys for repeat requests.
- Confidence scoring and explainability layer for AI recommendations.

### Performance optimization
- Lazy load heavy UI sections and virtualize large card lists.
- Cache map markers and image metadata.
- Add DB indexes based on query plans and high-frequency filters.

### Security hardening
- Externalize secrets and DB credentials to environment/config files.
- Introduce complete authentication + RBAC module.
- Add request throttling once REST services are introduced.

---

#

