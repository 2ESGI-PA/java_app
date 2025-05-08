package com.businesscare.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomEvenement() {
        return nomEvenement;
    }

    public void setNomEvenement(String nomEvenement) {
        this.nomEvenement = nomEvenement;
    }

    public String getTypeEvenement() {
        return typeEvenement;
    }

    public void setTypeEvenement(String typeEvenement) {
        this.typeEvenement = typeEvenement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public List<Planification> getPlanifications() {
        return planifications;
    }

    public void setPlanifications(List<Planification> planifications) {
        this.planifications = planifications;
    }
}