package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/** SkyKings-Crates - Phase 4 Crate-/Voucher-System. */
public class SkyKingsCrates extends JavaPlugin {

    private CrateRegistry crateRegistry;
    private CrateItemCodec crateItemCodec;

    @Override
    public void onEnable() {
        SkyKingsCoreAPI core = resolveCoreApi();
        if (core == null) {
            getLogger().severe("SkyKingsCoreAPI wurde nicht gefunden - SkyKings-Crates wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.crateRegistry = new CrateRegistry(this);
        this.crateItemCodec = new CrateItemCodec();
        getServer().getPluginManager().registerEvents(
                new CrateInteractionListener(crateRegistry, crateItemCodec, core), this);

        getLogger().info("SkyKings-Crates (Phase 4 Head-Crates + Preview/Open + Reward Tables + EV) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Crates deaktiviert.");
    }

    private SkyKingsCoreAPI resolveCoreApi() {
        try {
            RegisteredServiceProvider<SkyKingsCoreAPI> registration =
                    getServer().getServicesManager().getRegistration(SkyKingsCoreAPI.class);
            return registration == null ? null : registration.getProvider();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Konnte SkyKingsCoreAPI nicht aufloesen.", t);
            return null;
        }
    }

    public CrateRegistry getCrateRegistry() {
        return crateRegistry;
    }

    public CrateItemCodec getCrateItemCodec() {
        return crateItemCodec;
    }
}
