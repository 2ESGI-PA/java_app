package com.businesscare.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String PROPERTIES_FILE = "database.properties"; 

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.error("Désolé, impossible de trouver le fichier {}", PROPERTIES_FILE);
                
            } else {
                Properties props = new Properties();
                props.load(input);
                dbUrl = props.getProperty("db.url");
                dbUser = props.getProperty("db.user");
                dbPassword = props.getProperty("db.password");
                logger.info("Configuration de la base de données chargée depuis {}", PROPERTIES_FILE);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du chargement de la configuration de la base de données depuis {}", PROPERTIES_FILE, e);
            
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("La configuration de la base de données n'a pas été correctement chargée. Vérifiez le fichier " + PROPERTIES_FILE);
        }
        
        
        
        
        
        
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}