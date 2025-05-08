package com.businesscare.gui;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlUrl = getClass().getResource("ReportView.fxml");
        if (fxmlUrl == null) {
            System.err.println("Erreur: Impossible de trouver ReportView.fxml. Vérifiez le chemin dans src/main/resources/com/businesscare/gui/");
            throw new IOException("Cannot find ReportView.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Tableau de Bord - Business Care");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}