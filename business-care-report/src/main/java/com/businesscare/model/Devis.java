package com.businesscare.model;

import java.util.Date;

import com.businesscare.model.enums.QuoteStatus;

public class Devis {
    private String id;
    private String numeroDevis;
    private Date dateEmission;
    private double montantTotal;
    private QuoteStatus statut;

    public Devis(String id, String numeroDevis, Date dateEmission, double montantTotal, QuoteStatus statut) {
        this.id = id;
        this.numeroDevis = numeroDevis;
        this.dateEmission = dateEmission;
        this.montantTotal = montantTotal;
        this.statut = statut;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroDevis() {
        return numeroDevis;
    }

    public void setNumeroDevis(String numeroDevis) {
        this.numeroDevis = numeroDevis;
    }

    public Date getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(Date dateEmission) {
        this.dateEmission = dateEmission;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(double montantTotal) {
        this.montantTotal = montantTotal;
    }

    public QuoteStatus getStatut() {
        return statut;
    }

    public void setStatut(QuoteStatus statut) {
        this.statut = statut;
    }
}