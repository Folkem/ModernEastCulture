package com.folva.moderneastculture;

import com.folva.moderneastculture.model.Repository;
import static com.folva.moderneastculture.model.Repository.instance;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class.getName());
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 490;

    public static final Alert confirmationAlert;
    public static final Alert warningAlert;
    public static final Alert errorAlert;

    public static Stage stage;
    public static Map<String, Parent> formMap = new HashMap<>();

    static {
        try {
            Class.forName("com.folva.moderneastculture.model.Repository");
        } catch (ClassNotFoundException e) {
            logger.fatal("Repository class hasn't been found", e);
        }

        confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.getButtonTypes().remove(1);
        warningAlert = new Alert(Alert.AlertType.WARNING);
//        warningAlert.getButtonTypes().remove(1);
        errorAlert = new Alert(Alert.AlertType.ERROR);
//        errorAlert.getButtonTypes().remove(1);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Repository.instance.connectToDatabase();
        stage = primaryStage;
        runMenuForm();
    }

    @Override
    public void stop() throws Exception {
        Repository.instance.disconnectFromDatabase();
        super.stop();
    }

    /**
     * Do you need it right now?
     * @param formName form to open and with which replace the current scene
     */
    @Deprecated
    public static void openForm(String formName) {
        try {
            Parent root;
            if (formMap.containsKey(formName)) {
                root = formMap.get(formName);
            } else {
                root = FXMLLoader.load(Main.class.getResource("view/" + formName + ".fxml"),
                        Repository.namesBundle);
                formMap.put(formName, root);
            }
            Scene formScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            stage.setScene(formScene);
        } catch (Exception e) {
            logger.error("Error while opening " + formName + ": ", e);
        }
    }

    public static Parent getForm(String formName) {
        Parent root = new Group();
        try {
            if (formMap.containsKey(formName)) {
                root = formMap.get(formName);
            } else {
                root = FXMLLoader.load(Main.class.getResource("view/" + formName + ".fxml"), Repository.namesBundle);
                formMap.put(formName, root);
            }
        } catch (Exception e) {
            logger.error("Error while opening " + formName + ": ", e);
            Main.errorAlert.setContentText(instance.getNamesBundleValue("problemOccurred"));
            Main.errorAlert.show();
            System.exit(0);
        }

        return root;
    }

    private void runMenuForm() {
        try {
            Parent menuRoot = FXMLLoader.load(getClass().getResource("view/MenuForm.fxml"), Repository.namesBundle);
            Scene menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);

            stage.setTitle(instance.getNamesBundleValue("title"));
            // TODO: 16.04.2020 do you want your app to be resizable? also think about the image bg
            stage.setResizable(false);
            stage.setScene(menuScene);
            stage.show();

            formMap.put("MenuForm", menuRoot);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error while loading menu window", e);
            System.exit(0);
        }
    }
}
