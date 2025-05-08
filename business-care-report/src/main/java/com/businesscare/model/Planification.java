package com.businesscare.model;

import java.util.Date;

public class Planification {
    private String idPlanification;
    private String idEvenement;
    private String detailsPlanification;
    private Date datePlanification;

    public Planification(String idPlanification, String idEvenement, String detailsPlanification, Date datePlanification) {
        this.idPlanification = idPlanification;
        this.idEvenement = idEvenement;
        this.detailsPlanification = detailsPlanification;
        this.datePlanification = datePlanification;
    }

    public String getIdPlanification() {
        return idPlanification;
    }

    public void setIdPlanification(String idPlanification) {
        this.idPlanification = idPlanification;
    }

    public String getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(String idEvenement) {
        this.idEvenement = idEvenement;
    }

    public String getDetailsPlanification() {
        return detailsPlanification;
    }

    public void setDetailsPlanification(String detailsPlanification) {
        this.detailsPlanification = detailsPlanification;
    }

    public Date getDatePlanification() {
        return datePlanification;
    }

    public void setDatePlanification(Date datePlanification) {
        this.datePlanification = datePlanification;
    }
}