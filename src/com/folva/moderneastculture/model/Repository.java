package com.folva.moderneastculture.model;

import javafx.beans.property.SimpleBooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

@SuppressWarnings({"RedundantIfStatement", "UnnecessaryLocalVariable"})
public class Repository {

    private static final Logger logger = LogManager.getLogger(Repository.class);
    private static final Preferences prefs = Preferences.userRoot().node("com/folva/moderneastculture/config");
    private static final String CONNECTION_URL = "jdbc:sqlite:modern_east_culture.db3";
    private static final String PREFS_ADMIN_LOGIN = "superadmin_login";
    private static final String PREFS_ADMIN_PASSWORD = "superadmin_password";
    private static final String DEFAULT_ADMIN_LOGIN = "somebody@oncetold.me";
    private static final String DEFAULT_ADMIN_PASSWORD = "simplePassword";

    public static final Repository instance;
    public static final int FIRST_ANIME_PREMIERE_YEAR = 1958;
    public static final int FIRST_MANGA_PREMIERE_YEAR = 1900;
    public static final ResourceBundle namesBundle;
    public static final SimpleBooleanProperty adminIsAuthorizedProperty;

    private static String ADMIN_LOGIN;
    private static String ADMIN_PASSWORD;

    private Connection connection;

    static {
        instance = new Repository();

        ADMIN_LOGIN = prefs.get(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        ADMIN_PASSWORD = prefs.get(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        if (ADMIN_LOGIN.equalsIgnoreCase(DEFAULT_ADMIN_LOGIN))
            prefs.put(PREFS_ADMIN_LOGIN, DEFAULT_ADMIN_LOGIN);
        if (ADMIN_PASSWORD.equalsIgnoreCase(DEFAULT_ADMIN_PASSWORD))
            prefs.put(PREFS_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD);

        namesBundle = ResourceBundle.getBundle("ui_names", Locale.getDefault(), new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle
                    (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                    throws IllegalAccessException, IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                ResourceBundle bundle = null;
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    try {
                        // Only this line is changed to make it to read properties files as UTF-8.
                        bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    } finally {
                        stream.close();
                    }
                }
                return bundle;
            }
        });

        adminIsAuthorizedProperty = new SimpleBooleanProperty(false);
    }

    private Repository() {
    }

    public void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(CONNECTION_URL);
            logger.info("Database connection is successful.");
        } catch (SQLException e) {
            logger.error("Database connection was refused: ", e);
        }
    }

    public void disconnectFromDatabase() {
        try {
            if (connection != null)
                connection.close();
            logger.info("Database was successfully disconnected.");
        } catch (SQLException e) {
            logger.error("Database wasn't disconnected: ", e);
        }
    }

    public boolean loginIsCorrect(String login) {
        if (ADMIN_LOGIN.equals(login)) return true;

        return false;
    }

    public boolean passwordIsCorrect(String password) {
        if (ADMIN_PASSWORD.equals(password)) return true;

        return false;
    }

    public void updateAdminLogin(String newLogin) {
        if (loginIsValid(newLogin)) {
            ADMIN_LOGIN = newLogin;
            prefs.put(PREFS_ADMIN_LOGIN, ADMIN_LOGIN);
        }
    }

    public void updateAdminPassword(String newPassword) {
        if (passwordIsValid(newPassword)) {
            ADMIN_PASSWORD = newPassword;
            prefs.put(PREFS_ADMIN_PASSWORD, ADMIN_PASSWORD);
        }
    }

    public boolean loginIsValid(String login) {
        if (login == null || !login.matches("[a-zA-Z0-9]{3,20}")) return false;

        return true;
    }

    public boolean passwordIsValid(String password) {
        if (password == null || !password.matches("[a-zA-Z0-9]{3,20}")) return false;

        return true;
    }

    public String getNamesBundleValue(String key) {
        if (key == null) return "";

        return namesBundle.getString(key);
    }
}
