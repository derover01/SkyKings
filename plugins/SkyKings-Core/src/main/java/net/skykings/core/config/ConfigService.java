package net.skykings.core.config;

/** Zentrale Config-Verwaltung fuer SkyKings-Core (config.yml). */
public interface ConfigService {

    boolean isDebug();

    StorageType getStorageType();

    String getSqliteFileName();

    String getMysqlHost();

    int getMysqlPort();

    String getMysqlDatabase();

    String getMysqlUsername();

    String getMysqlPassword();

    boolean isMysqlUseSsl();
}
