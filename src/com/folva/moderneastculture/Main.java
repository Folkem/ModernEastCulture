package com.folva.moderneastculture;

import com.folva.moderneastculture.model.Repository;
import static com.folva.moderneastculture.model.Repository.instance;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class.getName());
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 490;

    public static final Alert confirmationAlert;
    public static final Alert warningAlert;
    public static final Alert errorAlert;

    public static Stage stage;
    public static Map<String, Pair<Parent, Object>> formMap = new HashMap<>();

    static {
        try {
            Class.forName("com.folva.moderneastculture.model.Repository");
        } catch (ClassNotFoundException e) {
            logger.fatal("Repository class hasn't been found", e);
        }

        confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.getButtonTypes().remove(1);
        confirmationAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        warningAlert = new Alert(Alert.AlertType.WARNING);
        warningAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
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
        System.exit(0);
    }

    public static Parent getForm(String formName) {
        Parent root = new Group();
        try {
            if (formMap.containsKey(formName)) {
                root = formMap.get(formName).getKey();
            } else {
                FXMLLoader loader = new FXMLLoader(getResource("/res/fxml_views/" + formName + ".fxml"), Repository.namesBundle);
                root = loader.load();
                formMap.put(formName, new Pair<>(root, loader.getController()));
            }
        } catch (Exception e) {
            logger.error("Error while opening " + formName + ": ", e);
            Main.errorAlert.setContentText(instance.getNamesBundleValue("problemOccurred"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
        }

        return root;
    }

    public static Object getControllerForForm(String formName) {
        Object controller = new Object();

        if (formMap.containsKey(formName)) {
            controller = formMap.get(formName).getValue();
        }

        return controller;
    }

    public static URL getResource(String resourceName) {
        return Main.class.getResource(resourceName);
    }

    public static InputStream getFileStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Main.errorAlert.setContentText(Repository.instance.getNamesBundleValue("problemOccurred"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
            return null;
        }
    }

    private void runMenuForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getResource("/res/fxml_views/MenuForm.fxml"), Repository.namesBundle);
            Parent menuRoot = loader.load();
            Scene menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);

            formMap.put("MenuForm", new Pair<>(menuRoot, loader.getController()));

            stage.setTitle(instance.getNamesBundleValue("title"));
            stage.setResizable(false);
            stage.setScene(menuScene);
            stage.show();
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error while loading menu window", e);
            Main.errorAlert.setContentText(instance.getNamesBundleValue("problemOccurred"));
            Main.errorAlert.showAndWait();
            System.exit(-1);
        }
    }
}
