package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

    public static ConfigLoader getInstance() {
        return getInstance(null);
    }

    public static ConfigLoader getInstance(String configPath) {
        return SingletonHolder.getInstance(configPath);
    }

    // Initialization-on-demand holder idiom for thread-safe lazy initialization
    private static class SingletonHolder {
        private static ConfigLoader getInstance(String configPath) {
            return new ConfigLoader(configPath);
        }
    }

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

    public void reload() {
        properties.clear();
        loadProperties();
    }

    public String getAdminEmail() {
        return getRequiredProperty("admin.email");
    }

    public String getAdminPassword() {
        return getRequiredProperty("admin.password");
    }

    public String getAdminNickname() {
        return getRequiredProperty("admin.nickName");
    }

    public String getDataDirectory() {
        return getRequiredProperty("data.directory");
    }

    public long getMaxFileSize() {
        String maxFileSizeStr = getRequiredProperty("max.file.size");
        try {
            return Long.parseLong(maxFileSizeStr);
        } catch (NumberFormatException e) {
            throw new ConfigLoadException("Invalid max.file.size value: " + maxFileSizeStr + ". It must be a valid long integer.", e);
        }
    }

    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigLoadException("Required property '" + key + "' not found or is empty in " + configPath);
        }
        return value;
    }
}

// Custom exception for configuration loading errors.
class ConfigLoadException extends RuntimeException {
    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}