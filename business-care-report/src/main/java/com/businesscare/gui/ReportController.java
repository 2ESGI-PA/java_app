package com.businesscare.gui;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.beans.property.SimpleStringProperty;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.service.DataGeneratorService;
import com.businesscare.service.PdfReportService;
import com.businesscare.service.StatisticsService;


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

    @FXML private Button generateButton;
    @FXML private TextArea statusTextArea;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Hyperlink openPdfLink;
    @FXML private TabPane tabPane;
    @FXML private TableView<ClientAccount> clientsTable;
    @FXML private TableColumn<ClientAccount, String> clientNameCol;
    @FXML private TableColumn<ClientAccount, String> clientTypeCol;
    @FXML private TableColumn<ClientAccount, String> clientCityCol;
    @FXML private TableColumn<ClientAccount, Double> clientCaCol;
    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, String> eventNameCol;
    @FXML private TableColumn<Evenement, String> eventTypeCol;
    @FXML private TableColumn<Evenement, String> eventLocationCol;
    @FXML private TableColumn<Evenement, String> eventStartDateCol;
    @FXML private TableView<Prestation> servicesTable;
    @FXML private TableColumn<Prestation, String> serviceNameCol;
    @FXML private TableColumn<Prestation, String> serviceTypeCol;
    @FXML private TableColumn<Prestation, Double> serviceCostCol;
    @FXML private TableColumn<Prestation, Boolean> serviceAvailabilityCol;

    private final DataGeneratorService dataGenerator;
    private final StatisticsService statisticsService;
    private final PdfReportService pdfReportService;

    private File generatedPdfFile = null;
    private static final String REPORT_FILENAME = "Rapport_Activite_Business_Care.pdf";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ReportController() {
        dataGenerator = new DataGeneratorService();
        statisticsService = new StatisticsService();
        pdfReportService = new PdfReportService(statisticsService);
    }

    @FXML
    private void initialize() {
        statusTextArea.appendText("Prêt.\n");
        openPdfLink.setVisible(false);
        progressIndicator.setVisible(false);

        clientNameCol.setCellValueFactory(new PropertyValueFactory<>("nomSociete"));
        clientTypeCol.setCellValueFactory(new PropertyValueFactory<>("typeClient"));
        clientCityCol.setCellValueFactory(new PropertyValueFactory<>("ville"));
        clientCaCol.setCellValueFactory(new PropertyValueFactory<>("chiffreAffairesAnnuel"));

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
        serviceAvailabilityCol.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));
    }

    @FXML
    private void handleGenerateReport() {
        generateButton.setDisable(true);
        statusTextArea.clear();
        statusTextArea.appendText("Lancement de la génération...\n");
        progressIndicator.setVisible(true);
        openPdfLink.setVisible(false);
        generatedPdfFile = null;

        clientsTable.getItems().clear();
        eventsTable.getItems().clear();
        servicesTable.getItems().clear();

        Task<ReportData> reportTask = new Task<ReportData>() {
            @Override
            protected ReportData call() throws Exception {
                updateMessage("Génération des données aléatoires...");
                List<ClientAccount> clientAccounts = dataGenerator.generateClientAccounts(35);
                List<Evenement> evenements = dataGenerator.generateEvenements(40, clientAccounts);
                List<Prestation> prestations = dataGenerator.generatePrestations(30, evenements);
                updateMessage(String.format("%d clients, %d événements, %d prestations générés.",
                                            clientAccounts.size(), evenements.size(), prestations.size()));

                updateMessage("Génération du rapport PDF...");
                pdfReportService.generateReport(clientAccounts, evenements, prestations, REPORT_FILENAME);

                File pdfFile = new File(REPORT_FILENAME);
                return new ReportData(clientAccounts, evenements, prestations, pdfFile);
            }
        };

        reportTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
             Platform.runLater(() -> statusTextArea.appendText(newMsg + "\n"));
        });

        reportTask.setOnSucceeded(e -> {
            ReportData result = reportTask.getValue();
            generatedPdfFile = result.pdfFile;

            Platform.runLater(() -> {
                 clientsTable.setItems(FXCollections.observableArrayList(result.clients));
                 eventsTable.setItems(FXCollections.observableArrayList(result.events));
                 servicesTable.setItems(FXCollections.observableArrayList(result.services));

                 statusTextArea.appendText("Données affichées.\nRapport PDF généré avec succès : " + generatedPdfFile.getName() + "\n");
                 generateButton.setDisable(false);
                 progressIndicator.setVisible(false);
                 openPdfLink.setVisible(true);
            });
        });

        reportTask.setOnFailed(e -> {
             Throwable ex = reportTask.getException();
             Platform.runLater(() -> {
                 statusTextArea.appendText("ERREUR lors de la génération : " + ex.getMessage() + "\n");
                 generateButton.setDisable(false);
                 progressIndicator.setVisible(false);
                 openPdfLink.setVisible(false);
                 showAlert(AlertType.ERROR, "Erreur de Génération",
                           "Une erreur est survenue lors de la création du rapport.\n" +
                           ex.getClass().getSimpleName() + ": " + ex.getMessage());
             });
             ex.printStackTrace();
        });

        new Thread(reportTask).start();
    }

    @FXML
    private void handleOpenPdf() {
        if (generatedPdfFile != null && generatedPdfFile.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(generatedPdfFile);
                    statusTextArea.appendText("Ouverture du fichier PDF...\n");
                } else {
                    statusTextArea.appendText("L'ouverture automatique de fichiers n'est pas supportée sur ce système.\n");
                    showAlert(AlertType.WARNING, "Ouverture non supportée", "Impossible d'ouvrir le fichier automatiquement.");
                }
            } catch (IOException | UnsupportedOperationException e) {
                statusTextArea.appendText("Erreur lors de l'ouverture du PDF : " + e.getMessage() + "\n");
                 showAlert(AlertType.ERROR, "Erreur d'Ouverture", "Impossible d'ouvrir le fichier PDF:\n" + e.getMessage());
            }
        } else {
             statusTextArea.appendText("Le fichier PDF n'a pas été trouvé ou n'a pas encore été généré.\n");
             showAlert(AlertType.WARNING, "Fichier non trouvé", "Générez d'abord le rapport.");
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
         Platform.runLater(() -> {
             Alert alert = new Alert(alertType);
             alert.setTitle(title);
             alert.setHeaderText(null);
             alert.setContentText(message);
             alert.showAndWait();
         });
    }
}