package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    private static final Logger logger = LogManager.getLogger(MenuController.class);

    @FXML
    private Tab tabAnime;
    @FXML
    private AnimeController animeController;
    @FXML
    private LoginController loginController;
    @FXML
    private Tab tabSettings;
    @FXML
    private TabPane tabPane;

    public MenuController() {
    }

    @FXML
    private void adminHasAuthorized() {
        logger.info("Admin has logged in!");

        Parent settingsPanel = Main.getForm("SettingsForm");
        tabSettings.setContent(settingsPanel);
    }

    @FXML
    private void adminHasUnauthorized() {
        logger.info("Admin has logged out!");

        Parent loginPanel = Main.getForm("LoginForm");
        tabSettings.setContent(loginPanel);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Repository.adminIsAuthorizedProperty.addListener(observable -> {
            if (Repository.adminIsAuthorizedProperty.get()) {
                adminHasAuthorized();
            } else {
                adminHasUnauthorized();
            }
        });

        Parent loginForm = (Parent)(tabSettings.getContent());
        Main.formMap.put("LoginForm", new Pair<>(loginForm, loginController));

        Parent animeContentForm = (Parent)(tabAnime.getContent());
        Main.formMap.put("AnimeForm", new Pair<>(animeContentForm, animeController));

        animeController.setTabAnime(tabAnime);

        tabPane.getSelectionModel().select(1);
    }
}
