package com.businesscare.model;

import java.util.ArrayList;
import java.util.List;

public class Prestation {
    private String id;
    private String nomPrestation;
    private String typePrestation;
    private String description;
    private double coutUnitaire;
    private boolean disponibilite;
    private List<String> idEvenementsAssocies;

    public Prestation(String id, String nomPrestation, String typePrestation, String description, double coutUnitaire, boolean disponibilite) {
        this.id = id;
        this.nomPrestation = nomPrestation;
        this.typePrestation = typePrestation;
        this.description = description;
        this.coutUnitaire = coutUnitaire;
        this.disponibilite = disponibilite;
        this.idEvenementsAssocies = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomPrestation() {
        return nomPrestation;
    }

    public void setNomPrestation(String nomPrestation) {
        this.nomPrestation = nomPrestation;
    }

    public String getTypePrestation() {
        return typePrestation;
    }

    public void setTypePrestation(String typePrestation) {
        this.typePrestation = typePrestation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCoutUnitaire() {
        return coutUnitaire;
    }

    public void setCoutUnitaire(double coutUnitaire) {
        this.coutUnitaire = coutUnitaire;
    }

    public boolean isDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    public List<String> getIdEvenementsAssocies() {
        return idEvenementsAssocies;
    }

    public void setIdEvenementsAssocies(List<String> idEvenementsAssocies) {
        this.idEvenementsAssocies = idEvenementsAssocies;
    }
}