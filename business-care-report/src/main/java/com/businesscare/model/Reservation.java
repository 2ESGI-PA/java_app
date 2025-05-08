package com.businesscare.model;

import java.util.Date;

public class Reservation {
    private String idReservation;
    private String idEvenement;
    private String idClient;
    private Date dateReservation;
    private int nombreParticipants;

    public Reservation(String idReservation, String idEvenement, String idClient, Date dateReservation, int nombreParticipants) {
        this.idReservation = idReservation;
        this.idEvenement = idEvenement;
        this.idClient = idClient;
        this.dateReservation = dateReservation;
        this.nombreParticipants = nombreParticipants;
    }

    public String getIdReservation() {
        return idReservation;
    }

    public void setIdReservation(String idReservation) {
        this.idReservation = idReservation;
    }

    public String getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(String idEvenement) {
        this.idEvenement = idEvenement;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public Date getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(Date dateReservation) {
        this.dateReservation = dateReservation;
    }

    public int getNombreParticipants() {
        return nombreParticipants;
    }

    public void setNombreParticipants(int nombreParticipants) {
        this.nombreParticipants = nombreParticipants;
    }
}