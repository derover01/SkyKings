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
    private VoucherRedemptionStore voucherStore;
    private IssuedItemStore issuedItemStore;

    @Override
    public void onEnable() {
        SkyKingsCoreAPI core = resolveCoreApi();
        if (core == null) {
            getLogger().severe("SkyKingsCoreAPI wurde nicht gefunden - SkyKings-Crates wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.crateRegistry = new CrateRegistry(this);
        // Muss vor allen Codecs entstehen: Default-Codecs binden sich im laufenden Plugin an
        // dieses Registry und akzeptieren danach nur serverseitig ausgegebene Serials.
        this.issuedItemStore = new IssuedItemStore(new File(getDataFolder(), "issued-items.txt"), getLogger());
        this.crateItemCodec = new CrateItemCodec();
        this.redemptionStore = new CrateRedemptionStore(new File(getDataFolder(), "redeemed-crates.txt"), getLogger());
        this.voucherStore = new VoucherRedemptionStore(new File(getDataFolder(), "redeemed-vouchers.txt"), getLogger());
        redemptionStore.initialize().thenRun(() -> getLogger().info("Crate Anti-Dupe Store bereit."));
        voucherStore.initialize().thenRun(() -> getLogger().info("Voucher Anti-Dupe Store bereit."));

        getServer().getPluginManager().registerEvents(
                new CrateInteractionListener(this, crateRegistry, crateItemCodec, redemptionStore, core), this);

        VoucherItemCodec voucherCodec = new VoucherItemCodec();
        getServer().getPluginManager().registerEvents(
                new VoucherRedeemListener(this, voucherCodec, voucherStore, core), this);

        PluginCommand crateCommand = getCommand("crate");
        if (crateCommand == null) {
            disableMissing("/crate");
            return;
        }
        CrateCommand crateExecutor = new CrateCommand(crateRegistry, crateItemCodec);
        crateCommand.setExecutor(crateExecutor);
        crateCommand.setTabCompleter(crateExecutor);

        PluginCommand rewardsCommand = getCommand("craterewards");
        if (rewardsCommand == null) {
            disableMissing("/craterewards");
            return;
        }
        CrateRewardsGui rewardsGui = new CrateRewardsGui(core.getGuiManager(), core, crateRegistry, crateItemCodec);
        rewardsCommand.setExecutor(new CrateRewardsCommand(rewardsGui));

        PluginCommand vouchersCommand = getCommand("gutscheine");
        if (vouchersCommand == null) {
            disableMissing("/gutscheine");
            return;
        }
        VoucherAdminGui voucherGui = new VoucherAdminGui(core.getGuiManager(), core, voucherCodec);
        vouchersCommand.setExecutor(new VouchersCommand(voucherGui));

        getLogger().info("SkyKings-Crates (Phase 4 Crates + Issued-Serials + Anti-Dupe + Open-All + CrateRewards + Voucher-System) aktiviert.");
    }

    private void disableMissing(String command) {
        getLogger().severe(command + " fehlt in plugin.yml - SkyKings-Crates wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        if (redemptionStore != null) redemptionStore.shutdown();
        if (voucherStore != null) voucherStore.shutdown();
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
