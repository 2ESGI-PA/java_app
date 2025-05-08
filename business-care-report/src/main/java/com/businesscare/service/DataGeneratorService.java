package com.businesscare.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.businesscare.model.Abonnement;
import com.businesscare.model.ClientAccount;
import com.businesscare.model.Devis;
import com.businesscare.model.Evenement;
import com.businesscare.model.Facture;
import com.businesscare.model.Planification;
import com.businesscare.model.Prestation;
import com.businesscare.model.Reservation;
import com.businesscare.model.enums.InvoiceStatus;
import com.businesscare.model.enums.QuoteStatus;
import com.businesscare.model.enums.SubscriptionTier;
import com.github.javafaker.Faker;

public class DataGeneratorService {

    private final Faker faker;
    private final Random random;

    public DataGeneratorService() {
        this.faker = new Faker(new Locale("fr"));
        this.random = new Random();
    }

    public List<ClientAccount> generateClientAccounts(int count) {
        List<ClientAccount> accounts = new ArrayList<>();
        String[] clientTypes = {"PME", "Grande Entreprise", "Startup", "Association"};
        for (int i = 0; i < count; i++) {
            String accountId = UUID.randomUUID().toString();
            ClientAccount account = new ClientAccount(
                accountId,
                faker.company().name(),
                faker.address().streetAddress(),
                faker.address().city(),
                clientTypes[random.nextInt(clientTypes.length)],
                faker.number().randomDouble(2, 5000, 1000000)
            );
            account.setAbonnements(generateAbonnements(random.nextInt(3) + 1));
            account.setDevis(generateDevis(random.nextInt(5) + 1));
            account.setFactures(generateFactures(random.nextInt(10) + 1, account.getDevis()));
            accounts.add(account);
        }
        return accounts;
    }

    private List<Abonnement> generateAbonnements(int count) {
        List<Abonnement> abonnements = new ArrayList<>();
        SubscriptionTier[] tiers = SubscriptionTier.values();
        for (int i = 0; i < count; i++) {
            Date startDate = faker.date().past(365 * 2, TimeUnit.DAYS);
            Date endDate = faker.date().future(365, TimeUnit.DAYS, startDate);
            abonnements.add(new Abonnement(
                UUID.randomUUID().toString(),
                tiers[random.nextInt(tiers.length)],
                startDate,
                endDate,
                faker.number().randomDouble(2, 50, 500)
            ));
        }
        return abonnements;
    }

    private List<Devis> generateDevis(int count) {
        List<Devis> devisList = new ArrayList<>();
        QuoteStatus[] statuses = QuoteStatus.values();
        for (int i = 0; i < count; i++) {
            devisList.add(new Devis(
                UUID.randomUUID().toString(),
                "DEV-" + faker.number().digits(6),
                faker.date().past(180, TimeUnit.DAYS),
                faker.number().randomDouble(2, 100, 10000),
                statuses[random.nextInt(statuses.length)]
            ));
        }
        return devisList;
    }

    private List<Facture> generateFactures(int count, List<Devis> relatedDevis) {
        List<Facture> factures = new ArrayList<>();
        InvoiceStatus[] statuses = InvoiceStatus.values();
        for (int i = 0; i < count; i++) {
            Date invoiceDate = faker.date().past(90, TimeUnit.DAYS);
            double amount;
            if (!relatedDevis.isEmpty() && random.nextBoolean()) {
                amount = relatedDevis.get(random.nextInt(relatedDevis.size())).getMontantTotal();
            } else {
                amount = faker.number().randomDouble(2, 50, 5000);
            }
            factures.add(new Facture(
                UUID.randomUUID().toString(),
                "FACT-" + faker.number().digits(7),
                invoiceDate,
                amount,
                statuses[random.nextInt(statuses.length)]
            ));
        }
        return factures;
    }

    public List<Evenement> generateEvenements(int count, List<ClientAccount> clients) {
        List<Evenement> evenements = new ArrayList<>();
        String[] eventTypes = {"Séminaire", "Conférence", "Atelier", "Team Building", "Lancement Produit"};
        for (int i = 0; i < count; i++) {
            String eventId = UUID.randomUUID().toString();
            Date startDate = faker.date().future(90, TimeUnit.DAYS);
            Date endDate = faker.date().future(5, TimeUnit.DAYS, startDate);
            Evenement evenement = new Evenement(
                eventId,
                faker.company().catchPhrase(),
                eventTypes[random.nextInt(eventTypes.length)],
                faker.lorem().paragraph(),
                startDate,
                endDate,
                faker.address().city() + " - " + faker.address().streetName(),
                faker.number().numberBetween(20, 500)
            );
            evenement.setReservations(generateReservations(random.nextInt(10) + 1, eventId, clients));
            evenement.setPlanifications(generatePlanifications(random.nextInt(3) + 1, eventId));
            evenements.add(evenement);
        }
        return evenements;
    }

    private List<Reservation> generateReservations(int count, String eventId, List<ClientAccount> clients) {
        List<Reservation> reservations = new ArrayList<>();
        if (clients.isEmpty()) return reservations;
        for (int i = 0; i < count; i++) {
            reservations.add(new Reservation(
                UUID.randomUUID().toString(),
                eventId,
                clients.get(random.nextInt(clients.size())).getId(),
                faker.date().past(30, TimeUnit.DAYS),
                faker.number().numberBetween(1, 50)
            ));
        }
        return reservations;
    }

    private List<Planification> generatePlanifications(int count, String eventId) {
        List<Planification> planifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            planifications.add(new Planification(
                UUID.randomUUID().toString(),
                eventId,
                "Salle " + faker.commerce().department() + ", Matériel: " + faker.commerce().productName(),
                faker.date().past(60, TimeUnit.DAYS)
            ));
        }
        return planifications;
    }

    public List<Prestation> generatePrestations(int count, List<Evenement> evenements) {
        List<Prestation> prestations = new ArrayList<>();
        String[] prestationTypes = {"Location Salle", "Matériel Audiovisuel", "Traiteur", "Animation", "Logistique"};
        for (int i = 0; i < count; i++) {
            Prestation prestation = new Prestation(
                UUID.randomUUID().toString(),
                faker.commerce().productName(),
                prestationTypes[random.nextInt(prestationTypes.length)],
                faker.lorem().sentence(),
                faker.number().randomDouble(2, 20, 1000),
                random.nextBoolean()
            );
            if (!evenements.isEmpty() && random.nextInt(3) > 0) {
                int numAssociatedEvents = random.nextInt(Math.min(3, evenements.size())) + 1;
                for(int j=0; j < numAssociatedEvents; j++){
                    prestation.getIdEvenementsAssocies().add(evenements.get(random.nextInt(evenements.size())).getId());
                }
            }
            prestations.add(prestation);
        }
        return prestations;
    }
}