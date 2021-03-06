package com.folva.moderneastculture.controller;

import com.folva.moderneastculture.Main;
import com.folva.moderneastculture.model.Repository;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

/**
 * Контроллер меню налаштувань. Відповідає лише за зміну облікових даних адміністратора
 */
public class SettingsController {

    @FXML
    private TextField tbNewLogin;
    @FXML
    private PasswordField tbNewPassword;

    @FXML
    private void onBExitClick() {
        Repository.adminIsAuthorizedProperty.set(false);
        tbNewLogin.clear();
        tbNewPassword.clear();
    }

    @FXML
    private void onBConfirmChanges() {
        String newLogin = tbNewLogin.getText();
        String newPassword = tbNewPassword.getText();
        if (fieldsAreValidated(newLogin, newPassword)) {
            Repository.instance.updateAdminLogin(newLogin);
            Repository.instance.updateAdminPassword(newPassword);
            Main.confirmationAlert.setContentText(
                    Repository.instance.getNamesBundleValue("adminCredentialsChangesSuccessfully"));
            Main.confirmationAlert.show();
        }
    }

    private boolean fieldsAreValidated(String login, String password) {
        boolean maybeTwoSentences = false;
        StringBuilder textBuilder = new StringBuilder();
        if (!Repository.instance.loginIsValid(login)) {
            textBuilder.append(Repository.instance.getNamesBundleValue("loginFormatIncorrect"));
            maybeTwoSentences = true;
        }
        if (!Repository.instance.passwordIsValid(password)) {
            if (maybeTwoSentences) textBuilder.append("\n");
            textBuilder.append(Repository.instance.getNamesBundleValue("passwordFormatIncorrect"));
        }

        if (textBuilder.length() > 0) {
            Main.warningAlert.setContentText(textBuilder.toString());
            Main.warningAlert.show();
            return false;
        }

        return true;
    }

}
