package net.skykings.core.discord;

/**
 * Optionale moduluebergreifende Discord-Ausgabe. Implementierung liegt in SkyKings-Admin,
 * damit Core/Combat/Crates keinen Discord-Bot direkt kennen muessen.
 */
public interface DiscordNotifier {
    boolean isEnabled();
    boolean isConfigured(String channelKey);
    void send(String channelKey, String message);
}
