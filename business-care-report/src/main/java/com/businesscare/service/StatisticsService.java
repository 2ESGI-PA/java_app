package com.businesscare.service;

import java.sql.SQLException; 
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;


public class StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    private DatabaseService databaseService;

    public StatisticsService() {
    }

    public StatisticsService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Map<String, Long> getClientCountBySubscriptionTier() {
        if (databaseService == null) {
            logger.error("DatabaseService non initialisé dans StatisticsService pour getClientCountBySubscriptionTier.");
            return Collections.emptyMap();
        }
        try {
            return databaseService.getClientCountBySubscriptionTier();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération de la répartition des clients par formule d'abonnement.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Double> getClientRevenueDistribution(double[] tranches) {
        if (databaseService == null) {
            logger.error("DatabaseService non initialisé dans StatisticsService pour getClientRevenueDistribution.");
            return Collections.emptyMap();
        }
        Map<String, Double> distribution = new LinkedHashMap<>();
        Map<String, Double> clientRevenues;
        try {
            clientRevenues = databaseService.getClientTotalContractValue();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération du CA par client pour la distribution.", e);
            return Collections.emptyMap();
        }

        for (int i = 0; i < tranches.length; i++) {
            final double lowerBound = (i == 0) ? 0 : tranches[i-1];
            final double upperBound = tranches[i];
            String trancheLabel;
            if (i == 0) {
                trancheLabel = "< " + upperBound + "€";
            } else if (i == tranches.length -1 && tranches.length > 1 && upperBound < lowerBound) { 
                 trancheLabel = "> " + lowerBound + "€";
            } else {
                 trancheLabel = lowerBound + "€ - " + upperBound + "€";
            }
            distribution.put(trancheLabel, 0.0);
        }
        
        if (tranches.length > 0) {
             boolean lastTrancheIsGreaterThan = true;
             if (tranches.length > 1) {
                 for(int k=1; k < tranches.length; k++) { if(tranches[k] < tranches[k-1]) {lastTrancheIsGreaterThan = false; break;} } 
             }
             
             if(lastTrancheIsGreaterThan && !distribution.containsKey("> " + tranches[tranches.length-1] + "€")){
                 distribution.put("> " + tranches[tranches.length - 1] + "€", 0.0);
             }
        }


         if (tranches.length == 0) {
            double totalRevenue = clientRevenues.values().stream().mapToDouble(Double::doubleValue).sum();
            if (totalRevenue > 0) distribution.put("Total", totalRevenue);
            return distribution;
        }


        for (Double revenue : clientRevenues.values()) {
            boolean assigned = false;
            for (int i = 0; i < tranches.length; i++) {
                double lowerBound = (i == 0) ? 0 : tranches[i-1];
                double upperBound = tranches[i];
                String trancheLabel;
                 if (i == 0) {
                    trancheLabel = "< " + upperBound + "€";
                     if (revenue < upperBound) {
                        distribution.merge(trancheLabel, revenue, Double::sum);
                        assigned = true;
                        break;
                    }
                } else {
                    trancheLabel = lowerBound + "€ - " + upperBound + "€";
                     if (revenue >= lowerBound && revenue < upperBound) {
                        distribution.merge(trancheLabel, revenue, Double::sum);
                        assigned = true;
                        break;
                    }
                }
            }
            if (!assigned && revenue >= tranches[tranches.length - 1]) {
                String trancheLabel = "> " + tranches[tranches.length - 1] + "€";
                
                if (distribution.containsKey(trancheLabel)) {
                    distribution.merge(trancheLabel, revenue, Double::sum);
                } else if (tranches.length == 1 && revenue >= 0 && !distribution.containsKey("< " + tranches[0] + "€" ) ) { 
                     
                }
            }
        }
        return distribution.entrySet().stream()
            .filter(entry -> entry.getValue() >= 0) 
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public Map<String, Long> getClientCountBySize() {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getClientCountBySize();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération de la répartition des clients par taille.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Long> getClientCountByIndustry(int limit) {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getClientCountByIndustry(limit);
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération de la répartition des clients par secteur.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Double> getTop5ClientsByTotalPaid() {
         if (databaseService == null) return Collections.emptyMap();
         try {
            return databaseService.getTopNClientsByTotalPaid(5);
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération du top 5 des clients par montant payé.", e);
            return Collections.emptyMap();
        }
    }
    
    public Map<String, Long> getEventCountByType(List<Evenement> evenements) {
        if (evenements == null) return Collections.emptyMap();
        return evenements.stream()
                         .collect(Collectors.groupingBy(Evenement::getTypeEvenement, Collectors.counting()));
    }


    public Map<String, Long> getEventCountByMonth() {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getEventCountByMonth();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération de la fréquence des événements par mois.", e);
            return Collections.emptyMap();
        }
    }
    
    public Map<String, Long> getEventDistributionByCapacity(double[] tranches) {
        if (databaseService == null) return Collections.emptyMap();
        Map<String, Long> distribution = new LinkedHashMap<>();
        List<Integer> capacities;
        try {
            capacities = databaseService.getEventCapacities();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération des capacités des événements.", e);
            return Collections.emptyMap();
        }

        for (int i = 0; i < tranches.length; i++) {
            final double lowerBound = (i == 0) ? 0 : tranches[i-1];
            final double upperBound = tranches[i];
            String trancheLabel;
            if (i == 0) {
                 trancheLabel = "0 - " + (int)upperBound;
            } else {
                 trancheLabel = (int)lowerBound + " - " + (int)upperBound;
            }
            distribution.put(trancheLabel, 0L);
        }
        if (tranches.length > 0) { 
            distribution.put("> " + (int)tranches[tranches.length - 1], 0L);
        }


         if (tranches.length == 0 && !capacities.isEmpty()) {
            distribution.put("Toutes capacités", (long)capacities.size());
            return distribution;
        }


        for (Integer capacity : capacities) {
            boolean assigned = false;
            for (int i = 0; i < tranches.length; i++) {
                double lowerBound = (i == 0) ? 0 : tranches[i-1];
                double upperBound = tranches[i];
                String trancheLabel;
                 if (i == 0) {
                    trancheLabel = "0 - " + (int)upperBound;
                     if (capacity >= lowerBound && capacity <= upperBound) { 
                        distribution.merge(trancheLabel, 1L, Long::sum);
                        assigned = true;
                        break;
                    }
                } else {
                    trancheLabel = (int)lowerBound + " - " + (int)upperBound;
                     if (capacity > lowerBound && capacity <= upperBound) {
                        distribution.merge(trancheLabel, 1L, Long::sum);
                        assigned = true;
                        break;
                    }
                }
            }
             if (!assigned && tranches.length > 0 && capacity > tranches[tranches.length - 1]) {
                String trancheLabel = "> " + (int)tranches[tranches.length - 1];
                distribution.merge(trancheLabel, 1L, Long::sum);
            }
        }
        return distribution.entrySet().stream()
            .filter(entry -> entry.getValue() >= 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public Map<String, Long> getEventStatusCounts() {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getEventStatusCounts();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération du statut des événements.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Integer> getTop5EventsByBooking() {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getTopNEventsByBooking(5);
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération du top 5 des événements par réservation.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Long> getServiceCountByType() {
        if (databaseService == null) return Collections.emptyMap();
        try {
            return databaseService.getServiceCountByType();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération des prestations par type.", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Long> getServiceDistributionByCost(double[] tranches) {
        if (databaseService == null) return Collections.emptyMap();
        Map<String, Long> distribution = new LinkedHashMap<>();
        List<Double> prices;
        try {
            prices = databaseService.getServicePrices();
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la récupération des prix des prestations.", e);
            return Collections.emptyMap();
        }

         for (int i = 0; i < tranches.length; i++) {
            final double lowerBound = (i == 0) ? 0 : tranches[i-1];
            final double upperBound = tranches[i];
            String trancheLabel;
            if (i == 0) {
                trancheLabel = "< " + upperBound + "€";
            } else {
                trancheLabel = lowerBound + "€ - " + upperBound + "€";
            }
            distribution.put(trancheLabel, 0L);
        }
        if (tranches.length > 0) {
             distribution.put("> " + tranches[tranches.length - 1] + "€", 0L);
        }


        if (tranches.length == 0 && !prices.isEmpty()) {
            distribution.put("Tous prix", (long)prices.size());
            return distribution;
        }


        for (Double price : prices) {
            boolean assigned = false;
            for (int i = 0; i < tranches.length; i++) {
                double lowerBound = (i == 0) ? 0 : tranches[i-1];
                double upperBound = tranches[i];
                String trancheLabel;

                if (i == 0) {
                    trancheLabel = "< " + upperBound + "€";
                     if (price < upperBound) {
                        distribution.merge(trancheLabel, 1L, Long::sum);
                        assigned = true;
                        break;
                    }
                } else {
                    trancheLabel = lowerBound + "€ - " + upperBound + "€";
                     if (price >= lowerBound && price < upperBound) {
                        distribution.merge(trancheLabel, 1L, Long::sum);
                        assigned = true;
                        break;
                    }
                }
            }
             if (!assigned && tranches.length > 0 && price >= tranches[tranches.length - 1]) {
                String trancheLabel = "> " + tranches[tranches.length - 1] + "€";
                distribution.merge(trancheLabel, 1L, Long::sum);
            }
        }
         return distribution.entrySet().stream()
            .filter(entry -> entry.getValue() >= 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public List<Prestation> getTop5PrestationsFrequentes(List<Prestation> prestations) {
         if (prestations == null) return Collections.emptyList();
         return prestations.stream()
                          .sorted(Comparator.comparingInt((Prestation p) -> p.getIdEvenementsAssocies().size()).reversed())
                          .limit(5)
                          .collect(Collectors.toList());
    }
}