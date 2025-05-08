package com.businesscare.model;

import java.util.Date;

import com.businesscare.model.enums.SubscriptionTier;

public class Abonnement {
    private String id;
    private SubscriptionTier typeAbonnement;
    private Date dateDebut;
    private Date dateFin;
    private double montant;

    public Abonnement(String id, SubscriptionTier typeAbonnement, Date dateDebut, Date dateFin, double montant) {
        this.id = id;
        this.typeAbonnement = typeAbonnement;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montant = montant;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionTier getTypeAbonnement() {
        return typeAbonnement;
    }

    public void setTypeAbonnement(SubscriptionTier typeAbonnement) {
        this.typeAbonnement = typeAbonnement;
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

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }
}