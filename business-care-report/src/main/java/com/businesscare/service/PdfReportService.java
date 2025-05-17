package com.businesscare.service;

import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.util.ChartUtil;

public class PdfReportService {
    private static final Logger logger = LoggerFactory.getLogger(PdfReportService.class);

    private final StatisticsService statisticsService;
    private final DatabaseService databaseService;
    private float currentY;
    private PDPageContentStream contentStream;
    private PDDocument document;
    private PDPage currentPage;

    private static final float PAGE_MARGIN_TOP = 90;
    private static final float PAGE_MARGIN_BOTTOM = 50;
    private static final float PAGE_MARGIN_SIDES = 40;
    private static final float CONTENT_START_Y_OFFSET = 100;
    private static final float ELEMENT_VERTICAL_SPACING = 25;
    private static final float ELEMENT_HORIZONTAL_SPACING = 20;
    private static final float CHART_TITLE_HEIGHT = 25;
    private static final float TEXT_BLOCK_LINE_HEIGHT = 14;
    private static final int TEXT_BLOCK_FONT_SIZE = 9;
    private static final PDType1Font FONT_HELVETICA = PDType1Font.HELVETICA;
    private static final PDType1Font FONT_HELVETICA_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final float USABLE_PAGE_WIDTH = PDRectangle.A4.getWidth() - (2 * PAGE_MARGIN_SIDES);
    private static final float ELEMENT_WIDTH_HALF = (USABLE_PAGE_WIDTH - ELEMENT_HORIZONTAL_SPACING) / 2;
    private static final int DEFAULT_CHART_HEIGHT_HALF_PAGE = 200;
    private static final int BAR_CHART_HEIGHT_HALF_PAGE = 280;
    private static final int LINE_CHART_HEIGHT_FULL_PAGE = 300;


    private static final Color COLOR_PRIMARY_HEADER_BG = new Color(41, 128, 185);
    private static final Color COLOR_PRIMARY_HEADER_TEXT = Color.WHITE;
    private static final Color COLOR_SECTION_TITLE_TEXT = new Color(52, 73, 94);
    private static final Color COLOR_ELEMENT_TITLE_BG = new Color(236, 240, 241);
    private static final Color COLOR_DIVIDER_LINE = new Color(189, 195, 199);
    private static final Color COLOR_FOOTER_TEXT = Color.DARK_GRAY;
    private static final Color COLOR_BODY_TEXT = Color.BLACK;

    private int elementsOnCurrentRow = 0;
    private float lastElementHeightOnRow = 0;

    private static final DecimalFormat euroFormatPdf = new DecimalFormat("#,##0.00 €");
    private static final DecimalFormat kEuroFormatPdf = new DecimalFormat("#,##0 k€");
    private static final DecimalFormat mEuroFormatPdf = new DecimalFormat("#,##0.00 M€");

    public PdfReportService(StatisticsService statisticsService, DatabaseService databaseService) {
        this.statisticsService = statisticsService;
        this.databaseService = databaseService;
        statisticsService_setDb(this.databaseService);
    }

    private String formatCurrency(double value) {
        if (value >= 1_000_000) {
            return mEuroFormatPdf.format(value / 1_000_000.0);
        } else if (value >= 1_000) {
            return kEuroFormatPdf.format(value / 1_000.0);
        } else {
            return euroFormatPdf.format(value);
        }
    }

    public void generateReport(List<ClientAccount> clients, List<Evenement> evenements, List<Prestation> prestations, String filePath) throws IOException {
        this.document = new PDDocument();
        this.statisticsService_setDb(this.databaseService);

        addCoverPage("Rapport d'Activité Stratégique", "BusinessCare");

        generateClientStatisticsPage();
        generateEventStatisticsPage(evenements);
        generatePrestationStatisticsPage(prestations);


        if (this.contentStream != null) {
            this.contentStream.close();
        }
        this.document.save(filePath);
        this.document.close();
        logger.info("Rapport PDF généré avec succès : {}", filePath);
    }

