package com.businesscare.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.businesscare.config.DatabaseConfig;
import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.service.DatabaseService;
import com.businesscare.service.PdfReportService;
import com.businesscare.service.StatisticsService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

class ReportData {
    final List<ClientAccount> clients;
    final List<Evenement> events;
    final List<Prestation> services;
    final File pdfFile;

    ReportData(List<ClientAccount> c, List<Evenement> e, List<Prestation> s, File f) {
        clients = (c != null) ? c : Collections.emptyList();
        events = (e != null) ? e : Collections.emptyList();
        services = (s != null) ? s : Collections.emptyList();
        pdfFile = f;
    }
}

public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @FXML private Button generateButton;
    @FXML private TextArea statusTextArea;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Hyperlink openPdfLink;

    @FXML private TableView<ClientAccount> clientsTable;
    @FXML private TableColumn<ClientAccount, String> clientNameCol;
    @FXML private TableColumn<ClientAccount, String> clientTypeCol;
    @FXML private TableColumn<ClientAccount, String> clientCityCol;
    @FXML private TableColumn<ClientAccount, String> clientCaCol;

    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, String> eventNameCol;
    @FXML private TableColumn<Evenement, String> eventTypeCol;
    @FXML private TableColumn<Evenement, String> eventLocationCol;
    @FXML private TableColumn<Evenement, String> eventStartDateCol;

    @FXML private TableView<Prestation> servicesTable;
    @FXML private TableColumn<Prestation, String> serviceNameCol;
    @FXML private TableColumn<Prestation, String> serviceTypeCol;
    @FXML private TableColumn<Prestation, Double> serviceCostCol;
    @FXML private TableColumn<Prestation, String> serviceAvailabilityCol;

    private final StatisticsService statisticsService;
    private PdfReportService pdfReportService; 

    private File generatedPdfFile = null;
    private static final String REPORT_FILENAME_PREFIX = "Rapport_Activite_Business_Care_";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private static final DecimalFormat euroFormat = new DecimalFormat("#,##0.00 €");
    private static final DecimalFormat kEuroFormat = new DecimalFormat("#,##0 k€");
    private static final DecimalFormat mEuroFormat = new DecimalFormat("#,##0.00 M€");


    public ReportController() {
        statisticsService = new StatisticsService();
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return "N/A";
        }
        double val = value.doubleValue();
        if (val >= 1_000_000) {
            return mEuroFormat.format(val / 1_000_000.0);
        } else if (val >= 1_000) {
            return kEuroFormat.format(val / 1_000.0);
        } else {
            return euroFormat.format(val);
        }
    }

    @FXML
    private void initialize() {
        statusTextArea.appendText("Prêt.\n");
        openPdfLink.setVisible(false);
        progressIndicator.setVisible(false);

        clientNameCol.setCellValueFactory(new PropertyValueFactory<>("nomSociete"));
        clientTypeCol.setCellValueFactory(new PropertyValueFactory<>("typeClient"));
        clientCityCol.setCellValueFactory(new PropertyValueFactory<>("ville"));
        clientCaCol.setCellValueFactory(cellData -> {
            Double ca = cellData.getValue().getChiffreAffairesAnnuel();
            return new SimpleStringProperty(formatCurrency(ca));
        });

        eventNameCol.setCellValueFactory(new PropertyValueFactory<>("nomEvenement"));
        eventTypeCol.setCellValueFactory(new PropertyValueFactory<>("typeEvenement"));
        eventLocationCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        eventStartDateCol.setCellValueFactory(cellData -> {
             Date date = cellData.getValue().getDateDebut();
             return new SimpleStringProperty(date == null ? "" : dateFormat.format(date));
        });

        serviceNameCol.setCellValueFactory(new PropertyValueFactory<>("nomPrestation"));
        serviceTypeCol.setCellValueFactory(new PropertyValueFactory<>("typePrestation"));
        serviceCostCol.setCellValueFactory(new PropertyValueFactory<>("coutUnitaire"));
        serviceAvailabilityCol.setCellValueFactory(cellData -> {
            boolean disponible = cellData.getValue().isDisponibilite();
            String displayText = disponible ? "✓ Oui" : "✗ Non";
            return new SimpleStringProperty(displayText);
        });
    }

    @FXML
    private void handleGenerateReport() {
        generateButton.setDisable(true);
        statusTextArea.clear();
        statusTextArea.appendText("Lancement de la génération du rapport...\n");
        progressIndicator.setVisible(true);
        openPdfLink.setVisible(false);
        generatedPdfFile = null;

        clientsTable.getItems().clear();
        eventsTable.getItems().clear();
        servicesTable.getItems().clear();

        Task<ReportData> reportTask = new Task<>() {
            @Override
            protected ReportData call() throws Exception {
                Connection conn = null;
                DatabaseService databaseService; 
                try {
                    updateMessage("Connexion à la base de données...");
                    conn = DatabaseConfig.getConnection();
                    databaseService = new DatabaseService(conn); 

                    statisticsService.setDatabaseService(databaseService);
                    pdfReportService = new PdfReportService(statisticsService, databaseService);


                    updateMessage("Récupération des données des clients...");
                    List<ClientAccount> clientAccounts = databaseService.getAllClientAccounts();
                    updateMessage(String.format("%d comptes clients récupérés.", clientAccounts.size()));

                    updateMessage("Récupération des données des événements...");
                    List<Evenement> evenements = databaseService.getAllEvenements();
                    updateMessage(String.format("%d événements récupérés.", evenements.size()));

                    updateMessage("Récupération des données des prestations...");
                    List<Prestation> prestations = databaseService.getAllPrestations();
                    updateMessage(String.format("%d prestations récupérées.", prestations.size()));

                    if (clientAccounts.size() < 30 || evenements.size() < 30 || prestations.size() < 30) {
                        String warningMsg = String.format(
                            "Attention: Moins de 30 enregistrements pour certaines données (Clients: %d, Evénements: %d, Prestations: %d). Le rapport pourrait être moins représentatif.",
                            clientAccounts.size(), evenements.size(), prestations.size()
                        );
                        updateMessage(warningMsg);
                        logger.warn(warningMsg);
                    }

                    updateMessage("Génération du rapport PDF...");
                    String reportFileNameWithTimestamp = REPORT_FILENAME_PREFIX + timestampFormat.format(new Date()) + ".pdf";
                    
                    if (pdfReportService == null) {
                        logger.error("PdfReportService n'a pas été initialisé !");
                        throw new IllegalStateException("PdfReportService n'a pas été initialisé.");
                    }
                    pdfReportService.generateReport(clientAccounts, evenements, prestations, reportFileNameWithTimestamp);

                    File pdfFile = new File(reportFileNameWithTimestamp);
                    return new ReportData(clientAccounts, evenements, prestations, pdfFile);

                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                            updateMessage("Connexion à la base de données fermée.");
                        } catch (SQLException ex) {
                            logger.error("Erreur lors de la fermeture de la connexion JDBC.", ex);
                        }
                    }
                }
            }
        };

        reportTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null) {
                Platform.runLater(() -> statusTextArea.appendText(newMsg + "\n"));
            }
        });

        reportTask.setOnSucceeded(e -> {
            ReportData result = reportTask.getValue();
            generatedPdfFile = result.pdfFile;

            Platform.runLater(() -> {
                 clientsTable.setItems(FXCollections.observableArrayList(result.clients));
                 eventsTable.setItems(FXCollections.observableArrayList(result.events));
                 servicesTable.setItems(FXCollections.observableArrayList(result.services));

                 statusTextArea.appendText("Données affichées dans les tableaux.\n");
                 if (generatedPdfFile != null && generatedPdfFile.exists()){
                    statusTextArea.appendText("Rapport PDF généré avec succès : " + generatedPdfFile.getAbsolutePath() + "\n");
                    openPdfLink.setVisible(true);
                 } else {
                    statusTextArea.appendText("Erreur : Le fichier PDF n'a pas été généré ou est introuvable.\n");
                    openPdfLink.setVisible(false);
                 }
                 generateButton.setDisable(false);
                 progressIndicator.setVisible(false);
            });
        });

        reportTask.setOnFailed(e -> {
             Throwable ex = reportTask.getException();
             logger.error("ERREUR majeure lors de la génération du rapport : {}", ex.getMessage(), ex);
             Platform.runLater(() -> {
                 statusTextArea.appendText("ERREUR : " + ex.getMessage() + "\nConsultez les logs pour plus de détails.\n");
                 generateButton.setDisable(false);
                 progressIndicator.setVisible(false);
                 openPdfLink.setVisible(false);
                 showAlert(AlertType.ERROR, "Erreur de Génération",
                           "Une erreur critique est survenue lors de la génération du rapport.\n" +
                           ex.getClass().getSimpleName() + ": " + ex.getMessage());
             });
        });

        new Thread(reportTask).start();
    }

    @FXML
    private void handleOpenPdf() {
        if (generatedPdfFile != null && generatedPdfFile.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(generatedPdfFile);
                    statusTextArea.appendText("Ouverture du fichier PDF : " + generatedPdfFile.getName() + "\n");
                } else {
                    String msg = "L'ouverture automatique de fichiers n'est pas supportée sur ce système.";
                    statusTextArea.appendText(msg + "\n");
                    showAlert(AlertType.WARNING, "Ouverture non supportée", msg + "\nLe fichier se trouve ici : " + generatedPdfFile.getAbsolutePath());
                }
            } catch (IOException | UnsupportedOperationException e) {
                 logger.error("Erreur lors de l'ouverture du PDF : {}", e.getMessage(), e);
                 statusTextArea.appendText("Erreur lors de l'ouverture du PDF : " + e.getMessage() + "\n");
                 showAlert(AlertType.ERROR, "Erreur d'Ouverture", "Impossible d'ouvrir le fichier PDF :\n" + e.getMessage() + "\nLe fichier se trouve ici : " + generatedPdfFile.getAbsolutePath());
            }
        } else {
             statusTextArea.appendText("Fichier PDF non trouvé ou non généré. Veuillez d'abord générer le rapport.\n");
             showAlert(AlertType.WARNING, "Fichier non trouvé", "Le rapport PDF n'a pas encore été généré ou est introuvable.");
        }
    }

    @FXML
    private void handleShowChangePasswordDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChangePasswordDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier le mot de passe");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            
            if (generateButton != null && generateButton.getScene() != null) {
                 dialogStage.initOwner(generateButton.getScene().getWindow());
            } else {
                 logger.warn("Impossible de définir le propriétaire pour la fenêtre de changement de mot de passe, la scène principale n'est pas encore chargée.");
            }

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            logger.error("Erreur lors de l'ouverture de la fenêtre de modification du mot de passe.", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de modification du mot de passe.");
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
         if (Platform.isFxApplicationThread()) {
            displayAlert(alertType, title, message);
         } else {
            Platform.runLater(() -> displayAlert(alertType, title, message));
         }
    }

    private void displayAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}