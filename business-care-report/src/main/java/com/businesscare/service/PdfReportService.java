package com.businesscare.service;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.util.ChartUtil;

public class PdfReportService {

    private final StatisticsService statisticsService;
    private float currentY;
    private PDPageContentStream contentStream;
    private PDDocument document;

    private static final float PAGE_PADDING = 50;
    private static final float CHART_VERTICAL_SPACING = 35;
    private static final float TEXT_BLOCK_HEIGHT_ESTIMATE = 100;
    private static final int DEFAULT_CHART_HEIGHT = 440;
    private static final int BAR_CHART_HEIGHT = 540;   
    private static final int DEFAULT_CHART_WIDTH = (int) (PDRectangle.A4.getWidth() - (2 * PAGE_PADDING) - 20);

    private static final Color HEADER_BG_COLOR = new Color(41, 128, 185);
    private static final Color HEADER_TEXT_COLOR = Color.WHITE;
    private static final Color SECTION_TITLE_COLOR = new Color(52, 73, 94);
    private static final Color DIVIDER_COLOR = new Color(189, 195, 199);
    private static final Color CHART_TITLE_BG = new Color(236, 240, 241);

    public PdfReportService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public void generateReport(List<ClientAccount> clients, List<Evenement> evenements, List<Prestation> prestations, String filePath) throws IOException {
        this.document = new PDDocument();
        addCoverPage("Rapport Statistique BusinessCare");
        generateClientsSection(clients);
        generateEventsSection(evenements);
        generatePrestationsSection(prestations);
        if (this.contentStream != null) {
            this.contentStream.close();
        }
        this.document.save(filePath);
        this.document.close();
    }

    private void addCoverPage(String title) throws IOException {
         PDPage coverPage = new PDPage(PDRectangle.A4);
        this.document.addPage(coverPage);
        try (PDPageContentStream cs = new PDPageContentStream(this.document, coverPage)) {
            cs.setNonStrokingColor(HEADER_BG_COLOR);
            cs.addRect(0, PDRectangle.A4.getHeight() - 150, PDRectangle.A4.getWidth(), 150);
            cs.fill();

            cs.beginText();
            cs.setNonStrokingColor(HEADER_TEXT_COLOR);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 28);
            cs.newLineAtOffset(PAGE_PADDING, PDRectangle.A4.getHeight() - 90);
            cs.showText(title);
            cs.endText();

            cs.beginText();
            cs.setNonStrokingColor(HEADER_TEXT_COLOR);
            cs.setFont(PDType1Font.HELVETICA, 14);
            cs.newLineAtOffset(PAGE_PADDING, PDRectangle.A4.getHeight() - 120);
            cs.showText("Généré le " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            cs.endText();

            cs.beginText();
            cs.setNonStrokingColor(Color.DARK_GRAY);
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLineAtOffset(PAGE_PADDING, 40);
            cs.showText("© BusinessCare - Document confidentiel");
            cs.endText();
        }
        this.contentStream = null;
    }

    private void prepareNewPage(String title) throws IOException {
        if (this.contentStream != null) {
            this.contentStream.close();
        }
        PDPage page = new PDPage(PDRectangle.A4);
        this.document.addPage(page);
        this.contentStream = new PDPageContentStream(this.document, page);

        contentStream.setNonStrokingColor(HEADER_BG_COLOR);
        contentStream.addRect(0, PDRectangle.A4.getHeight() - 80, PDRectangle.A4.getWidth(), 80);
        contentStream.fill();

        addHeader(title);
        addFooter(page.getMediaBox().getWidth() / 2, 30, this.document.getNumberOfPages() - 1);

        contentStream.setStrokingColor(DIVIDER_COLOR);
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(PAGE_PADDING, PDRectangle.A4.getHeight() - 90);
        contentStream.lineTo(PDRectangle.A4.getWidth() - PAGE_PADDING, PDRectangle.A4.getHeight() - 90);
        contentStream.stroke();

        this.currentY = PDRectangle.A4.getHeight() - PAGE_PADDING - 60;
    }

