package com.businesscare.gui;

import com.businesscare.service.PasswordService; 

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class PasswordController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    private PasswordService passwordService; 
    private boolean authenticated = false;

    public void initialize() { 
        this.passwordService = new PasswordService();
        
        if (!passwordService.isPasswordSet()) {
            passwordService.setDefaultPassword();
            statusLabel.setText("Mot de passe par défaut 'esgi' configuré. Veuillez vous connecter.");
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @FXML
    private void handlePasswordValidation() {
        String enteredPassword = passwordField.getText();
        if (passwordService.verifyPassword(enteredPassword)) {
            authenticated = true;
            statusLabel.setText("Mot de passe correct !");
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.close();
        } else {
            statusLabel.setText("Mot de passe incorrect. Veuillez réessayer.");
            passwordField.clear();
            authenticated = false;
        }
    }
}