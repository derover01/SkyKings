package net.skykings.admin.discord;

import net.skykings.core.discord.DiscordNotifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Sichere ausgehende Discord-Bot-Bridge ohne Token im Repository.
 * Der Bot-Token wird ausschliesslich aus SKYKINGS_DISCORD_BOT_TOKEN gelesen.
 */
public final class DiscordBridge implements DiscordNotifier {

    private final JavaPlugin plugin;
    private final File configFile;
    private final ExecutorService executor;
    private volatile YamlConfiguration config;

    public DiscordBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "discord.yml");
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SkyKings-Discord-Bridge");
            thread.setDaemon(true);
            return thread;
        });
        load();
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.config = YamlConfiguration.loadConfiguration(configFile);
        boolean changed = false;
        if (!config.contains("enabled")) { config.set("enabled", false); changed = true; }
        if (!config.contains("channels.staff")) { config.set("channels.staff", ""); changed = true; }
        if (!config.contains("channels.audit")) { config.set("channels.audit", ""); changed = true; }
        if (!config.contains("channels.events")) { config.set("channels.events", ""); changed = true; }
        if (!config.contains("channels.status")) { config.set("channels.status", ""); changed = true; }
        if (changed) {
            try { config.save(configFile); }
            catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "discord.yml konnte nicht gespeichert werden", ex); }
        }
    }

    @Override
    public boolean isEnabled() {
        return config.getBoolean("enabled", false) && token() != null;
    }

    public String configuredChannel(String key) {
        return config.getString("channels." + normalize(key), "").trim();
    }

    @Override
    public boolean isConfigured(String key) {
        return isEnabled() && isNumericId(configuredChannel(key));
    }

    @Override
    public void send(String channelKey, String message) {
        String channelId = configuredChannel(channelKey);
        if (!isEnabled() || !isNumericId(channelId) || message == null || message.trim().isEmpty()) return;
        String safe = message.length() > 1900 ? message.substring(0, 1900) : message;
        executor.submit(() -> postMessage(channelId, safe));
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void postMessage(String channelId, String message) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://discord.com/api/v10/channels/" + channelId + "/messages");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bot " + token());
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("User-Agent", "SkyKings/2.0");

            byte[] body = ("{\"content\":\"" + jsonEscape(message) + "\"}").getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("Discord API Antwort " + code + ": " + readBody(connection));
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Discord Nachricht konnte nicht gesendet werden", ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readBody(HttpURLConnection connection) {
        try {
            InputStream input = connection.getErrorStream();
            if (input == null) return "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && out.length() < 1000) out.append(line);
            return out.toString();
        } catch (Exception ignored) { return ""; }
    }

    private String token() {
        String value = System.getenv("SKYKINGS_DISCORD_BOT_TOKEN");
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean isNumericId(String raw) {
        if (raw == null || raw.length() < 10 || raw.length() > 24) return false;
        for (int i = 0; i < raw.length(); i++) if (!Character.isDigit(raw.charAt(i))) return false;
        return true;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String jsonEscape(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(' ');
                    else out.append(c);
            }
        }
        return out.toString();
    }
}
