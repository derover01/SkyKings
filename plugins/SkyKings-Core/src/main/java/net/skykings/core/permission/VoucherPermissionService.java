package net.skykings.core.permission;

import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Gemeinsame Source of Truth fuer /rechte und spaetere Permission-Gutscheine. */
public final class VoucherPermissionService {

    public enum GrantStatus {
        GRANTED,
        UNKNOWN_PERMISSION,
        BRIDGE_UNAVAILABLE
    }

    private final PermissionBridge permissionBridge;
    private final LoggingService loggingService;
    private final List<VoucherPermission> permissions;

    public VoucherPermissionService(JavaPlugin plugin, PermissionBridge permissionBridge, LoggingService loggingService) {
        this.permissionBridge = permissionBridge;
        this.loggingService = loggingService;
        this.permissions = Collections.unmodifiableList(load(plugin));
    }

    public Collection<VoucherPermission> getAll() {
        return permissions;
    }

    public VoucherPermission find(String input) {
        for (VoucherPermission permission : permissions) {
            if (permission.matches(input)) {
                return permission;
            }
        }
        return null;
    }

    public GrantStatus grant(UUID uuid, String input, String actor) {
        VoucherPermission permission = find(input);
        if (permission == null) {
            return GrantStatus.UNKNOWN_PERMISSION;
        }
        if (!permissionBridge.isAvailable()) {
            return GrantStatus.BRIDGE_UNAVAILABLE;
        }
        permissionBridge.grantPermission(uuid, permission.getNode());
        loggingService.log(new AuditEvent(AuditEventType.PERMISSION_GRANT, uuid, actor, null,
                "voucherPermission=" + permission.getId() + ", node=" + permission.getNode()));
        return GrantStatus.GRANTED;
    }

    private List<VoucherPermission> load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "voucher-permissions.yml");
        if (!file.exists()) {
            plugin.saveResource("voucher-permissions.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("permissions");
        if (root == null) {
            return Collections.emptyList();
        }
        List<VoucherPermission> loaded = new ArrayList<VoucherPermission>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            String node = section.getString("node", "").trim().toLowerCase(Locale.ROOT);
            if (node.isEmpty()) continue;
            String display = ChatColor.translateAlternateColorCodes('&',
                    section.getString("display-name", id));
            loaded.add(new VoucherPermission(id.toLowerCase(Locale.ROOT), node, display,
                    new java.util.HashSet<String>(section.getStringList("aliases"))));
        }
        return loaded;
    }
}