    private void addHeader(String title) throws IOException {
        this.contentStream.beginText();
        this.contentStream.setNonStrokingColor(HEADER_TEXT_COLOR);
        this.contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
        this.contentStream.newLineAtOffset(PAGE_PADDING, PDRectangle.A4.getHeight() - 55);
        this.contentStream.showText(title);
        this.contentStream.endText();
    }

    private void addFooter(float xCenter, float y, int pageNumber) throws IOException {
        this.contentStream.beginText();
        this.contentStream.setNonStrokingColor(Color.DARK_GRAY);
        this.contentStream.setFont(PDType1Font.HELVETICA, 10);
        String pageText = "Page " + pageNumber;
        float textWidth = PDType1Font.HELVETICA.getStringWidth(pageText) / 1000 * 10;
        this.contentStream.newLineAtOffset(xCenter - (textWidth / 2), y);
        this.contentStream.showText(pageText);
        this.contentStream.endText();
    }

    private void addText(List<String> lines, float x, float y, PDType1Font font, int fontSize, int leading) throws IOException {
        this.contentStream.beginText();
        this.contentStream.setNonStrokingColor(Color.BLACK);
        this.contentStream.setFont(font, fontSize);
        this.contentStream.setLeading(leading);
        this.contentStream.newLineAtOffset(x, y - fontSize);
        for (String line : lines) {
            this.contentStream.showText(line);
            this.contentStream.newLine();
        }
        this.contentStream.endText();
    }

    private boolean hasEnoughSpace(float elementHeight) {
        return (this.currentY - (elementHeight + 25) - CHART_VERTICAL_SPACING) > PAGE_PADDING;
    }

    private void drawChartTitle(String title, float yPosition) throws IOException {
        contentStream.setNonStrokingColor(CHART_TITLE_BG);
        contentStream.addRect(PAGE_PADDING, yPosition - 25, DEFAULT_CHART_WIDTH, 25);
        contentStream.fill();

        contentStream.beginText();
        contentStream.setNonStrokingColor(SECTION_TITLE_COLOR);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(PAGE_PADDING + 10, yPosition - 17);
        contentStream.showText(title);
        contentStream.endText();
    }

    private void drawChart(byte[] chartBytes, String title, float yPosition, int chartHeight) throws IOException {
        if (chartBytes == null || chartBytes.length == 0) {
             System.err.println("Avertissement: Tentative de dessin d'un graphique vide: " + title);
             this.currentY = yPosition - CHART_VERTICAL_SPACING;
             return;
        }

        float titleHeight = 25;
        float totalElementHeight = chartHeight + titleHeight;
        float chartContentY = yPosition - titleHeight;

        drawChartTitle(title, yPosition);

        contentStream.setStrokingColor(DIVIDER_COLOR);
        contentStream.setLineWidth(1.0f);
        contentStream.addRect(PAGE_PADDING, yPosition - totalElementHeight, DEFAULT_CHART_WIDTH, totalElementHeight);
        contentStream.stroke();

        PDImageXObject chartImage = PDImageXObject.createFromByteArray(this.document, chartBytes, title);
        float imageMargin = 5;
        float imageY = chartContentY - imageMargin - (chartHeight - (2*imageMargin));
        float imageX = PAGE_PADDING + imageMargin;
        float imageWidth = DEFAULT_CHART_WIDTH - (2*imageMargin);
        float imageHeight = chartHeight - (2*imageMargin);

        this.contentStream.drawImage(chartImage, imageX, imageY, imageWidth, imageHeight);

        this.currentY = yPosition - totalElementHeight - CHART_VERTICAL_SPACING;
    }

