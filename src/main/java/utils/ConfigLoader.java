package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A utility class for loading configuration properties from a properties file.
 * This class follows the Singleton pattern to ensure only one instance is created.
 * It provides methods to access admin credentials and other configuration settings.
 */
public class ConfigLoader {
    private static final String DEFAULT_CONFIG_PATH = "config.properties";
    private final Properties properties;
    private final String configPath;

    // Private constructor to prevent instantiation
    private ConfigLoader(String configPath) {
        this.configPath = configPath != null && !configPath.isEmpty() ? configPath : DEFAULT_CONFIG_PATH;
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Gets the singleton instance of ConfigLoader with the default configuration path.
     *
     * @return The singleton instance of ConfigLoader.
     */
    public static ConfigLoader getInstance() {
        return getInstance(null);
    }

    /**
     * Gets the singleton instance of ConfigLoader with a custom configuration path.
     *
     * @param configPath The path to the configuration file. If null or empty, the default path is used.
     * @return The singleton instance of ConfigLoader.
     */
    public static ConfigLoader getInstance(String configPath) {
        return SingletonHolder.getInstance(configPath);
    }

    // Initialization-on-demand holder idiom for thread-safe lazy initialization
    private static class SingletonHolder {
        private static ConfigLoader getInstance(String configPath) {
            return new ConfigLoader(configPath);
        }
    }

    /**
     * Loads the properties from the specified configuration file.
     *
     * @throws ConfigLoadException If the configuration file cannot be found or loaded.
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (input == null) {
                throw new ConfigLoadException("Configuration file not found: " + configPath);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load configuration file: " + configPath, e);
        }
    }

    /**
     * Reloads the properties from the configuration file.
     *
     * @throws ConfigLoadException If the configuration file cannot be reloaded.
     */
    public void reload() {
        properties.clear();
        loadProperties();
    }

    /**
     * Gets the admin email from the configuration.
     *
     * @return The admin email.
     * @throws ConfigLoadException If the admin email property is not found.
     */
    public String getAdminEmail() {
        return getRequiredProperty("admin.email");
    }

    /**
     * Gets the admin password from the configuration.
     *
     * @return The admin password.
     * @throws ConfigLoadException If the admin password property is not found.
     */
    public String getAdminPassword() {
        return getRequiredProperty("admin.password");
    }

    /**
     * Gets the admin nickname from the configuration.
     *
     * @return The admin nickname.
     * @throws ConfigLoadException If the admin nickname property is not found.
     */
    public String getAdminNickname() {
        return getRequiredProperty("admin.nickName");
    }

    /**
     * Gets the data directory path from the configuration.
     *
     * @return The data directory path.
     * @throws ConfigLoadException If the data directory property is not found.
     */
    public String getDataDirectory() {
        return getRequiredProperty("data.directory");
    }

    /**
     * Gets the maximum file size from the configuration.
     *
     * @return The maximum file size in bytes.
     * @throws ConfigLoadException If the max file size property is not found or is invalid.
     */
    public long getMaxFileSize() {
        String maxFileSizeStr = getRequiredProperty("max.file.size");
        try {
            return Long.parseLong(maxFileSizeStr);
        } catch (NumberFormatException e) {
            throw new ConfigLoadException("Invalid max.file.size value: " + maxFileSizeStr + ". It must be a valid long integer.", e);
        }
    }

    /**
     * Retrieves a required property from the configuration.
     *
     * @param key The key of the property to retrieve.
     * @return The value of the property.
     * @throws ConfigLoadException If the property is not found or is empty.
     */
    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigLoadException("Required property '" + key + "' not found or is empty in " + configPath);
        }
        return value;
    }
}

/**
 * Custom exception for configuration loading errors.
 */
class ConfigLoadException extends RuntimeException {
    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}