    private void statisticsService_setDb(DatabaseService dbService){
        if(this.statisticsService != null && dbService != null){
            this.statisticsService.setDatabaseService(dbService);
        } else {
            if (this.statisticsService == null) {
                logger.error("StatisticsService est null dans PdfReportService.statisticsService_setDb");
            }
            if (dbService == null) {
                 logger.error("DatabaseService (dbService) est null dans PdfReportService.statisticsService_setDb");
            }
        }
    }


    private void addCoverPage(String title, String subtitle) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        this.document.addPage(coverPage);
        try (PDPageContentStream cs = new PDPageContentStream(this.document, coverPage)) {
            cs.setNonStrokingColor(COLOR_PRIMARY_HEADER_BG);
            cs.addRect(0, PDRectangle.A4.getHeight() - 180, PDRectangle.A4.getWidth(), 180);
            cs.fill();

            cs.beginText();
            cs.setNonStrokingColor(COLOR_PRIMARY_HEADER_TEXT);
            cs.setFont(FONT_HELVETICA_BOLD, 32);
            float titleWidth = FONT_HELVETICA_BOLD.getStringWidth(title) / 1000 * 32;
            cs.newLineAtOffset((PDRectangle.A4.getWidth() - titleWidth) / 2, PDRectangle.A4.getHeight() - 100);
            cs.showText(title);
            cs.endText();

            if (subtitle != null && !subtitle.isEmpty()) {
                cs.beginText();
                cs.setFont(FONT_HELVETICA, 18);
                float subtitleWidth = FONT_HELVETICA.getStringWidth(subtitle) / 1000 * 18;
                cs.newLineAtOffset((PDRectangle.A4.getWidth() - subtitleWidth) / 2, PDRectangle.A4.getHeight() - 130);
                cs.showText(subtitle);
                cs.endText();
            }

            String dateGen = "Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM uuuu"));
            cs.beginText();
            cs.setNonStrokingColor(COLOR_SECTION_TITLE_TEXT);
            cs.setFont(FONT_HELVETICA, 12);
            float dateWidth = FONT_HELVETICA.getStringWidth(dateGen) / 1000 * 12;
            cs.newLineAtOffset((PDRectangle.A4.getWidth() - dateWidth) / 2, PDRectangle.A4.getHeight() / 2);
            cs.showText(dateGen);
            cs.endText();

            String footerText = "© " + LocalDate.now().getYear() + " BusinessCare - Document confidentiel";
            cs.beginText();
            cs.setNonStrokingColor(COLOR_FOOTER_TEXT);
            cs.setFont(FONT_HELVETICA, 9);
            float footerWidth = FONT_HELVETICA.getStringWidth(footerText) / 1000 * 9;
            cs.newLineAtOffset((PDRectangle.A4.getWidth() - footerWidth) / 2, PAGE_MARGIN_BOTTOM - 20);
            cs.showText(footerText);
            cs.endText();
        }
        this.contentStream = null;
    }


    private void prepareNewPage(String sectionTitle) throws IOException {
        if (this.contentStream != null) {
            this.contentStream.close();
        }
        currentPage = new PDPage(PDRectangle.A4);
        this.document.addPage(currentPage);
        this.contentStream = new PDPageContentStream(this.document, currentPage);

        contentStream.setNonStrokingColor(COLOR_PRIMARY_HEADER_BG);
        contentStream.addRect(0, PDRectangle.A4.getHeight() - PAGE_MARGIN_TOP + 20, PDRectangle.A4.getWidth(), PAGE_MARGIN_TOP -20 );
        contentStream.fill();

        contentStream.beginText();
        contentStream.setNonStrokingColor(COLOR_PRIMARY_HEADER_TEXT);
        contentStream.setFont(FONT_HELVETICA_BOLD, 18);
        contentStream.newLineAtOffset(PAGE_MARGIN_SIDES, PDRectangle.A4.getHeight() - (PAGE_MARGIN_TOP / 2) - 5 );
        contentStream.showText(sectionTitle + " (Page " + (this.document.getNumberOfPages() -1 ) + ")");
        contentStream.endText();

        contentStream.setStrokingColor(COLOR_DIVIDER_LINE);
        contentStream.setLineWidth(1f);
        contentStream.moveTo(PAGE_MARGIN_SIDES, PDRectangle.A4.getHeight() - PAGE_MARGIN_TOP);
        contentStream.lineTo(PDRectangle.A4.getWidth() - PAGE_MARGIN_SIDES, PDRectangle.A4.getHeight() - PAGE_MARGIN_TOP);
        contentStream.stroke();

        this.currentY = PDRectangle.A4.getHeight() - CONTENT_START_Y_OFFSET;
        addPageFooter();

        elementsOnCurrentRow = 0;
        lastElementHeightOnRow = 0;
    }

    private void addPageFooter() throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(COLOR_FOOTER_TEXT);
        contentStream.setFont(FONT_HELVETICA, 9);
        String pageText = "Page " + (this.document.getNumberOfPages() - 1) ;
        float textWidth = FONT_HELVETICA.getStringWidth(pageText) / 1000 * 9;
        contentStream.newLineAtOffset((currentPage.getMediaBox().getWidth() - textWidth) / 2, PAGE_MARGIN_BOTTOM - 20);
        contentStream.showText(pageText);
        contentStream.endText();
    }

    private float getCurrentX() {
        if (elementsOnCurrentRow % 2 == 0) {
            return PAGE_MARGIN_SIDES;
        } else {
            return PAGE_MARGIN_SIDES + ELEMENT_WIDTH_HALF + ELEMENT_HORIZONTAL_SPACING;
        }
    }

    private boolean hasEnoughSpace(float elementHeight, String sectionTitle) throws IOException {
        float requiredSpace = elementHeight + CHART_TITLE_HEIGHT;
         if (elementsOnCurrentRow % 2 == 0) {
            if (currentY - requiredSpace < PAGE_MARGIN_BOTTOM + 30) {
                prepareNewPage(sectionTitle);
                return true;
            }
        } else {
            if (currentY - Math.max(lastElementHeightOnRow, requiredSpace) < PAGE_MARGIN_BOTTOM + 30) {
                 prepareNewPage(sectionTitle);
                 return true;
            }
        }
        return true;
    }

    private void advancePosition(float elementHeight) {
        if (elementsOnCurrentRow % 2 == 0) {
            lastElementHeightOnRow = elementHeight + CHART_TITLE_HEIGHT;
        } else {
            currentY -= Math.max(lastElementHeightOnRow, elementHeight + CHART_TITLE_HEIGHT) + ELEMENT_VERTICAL_SPACING;
            lastElementHeightOnRow = 0;
        }
        elementsOnCurrentRow++;
    }

    private void drawChartOrTextBlock(String title, byte[] chartBytes, List<String> textLines, String sectionTitle, boolean useFullWidth, float chartHeightOverride) throws IOException {
        float elementWidth = useFullWidth ? USABLE_PAGE_WIDTH : ELEMENT_WIDTH_HALF;
        int defaultChartHeight = useFullWidth ? DEFAULT_CHART_HEIGHT_HALF_PAGE * 2 : DEFAULT_CHART_HEIGHT_HALF_PAGE;

        if (title.toLowerCase().contains("par ca") || title.toLowerCase().contains("secteur d'activité") || title.toLowerCase().contains("par capacité") || title.toLowerCase().contains("par coût") || title.toLowerCase().contains("top 5 prestataires")) {
             defaultChartHeight = useFullWidth ? BAR_CHART_HEIGHT_HALF_PAGE * 2 : BAR_CHART_HEIGHT_HALF_PAGE;
        }
        if (title.toLowerCase().contains("par mois")) {
            defaultChartHeight = LINE_CHART_HEIGHT_FULL_PAGE;
        }


        float actualElementHeight = chartBytes != null ? (chartHeightOverride > 0 ? chartHeightOverride : defaultChartHeight) : (textLines.size() * TEXT_BLOCK_LINE_HEIGHT) + 20;

        if (!hasEnoughSpace(actualElementHeight, sectionTitle)) {
        }

        float currentX = useFullWidth ? PAGE_MARGIN_SIDES : getCurrentX();
        float yPos = currentY;

        contentStream.setNonStrokingColor(COLOR_ELEMENT_TITLE_BG);
        contentStream.addRect(currentX, yPos - CHART_TITLE_HEIGHT, elementWidth, CHART_TITLE_HEIGHT);
        contentStream.fill();

        contentStream.beginText();
        contentStream.setNonStrokingColor(COLOR_SECTION_TITLE_TEXT);
        contentStream.setFont(FONT_HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(currentX + 5, yPos - (CHART_TITLE_HEIGHT / 2) - 4);
        contentStream.showText(title);
        contentStream.endText();

        contentStream.setStrokingColor(COLOR_DIVIDER_LINE);
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(currentX, yPos - CHART_TITLE_HEIGHT - actualElementHeight, elementWidth, CHART_TITLE_HEIGHT + actualElementHeight);
        contentStream.stroke();

        if (chartBytes != null) {
            if (chartBytes.length == 0) {
                logger.warn("Tentative de dessin d'un graphique vide (0 bytes) pour : {}", title);
                List<String> errorText = List.of("(Données non disponibles", " pour ce graphique)");
                drawTextInsideBox(errorText, currentX, yPos - CHART_TITLE_HEIGHT, elementWidth, actualElementHeight);
            } else {
                try {
                    PDImageXObject chartImage = PDImageXObject.createFromByteArray(this.document, chartBytes, title);
                    float imgMargin = 5;
                    contentStream.drawImage(chartImage, currentX + imgMargin, yPos - CHART_TITLE_HEIGHT - actualElementHeight + imgMargin, elementWidth - (2*imgMargin), actualElementHeight - (2*imgMargin));
                } catch (IOException e) {
                    logger.error("Impossible de créer PDImageXObject pour le graphique : {}. Bytes length: {}", title, chartBytes.length, e);
                     List<String> errorText = List.of("(Erreur chargement image", " pour ce graphique)");
                     drawTextInsideBox(errorText, currentX, yPos - CHART_TITLE_HEIGHT, elementWidth, actualElementHeight);
                }
            }
        } else if (textLines != null) {
            drawTextInsideBox(textLines, currentX, yPos - CHART_TITLE_HEIGHT, elementWidth, actualElementHeight);
        }

        if (useFullWidth) {
            currentY -= (actualElementHeight + CHART_TITLE_HEIGHT + ELEMENT_VERTICAL_SPACING);
            elementsOnCurrentRow = 0;
            lastElementHeightOnRow = 0;
        } else {
            advancePosition(actualElementHeight);
        }
    }

    private void drawTextInsideBox(List<String> lines, float boxX, float boxY, float boxWidth, float boxHeight) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(COLOR_BODY_TEXT);
        contentStream.setFont(FONT_HELVETICA, TEXT_BLOCK_FONT_SIZE);
        contentStream.setLeading(TEXT_BLOCK_LINE_HEIGHT);

        float textStartY = boxY - TEXT_BLOCK_LINE_HEIGHT + (TEXT_BLOCK_LINE_HEIGHT - TEXT_BLOCK_FONT_SIZE)/2 ;
        float textMargin = 10;
        contentStream.newLineAtOffset(boxX + textMargin, textStartY);

        for (String line : lines) {
            String remainingLine = line;

            while (!remainingLine.isEmpty()) {
                int breakPoint = findBreakPoint(remainingLine, boxWidth - (2 * textMargin), FONT_HELVETICA, TEXT_BLOCK_FONT_SIZE);
                String lineToShow = remainingLine.substring(0, breakPoint);
                remainingLine = remainingLine.substring(breakPoint).trim();

                if (textStartY < boxY - boxHeight + TEXT_BLOCK_LINE_HEIGHT) {
                    if (!remainingLine.isEmpty() || lines.indexOf(line) < lines.size() -1 ) {
                        contentStream.showText("...");
                    }
                    contentStream.endText();
                    return;
                }

                contentStream.showText(lineToShow);
                if (!remainingLine.isEmpty()) {
                    contentStream.newLineAtOffset(0, -TEXT_BLOCK_LINE_HEIGHT);
                    textStartY -= TEXT_BLOCK_LINE_HEIGHT;
                }
            }
            if (lines.indexOf(line) < lines.size() -1 && textStartY >= boxY - boxHeight + TEXT_BLOCK_LINE_HEIGHT *2) {
                 contentStream.newLineAtOffset(0, -TEXT_BLOCK_LINE_HEIGHT);
                 textStartY -= TEXT_BLOCK_LINE_HEIGHT;
            } else if (lines.indexOf(line) == lines.size() -1) {
            } else {
                break;
            }
        }
        contentStream.endText();
    }

    private int findBreakPoint(String text, float maxWidth, PDType1Font font, int fontSize) throws IOException {
        float currentWidth = 0;
        int lastSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            currentWidth += font.getStringWidth(String.valueOf(c)) / 1000 * fontSize;
            if (Character.isWhitespace(c)) {
                lastSpace = i;
            }
            if (currentWidth > maxWidth) {
                return (lastSpace != -1 && lastSpace > 0) ? lastSpace : i;
            }
        }
        return text.length();
    }

    private void generateClientStatisticsPage() throws IOException {
        String sectionTitle = "Statistiques des Comptes Clients";
        prepareNewPage(sectionTitle);
        int chartWidth = (int) ELEMENT_WIDTH_HALF;
        int chartHeight = DEFAULT_CHART_HEIGHT_HALF_PAGE;
        int barChartHeight = BAR_CHART_HEIGHT_HALF_PAGE;

        Map<String, Long> repartitionAbonnement = statisticsService.getClientCountBySubscriptionTier();
        List<String> abonnementCategories = new ArrayList<>(repartitionAbonnement.keySet());
        List<Number> abonnementValues = new ArrayList<>(repartitionAbonnement.values());
        Map<String, List<Number>> abonnementMap = Collections.singletonMap("Clients", abonnementValues);
        drawChartOrTextBlock("Répartition par Formule d'Abonnement", ChartUtil.createBarChartImage("Répartition par Formule d'Abonnement", "Formule", "Nombre de Clients", abonnementMap, abonnementCategories, chartWidth, barChartHeight, true), null, sectionTitle, false, 0);

        double[] caTranches = {5000, 15000, 50000};
        Map<String, Double> repartitionCA = statisticsService.getClientRevenueDistribution(caTranches);
        Map<String, Number> repartitionCAPie = repartitionCA.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        drawChartOrTextBlock("Répartition du CA Actif par Client (Tranches)", ChartUtil.createPieChartImage("Répartition du CA Actif par Client (Tranches)", repartitionCAPie, chartWidth, chartHeight), null, sectionTitle, false, 0);

        Map<String, Long> repartitionTaille = statisticsService.getClientCountBySize();
        Map<String, Number> repartitionTaillePie = repartitionTaille.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        drawChartOrTextBlock("Répartition par Taille d'Entreprise", ChartUtil.createPieChartImage("Répartition par Taille d'Entreprise", repartitionTaillePie, chartWidth, chartHeight), null, sectionTitle, false, 0);

        Map<String, Long> repartitionSecteur = statisticsService.getClientCountByIndustry(5);
        List<String> secteurCategories = new ArrayList<>(repartitionSecteur.keySet());
        List<Number> secteurValues = new ArrayList<>(repartitionSecteur.values());
        Map<String, List<Number>> secteurMap = Collections.singletonMap("Clients", secteurValues);
        drawChartOrTextBlock("Répartition par Secteur d'Activité (Top 5)", ChartUtil.createBarChartImage("Répartition par Secteur d'Activité (Top 5)", "Secteur", "Nombre de Clients", secteurMap, secteurCategories, chartWidth, barChartHeight, false), null, sectionTitle, false, 0);

        Map<String, Double> top5Clients = statisticsService.getTop5ClientsByTotalPaid();
        List<String> topClientsText = new ArrayList<>();
        topClientsText.add("Basé sur le Montant Total Facturé et Payé:");
        if (top5Clients.isEmpty()) {
            topClientsText.add("(Aucune donnée disponible)");
        } else {
            top5Clients.forEach((name, amount) -> topClientsText.add(String.format("• %s - %s", name, formatCurrency(amount))));
        }
        if (elementsOnCurrentRow % 2 != 0) {
            currentY -= Math.max(lastElementHeightOnRow, 0) + ELEMENT_VERTICAL_SPACING;
            elementsOnCurrentRow = 0; lastElementHeightOnRow = 0;
        }
        drawChartOrTextBlock("Top 5 Clients les Plus Fidèles", null, topClientsText, sectionTitle, true, 0);
    }

    private void generateEventStatisticsPage(List<Evenement> evenements) throws IOException {
        String sectionTitle = "Statistiques des Événements";
        prepareNewPage(sectionTitle);
        int chartWidth = (int) ELEMENT_WIDTH_HALF;
        int chartHeight = DEFAULT_CHART_HEIGHT_HALF_PAGE;
        int barChartHeight = BAR_CHART_HEIGHT_HALF_PAGE;
        int lineChartHeight = LINE_CHART_HEIGHT_FULL_PAGE;

        Map<String, Long> repartitionType = statisticsService.getEventCountByType(evenements);
        Map<String, Number> repartitionTypePie = repartitionType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        drawChartOrTextBlock("Répartition par Type/Catégorie", ChartUtil.createPieChartImage("Répartition par Type/Catégorie", repartitionTypePie, chartWidth, chartHeight), null, sectionTitle, false, 0);

        Map<String, Long> freqMois = statisticsService.getEventCountByMonth();
        Map<String, Map<String, Number>> freqMoisLine = Collections.singletonMap("Événements", freqMois.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Number)e.getValue())));
        if (elementsOnCurrentRow % 2 != 0) {
            currentY -= Math.max(lastElementHeightOnRow, 0) + ELEMENT_VERTICAL_SPACING;
            elementsOnCurrentRow = 0; lastElementHeightOnRow = 0;
        }
        drawChartOrTextBlock("Fréquence par Mois", ChartUtil.createLineChartImage("Fréquence par Mois", "Mois", "Nombre d'Événements", freqMoisLine, (int)USABLE_PAGE_WIDTH, lineChartHeight), null, sectionTitle, true, lineChartHeight);

        double[] capaciteTranches = {50, 100, 200};
        Map<String, Long> repartitionCapacite = statisticsService.getEventDistributionByCapacity(capaciteTranches);
        List<String> capaciteCategories = new ArrayList<>(repartitionCapacite.keySet());
        List<Number> capaciteValues = new ArrayList<>(repartitionCapacite.values());
        Map<String, List<Number>> capaciteMap = Collections.singletonMap("Événements", capaciteValues);
        drawChartOrTextBlock("Répartition par Capacité d'Accueil", ChartUtil.createBarChartImage("Répartition par Capacité d'Accueil", "Capacité", "Nombre d'Événements", capaciteMap, capaciteCategories, chartWidth, barChartHeight, false), null, sectionTitle, false, 0);

        Map<String, Long> statutEvenements = statisticsService.getEventStatusCounts();
        Map<String, Number> statutEvenementsPie = statutEvenements.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        drawChartOrTextBlock("Statut des Événements (Actifs/Inactifs)", ChartUtil.createPieChartImage("Statut des Événements", statutEvenementsPie, chartWidth, chartHeight), null, sectionTitle, false, 0);

        Map<String, Integer> top5Events = statisticsService.getTop5EventsByBooking();
        List<String> topEventsText = new ArrayList<>();
        topEventsText.add("Basé sur le Nombre de Réservations:");
         if (top5Events.isEmpty()) {
            topEventsText.add("(Aucune donnée disponible)");
        } else {
            top5Events.forEach((name, count) -> topEventsText.add(String.format("• %s - %d réservations", name, count)));
        }
        if (elementsOnCurrentRow % 2 != 0) {
            currentY -= Math.max(lastElementHeightOnRow, 0) + ELEMENT_VERTICAL_SPACING;
            elementsOnCurrentRow = 0; lastElementHeightOnRow = 0;
        }
        drawChartOrTextBlock("Top 5 Événements les Plus Demandés", null, topEventsText, sectionTitle, true, 0);
    }

    private void generatePrestationStatisticsPage(List<Prestation> prestations) throws IOException {
        String sectionTitle = "Statistiques des Prestations";
        prepareNewPage(sectionTitle);
        int chartWidth = (int) ELEMENT_WIDTH_HALF;
        int chartHeight = DEFAULT_CHART_HEIGHT_HALF_PAGE;
        int barChartHeight = BAR_CHART_HEIGHT_HALF_PAGE;

        Map<String, Long> repartitionType = statisticsService.getServiceCountByType();
        Map<String, Number> repartitionTypePie = repartitionType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        drawChartOrTextBlock("Répartition par Type (Médical/Non-Médical)", ChartUtil.createPieChartImage("Répartition par Type", repartitionTypePie, chartWidth, chartHeight), null, sectionTitle, false, 0);

        double[] coutTranches = {50, 100, 250};
        Map<String, Long> distributionCout = statisticsService.getServiceDistributionByCost(coutTranches);
        List<String> coutCategories = new ArrayList<>(distributionCout.keySet());
        List<Number> coutValues = new ArrayList<>(distributionCout.values());
        Map<String, List<Number>> coutMap = Collections.singletonMap("Prestations", coutValues);
        drawChartOrTextBlock("Distribution par Coût", ChartUtil.createBarChartImage("Distribution par Coût", "Tranche de Prix", "Nombre de Prestations", coutMap, coutCategories, chartWidth, barChartHeight, false), null, sectionTitle, false, 0);

        String suiteSectionTitle = "Statistiques des Prestations - Suite";
        prepareNewPage(suiteSectionTitle);
        
        Map<String, Long> topProvidersByService = statisticsService.getTopProvidersByServiceCount(5);
        if (!topProvidersByService.isEmpty()) {
            List<String> providerNames = new ArrayList<>(topProvidersByService.keySet());
            List<Number> serviceCounts = new ArrayList<>(topProvidersByService.values());
            Map<String, List<Number>> providerServiceMap = Collections.singletonMap("Nb. Prestations", serviceCounts);
            drawChartOrTextBlock(
                "Top 5 Prestataires par Nb. de Prestations",
                ChartUtil.createBarChartImage(
                    "Top Prestataires par Nb. de Prestations",
                    "Prestataire", "Nombre de Prestations",
                    providerServiceMap, providerNames,
                    chartWidth, barChartHeight, false
                ),
                null, suiteSectionTitle, false, 0 
            );
        } else {
            drawChartOrTextBlock("Top 5 Prestataires par Nb. de Prestations", null, List.of("(Aucune donnée disponible)"), suiteSectionTitle, false, 0);
        }

        Map<String, Long> serviceAvailability = statisticsService.getServiceAvailabilityDistribution();
        Map<String, Number> serviceAvailabilityPie = serviceAvailability.entrySet().stream()
                                                        .collect(Collectors.toMap(Map.Entry::getKey, e-> (Number)e.getValue()));
        logger.info("Data for Disponibilité des Prestations pie chart: " + serviceAvailabilityPie);
                                                        
        if (!serviceAvailabilityPie.isEmpty()) {
            drawChartOrTextBlock(
                "Disponibilité des Prestations",
                ChartUtil.createPieChartImage(
                    "Disponibilité des Prestations",
                    serviceAvailabilityPie,
                    chartWidth, chartHeight
                ),
                null, suiteSectionTitle, false, 0 
            );
        } else {
             drawChartOrTextBlock("Disponibilité des Prestations", null, List.of("(Aucune donnée disponible)"), suiteSectionTitle, false, 0);
        }

        if (elementsOnCurrentRow % 2 != 0) {
             currentY -= Math.max(lastElementHeightOnRow, 0) + ELEMENT_VERTICAL_SPACING;
             elementsOnCurrentRow = 0;
             lastElementHeightOnRow = 0;
        }
    }
}