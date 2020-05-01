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

/**
 * Контроллер меню, яке містить всі інші підменю. Відповідає за переключення з
 * одного підменю до іншого тип, що надає доступ до вкладок відповідних підменю,
 * а також за переключення підменю авторизації та підменю налаштувань
 */
public class MenuController implements Initializable {

    private static final Logger logger = LogManager.getLogger(MenuController.class);

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab tabAnime;
    @FXML
    private AnimeController animeController;
    @FXML
    private Tab tabComics;
    @FXML
    private ComicsController comicsController;
    @FXML
    private Tab tabLogin;
    @FXML
    private LoginController loginController;

    @FXML
    private void adminHasAuthorized() {
        logger.info("Admin has logged in!");

        Parent settingsPanel = Main.getForm("SettingsForm");
        tabLogin.setContent(settingsPanel);
    }

    @FXML
    private void adminHasUnauthorized() {
        logger.info("Admin has logged out!");

        Parent loginPanel = Main.getForm("LoginForm");
        tabLogin.setContent(loginPanel);
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

        Parent loginForm = (Parent)(tabLogin.getContent());
        Main.formMap.put("LoginForm", new Pair<>(loginForm, loginController));

        Parent animeContentForm = (Parent)(tabAnime.getContent());
        Main.formMap.put("AnimeForm", new Pair<>(animeContentForm, animeController));

        Parent comicsContentForm = (Parent)(tabComics.getContent());
        Main.formMap.put("ComicsForm", new Pair<>(comicsContentForm, comicsController));

        animeController.setTabAnime(tabAnime);
        comicsController.setTabComics(tabComics);

        tabPane.getSelectionModel().select(1);
    }
}
