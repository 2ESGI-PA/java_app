package com.businesscare.gui;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        
        FXMLLoader passwordLoader = new FXMLLoader(getClass().getResource("PasswordDialog.fxml"));
        Parent passwordRoot = passwordLoader.load();
        PasswordController passwordController = passwordLoader.getController();

        Stage passwordStage = new Stage();
        passwordStage.setTitle("Authentification requise");
        passwordStage.initModality(Modality.APPLICATION_MODAL); 
        passwordStage.initOwner(primaryStage); 
        passwordStage.setScene(new Scene(passwordRoot));
        
        
        

        passwordStage.showAndWait(); 

        if (passwordController.isAuthenticated()) {
            
            URL fxmlUrl = getClass().getResource("ReportView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Erreur: Impossible de trouver ReportView.fxml. Vérifiez le chemin dans src/main/resources/com/businesscare/gui/");
                
                showAlert("Erreur Critique", "Fichier de vue principal introuvable (ReportView.fxml). L'application va se fermer.");
                return; 
            }

            FXMLLoader mainLoader = new FXMLLoader(fxmlUrl);
            Parent mainRoot = mainLoader.load();

            Scene scene = new Scene(mainRoot, 800, 600);

            primaryStage.setTitle("Tableau de Bord - Business Care");
            primaryStage.setScene(scene);
            primaryStage.show();
        } else {
            
            System.out.println("Authentification échouée ou annulée. Fermeture de l'application.");
            
             showAlert("Authentification Échouée", "Le mot de passe n'a pas été fourni ou était incorrect. L'application va se fermer.");
            
        }
    }
    
    
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }
}