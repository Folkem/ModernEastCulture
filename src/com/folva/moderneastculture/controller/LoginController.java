package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Контроллер підменю авторизації. Відповідає лише за саму авторизацію
 */
public class LoginController implements Initializable {

    @FXML
    private PasswordField tbPassword;
    @FXML
    private TextField tbLogin;

    @FXML
    private void onBLogInClick() {
        String login = tbLogin.getText();
        String password = tbPassword.getText();

        boolean maybeTwoSentences = false;
        StringBuilder textBuilder = new StringBuilder();

        if (!Repository.instance.loginIsCorrect(login)) {
            textBuilder.append(Repository.instance.getNamesBundleValue("loginIncorrect"));
            maybeTwoSentences = true;
        }
        if (!Repository.instance.passwordIsCorrect(password)) {
            if (maybeTwoSentences) textBuilder.append("\n");
            textBuilder.append(Repository.instance.getNamesBundleValue("passwordIncorrect"));
        }

        if (textBuilder.length() > 0) {
            Main.warningAlert.setContentText(textBuilder.toString());
            Main.warningAlert.show();
            return;
        }

        Main.confirmationAlert.setContentText(Repository.instance.getNamesBundleValue("credentialsAreCorrect"));
        Main.confirmationAlert.show();

        tbLogin.setText("");
        tbPassword.setText("");

        Repository.adminIsAuthorizedProperty.set(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
