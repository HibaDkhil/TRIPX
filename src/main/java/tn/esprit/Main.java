package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // USER WINDOW
        FXMLLoader userLoader =
                new FXMLLoader(Main.class.getResource("/fxml.user/TransportUserInterface.fxml"));
        Stage userStage = new Stage();
        userStage.setScene(new Scene(userLoader.load()));
        userStage.setTitle("User Interface");
        userStage.show();

        // ADMIN WINDOW
        FXMLLoader adminLoader =
                new FXMLLoader(Main.class.getResource("/fxml/admin/transportaadmindashboard.fxml"));
        Stage adminStage = new Stage();
        adminStage.setScene(new Scene(adminLoader.load()));
        adminStage.setTitle("Admin Dashboard");
        adminStage.show();
    }

}
