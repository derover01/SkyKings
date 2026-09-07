package net.skykings.combat.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuestServiceSettlementTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void completedDuelRewardPersistsClaimAndDoesNotPayTwiceAfterReload() throws Exception {
        Fixture fixture = fixture(true);

        try (MockedStatic<SkyKingsCurrencyItems> currency = mockStatic(SkyKingsCurrencyItems.class)) {
            ItemStack starReward = starReward(2);
            currency.when(() -> SkyKingsCurrencyItems.star(2)).thenReturn(starReward);

            fixture.questService.recordDuelWin(fixture.player);

            assertTrue(fixture.questService.claimed(fixture.uuid, "daily.claimed-duels"));
            verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(125_000L), eq("QUEST_REWARD"), anyString());
            verify(fixture.economy, times(1)).persistNow(fixture.uuid);
            verify(fixture.inventory, times(1)).addItem(any(ItemStack[].class));

            QuestService reloaded = new QuestService(fixture.combatPlugin, fixture.economy);
            reloaded.recordDuelWin(fixture.player);

            verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(125_000L), eq("QUEST_REWARD"), anyString());
            verify(fixture.economy, times(1)).persistNow(fixture.uuid);
            verify(fixture.inventory, times(1)).addItem(any(ItemStack[].class));
        }
    }

    @Test
    public void failedCoinCommitKeepsClaimReservedAndSettlementPending() throws Exception {
        Fixture fixture = fixture(false);

        try (MockedStatic<SkyKingsCurrencyItems> currency = mockStatic(SkyKingsCurrencyItems.class)) {
            ItemStack starReward = starReward(2);
            currency.when(() -> SkyKingsCurrencyItems.star(2)).thenReturn(starReward);

            fixture.questService.recordDuelWin(fixture.player);

            assertTrue(fixture.questService.claimed(fixture.uuid, "daily.claimed-duels"));
            assertTrue(fixture.journal.hasPendingFor(fixture.uuid));
            verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(125_000L), eq("QUEST_REWARD"), anyString());
            verify(fixture.inventory, never()).addItem(any(ItemStack[].class));

            QuestService reloaded = new QuestService(fixture.combatPlugin, fixture.economy);
            reloaded.recordDuelWin(fixture.player);

            verify(fixture.economy, times(1)).deposit(eq(fixture.uuid), eq(125_000L), eq("QUEST_REWARD"), anyString());
            verify(fixture.inventory, never()).addItem(any(ItemStack[].class));
        }
    }

    private Fixture fixture(boolean persistCoins) throws Exception {
        File coreFolder = temporaryFolder.newFolder("core-" + UUID.randomUUID());
        File combatFolder = temporaryFolder.newFolder("combat-" + UUID.randomUUID());
        Logger logger = Logger.getLogger("QuestServiceSettlementTest");

        JavaPlugin corePlugin = mock(JavaPlugin.class);
        when(corePlugin.getDataFolder()).thenReturn(coreFolder);
        when(corePlugin.getLogger()).thenReturn(logger);
        GameplaySettlementJournal journal = new GameplaySettlementJournal(corePlugin);

        JavaPlugin combatPlugin = mock(JavaPlugin.class);
        when(combatPlugin.getDataFolder()).thenReturn(combatFolder);
        when(combatPlugin.getLogger()).thenReturn(logger);

        EconomyService economy = mock(EconomyService.class);
        when(economy.canDeposit(any(UUID.class), anyLong())).thenReturn(true);
        when(economy.persistNow(any(UUID.class))).thenReturn(persistCoins);

        UUID uuid = UUID.randomUUID();
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(anyInt())).thenReturn(null);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<Integer, ItemStack>());

        new SeasonProgressService(combatPlugin);
        QuestService questService = new QuestService(combatPlugin, economy);
        return new Fixture(uuid, player, inventory, economy, combatPlugin, journal, questService);
    }

    private ItemStack starReward(int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(amount);
        when(item.getMaxStackSize()).thenReturn(64);
        return item;
    }

    private static final class Fixture {
        private final UUID uuid;
        private final org.bukkit.entity.Player player;
        private final PlayerInventory inventory;
        private final EconomyService economy;
        private final JavaPlugin combatPlugin;
        private final GameplaySettlementJournal journal;
        private final QuestService questService;

        private Fixture(UUID uuid, org.bukkit.entity.Player player, PlayerInventory inventory,
                        EconomyService economy, JavaPlugin combatPlugin,
                        GameplaySettlementJournal journal, QuestService questService) {
            this.uuid = uuid;
            this.player = player;
            this.inventory = inventory;
            this.economy = economy;
            this.combatPlugin = combatPlugin;
            this.journal = journal;
            this.questService = questService;
        }
    }
}
