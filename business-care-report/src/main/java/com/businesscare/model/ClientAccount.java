package com.businesscare.model;

import java.util.ArrayList;
import java.util.List;

public class ClientAccount {
    private String id;
    private String nomSociete;
    private String adresse;
    private String ville;
    private String typeClient;
    private double chiffreAffairesAnnuel;
    private List<Abonnement> abonnements;
    private List<Devis> devis;
    private List<Facture> factures;

    public ClientAccount(String id, String nomSociete, String adresse, String ville, String typeClient, double chiffreAffairesAnnuel) {
        this.id = id;
        this.nomSociete = nomSociete;
        this.adresse = adresse;
        this.ville = ville;
        this.typeClient = typeClient;
        this.chiffreAffairesAnnuel = chiffreAffairesAnnuel;
        this.abonnements = new ArrayList<>();
        this.devis = new ArrayList<>();
        this.factures = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomSociete() {
        return nomSociete;
    }

    public void setNomSociete(String nomSociete) {
        this.nomSociete = nomSociete;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getTypeClient() {
        return typeClient;
    }

    public void setTypeClient(String typeClient) {
        this.typeClient = typeClient;
    }

    public double getChiffreAffairesAnnuel() {
        return chiffreAffairesAnnuel;
    }

    public void setChiffreAffairesAnnuel(double chiffreAffairesAnnuel) {
        this.chiffreAffairesAnnuel = chiffreAffairesAnnuel;
    }

    public List<Abonnement> getAbonnements() {
        return abonnements;
    }

    public void setAbonnements(List<Abonnement> abonnements) {
        this.abonnements = abonnements;
    }

    public List<Devis> getDevis() {
        return devis;
    }

    public void setDevis(List<Devis> devis) {
        this.devis = devis;
    }

    public List<Facture> getFactures() {
        return factures;
    }

    public void setFactures(List<Facture> factures) {
        this.factures = factures;
    }
}