package net.skykings.core.storage.sqlite;

import net.skykings.core.logging.AuditEvent;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.storage.DataStore;
import net.skykings.core.storage.DataStoreException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite-Implementierung von {@link DataStore} fuer die lokale Entwicklung (Phase 1A).
 *
 * <p>Der Spigot-1.8.8-Server-JAR buendelt bereits einen SQLite-JDBC-Treiber
 * (org.xerial:sqlite-jdbc 3.7.2, siehe org/sqlite/JDBC.class im Server-JAR), daher ist
 * fuer diese Klasse KEINE zusaetzliche Maven-Abhaengigkeit noetig - wir sprechen nur
 * gegen die JDK-eigenen java.sql-Schnittstellen. {@link Class#forName} laedt den Treiber
 * defensiv, da JDBC-4-Autodiscovery ueber PluginClassLoader-Hierarchien in alten
 * Bukkit-Versionen nicht immer zuverlaessig funktioniert.
 *
 * <p>Die verwendete Treiberversion (3.7.2, Stand 2011) unterstuetzt noch KEIN
 * {@code INSERT ... ON CONFLICT DO UPDATE} (SQLite-Upsert kam erst mit 3.24+). Deshalb wird
 * bewusst das seit jeher unterstuetzte {@code INSERT OR REPLACE} verwendet.
 *
 * <p>Alle Methoden sind auf der gemeinsamen {@link Connection} synchronisiert, da eine
 * einzelne JDBC-Connection nicht nebenlaeufig von mehreren Threads genutzt werden darf
 * (Aufrufe kommen sowohl vom Haupt-Thread als auch vom SkyKings-Core-DB-Executor).
 */
public final class SQLiteDataStore implements DataStore {

    private static final String CREATE_PLAYER_PROFILES = "CREATE TABLE IF NOT EXISTS player_profiles ("
            + "uuid TEXT PRIMARY KEY, "
            + "last_known_name TEXT NOT NULL, "
            + "rank TEXT NOT NULL, "
            + "coins INTEGER NOT NULL DEFAULT 0, "
            + "netherstars INTEGER NOT NULL DEFAULT 0, "
            + "created_at INTEGER NOT NULL, "
            + "last_seen INTEGER NOT NULL)";

    private static final String CREATE_COOLDOWNS = "CREATE TABLE IF NOT EXISTS cooldowns ("
            + "uuid TEXT NOT NULL, "
            + "cooldown_key TEXT NOT NULL, "
            + "expires_at INTEGER NOT NULL, "
            + "PRIMARY KEY (uuid, cooldown_key))";

    private static final String CREATE_AUDIT_LOG = "CREATE TABLE IF NOT EXISTS audit_log ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "event_timestamp INTEGER NOT NULL, "
            + "event_type TEXT NOT NULL, "
            + "target_uuid TEXT, "
            + "actor TEXT, "
            + "amount INTEGER, "
            + "details TEXT)";

    private final File databaseFile;
    private final Logger logger;
    private Connection connection;

    public SQLiteDataStore(File databaseFile, Logger logger) {
        this.databaseFile = databaseFile;
        this.logger = logger;
    }

    @Override
    public synchronized void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DataStoreException("SQLite-JDBC-Treiber (org.sqlite.JDBC) nicht gefunden. "
                    + "Dieser Treiber wird normalerweise vom Spigot/CraftBukkit-Server-JAR bereitgestellt.", e);
        }

        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new DataStoreException("Konnte Datenverzeichnis fuer SQLite nicht anlegen: " + parent);
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_PLAYER_PROFILES);
                statement.executeUpdate(CREATE_COOLDOWNS);
                statement.executeUpdate(CREATE_AUDIT_LOG);
            }
        } catch (SQLException e) {
            throw new DataStoreException("Konnte SQLite-Datenbank nicht initialisieren: " + databaseFile, e);
        }
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Fehler beim Schliessen der SQLite-Verbindung", e);
        }
    }

    @Override
    public synchronized Optional<PlayerProfile> loadProfile(UUID uuid) {
        String sql = "SELECT last_known_name, rank, coins, netherstars, created_at, last_seen "
                + "FROM player_profiles WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String name = rs.getString("last_known_name");
                Rank rank = parseRank(rs.getString("rank"));
                long coins = rs.getLong("coins");
                long netherstars = rs.getLong("netherstars");
                long createdAt = rs.getLong("created_at");
                long lastSeen = rs.getLong("last_seen");
                return Optional.of(new PlayerProfile(uuid, name, rank, coins, netherstars, createdAt, lastSeen));
            }
        } catch (SQLException e) {
            throw new DataStoreException("Konnte PlayerProfile nicht laden: " + uuid, e);
        }
    }

    @Override
    public synchronized void saveProfile(PlayerProfile profile) {
        String sql = "INSERT OR REPLACE INTO player_profiles "
                + "(uuid, last_known_name, rank, coins, netherstars, created_at, last_seen) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getLastKnownName());
            ps.setString(3, profile.getRank().name());
            ps.setLong(4, profile.getCoins());
            ps.setLong(5, profile.getNetherstars());
            ps.setLong(6, profile.getCreatedAt());
            ps.setLong(7, profile.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Konnte PlayerProfile nicht speichern: " + profile.getUuid(), e);
        }
    }

    @Override
    public synchronized Optional<Long> loadCooldown(UUID uuid, String key) {
        String sql = "SELECT expires_at FROM cooldowns WHERE uuid = ? AND cooldown_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rs.getLong("expires_at"));
            }
        } catch (SQLException e) {
            throw new DataStoreException("Konnte Cooldown nicht laden: " + uuid + "/" + key, e);
        }
    }

    @Override
    public synchronized void saveCooldown(UUID uuid, String key, long expiresAtMillis) {
        String sql = "INSERT OR REPLACE INTO cooldowns (uuid, cooldown_key, expires_at) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.setLong(3, expiresAtMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Konnte Cooldown nicht speichern: " + uuid + "/" + key, e);
        }
    }

    @Override
    public synchronized void deleteCooldown(UUID uuid, String key) {
        String sql = "DELETE FROM cooldowns WHERE uuid = ? AND cooldown_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Konnte Cooldown nicht loeschen: " + uuid + "/" + key, e);
        }
    }

    @Override
    public synchronized void appendAuditEvent(AuditEvent event) {
        String sql = "INSERT INTO audit_log (event_timestamp, event_type, target_uuid, actor, amount, details) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, event.getTimestamp());
            ps.setString(2, event.getType().name());
            ps.setString(3, event.getTargetUuid() != null ? event.getTargetUuid().toString() : null);
            ps.setString(4, event.getActor());
            if (event.getAmount() != null) {
                ps.setLong(5, event.getAmount());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setString(6, event.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Konnte Audit-Event nicht speichern", e);
        }
    }

    private Rank parseRank(String raw) {
        try {
            return Rank.valueOf(raw);
        } catch (IllegalArgumentException e) {
            logger.warning("Unbekannter Rank-Wert '" + raw + "' in der Datenbank, verwende Fallback " + Rank.SPIELER);
            return Rank.SPIELER;
        }
    }
}