    private void drawTextBlock(List<String> textLines, String title, float yPosition, int fontSize, int leading) throws IOException {
        float titleHeight = 25;
        float textContentHeight = (textLines.size() * leading);
        float textPadding = 10;
        float totalElementHeight = textContentHeight + titleHeight + textPadding;

        drawChartTitle(title, yPosition);

        contentStream.setStrokingColor(DIVIDER_COLOR);
        contentStream.setLineWidth(1.0f); 
        contentStream.addRect(PAGE_PADDING, yPosition - totalElementHeight, DEFAULT_CHART_WIDTH, totalElementHeight);
        contentStream.stroke();

        addText(textLines, PAGE_PADDING + 10, yPosition - titleHeight - (textPadding / 2), PDType1Font.HELVETICA, fontSize, leading);
        this.currentY = yPosition - totalElementHeight - CHART_VERTICAL_SPACING;
    }

    private void generateClientsSection(List<ClientAccount> clients) throws IOException {
        prepareNewPage("Statistiques des Comptes Clients (1)");
        int pageNum = 1;
        String baseTitle = "Statistiques des Comptes Clients";

        Map<String, Long> repartitionType = statisticsService.getClientRepartitionParType(clients);
        if (!repartitionType.isEmpty()) {
            String chartTitle = "Répartition par Type de Client";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                repartitionType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        Map<String, Double> repartitionCA = statisticsService.getClientRepartitionParCA(clients);
        Map<String, List<Number>> barChartCAMap = repartitionCA.entrySet().stream()
            .collect(Collectors.toMap(
                e -> "CA",
                e -> new ArrayList<>(Collections.singletonList(e.getValue())),
                (oldList, newList) -> { oldList.addAll(newList); return oldList; },
                java.util.LinkedHashMap::new
            ));
        List<String> caCategories = new ArrayList<>(repartitionCA.keySet());
        if (!caCategories.isEmpty() && barChartCAMap.containsKey("CA") && !barChartCAMap.get("CA").isEmpty()) {
            String chartTitle = "Répartition par CA (Top 5)";
            if (!hasEnoughSpace(BAR_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createBarChartImage(chartTitle,
                "Clients", "Chiffre d'Affaires (€)",
                barChartCAMap, caCategories, DEFAULT_CHART_WIDTH, BAR_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, BAR_CHART_HEIGHT);
        }

        Map<String, Long> repartitionVille = statisticsService.getClientRepartitionParVille(clients);
        if (!repartitionVille.isEmpty()) {
            String chartTitle = "Répartition Géographique (Ville)";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                repartitionVille.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        List<ClientAccount> top5Fideles = statisticsService.getTop5ClientsFideles(clients);
        List<String> topClientsText = new ArrayList<>();
        String blockTitle = "Top 5 Clients Fidèles";
        List<String> clientDetails = top5Fideles.stream()
            .map(c -> "• " + c.getNomSociete() + " (Abo: " + c.getAbonnements().size() + ", CA: " + String.format("%,.0f €", c.getChiffreAffairesAnnuel()) + ")")
            .collect(Collectors.toList());
        if (clientDetails.isEmpty()) {
            topClientsText.add("(Aucune donnée disponible)");
        } else {
            topClientsText.addAll(clientDetails);
        }
        if (!hasEnoughSpace(TEXT_BLOCK_HEIGHT_ESTIMATE)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
        drawTextBlock(topClientsText, blockTitle, this.currentY, 9, 14);
   }

    // event section 
    private void generateEventsSection(List<Evenement> evenements) throws IOException {
        prepareNewPage("Statistiques des Évènements (1)");
        int pageNum = 1;
        String baseTitle = "Statistiques des Évènements";

        Map<String, Long> repartitionType = statisticsService.getEvenementRepartitionParType(evenements);
        if (!repartitionType.isEmpty()) {
             String chartTitle = "Répartition par Type d'Évènement";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                repartitionType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        Map<String, Integer> frequentation = statisticsService.getFrequentationEvenements(evenements);
        if(!frequentation.isEmpty()){
            String chartTitle = "Participants par Évènement (Top 5)";
             List<String> eventCategories = new ArrayList<>(frequentation.keySet());
             List<String> truncatedEventCategories = eventCategories.stream()
                .map(s -> s.length() > 35 ? s.substring(0, 34) + "..." : s)
                .collect(Collectors.toList());

            if (!hasEnoughSpace(BAR_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createBarChartImage(chartTitle,
                "Évènements", "Nbr. Participants",
                Map.of("Participants", new ArrayList<>(frequentation.values())),
                truncatedEventCategories,
                DEFAULT_CHART_WIDTH, BAR_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, BAR_CHART_HEIGHT);
        }

        Map<String, Long> repartitionLieu = statisticsService.getEvenementRepartitionParLieu(evenements);
        if (!repartitionLieu.isEmpty()) {
            String chartTitle = "Répartition par Lieu";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                repartitionLieu.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        List<Evenement> top5Demandes = statisticsService.getTop5EvenementsDemandes(evenements);
        List<String> topEventsText = new ArrayList<>();
        String blockTitle = "Top 5 Évènements Demandés";
        List<String> eventDetails = top5Demandes.stream()
            .map(e -> "• " + e.getNomEvenement() + " (Rés: " + e.getReservations().size() + ")")
            .collect(Collectors.toList());
        if (eventDetails.isEmpty()) {
            topEventsText.add("(Aucune donnée disponible)");
        } else {
            topEventsText.addAll(eventDetails);
        }
        if (!hasEnoughSpace(TEXT_BLOCK_HEIGHT_ESTIMATE)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
        drawTextBlock(topEventsText, blockTitle, this.currentY, 9, 14);
   }

    // prestation
    private void generatePrestationsSection(List<Prestation> prestations) throws IOException {
        prepareNewPage("Statistiques des Prestations (1)");
        int pageNum = 1;
        String baseTitle = "Statistiques des Prestations";

        Map<String, Long> repartitionType = statisticsService.getPrestationRepartitionParType(prestations);
        if (!repartitionType.isEmpty()) {
             String chartTitle = "Répartition par Type de Prestation";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                repartitionType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        Map<String, Double> repartitionCout = statisticsService.getPrestationRepartitionParCout(prestations);
        if(!repartitionCout.isEmpty()){
            String chartTitle = "Coût Total par Type (Top 5)";
            if (!hasEnoughSpace(BAR_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createBarChartImage(chartTitle,
                "Type Prestation", "Coût Total (€)",
                Map.of("Coût", new ArrayList<>(repartitionCout.values())),
                new ArrayList<>(repartitionCout.keySet()),
                DEFAULT_CHART_WIDTH, BAR_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, BAR_CHART_HEIGHT);
        }

        Map<String, Long> dispo = statisticsService.getPrestationDisponibilite(prestations);
        if (!dispo.isEmpty()) {
             String chartTitle = "Disponibilité des Prestations";
            if (!hasEnoughSpace(DEFAULT_CHART_HEIGHT)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
            byte[] chartBytes = ChartUtil.createPieChartImage(chartTitle,
                dispo.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())),
                DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT);
            drawChart(chartBytes, chartTitle, this.currentY, DEFAULT_CHART_HEIGHT);
        }

        List<Prestation> top5Frequentes = statisticsService.getTop5PrestationsFrequentes(prestations);
        List<String> topPrestationsText = new ArrayList<>();
        String blockTitle = "Top 5 Prestations Fréquentes";
        List<String> prestationDetails = top5Frequentes.stream()
            .map(p -> "• " + p.getNomPrestation() + " (Util: " + p.getIdEvenementsAssocies().size() + ")")
            .collect(Collectors.toList());
        if (prestationDetails.isEmpty()) {
            topPrestationsText.add("(Aucune donnée disponible)");
        } else {
            topPrestationsText.addAll(prestationDetails);
        }
        if (!hasEnoughSpace(TEXT_BLOCK_HEIGHT_ESTIMATE)) { pageNum++; prepareNewPage(baseTitle + " (" + pageNum + ")"); }
        drawTextBlock(topPrestationsText, blockTitle, this.currentY, 9, 14);
    }
}