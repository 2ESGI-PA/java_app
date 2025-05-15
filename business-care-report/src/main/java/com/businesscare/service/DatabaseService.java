package com.businesscare.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.businesscare.model.Abonnement;
import com.businesscare.model.ClientAccount;
import com.businesscare.model.Devis;
import com.businesscare.model.Evenement;
import com.businesscare.model.Facture;
import com.businesscare.model.Prestation;
import com.businesscare.model.Reservation;
import com.businesscare.model.enums.InvoiceStatus;
import com.businesscare.model.enums.QuoteStatus;
import com.businesscare.model.enums.SubscriptionTier;

public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private final Connection connection;

    public DatabaseService(Connection connection) {
        this.connection = connection;
    }

    public List<ClientAccount> getAllClientAccounts() throws SQLException {
        List<ClientAccount> accounts = new ArrayList<>();
        String sql = "SELECT id, name AS nom_societe, address AS adresse, city AS ville, industry AS type_client, size FROM company WHERE status = 'ACTIVE'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String companyIdStr = String.valueOf(rs.getInt("id"));
                double chiffreAffaires = calculateChiffreAffairesAnnuelsReels(companyIdStr);
                ClientAccount account = new ClientAccount(
                        companyIdStr,
                        rs.getString("nom_societe"),
                        rs.getString("adresse"),
                        rs.getString("ville"),
                        rs.getString("type_client"),
                        chiffreAffaires
                );
                account.setAbonnements(getAbonnementsForCompany(companyIdStr));
                account.setDevis(getDevisForCompany(companyIdStr));
                account.setFactures(getFacturesForCompany(companyIdStr));
                accounts.add(account);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des comptes clients: {}", e.getMessage(), e);
            throw e;
        }
        logger.info("{} comptes clients chargés.", accounts.size());
        return accounts;
    }

    private double calculateChiffreAffairesAnnuelsReels(String companyId) throws SQLException {
        double totalCA = 0.0;
        String sql = "SELECT SUM(total_amount) AS total_ca FROM invoice WHERE company_id = ? AND status = 'payed'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(companyId));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalCA = rs.getDouble("total_ca");
                }
            }
        }
        return totalCA;
    }

    public List<Evenement> getAllEvenements() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT id, name AS nom_evenement, description, start_date AS date_debut, end_date AS date_fin, location AS lieu, capacity AS capacite_max, is_active FROM event";
        logger.debug("Exécution de la requête pour getAllEvenements: {}", sql);

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String eventIdStr = String.valueOf(rs.getInt("id"));
                String typeEvenement = "Non spécifié";

                Evenement evenement = new Evenement(
                        eventIdStr,
                        rs.getString("nom_evenement"),
                        typeEvenement,
                        rs.getString("description"),
                        rs.getTimestamp("date_debut"),
                        rs.getTimestamp("date_fin"),
                        rs.getString("lieu"),
                        rs.getInt("capacite_max")
                );
                evenement.setReservations(getReservationsForEvent(eventIdStr));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des événements: {}", e.getMessage(), e);
            throw e;
        }
        logger.info("{} événements chargés.", evenements.size());
        return evenements;
    }

    public List<Prestation> getAllPrestations() throws SQLException {
        List<Prestation> prestations = new ArrayList<>();
        String sql = "SELECT id, title AS nom_prestation, description, price AS cout_unitaire, is_available AS disponibilite, is_medical FROM service";
        logger.debug("Exécution de la requête pour getAllPrestations: {}", sql);

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String prestationIdStr = String.valueOf(rs.getInt("id"));
                String typePrestation = rs.getBoolean("is_medical") ? "Médical" : "Bien-être/Autre";
                boolean disponibiliteFromDb = rs.getBoolean("disponibilite");

                Prestation prestation = new Prestation(
                        prestationIdStr,
                        rs.getString("nom_prestation"),
                        typePrestation,
                        rs.getString("description"),
                        rs.getDouble("cout_unitaire"),
                        disponibiliteFromDb
                );
                prestations.add(prestation);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des prestations: {}", e.getMessage(), e);
            throw e;
        }
        logger.info("{} prestations chargées au total (disponibles et non disponibles).", prestations.size());
        return prestations;
    }

    private List<Abonnement> getAbonnementsForCompany(String companyId) throws SQLException {
        List<Abonnement> abonnements = new ArrayList<>();
        String sql = "SELECT id, startDate, endDate, price, subscriptionTier FROM contract WHERE company_id = ? AND status = 'active'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(companyId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tierStr = rs.getString("subscriptionTier");
                    SubscriptionTier tier = SubscriptionTier.BASIC;
                    if (tierStr != null) {
                        try {
                            switch (tierStr.toLowerCase()) {
                                case "starter": tier = SubscriptionTier.BASIC; break;
                                case "basic": tier = SubscriptionTier.STANDARD; break;
                                case "premium": tier = SubscriptionTier.PREMIUM; break;
                                case "custom": case "enterprise": tier = SubscriptionTier.ENTERPRISE; break;
                                default:
                                    logger.warn("Valeur de subscriptionTier non reconnue: '{}' pour contrat ID {}. Utilisation de BASIC par défaut.", tierStr, rs.getInt("id"));
                                    break;
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warn("Erreur de parsing pour subscriptionTier: '{}' - {}. Utilisation de BASIC par défaut.", tierStr, e.getMessage());
                        }
                    }
                    abonnements.add(new Abonnement(
                        String.valueOf(rs.getInt("id")),
                        tier,
                        rs.getDate("startDate"),
                        rs.getDate("endDate"),
                        rs.getDouble("price")
                    ));
                }
            }
        }
        return abonnements;
    }

    private List<Devis> getDevisForCompany(String companyId) throws SQLException {
        List<Devis> devisList = new ArrayList<>();
        String sql = "SELECT id, quote_number, created_at, estimated_annual_total, status FROM quote WHERE company_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(companyId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                     String statusStr = rs.getString("status");
                     QuoteStatus status = QuoteStatus.PENDING;
                     if (statusStr != null) {
                         try {
                            switch(statusStr.toLowerCase()) {
                                case "pending": status = QuoteStatus.PENDING; break;
                                case "sent": status = QuoteStatus.SENT; break;
                                case "accepted": case "contracted": status = QuoteStatus.ACCEPTED; break;
                                case "rejected": case "expired": status = QuoteStatus.REJECTED; break;
                                case "approved": status = QuoteStatus.APPROVED; break;
                                default:
                                    logger.warn("Valeur de status de devis non reconnue: '{}' pour devis ID {}. Utilisation de PENDING par défaut.", statusStr, rs.getInt("id"));
                                    break;
                            }
                         } catch (IllegalArgumentException e) {
                            logger.warn("Erreur de parsing pour status devis: '{}' - {}. Utilisation de PENDING par défaut.", statusStr, e.getMessage());
                         }
                     }
                    devisList.add(new Devis(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("quote_number"),
                        rs.getTimestamp("created_at"),
                        rs.getDouble("estimated_annual_total"),
                        status
                    ));
                }
            }
        }
        return devisList;
    }

    private List<Facture> getFacturesForCompany(String companyId) throws SQLException {
        List<Facture> factures = new ArrayList<>();
        String sql = "SELECT id, invoice_number, invoice_date, total_amount, status FROM invoice WHERE company_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(companyId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String statusStr = rs.getString("status");
                    InvoiceStatus status = InvoiceStatus.PENDING;
                     if (statusStr != null) {
                         try {
                            switch(statusStr.toLowerCase()) {
                                case "pending": status = InvoiceStatus.PENDING; break;
                                case "payed": case "paid": status = InvoiceStatus.PAID; break;
                                case "overdue": status = InvoiceStatus.OVERDUE; break;
                                case "cancelled": status = InvoiceStatus.CANCELLED; break;
                                default:
                                     logger.warn("Valeur de status de facture non reconnue: '{}' pour facture ID {}. Utilisation de PENDING par défaut.", statusStr, rs.getInt("id"));
                                     break;
                            }
                         } catch (IllegalArgumentException e) {
                            logger.warn("Erreur de parsing pour status facture: '{}' - {}. Utilisation de PENDING par défaut.", statusStr, e.getMessage());
                         }
                     }
                    factures.add(new Facture(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("invoice_number"),
                        rs.getDate("invoice_date"),
                        rs.getDouble("total_amount"),
                        status
                    ));
                }
            }
        }
        return factures;
    }

    private List<Reservation> getReservationsForEvent(String eventId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT b.id, b.employee_id, b.booking_date, e.company_id " +
                     "FROM booking b " +
                     "JOIN employee e ON b.employee_id = e.id " +
                     "WHERE b.event_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(eventId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int nombreParticipants = 1;
                    reservations.add(new Reservation(
                        String.valueOf(rs.getInt("id")),
                        eventId,
                        String.valueOf(rs.getInt("company_id")),
                        rs.getTimestamp("booking_date"),
                        nombreParticipants
                    ));
                }
            }
        }
        return reservations;
    }

    public Map<String, Long> getClientCountBySubscriptionTier() throws SQLException {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT subscriptionTier, COUNT(DISTINCT company_id) as count FROM contract WHERE status = 'active' GROUP BY subscriptionTier";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String tier = rs.getString("subscriptionTier");
                if (tier == null || tier.isEmpty()) {
                    tier = "Non défini";
                }
                counts.put(tier, rs.getLong("count"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la répartition des clients par formule d'abonnement: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    public Map<String, Double> getClientTotalContractValue() throws SQLException {
        Map<String, Double> clientValues = new HashMap<>();
        String sql = "SELECT c.name, SUM(ct.price) as total_value " +
                     "FROM company c JOIN contract ct ON c.id = ct.company_id " +
                     "WHERE ct.status = 'active' " +
                     "GROUP BY c.id, c.name";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                clientValues.put(rs.getString("name"), rs.getDouble("total_value"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du CA par client (contrats actifs): {}", e.getMessage(), e);
            throw e;
        }
        return clientValues;
    }

    public Map<String, Long> getClientCountBySize() throws SQLException {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT size, COUNT(*) as count FROM company WHERE status = 'ACTIVE' GROUP BY size";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String size = rs.getString("size");
                 if (size == null || size.isEmpty()) {
                    size = "Non défini";
                }
                counts.put(size, rs.getLong("count"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la répartition des clients par taille: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    public Map<String, Long> getClientCountByIndustry(int limit) throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        String sql = "SELECT industry, COUNT(*) as count FROM company WHERE status = 'ACTIVE' GROUP BY industry ORDER BY count DESC" + (limit > 0 ? " LIMIT " + limit : "");
        long autresCount = 0;
        int currentCount = 0;

        try (PreparedStatement pstmtTotal = connection.prepareStatement("SELECT COUNT(DISTINCT industry) FROM company WHERE status = 'ACTIVE'");
             ResultSet rsTotal = pstmtTotal.executeQuery()) {
            if (rsTotal.next() && rsTotal.getInt(1) > limit && limit > 0) {
                 String sqlAll = "SELECT industry, COUNT(*) as count FROM company WHERE status = 'ACTIVE' GROUP BY industry ORDER BY count DESC";
                 try (PreparedStatement pstmtAll = connection.prepareStatement(sqlAll); ResultSet rsAll = pstmtAll.executeQuery()){
                    while(rsAll.next()){
                        if(currentCount < limit) {
                            String industry = rsAll.getString("industry");
                            if (industry == null || industry.isEmpty()) {
                                industry = "Non défini";
                            }
                            counts.put(industry, rsAll.getLong("count"));
                        } else {
                            autresCount += rsAll.getLong("count");
                        }
                        currentCount++;
                    }
                    if (autresCount > 0) {
                        counts.put("Autres", autresCount);
                    }
                 }
            } else {
                 try (PreparedStatement pstmt = connection.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                     while(rs.next()){
                        String industry = rs.getString("industry");
                        if (industry == null || industry.isEmpty()) {
                            industry = "Non défini";
                        }
                        counts.put(industry, rs.getLong("count"));
                     }
                 }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la répartition des clients par secteur d'activité: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }


    public Map<String, Double> getTopNClientsByTotalPaid(int n) throws SQLException {
        Map<String, Double> topClients = new LinkedHashMap<>();
        String sql = "SELECT c.name, SUM(i.total_amount) as total_paid " +
                     "FROM company c JOIN invoice i ON c.id = i.company_id " +
                     "WHERE i.status = 'payed' " +
                     "GROUP BY c.id, c.name ORDER BY total_paid DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, n);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topClients.put(rs.getString("name"), rs.getDouble("total_paid"));
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du Top {} des clients par montant payé: {}", n, e.getMessage(), e);
            throw e;
        }
        return topClients;
    }

    public Map<String, Long> getEventCountByMonth() throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(start_date, '%Y-%m') as month, COUNT(*) as count " +
                     "FROM event WHERE is_active = 1 " +
                     "GROUP BY month ORDER BY month ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("month"), rs.getLong("count"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de la fréquence des événements par mois: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    public List<Integer> getEventCapacities() throws SQLException {
        List<Integer> capacities = new ArrayList<>();
        String sql = "SELECT capacity FROM event WHERE is_active = 1 AND capacity IS NOT NULL AND capacity > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                capacities.add(rs.getInt("capacity"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des capacités des événements: {}", e.getMessage(), e);
            throw e;
        }
        return capacities;
    }

    public Map<String, Long> getEventStatusCounts() throws SQLException {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT is_active, COUNT(*) as count FROM event GROUP BY is_active";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String status = rs.getBoolean("is_active") ? "Actifs" : "Inactifs";
                counts.put(status, rs.getLong("count"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du statut des événements: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    public Map<String, Integer> getTopNEventsByBooking(int n) throws SQLException {
        Map<String, Integer> topEvents = new LinkedHashMap<>();
        String sql = "SELECT e.name, COUNT(b.id) as num_reservations " +
                     "FROM event e JOIN booking b ON e.id = b.event_id " +
                     "WHERE e.is_active = 1 " +
                     "GROUP BY e.id, e.name ORDER BY num_reservations DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, n);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topEvents.put(rs.getString("name"), rs.getInt("num_reservations"));
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du Top {} des événements par réservation: {}", n, e.getMessage(), e);
            throw e;
        }
        return topEvents;
    }

    public Map<String, Long> getServiceCountByType() throws SQLException {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT is_medical, COUNT(*) as count FROM service GROUP BY is_medical";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String type = rs.getBoolean("is_medical") ? "Médical" : "Non-Médical";
                counts.put(type, rs.getLong("count"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des prestations par type: {}", e.getMessage(), e);
            throw e;
        }
        return counts;
    }

    public List<Double> getServicePrices() throws SQLException {
        List<Double> prices = new ArrayList<>();
        String sql = "SELECT price FROM service WHERE price IS NOT NULL AND price >= 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                prices.add(rs.getDouble("price"));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des prix des prestations: {}", e.getMessage(), e);
            throw e;
        }
        return prices;
    }
}