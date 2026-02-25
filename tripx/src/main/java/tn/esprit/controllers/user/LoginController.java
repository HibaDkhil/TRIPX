package tn.esprit.controllers.user;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.controllers.admin.DashboardController;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.utils.ValidationUtils;
import tn.esprit.services.EmailReputationService;

import java.util.List;
import java.util.Optional;

import java.io.IOException;



public class LoginController {

    @FXML
    private AnchorPane leftContainer;

    // LOGIN SIDE
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField pwdField;

    @FXML
    private Button togglePwdBtn;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Button loginBtn;

    @FXML
    private Button createAccountBtn;

    @FXML
    private Button googleBtn;

    @FXML
    private Button facebookBtn;

    @FXML
    private Button linkedinBtn;

    // SIGNUP SIDE
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailFieldSignup;
    @FXML
    private PasswordField pwdFieldSignup;
    @FXML
    private Button togglePwdSignupBtn;
    @FXML
    private PasswordField confirmPwdField;
    @FXML
    private Button signUpBtn;
    @FXML
    private Button backToLoginBtn;
    @FXML
    private Label firstNameError;
    @FXML
    private Label lastNameError;
    @FXML
    private Label emailError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmPasswordError;


    // Hidden TextFields for showing passwords
    private TextField pwdFieldVisible;
    private TextField pwdFieldSignupVisible;

    // Track visibility state
    private boolean isPwdVisible = false;
    private boolean isPwdSignupVisible = false;

    @FXML
    public void initialize() {
        setupButtonHover();
        setupPasswordToggle();
    }

    private void setupPasswordToggle() {
        // Create hidden TextField for login password (same position/style as PasswordField)
        pwdFieldVisible = new TextField();
        pwdFieldVisible.setPromptText("Password");
        pwdFieldVisible.setStyle(pwdField.getStyle());
        pwdFieldVisible.setPrefWidth(pwdField.getPrefWidth());
        pwdFieldVisible.setPrefHeight(pwdField.getPrefHeight());
        pwdFieldVisible.setVisible(false);
        pwdFieldVisible.setManaged(false);

        // Add to same parent (HBox)
        ((javafx.scene.layout.HBox) pwdField.getParent()).getChildren().add(0, pwdFieldVisible);

        // Bind text between PasswordField and TextField
        pwdFieldVisible.textProperty().bindBidirectional(pwdField.textProperty());

        // Setup toggle button for login password
        togglePwdBtn.setOnAction(e -> togglePasswordVisibility());


        // Create hidden TextField for signup password
        pwdFieldSignupVisible = new TextField();
        pwdFieldSignupVisible.setPromptText("Password");
        pwdFieldSignupVisible.setStyle(pwdFieldSignup.getStyle());
        pwdFieldSignupVisible.setPrefWidth(pwdFieldSignup.getPrefWidth());
        pwdFieldSignupVisible.setPrefHeight(pwdFieldSignup.getPrefHeight());
        pwdFieldSignupVisible.setVisible(false);
        pwdFieldSignupVisible.setManaged(false);

        // Add to same parent (HBox)
        ((javafx.scene.layout.HBox) pwdFieldSignup.getParent()).getChildren().add(0, pwdFieldSignupVisible);

        // Bind text between PasswordField and TextField
        pwdFieldSignupVisible.textProperty().bindBidirectional(pwdFieldSignup.textProperty());

        // Setup toggle button for signup password
        togglePwdSignupBtn.setOnAction(e -> togglePasswordSignupVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPwdVisible) {
            // Hide password (show dots)
            pwdField.setVisible(true);
            pwdField.setManaged(true);
            pwdFieldVisible.setVisible(false);
            pwdFieldVisible.setManaged(false);
            
            // Icon logic to match signup
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/eyeIcon.png")));
            icon.setFitWidth(19);
            icon.setFitHeight(22);
            togglePwdBtn.setGraphic(icon);
            togglePwdBtn.setText(""); // Remove text if using graphic

            isPwdVisible = false;
        } else {
            // Show password (show text)
            pwdField.setVisible(false);
            pwdField.setManaged(false);
            pwdFieldVisible.setVisible(true);
            pwdFieldVisible.setManaged(true);
            
            // Icon logic (if you have eye-closed.png, otherwise just keep current icon or text)
            // For now, let's keep it visible since user wants it like the other eye
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/eyeIcon.png")));
            icon.setFitWidth(19);
            icon.setFitHeight(22);
            togglePwdBtn.setGraphic(icon);
            togglePwdBtn.setText("");

            isPwdVisible = true;
        }
    }

