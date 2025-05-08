package com.businesscare;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.service.DataGeneratorService;
import com.businesscare.service.PdfReportService;
import com.businesscare.service.StatisticsService;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        DataGeneratorService dataGenerator = new DataGeneratorService();
        StatisticsService statisticsService = new StatisticsService();
        PdfReportService pdfReportService = new PdfReportService(statisticsService);

        logger.info("Génération des données aléatoires...");
        List<ClientAccount> clientAccounts = dataGenerator.generateClientAccounts(35);
        List<Evenement> evenements = dataGenerator.generateEvenements(40, clientAccounts);
        List<Prestation> prestations = dataGenerator.generatePrestations(30, evenements);

        logger.info("{} comptes clients générés.", clientAccounts.size());
        logger.info("{} évènements générés.", evenements.size());
        logger.info("{} prestations générées.", prestations.size());

        try {
            logger.info("Génération du rapport PDF...");
            pdfReportService.generateReport(clientAccounts, evenements, prestations, "Rapport_Activite_Business_Care.pdf");
            logger.info("Rapport PDF généré avec succès: Rapport_Activite_Business_Care.pdf");
        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport PDF:", e);
        }
    }
}