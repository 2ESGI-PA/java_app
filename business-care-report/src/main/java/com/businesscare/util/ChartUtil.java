package com.businesscare.util;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart; 
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;

public class ChartUtil {

    private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
    private static final Font AXIS_TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Color PLOT_BACKGROUND = new Color(248, 248, 248);
    private static final Color[] CUSTOM_COLORS = new Color[]{
        new Color(224, 68, 14), new Color(230, 105, 62), new Color(236, 143, 110),
        new Color(243, 180, 159), new Color(249, 218, 208)
    };

    public static byte[] createPieChartImage(String title, Map<String, Number> data, int width, int height) throws IOException {
        PieChart chart = new PieChartBuilder().width(width).height(height).title("").build();
        PieStyler pieStyler = chart.getStyler();

        pieStyler.setAntiAlias(true);
        pieStyler.setLegendVisible(true);
        pieStyler.setLegendFont(SMALL_FONT);
        pieStyler.setPlotContentSize(.7);
        pieStyler.setStartAngleInDegrees(90);
        pieStyler.setPlotBorderVisible(true);
        pieStyler.setPlotBackgroundColor(PLOT_BACKGROUND);
        pieStyler.setChartBackgroundColor(Color.WHITE);

        if (title.contains("Ville") || title.contains("Lieu")) {
             pieStyler.setLabelsVisible(false);
        } else {
            pieStyler.setLabelsVisible(true);
            pieStyler.setLabelType(PieStyler.LabelType.Percentage);
            pieStyler.setLabelsFont(SMALL_FONT);
            pieStyler.setLabelsDistance(1.1);
        }

        for (Map.Entry<String, Number> entry : data.entrySet()) {
            chart.addSeries(entry.getKey(), entry.getValue());
        }

        return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
    }

    public static byte[] createBarChartImage(String title, String xAxisTitle, String yAxisTitle, Map<String, List<Number>> seriesMap, List<String> categories, int width, int height) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).title("")
                                                     .xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
        CategoryStyler categoryStyler = chart.getStyler();

        categoryStyler.setSeriesColors(CUSTOM_COLORS);
        categoryStyler.setAntiAlias(true);

        categoryStyler.setLegendPosition(Styler.LegendPosition.InsideNW);
        categoryStyler.setLegendFont(SMALL_FONT);

        categoryStyler.setAxisTickLabelsFont(SMALL_FONT);
        categoryStyler.setXAxisLabelRotation(90);
        categoryStyler.setAxisTitleFont(AXIS_TITLE_FONT);

        categoryStyler.setAxisTickMarksColor(Color.DARK_GRAY);

        categoryStyler.setLabelsVisible(false);

        categoryStyler.setAvailableSpaceFill(0.65);
        categoryStyler.setPlotGridLinesVisible(false);
        categoryStyler.setPlotBorderVisible(true);
        categoryStyler.setPlotBackgroundColor(PLOT_BACKGROUND);
        categoryStyler.setChartBackgroundColor(Color.WHITE);

        for (Map.Entry<String, List<Number>> entry : seriesMap.entrySet()) {
            if (entry.getValue() != null && categories != null && entry.getValue().size() == categories.size()) {
                chart.addSeries(entry.getKey(), categories, entry.getValue());
            } else {
                 String valSize = (entry.getValue() == null) ? "null" : String.valueOf(entry.getValue().size());
                 String catSize = (categories == null) ? "null" : String.valueOf(categories.size());
                 System.err.println("Avertissement: Données invalides pour la série '" + entry.getKey() + "'. Taille valeurs: " + valSize + ", Taille catégories: " + catSize + ".");
            }
        }

        return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
    }
}