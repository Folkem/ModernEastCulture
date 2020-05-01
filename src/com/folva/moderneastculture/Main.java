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

/**
 * Головний клас з точкою входу в додаток, головними вспливаючими
 * вікнами та деякими методами-утилітами
 */
public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class.getName());
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 490;

    /**
     * Вспливаюче вікно "підтвердження" або ж якоїсь інформації
     */
    public static final Alert confirmationAlert;
    /**
     * Вспливаюче вікно попереджень або якихось малих помилок
     */
    public static final Alert warningAlert;
    /**
     * Вспливаюче вікно середніх та великих помилок
     */
    public static final Alert errorAlert;

    /**
     * Головне (і єдине) вікно, на якому і висить весь інтерфейс програми
     */
    public static Stage stage;
    /**
     * Колекція пар "назва меню-пара{об'єкт Parent меню-контроллер меню}"
     */
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

    /**
     * Головна точка входу програми
     * @param args аргументи командного рядку
     */
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

    /**
     * @param formName Назва меню, яке потрібно отримати
     * @return об'єкт Parent, який містить в собі все меню, за яким буде закріплений
     * його відповідний контроллер, якщо він є, і null, якщо нема
     */
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

    /**
     * @param formName Назва меню, контроллер якого потрібно отримати
     * @return об'єкт контроллеру меню, якщо він є, і null, якщо нема
     */
    public static Object getControllerForForm(String formName) {
        Object controller = new Object();

        if (formMap.containsKey(formName)) {
            controller = formMap.get(formName).getValue();
        }

        return controller;
    }

    /**
     * @param resourceName Ім'я ресурсу
     * @return шлях до ресурсу у вигляді URL-об'єкту, якщо він є, і null, якщо нема
     */
    public static URL getResource(String resourceName) {
        return Main.class.getResource(resourceName);
    }

    /**
     * @param file Файл, входячий поток якого потрібно отримати
     * @return входячий поток відповідного файла, якщо він є, і null, якщо нема.
     * При помилці додаток закривається, перед цим виводячи повідомлення про помилку.
     */
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
