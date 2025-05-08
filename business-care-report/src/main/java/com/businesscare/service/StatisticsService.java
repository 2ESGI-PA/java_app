package com.businesscare.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.businesscare.model.ClientAccount;
import com.businesscare.model.Evenement;
import com.businesscare.model.Prestation;
import com.businesscare.model.Reservation;

public class StatisticsService {

    public Map<String, Long> getClientRepartitionParType(List<ClientAccount> clients) {
        return clients.stream()
                      .collect(Collectors.groupingBy(ClientAccount::getTypeClient, Collectors.counting()));
    }

    public Map<String, Double> getClientRepartitionParCA(List<ClientAccount> clients) {
        Map<String, Double> repartitionCA = new HashMap<>();
        for (ClientAccount client : clients) {
            repartitionCA.put(client.getNomSociete(), client.getChiffreAffairesAnnuel());
        }
        return repartitionCA.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public List<ClientAccount> getTop5ClientsFideles(List<ClientAccount> clients) {
        return clients.stream()
                      .sorted(Comparator.comparingInt((ClientAccount c) -> c.getAbonnements().size())
                                        .thenComparingDouble(ClientAccount::getChiffreAffairesAnnuel).reversed())
                      .limit(5)
                      .collect(Collectors.toList());
    }
    
    public Map<String, Long> getClientRepartitionParVille(List<ClientAccount> clients) {
        return clients.stream()
                      .collect(Collectors.groupingBy(ClientAccount::getVille, Collectors.counting()));
    }

    public Map<String, Long> getEvenementRepartitionParType(List<Evenement> evenements) {
        return evenements.stream()
                         .collect(Collectors.groupingBy(Evenement::getTypeEvenement, Collectors.counting()));
    }
    
    public Map<String, Long> getEvenementRepartitionParLieu(List<Evenement> evenements) {
        return evenements.stream()
                         .collect(Collectors.groupingBy(Evenement::getLieu, Collectors.counting()));
    }


    public List<Evenement> getTop5EvenementsDemandes(List<Evenement> evenements) {
        return evenements.stream()
                         .sorted(Comparator.comparingInt((Evenement e) -> e.getReservations().size()).reversed())
                         .limit(5)
                         .collect(Collectors.toList());
    }
    
    public Map<String, Integer> getFrequentationEvenements(List<Evenement> evenements) {
        Map<String, Integer> frequentation = new HashMap<>();
        for (Evenement evenement : evenements) {
            int totalParticipants = evenement.getReservations().stream().mapToInt(Reservation::getNombreParticipants).sum();
            frequentation.put(evenement.getNomEvenement(), totalParticipants);
        }
        return frequentation.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public Map<String, Long> getPrestationRepartitionParType(List<Prestation> prestations) {
        return prestations.stream()
                          .collect(Collectors.groupingBy(Prestation::getTypePrestation, Collectors.counting()));
    }
    
    public Map<String, Double> getPrestationRepartitionParCout(List<Prestation> prestations) {
        Map<String, Double> repartitionCout = new HashMap<>();
        for(Prestation prestation : prestations) {
            repartitionCout.merge(prestation.getTypePrestation(), prestation.getCoutUnitaire(), Double::sum);
        }
         return repartitionCout.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public List<Prestation> getTop5PrestationsFrequentes(List<Prestation> prestations) {
         return prestations.stream()
                          .sorted(Comparator.comparingInt((Prestation p) -> p.getIdEvenementsAssocies().size()).reversed())
                          .limit(5)
                          .collect(Collectors.toList());
    }
    
    public Map<String, Long> getPrestationDisponibilite(List<Prestation> prestations) {
        Map<String, Long> dispoMap = new HashMap<>();
        dispoMap.put("Disponible", prestations.stream().filter(Prestation::isDisponibilite).count());
        dispoMap.put("Non Disponible", prestations.stream().filter(p -> !p.isDisponibilite()).count());
        return dispoMap;
    }
}