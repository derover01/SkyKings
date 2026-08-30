package net.skykings.crates;

import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/** SkyKings-Crates - Phase 4 Crate-/Voucher-System. */
public class SkyKingsCrates extends JavaPlugin {

    private CrateRegistry crateRegistry;
    private CrateItemCodec crateItemCodec;
    private CrateRedemptionStore redemptionStore;

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
        this.redemptionStore = new CrateRedemptionStore(new File(getDataFolder(), "redeemed-crates.txt"), getLogger());
        redemptionStore.initialize().thenRun(() -> getLogger().info("Crate Anti-Dupe Store bereit."));

        getServer().getPluginManager().registerEvents(
                new CrateInteractionListener(this, crateRegistry, crateItemCodec, redemptionStore, core), this);

        PluginCommand crateCommand = getCommand("crate");
        if (crateCommand == null) {
            getLogger().severe("/crate fehlt in plugin.yml - SkyKings-Crates wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        CrateCommand crateExecutor = new CrateCommand(crateRegistry, crateItemCodec);
        crateCommand.setExecutor(crateExecutor);
        crateCommand.setTabCompleter(crateExecutor);

        PluginCommand rewardsCommand = getCommand("craterewards");
        if (rewardsCommand == null) {
            getLogger().severe("/craterewards fehlt in plugin.yml - SkyKings-Crates wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        CrateRewardsGui rewardsGui = new CrateRewardsGui(core.getGuiManager(), core, crateRegistry, crateItemCodec);
        rewardsCommand.setExecutor(new CrateRewardsCommand(rewardsGui));

        getLogger().info("SkyKings-Crates (Phase 4 Head-Crates + Preview/Open + Reward Tables + EV + Anti-Dupe + Open-All + CrateRewards) aktiviert.");
    }

    @Override
    public void onDisable() {
        if (redemptionStore != null) redemptionStore.shutdown();
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

    public CrateRegistry getCrateRegistry() { return crateRegistry; }
    public CrateItemCodec getCrateItemCodec() { return crateItemCodec; }
}
