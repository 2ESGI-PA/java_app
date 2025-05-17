package com.businesscare.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartUtil {
    private static final Logger logger = LoggerFactory.getLogger(ChartUtil.class);

    private static final Font LEGEND_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font AXIS_TICK_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);
    private static final Font AXIS_TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Font LABEL_FONT_PIE = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

    private static final Color PLOT_BACKGROUND_COLOR = new Color(248, 248, 248);
    private static final Color CHART_BACKGROUND_COLOR = Color.WHITE;
    private static final Color AXIS_TICK_MARKS_COLOR = Color.DARK_GRAY;
    private static final Color DIVIDER_LINE_COLOR = new Color(189, 195, 199);


    private static final Color[] CUSTOM_CHART_COLORS = new Color[]{
        new Color(31, 119, 180), new Color(255, 127, 14), new Color(44, 160, 44),
        new Color(214, 39, 40), new Color(148, 103, 189), new Color(140, 86, 75),
        new Color(227, 119, 194), new Color(127, 127, 127), new Color(188, 189, 34),
        new Color(23, 190, 207)
    };

    public static byte[] createPieChartImage(String title, Map<String, Number> data, int width, int height) throws IOException {
        if (data == null || data.isEmpty() || data.values().stream().allMatch(val -> val == null || val.doubleValue() <= 0)) {
            logger.warn("Données vides ou non positives pour le graphique à secteurs: '{}'. Un graphique de remplacement sera généré.", title);
            return createPlaceholderImage("Pas de données pour : " + title, width, height);
        }

        PieChart chart = new PieChartBuilder().width(width).height(height).title("").build();
        PieStyler pieStyler = chart.getStyler();

        pieStyler.setSeriesColors(CUSTOM_CHART_COLORS);
        pieStyler.setAntiAlias(true);
        pieStyler.setLegendVisible(true);
        pieStyler.setLegendFont(LEGEND_FONT);
        pieStyler.setLegendLayout(Styler.LegendLayout.Vertical);
        pieStyler.setLegendPosition(Styler.LegendPosition.OutsideE);

        pieStyler.setPlotContentSize(.7);
        pieStyler.setStartAngleInDegrees(90);
        pieStyler.setPlotBorderVisible(false);
        pieStyler.setPlotBackgroundColor(PLOT_BACKGROUND_COLOR);
        pieStyler.setChartBackgroundColor(CHART_BACKGROUND_COLOR);

        boolean showLabels = !(title.toLowerCase().contains("ville") || title.toLowerCase().contains("lieu"));
        pieStyler.setLabelsVisible(showLabels);
        if (showLabels) {
            pieStyler.setLabelType(PieStyler.LabelType.Percentage);
            pieStyler.setLabelsFont(LABEL_FONT_PIE);
            pieStyler.setLabelsDistance(1.1);
            pieStyler.setDecimalPattern("#.# %");
        }
        
        pieStyler.setSumVisible(false);

        boolean dataAdded = false;
        Map<String, Number> sortedData = new TreeMap<>(data);

        for (Map.Entry<String, Number> entry : sortedData.entrySet()) {
            if (entry.getValue() != null && entry.getValue().doubleValue() > 0) {
                 chart.addSeries(entry.getKey(), entry.getValue());
                 dataAdded = true;
            } else {
                 logger.warn("Série '{}' pour le graphique '{}' a une valeur nulle ou non positive et ne sera pas affichée.", entry.getKey(), title);
            }
        }
        
        if (!dataAdded) {
            logger.warn("Aucune série avec des données valides (>0) pour le graphique à secteurs: '{}'", title);
            return createPlaceholderImage("Données insuffisantes pour : " + title, width, height);
        }
        try {
            return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            logger.error("Erreur IO lors de la génération de l'image du graphique à secteurs '{}': {}", title, e.getMessage(), e);
            return createPlaceholderImage("Erreur graphique IO : " + title, width, height);
        }
    }

    public static byte[] createBarChartImage(String title, String xAxisTitle, String yAxisTitle, 
                                             Map<String, List<Number>> seriesMap, List<String> categories, 
                                             int width, int height, boolean isHorizontal) throws IOException {
         if (seriesMap == null || seriesMap.isEmpty() || categories == null || categories.isEmpty() || seriesMap.values().stream().allMatch(list -> list == null || list.isEmpty())) {
            logger.warn("Données vides ou non valides pour le graphique à barres: '{}'. Un graphique de remplacement sera généré.", title);
            return createPlaceholderImage("Pas de données pour : " + title, width, height);
        }

        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).title("")
                                                     .xAxisTitle(isHorizontal ? yAxisTitle : xAxisTitle) 
                                                     .yAxisTitle(isHorizontal ? xAxisTitle : yAxisTitle)
                                                     .build();
        CategoryStyler categoryStyler = chart.getStyler();

        if (isHorizontal) {
            categoryStyler.setAvailableSpaceFill(0.40);
            categoryStyler.setLegendPosition(Styler.LegendPosition.InsideS);
            categoryStyler.setXAxisLabelRotation(0);
        } else {
            categoryStyler.setAvailableSpaceFill(0.60);
            categoryStyler.setLegendPosition(Styler.LegendPosition.InsideNW);
            categoryStyler.setXAxisLabelRotation(90);
        }

        categoryStyler.setSeriesColors(CUSTOM_CHART_COLORS);
        categoryStyler.setAntiAlias(true);
        categoryStyler.setLegendFont(LEGEND_FONT);
        categoryStyler.setAxisTickLabelsFont(AXIS_TICK_FONT);
        categoryStyler.setAxisTitleFont(AXIS_TITLE_FONT);
        
        categoryStyler.setYAxisDecimalPattern("#,##0.0"); 

        categoryStyler.setAxisTickMarksColor(AXIS_TICK_MARKS_COLOR);
        categoryStyler.setLabelsVisible(false);
        categoryStyler.setPlotGridLinesVisible(true);
        categoryStyler.setPlotGridLinesColor(new Color(220,220,220));
        categoryStyler.setPlotBorderVisible(false);
        categoryStyler.setPlotBackgroundColor(PLOT_BACKGROUND_COLOR);
        categoryStyler.setChartBackgroundColor(CHART_BACKGROUND_COLOR);

        boolean dataAdded = false;
        for (Map.Entry<String, List<Number>> entry : seriesMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty() && categories.size() == entry.getValue().size()) {
                try {
                    chart.addSeries(entry.getKey(), new ArrayList<>(categories), entry.getValue());
                    dataAdded = true;
                } catch (IllegalArgumentException e) {
                     logger.error("Erreur lors de l'ajout de la série '{}' au graphique '{}': {}. Catégories: {}, Valeurs: {}",
                                 entry.getKey(), title, e.getMessage(), categories.size(), entry.getValue().size());
                }
            } else {
                 String valSize = (entry.getValue() == null) ? "null" : String.valueOf(entry.getValue().size());
                 String catSize = String.valueOf(categories.size());
                 logger.warn("Données invalides ou incohérentes pour la série '{}' du graphique à barres '{}'. Taille valeurs: {}, Taille catégories: {}.",
                             entry.getKey(), title, valSize, catSize);
            }
        }
        
        if (!dataAdded) {
            logger.warn("Aucune série valide n'a pu être ajoutée au graphique à barres: '{}'", title);
            return createPlaceholderImage("Données insuffisantes pour : " + title, width, height);
        }
        try {
            return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            logger.error("Erreur IO lors de la génération de l'image du graphique à barres '{}': {}", title, e.getMessage(), e);
            return createPlaceholderImage("Erreur graphique IO : " + title, width, height);
        }
    }

    public static byte[] createLineChartImage(String title, String xAxisTitle, String yAxisTitle,
                                            Map<String, Map<String, Number>> seriesDataMap, 
                                            int width, int height) throws IOException {

        if (seriesDataMap == null || seriesDataMap.isEmpty() || seriesDataMap.values().stream().allMatch(map -> map == null || map.isEmpty())) {
            logger.warn("Données vides pour le graphique en lignes: '{}'. Un graphique de remplacement sera généré.", title);
            return createPlaceholderImage("Pas de données pour : " + title, width, height);
        }

        XYChart chart = new XYChartBuilder().width(width).height(height).title("").xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
        XYStyler styler = chart.getStyler();

        styler.setSeriesColors(CUSTOM_CHART_COLORS);
        styler.setAntiAlias(true);
        styler.setLegendPosition(Styler.LegendPosition.InsideNW);
        styler.setLegendFont(LEGEND_FONT);
        styler.setAxisTickLabelsFont(AXIS_TICK_FONT);
        styler.setXAxisLabelRotation(45);
        styler.setAxisTitleFont(AXIS_TITLE_FONT);
        styler.setYAxisDecimalPattern("#,##0");
        styler.setAxisTickMarksColor(AXIS_TICK_MARKS_COLOR);
        styler.setPlotGridLinesVisible(true);
        styler.setPlotGridLinesColor(new Color(220, 220, 220));
        styler.setPlotBorderVisible(false);
        styler.setPlotBackgroundColor(PLOT_BACKGROUND_COLOR);
        styler.setChartBackgroundColor(CHART_BACKGROUND_COLOR);
        styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        styler.setMarkerSize(5);
        styler.setDatePattern("MM/yyyy");

        boolean dataAdded = false;
        SimpleDateFormat sdfInputParser = new SimpleDateFormat("yyyy-MM");
        sdfInputParser.setLenient(false);

        for (Map.Entry<String, Map<String, Number>> seriesEntry : seriesDataMap.entrySet()) {
            String seriesName = seriesEntry.getKey();
            Map<String, Number> dataPoints = seriesEntry.getValue();

            if (dataPoints == null || dataPoints.isEmpty()) {
                logger.warn("Aucun point de données pour la série '{}' du graphique en lignes '{}'", seriesName, title);
                continue;
            }
            
            List<Date> xData = new ArrayList<>();
            List<Number> yData = new ArrayList<>();

            Map<Date, Number> sortedDataPoints = new TreeMap<>();
            for(Map.Entry<String, Number> point : dataPoints.entrySet()){
                try {
                    Date date = sdfInputParser.parse(point.getKey());
                    sortedDataPoints.put(date, point.getValue());
                } catch (ParseException e) {
                    logger.warn("Format de date invalide '{}' pour le graphique en lignes '{}'. Point ignoré.", point.getKey(), title);
                }
            }
            
            for(Map.Entry<Date, Number> sortedPoint : sortedDataPoints.entrySet()){
                xData.add(sortedPoint.getKey());
                yData.add(sortedPoint.getValue());
            }

            if (!xData.isEmpty() && !yData.isEmpty()) {
                XYSeries series = chart.addSeries(seriesName, xData, yData);
                series.setMarker(SeriesMarkers.CIRCLE);
                dataAdded = true;
            }
        }

        if (!dataAdded) {
            logger.warn("Aucune série valide n'a pu être ajoutée au graphique en lignes: '{}'", title);
            return createPlaceholderImage("Données insuffisantes pour : " + title, width, height);
        }
        
        try {
            return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            logger.error("Erreur IO lors de la génération de l'image du graphique en lignes '{}': {}", title, e.getMessage(), e);
            return createPlaceholderImage("Erreur graphique IO : " + title, width, height);
        }
    }


    private static byte[] createPlaceholderImage(String message, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(PLOT_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        
        String[] lines = message.split(" : ");
        if (lines.length > 1) {
            int stringWidth1 = g2d.getFontMetrics().stringWidth(lines[0] + " :");
            int stringHeight1 = g2d.getFontMetrics().getAscent() - g2d.getFontMetrics().getDescent();
            int stringWidth2 = g2d.getFontMetrics().stringWidth(lines[1]);
            g2d.drawString(lines[0] + " :", (width - stringWidth1) / 2, height / 2 - stringHeight1 / 2);
            g2d.drawString(lines[1], (width - stringWidth2) / 2, height / 2 + stringHeight1 / 2 + 5);
        } else {
            int stringWidth = g2d.getFontMetrics().stringWidth(message);
            int stringHeight = g2d.getFontMetrics().getAscent() - g2d.getFontMetrics().getDescent();
            g2d.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);
        }

        g2d.setColor(DIVIDER_LINE_COLOR);
        g2d.drawRect(1, 1, width - 2, height - 2);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}