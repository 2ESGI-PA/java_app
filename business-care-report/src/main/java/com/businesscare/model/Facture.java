package com.businesscare.model;

import java.util.Date;

import com.businesscare.model.enums.InvoiceStatus;

public class Facture {
    private String id;
    private String numeroFacture;
    private Date dateFacturation;
    private double montantTotal;
    private InvoiceStatus statutPaiement;

    public Facture(String id, String numeroFacture, Date dateFacturation, double montantTotal, InvoiceStatus statutPaiement) {
        this.id = id;
        this.numeroFacture = numeroFacture;
        this.dateFacturation = dateFacturation;
        this.montantTotal = montantTotal;
        this.statutPaiement = statutPaiement;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public Date getDateFacturation() {
        return dateFacturation;
    }

    public void setDateFacturation(Date dateFacturation) {
        this.dateFacturation = dateFacturation;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(double montantTotal) {
        this.montantTotal = montantTotal;
    }

    public InvoiceStatus getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(InvoiceStatus statutPaiement) {
        this.statutPaiement = statutPaiement;
    }
}