    private void togglePasswordSignupVisibility() {
        if (isPwdSignupVisible) {
            // Hide password (show dots)
            pwdFieldSignup.setVisible(true);
            pwdFieldSignup.setManaged(true);
            pwdFieldSignupVisible.setVisible(false);
            pwdFieldSignupVisible.setManaged(false);

            // Change icon back to eye-open
            ImageView icon = (ImageView) togglePwdSignupBtn.getGraphic();
            icon.setImage(new Image(getClass().getResourceAsStream("/images/eyeIcon.png")));

            isPwdSignupVisible = false;
        } else {
            // Show password (show text)
            pwdFieldSignup.setVisible(false);
            pwdFieldSignup.setManaged(false);
            pwdFieldSignupVisible.setVisible(true);
            pwdFieldSignupVisible.setManaged(true);

            // Change icon to eye-closed (if you have it)
            ImageView icon = (ImageView) togglePwdSignupBtn.getGraphic();
            // If you have eye-closed.png, uncomment this:
            // icon.setImage(new Image(getClass().getResourceAsStream("/images/eye-closed.png")));

            isPwdSignupVisible = true;
        }
    }

    private void setupButtonHover() {
        // LOGIN button hover (white → #001737)
        loginBtn.setOnMouseEntered(e -> {
            loginBtn.setStyle("-fx-background-color: #001737; -fx-border-color: #001737; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });
        loginBtn.setOnMouseExited(e -> {
            loginBtn.setStyle("-fx-background-color: white; -fx-border-color: #001737; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: #001737; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });

        // CREATE ACCOUNT button hover (#001737 → white)
        createAccountBtn.setOnMouseEntered(e -> {
            createAccountBtn.setStyle("-fx-background-color: white; -fx-border-color: #001737; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: #001737; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });
        createAccountBtn.setOnMouseExited(e -> {
            createAccountBtn.setStyle("-fx-background-color: #001737; -fx-border-color: #001737; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });

        // SIGN UP button hover
        signUpBtn.setOnMouseEntered(e -> {
            signUpBtn.setStyle("-fx-background-color: #EEEBE8; -fx-border-color: #EEEBE8; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: #001737; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });
        signUpBtn.setOnMouseExited(e -> {
            signUpBtn.setStyle("-fx-background-color: #001737; -fx-border-color: EEEBE8; -fx-border-width: 1; -fx-border-radius: 15; -fx-background-radius: 25; -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        });

        // BACK TO LOGIN button hover
        backToLoginBtn.setOnMouseEntered(e -> {
            backToLoginBtn.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: #001737; -fx-border-width: 2; -fx-border-radius: 25; -fx-background-radius: 25; -fx-text-fill: #001737; -fx-font-family: 'Poppins'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        });
        backToLoginBtn.setOnMouseExited(e -> {
            backToLoginBtn.setStyle("-fx-background-color: white; -fx-border-color: #3a3a3a; -fx-border-width: 1; -fx-border-radius: 15; -fx-background-radius: 25; -fx-text-fill: #001737; -fx-font-family: 'Poppins'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        });
    }

    @FXML
    private Label loginEmailError;
    @FXML
    private Label loginPasswordError;

    @FXML
    private void handleLogin() {
        // Clear previous error messages
        loginEmailError.setText("");
        loginPasswordError.setText("");

        String email = emailField.getText().trim();
        String password = pwdField.getText();

        boolean hasError = false;

        //  Check for empty fields and invalid email format
        if (email.isEmpty()) {
            loginEmailError.setText("Please fill your email");
            hasError = true;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            loginEmailError.setText("Invalid email format");
            hasError = true;
        }

        if (password.isEmpty()) {
            loginPasswordError.setText("Please fill your password");
            hasError = true;
        }

        if (hasError) return;

        //  Attempt login
        UserService userService = new UserService();
        User loggedInUser = null;
        try {
            loggedInUser = userService.findByEmail(email);
        } catch (Exception e) {
            // This is just a safety catch
        }

        if (loggedInUser == null) {
            // Check if it's because the user doesn't exist or because DB is offline
            if (tn.esprit.utils.MyDB.getInstance().getConx() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText("Communications link failure");
                alert.setContentText("The database is offline. Please start XAMPP/MySQL Control Panel and refresh.");
                alert.showAndWait();
                return;
            }
            loginPasswordError.setText("Incorrect email or password");
            return;
        }

        boolean passwordMatch = false;
        if (loggedInUser != null) {
            String storedPassword = loggedInUser.getPassword();
            try {
                // Try BCrypt check
                passwordMatch = BCrypt.checkpw(password, storedPassword);
            } catch (IllegalArgumentException e) {
                // Fallback for legacy plain-text passwords
                System.out.println("DEBUG: Legacy password detected, falling back to plain-text check.");
                passwordMatch = password.equals(storedPassword);
            }
        }

        if (loggedInUser == null || !passwordMatch) {
            loginPasswordError.setText("Incorrect email or password");
            return;
        }

        System.out.println("Login successful!");
        tn.esprit.utils.SessionManager.setCurrentUserId(loggedInUser.getUserId());

        //  GET THE FULL USER OBJECT (THIS IS THE KEY FIX)

        String role = userService.getRoleByEmail(email);

        try {
            if (role != null && role.toLowerCase().startsWith("admin")) {
                // Load Admin Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setRole(role);
                controller.setCurrentUser(loggedInUser);

                Stage stage = (Stage) loginBtn.getScene().getWindow();
                double width = stage.getWidth();
                double height = stage.getHeight();
                
                Scene scene = new Scene(root, width, height);
                stage.setScene(scene);
                stage.setResizable(true);
                stage.centerOnScreen();
                stage.show();
            } else {
                // Load User Home (Front Office)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
                Parent root = loader.load();

                // Pass the logged-in user to HomeController
                HomeController controller = loader.getController();
                controller.setUser(loggedInUser);
                
                Stage stage = (Stage) loginBtn.getScene().getWindow();
                double width = stage.getWidth();
                double height = stage.getHeight();
                
                Scene scene = new Scene(root, width, height);
                stage.setScene(scene);
                stage.setResizable(true);
                stage.centerOnScreen();
                stage.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + e.getMessage());
        }
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/forgot_password.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load Forgot Password view: " + e.getMessage());
        }
    }


    @FXML
    private void handleCreateAccount() {
        TranslateTransition slide = new TranslateTransition();
        slide.setDuration(Duration.seconds(1.5));
        slide.setNode(leftContainer);
        slide.setToX(640);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        slide.play();

        System.out.println("Sliding to create account!");
    }

    @FXML
    private void handleBackToLogin() {
        TranslateTransition slide = new TranslateTransition();
        slide.setDuration(Duration.seconds(1.5));
        slide.setNode(leftContainer);
        slide.setToX(0);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        slide.play();

        System.out.println("Sliding back to login!");
    }


    @FXML
    private void handleSignUp() {
        // Clear previous errors and styles
        clearSignupErrors();

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailFieldSignup.getText().trim();
        String password = pwdFieldSignup.getText();
        String confirmPassword = confirmPwdField.getText();

        boolean hasError = false;

        // Validate First Name
        if (!ValidationUtils.isValidName(firstName)) {
            firstNameError.setText(ValidationUtils.isNotEmpty(firstName) ?
                    ValidationUtils.getNameError("First name") :
                    ValidationUtils.getRequiredFieldError("First name"));
            firstNameField.getStyleClass().add("input-error");
            hasError = true;
        }

        // Validate Last Name
        if (!ValidationUtils.isValidName(lastName)) {
            lastNameError.setText(ValidationUtils.isNotEmpty(lastName) ?
                    ValidationUtils.getNameError("Last name") :
                    ValidationUtils.getRequiredFieldError("Last name"));
            lastNameField.getStyleClass().add("input-error");
            hasError = true;
        }

        // Validate Email
        if (!ValidationUtils.isValidEmail(email)) {
            emailError.setText(ValidationUtils.isNotEmpty(email) ?
                    ValidationUtils.getEmailError() :
                    ValidationUtils.getRequiredFieldError("Email"));
            emailFieldSignup.getStyleClass().add("input-error");
            hasError = true;
        }

        // Validate Password
        if (!ValidationUtils.isValidPassword(password)) {
            passwordError.setText(ValidationUtils.isNotEmpty(password) ?
                    ValidationUtils.getPasswordError() :
                    ValidationUtils.getRequiredFieldError("Password"));
            pwdFieldSignup.getStyleClass().add("input-error");
            hasError = true;
        }

        // Validate Confirm Password
        if (!ValidationUtils.passwordsMatch(password, confirmPassword)) {
            confirmPasswordError.setText(ValidationUtils.isNotEmpty(confirmPassword) ?
                    ValidationUtils.getPasswordMismatchError() :
                    ValidationUtils.getRequiredFieldError("Confirm password"));
            confirmPwdField.getStyleClass().add("input-error");
            hasError = true;
        }

        if (hasError) return; // stop if any error

        // ========== NEW: Check email reputation with AbstractAPI ==========
        EmailReputationService reputationService = new EmailReputationService();
        EmailReputationService.EmailReputation reputation = reputationService.checkEmailReputation(email);

        if (reputation == null) {
            // API error - show warning but allow registration
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Email verification service unavailable");
            alert.setContentText("We couldn't verify your email. You can still register, but please ensure you're using a valid email.");
            alert.showAndWait();
        } else {
            // Check if email is valid
            if (!reputation.isFormatValid() || !reputation.isSmtpValid()) {
                emailError.setText("Email appears invalid: " + reputation.getStatusDetail());
                emailFieldSignup.getStyleClass().add("input-error");
                return;
            }

            // Check if email is disposable
            if (reputation.isDisposable()) {
                emailError.setText("Disposable emails are not allowed");
                emailFieldSignup.getStyleClass().add("input-error");
                return;
            }

            // Check for data breaches
            if (reputation.getTotalBreaches() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Security Alert");
                alert.setHeaderText("Email found in data breaches");
                alert.setContentText("This email appears in " + reputation.getTotalBreaches() +
                        " data breach(es). It's recommended to use a strong, unique password.\n\nDo you want to continue?");

                alert.getDialogPane().getButtonTypes().clear();
                alert.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);

                Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                yesButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                noButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.YES) {
                    return; // User cancelled
                }
            }

            // Optional: Show email quality info
            System.out.println("Email quality score: " + (reputation.getScore() * 100) + "%");
        }
        // ========== END OF API INTEGRATION ==========

        //  Hash password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        //  Create User object
        User user = new User(firstName, lastName, email, hashedPassword);

        UserService userService = new UserService();
        boolean success = userService.createUser(user);

        if (success) {
            System.out.println("User created successfully in DB!");

            // Redirect to Onboarding
            try {
                // Fetch the user to get the ID
                User newUser = userService.findByEmail(email);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/onboarding.fxml"));
                Parent root = loader.load();

                OnboardingController controller = loader.getController();
                controller.setUser(newUser);

                Stage stage = (Stage) signUpBtn.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load onboarding.fxml");
            }
        } else {
            emailError.setText("Email already exists");
            emailFieldSignup.getStyleClass().add("input-error");
        }
    }

    @FXML
    private void handleGoogleLogin() {
        processSocialLogin("Google");
    }

    @FXML
    private void handleFacebookLogin() {
        processSocialLogin("Facebook");
    }

    @FXML
    private void handleLinkedInLogin() {
        processSocialLogin("LinkedIn");
    }

    /**
     * Simulated Social Login Flow
     * 1. Opens the provider's login page in the system browser.
     * 2. Prompts the user to enter their social account email in a dialog.
     * 3. Validates the email against the database.
     */
    private void processSocialLogin(String provider) {
        String url = "";
        switch (provider) {
            case "Google" -> url = "https://accounts.google.com/";
            case "Facebook" -> url = "https://www.facebook.com/login";
            case "LinkedIn" -> url = "https://www.linkedin.com/login";
        }

        System.out.println("Opening " + provider + " login page: " + url);
        
        // Open browser
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                // Fallback for environments where Desktop isn't supported
                System.out.println("Desktop browsing not supported. Please login at: " + url);
            }
        } catch (Exception e) {
            System.err.println("Could not open browser: " + e.getMessage());
        }

        // Show a dialog to simulate finishing the social login
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle(provider + " Login");
        dialog.setHeaderText("Complete your " + provider + " sign-in");
        dialog.setContentText("Enter the email associated with your " + provider + " account:");

        java.util.Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            String socialEmail = result.get().trim();
            if (socialEmail.isEmpty()) return;

            UserService userService = new UserService();
            User testUser = null;
            try {
                testUser = userService.findByEmail(socialEmail);
            } catch (Exception e) {
                System.err.println("Database error during social login check: " + e.getMessage());
            }

            if (testUser == null) {
                // USER DOES NOT EXIST - Prompt to create account
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Account Not Found");
                alert.setHeaderText("No TripX account linked to this email");
                alert.setContentText("We couldn't find an account for " + socialEmail + ". Please create an account first to link your " + provider + " login.");
                alert.showAndWait();
                
                // Switch to signup side automatically
                handleCreateAccount();
                emailFieldSignup.setText(socialEmail);
                return;
            }

            // SUCCESS - User exists, redirect
            System.out.println("Social login successful! Redirecting user: " + testUser.getEmail());
            tn.esprit.utils.SessionManager.setCurrentUserId(testUser.getUserId());
            
            String role = userService.getRoleByEmail(socialEmail);

            try {
                if (role != null && role.toLowerCase().startsWith("admin")) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/dashboard.fxml"));
                    Parent root = loader.load();
                    DashboardController controller = loader.getController();
                    controller.setRole(role);
                    controller.setCurrentUser(testUser);

                    Stage stage = (Stage) googleBtn.getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();
                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
                    Parent root = loader.load();
                    HomeController controller = loader.getController();
                    controller.setUser(testUser);
                    
                    Stage stage = (Stage) googleBtn.getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Navigation Failed");
                alert.setContentText("Could not load the dashboard: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void clearSignupErrors() {
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        passwordError.setText("");
        confirmPasswordError.setText("");
        
        firstNameField.getStyleClass().remove("input-error");
        lastNameField.getStyleClass().remove("input-error");
        emailFieldSignup.getStyleClass().remove("input-error");
        pwdFieldSignup.getStyleClass().remove("input-error");
        confirmPwdField.getStyleClass().remove("input-error");
    }



}