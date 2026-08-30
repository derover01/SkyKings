package net.skykings.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class ConfigServiceImpl implements ConfigService {

    private final JavaPlugin plugin;

    public ConfigServiceImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    @Override
    public boolean isDebug() {
        return config().getBoolean("debug", false);
    }

    @Override
    public StorageType getStorageType() {
        String raw = config().getString("storage.type", "SQLITE");
        try {
            return StorageType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unbekannter storage.type '" + raw + "' in config.yml, verwende Fallback SQLITE.");
            return StorageType.SQLITE;
        }
    }

    @Override
    public String getSqliteFileName() {
        return config().getString("storage.sqlite.file", "skykings-core.db");
    }

    @Override
    public String getMysqlHost() {
        return config().getString("storage.mysql.host", "localhost");
    }

    @Override
    public int getMysqlPort() {
        return config().getInt("storage.mysql.port", 3306);
    }

    @Override
    public String getMysqlDatabase() {
        return config().getString("storage.mysql.database", "skykings");
    }

    @Override
    public String getMysqlUsername() {
        return config().getString("storage.mysql.username", "skykings");
    }

    @Override
    public String getMysqlPassword() {
        return config().getString("storage.mysql.password", "");
    }

    @Override
    public boolean isMysqlUseSsl() {
        return config().getBoolean("storage.mysql.useSSL", false);
    }
}
