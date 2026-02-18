# Admin Shell Kit

Reusable template extracted from the admin dashboard for teammates working on other modules.

## Included Files

- `fxml/AdminShell.fxml`
- `css/admin-shell.css`
- `java/AdminShellController.java`

## What This Kit Covers

- Left sidebar layout + section toggles
- Top header/topbar layout
- Sidebar hide/show animation
- Submenu expand/collapse animation
- Dark mode toggle class (`dark-mode`)
- Center content placeholder (`centerContentHost`) for module injection

## What This Kit Does NOT Include

- Module-specific business logic (CRUD, services, filters, charts data)
- Module-specific cards/tables/forms
- Existing accommodation dashboard logic

## How To Integrate (Teammates)

1. Copy `AdminShell.fxml` to your resources path, for example:
   - `src/main/resources/fxml/adminn/AdminShell.fxml`
2. Copy `admin-shell.css` to:
   - `src/main/resources/css/admin-shell.css`
3. Copy `AdminShellController.java` to your Java package and update:
   - package line
   - `fx:controller` in `AdminShell.fxml`
4. Ensure logo exists:
   - `src/main/resources/images/tripx-logo.png`
5. Load shell FXML in your module entry controller.
6. Load your module content FXML separately.
7. Call `setCenterContent(moduleNode)` on `AdminShellController`.
8. Wire menu item navigation handlers for your own routes/pages.

## Minimal Integration Example

```java
FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/adminn/AdminShell.fxml"));
Parent shellRoot = shellLoader.load();
AdminShellController shellController = shellLoader.getController();

FXMLLoader moduleLoader = new FXMLLoader(getClass().getResource("/fxml/adminn/your-module-view.fxml"));
Node moduleNode = moduleLoader.load();

shellController.setCenterContent(moduleNode);
```

## Contract To Keep Stable

- Keep these `fx:id` values unchanged unless controller is updated:
  - `sidebar`, `sidebarToggle`, `sidebarOpenButton`
  - `dashboardToggle`, `usersToggle`, `accommodationsToggle`, `destinationsToggle`
  - `dashboardMenu`, `usersMenu`, `accommodationsMenu`, `destinationsMenu`
  - `darkModeToggle`, `languageSelector`, `profileDropdown`
  - `centerContentHost`
- Keep these style classes unchanged in FXML/CSS:
  - `sidebar-container`, `main-header`, `menu-item`, `menu-toggle`, `breadcrumb-item`

## Notes

- This kit is isolated and does not modify your current accommodation admin files.
- Teammates can adopt this shell while you keep your current implementation unchanged.
