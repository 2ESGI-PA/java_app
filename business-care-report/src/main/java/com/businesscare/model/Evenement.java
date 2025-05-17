package com.businesscare.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Evenement {
    private String id;
    private String nomEvenement;
    private String typeEvenement;
    private String description;
    private Date dateDebut;
    private Date dateFin;
    private String lieu;
    private int capaciteMax;
    private List<Reservation> reservations;
    private List<Planification> planifications;
    private String planningSummary;

    public Evenement(String id, String nomEvenement, String typeEvenement, String description, Date dateDebut, Date dateFin, String lieu, int capaciteMax) {
        this.id = id;
        this.nomEvenement = nomEvenement;
        this.typeEvenement = typeEvenement;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
        this.reservations = new ArrayList<>();
        this.planifications = new ArrayList<>();
        this.planningSummary = ""; 
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNomEvenement() { return nomEvenement; }
    public void setNomEvenement(String nomEvenement) { this.nomEvenement = nomEvenement; }
    public String getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(String typeEvenement) { this.typeEvenement = typeEvenement; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }
    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
    public List<Planification> getPlanifications() { return planifications; }
    public void setPlanifications(List<Planification> planifications) { this.planifications = planifications; }
    public String getPlanningSummary() { return planningSummary; }
    public void setPlanningSummary(String planningSummary) { this.planningSummary = planningSummary; }

    public String generatePlanningSummary(List<Reservation> reservations) {
        StringBuilder summary = new StringBuilder();
        summary.append("Participants: ").append(reservations.size()).append("<br>");

        Set<String> companies = new HashSet<>();
        for (Reservation res : reservations) {
            String companyName = getCompanyNameFromId(res.getIdClient());
            if (companyName != null) {
                companies.add(companyName);
            }
        }
        summary.append("Entreprises: ").append(companies).append("<br>");

        Set<String> services = new HashSet<>();
        for (Reservation res : reservations) {
            String serviceName = getServiceNameFromId(res.getIdEvenement());
            if (serviceName != null) {
                services.add(serviceName);
            }
        }
        summary.append("Services: ").append(services).append("<br>");

        summary.append("Du ").append(this.dateDebut).append(" au ").append(this.dateFin);

        return summary.toString();
    }

    private String getCompanyNameFromId(String idClient) {
        return "Company " + idClient;
    }

    private String getServiceNameFromId(String idEvenement) {
        return "Service for Event " + idEvenement;
    }
}