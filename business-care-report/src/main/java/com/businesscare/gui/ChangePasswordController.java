package com.businesscare.gui;

import com.businesscare.service.PasswordService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class ChangePasswordController {

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Label statusChangeLabel;

    private PasswordService passwordService;

    public void initialize() {
        this.passwordService = new PasswordService(); 
                                                 
        statusChangeLabel.setText("");
    }

    @FXML
    private void handleChangePassword() {
        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmNewPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            statusChangeLabel.setText("Tous les champs sont requis.");
            return;
        }

        if (!passwordService.verifyPassword(oldPass)) {
            statusChangeLabel.setText("L'ancien mot de passe est incorrect.");
            return;
        }

        if (newPass.length() < 4) { 
            statusChangeLabel.setText("Le nouveau mot de passe doit contenir au moins 4 caractères.");
            return;
        }
        
        if (newPass.equals(oldPass)) {
            statusChangeLabel.setText("Le nouveau mot de passe doit être différent de l'ancien.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            statusChangeLabel.setText("Les nouveaux mots de passe ne correspondent pas.");
            return;
        }

        passwordService.setPassword(newPass);
        statusChangeLabel.setStyle("-fx-text-fill: green;");
        statusChangeLabel.setText("Mot de passe modifié avec succès !");
        
        
        
        
        
    }
}