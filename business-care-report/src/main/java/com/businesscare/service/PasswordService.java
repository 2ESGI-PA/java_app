package com.businesscare.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
    private static final String PROPERTIES_FILE_NAME = "auth.properties";
    private static final String HASH_PROPERTY = "password.hash";
    private static final String SALT_PROPERTY = "password.salt";
    private static final String DEFAULT_PASSWORD = "esgi"; 

    private Properties authProps;
    private File propertiesFile;

    public PasswordService() {
        
        
        
        String workingDirectory = System.getProperty("user.dir");
        propertiesFile = new File(workingDirectory, PROPERTIES_FILE_NAME);
        
        authProps = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        if (propertiesFile.exists()) {
            try (InputStream input = new FileInputStream(propertiesFile)) {
                authProps.load(input);
                logger.info("Fichier d'authentification chargé depuis {}", propertiesFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Impossible de charger le fichier d'authentification.", e);
                
            }
        } else {
            logger.warn("Fichier d'authentification introuvable à {}. Un nouveau sera créé avec le mot de passe par défaut.", propertiesFile.getAbsolutePath());
            setDefaultPassword();
        }
        
        if (authProps.getProperty(HASH_PROPERTY) == null || authProps.getProperty(SALT_PROPERTY) == null) {
             logger.warn("Mot de passe ou sel manquant dans le fichier d'authentification. Réinitialisation au mot de passe par défaut.");
             setDefaultPassword();
        }
    }

    private void saveProperties() {
        try (OutputStream output = new FileOutputStream(propertiesFile)) {
            authProps.store(output, "Authentication Properties - NE PAS MODIFIER MANUELLEMENT");
            logger.info("Fichier d'authentification sauvegardé dans {}", propertiesFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Impossible de sauvegarder le fichier d'authentification.", e);
        }
    }

    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Algorithme de hachage SHA-256 non trouvé.", e);
            throw new RuntimeException("Erreur de configuration de la sécurité interne.", e);
        }
    }

    public boolean verifyPassword(String providedPassword) {
        String storedHash = authProps.getProperty(HASH_PROPERTY);
        String storedSaltBase64 = authProps.getProperty(SALT_PROPERTY);

        if (storedHash == null || storedSaltBase64 == null) {
            logger.warn("Aucun mot de passe haché ou sel stocké. Vérification impossible.");
            if (DEFAULT_PASSWORD.equals(providedPassword)){
                 logger.info("Mot de passe par défaut utilisé car aucun mot de passe configuré.");
                 
                 return true; 
            }
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(storedSaltBase64);
        String providedPasswordHash = hashPassword(providedPassword, salt);
        return providedPasswordHash.equals(storedHash);
    }

    public void setPassword(String newPassword) {
        byte[] salt = generateSalt();
        String newHash = hashPassword(newPassword, salt);
        authProps.setProperty(HASH_PROPERTY, newHash);
        authProps.setProperty(SALT_PROPERTY, Base64.getEncoder().encodeToString(salt));
        saveProperties();
        logger.info("Mot de passe mis à jour avec succès.");
    }
    
    public void setDefaultPassword() {
        setPassword(DEFAULT_PASSWORD);
        logger.info("Mot de passe par défaut (\"esgi\") défini.");
    }

    
    public boolean isPasswordSet() {
        return authProps.getProperty(HASH_PROPERTY) != null && authProps.getProperty(SALT_PROPERTY) != null;
    }
}