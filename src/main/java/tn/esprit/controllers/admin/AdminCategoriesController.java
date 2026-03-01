package tn.esprit.controllers.admin;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.PackCategory;
import tn.esprit.services.PackCategoryService;
import tn.esprit.services.PackService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminCategoriesController {

    @FXML private FlowPane categoriesGrid;
    @FXML private TextField searchField;
    @FXML private Button btnAddCategory;
    @FXML private Label lblTotalCategories, lblTotalPacks;

    private PackCategoryService categoryService;
    private PackService packService;

    private List<PackCategory> allCategories;
    private tn.esprit.entities.User currentUser;
    private String userRole;

    public void initialize() {
        categoryService = new PackCategoryService();
        packService = new PackService();

        loadCategories();
        updateStats();

        btnAddCategory.setOnAction(e -> handleAddCategory());
        searchField.textProperty().addListener((obs, old, newVal) -> filterCategories());
    }

    public void setCurrentUser(tn.esprit.entities.User user) {
        this.currentUser = user;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    private void loadCategories() {
        try {
            allCategories = categoryService.afficherList();
            displayCategories(allCategories);
        } catch (SQLException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    private void displayCategories(List<PackCategory> categories) {
        categoriesGrid.getChildren().clear();

        for (PackCategory category : categories) {
            categoriesGrid.getChildren().add(createCategoryCard(category));
        }
    }

    private VBox createCategoryCard(PackCategory category) {
        VBox card = new VBox(16);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 24; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2); -fx-cursor: hand;");

        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(79,172,254,0.3), 20, 0, 0, 8);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-translate-y: -4;", ""));
        });

        try {
            // Icon + Name
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label("🏷");
            icon.setStyle("-fx-font-size: 32px;");

            Label name = new Label(category.getName());
            name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A202C;");
            name.setWrapText(true);
            HBox.setHgrow(name, Priority.ALWAYS);

            header.getChildren().addAll(icon, name);

            // Pack count
            int packCount = packService.getByCategory(category.getIdCategory()).size();

            HBox countBox = new HBox(8);
            countBox.setAlignment(Pos.CENTER_LEFT);
            countBox.setStyle("-fx-background-color: #F7FAFC; -fx-background-radius: 10; -fx-padding: 12;");

            Label countLabel = new Label("📦 " + packCount + " pack" + (packCount != 1 ? "s" : ""));
            countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A5568; -fx-font-weight: 600;");

            countBox.getChildren().add(countLabel);

            // Actions
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER);

            Button editBtn = new Button("✏️ Edit");
            editBtn.setStyle("-fx-background-color: #4facfe; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
            editBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setOnAction(e -> handleEditCategory(category));

            Button deleteBtn = new Button("🗑️");
            deleteBtn.setStyle("-fx-background-color: #FED7D7; -fx-text-fill: #C53030; -fx-font-weight: 600; " +
                    "-fx-background-radius: 8; -fx-padding: 10 16; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> handleDeleteCategory(category));

            actions.getChildren().addAll(editBtn, deleteBtn);

            card.getChildren().addAll(header, countBox, actions);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return card;
    }

    private void updateStats() {
        if (allCategories == null || allCategories.isEmpty()) {
            lblTotalCategories.setText("0");
            lblTotalPacks.setText("0");
            return;
        }

        lblTotalCategories.setText(String.valueOf(allCategories.size()));

        try {
            int totalPacks = packService.afficherList().size();
            lblTotalPacks.setText(String.valueOf(totalPacks));
        } catch (SQLException e) {
            lblTotalPacks.setText("0");
        }
    }

    private void filterCategories() {
        if (allCategories == null) return;

        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            displayCategories(allCategories);
        } else {
            List<PackCategory> filtered = allCategories.stream()
                    .filter(cat -> cat.getName().toLowerCase().contains(searchText))
                    .toList();
            displayCategories(filtered);
        }
    }

    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Category");
        dialog.setHeaderText("Create New Pack Category");
        dialog.setContentText("Category Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    PackCategory newCat = new PackCategory(name.trim());
                    categoryService.add(newCat);
                    loadCategories();
                    updateStats();
                    showInfo("Category added successfully!");
                } catch (SQLException e) {
                    showError("Failed to add category: " + e.getMessage());
                }
            }
        });
    }

    private void handleEditCategory(PackCategory category) {
        if (category == null) return;

        TextInputDialog dialog = new TextInputDialog(category.getName());
        dialog.setTitle("Edit Category");
        dialog.setHeaderText("Edit Pack Category");
        dialog.setContentText("Category Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    category.setName(name.trim());
                    categoryService.modifier(category);
                    loadCategories();
                    showInfo("Category updated successfully!");
                } catch (SQLException e) {
                    showError("Failed to update category: " + e.getMessage());
                }
            }
        });
    }

    private void handleDeleteCategory(PackCategory category) {
        if (category == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Category");
        confirm.setHeaderText("Delete " + category.getName() + "?");
        confirm.setContentText("Packs in this category will have their category set to NULL.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categoryService.delete(category);
                loadCategories();
                updateStats();
                showInfo("Category deleted successfully!");
            } catch (SQLException e) {
                showError("Failed to delete category: " + e.getMessage());
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
