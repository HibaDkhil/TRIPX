package tn.esprit.controllers.user;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.controllers.admin.DashboardController;
import tn.esprit.utils.ValidationUtils;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.Parent;



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
            togglePwdBtn.setText("👁️");
            isPwdVisible = false;
        } else {
            // Show password (show text)
            pwdField.setVisible(false);
            pwdField.setManaged(false);
            pwdFieldVisible.setVisible(true);
            pwdFieldVisible.setManaged(true);
            togglePwdBtn.setText("👁️");
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

        // ✅ Check for empty fields and invalid email format
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
        boolean loggedIn = userService.login(email, password);

        if (!loggedIn) {
            loginPasswordError.setText("Incorrect email or password");
            return;
        }

        System.out.println("Login successful!");

        // ✅ GET THE FULL USER OBJECT (THIS IS THE KEY FIX)
        User loggedInUser = userService.findByEmail(email);
        String role = userService.getRoleByEmail(email);

        try {
            if ("ADMIN".equalsIgnoreCase(role)) {
                // Load Admin Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setRole(role);
                controller.setCurrentUser(loggedInUser);

                Stage stage = (Stage) loginBtn.getScene().getWindow();
                Scene scene = new Scene(root);
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
                Scene scene = new Scene(root);
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

        //  Create User object
        User user = new User(firstName, lastName, email, password);

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